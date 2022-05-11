package rs.rs2.cgra.app

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.beust.jcommander.ParametersDelegate
import de.tu_darmstadt.rs.nativeSim.components.accelerationManager.INativeAccelerationManager
import de.tu_darmstadt.rs.nativeSim.components.accelerationManager.NativeKernelDescriptor
import de.tu_darmstadt.rs.nativeSim.components.profiling.ILoopProfiler
import de.tu_darmstadt.rs.nativeSim.synthesis.accelerationManager.IAccelerationManagerBuilder
import de.tu_darmstadt.rs.riscv.RvArchInfo
import de.tu_darmstadt.rs.riscv.impl.synthesis.accelerationManager.accelerateMostRelevantFunctions
import de.tu_darmstadt.rs.riscv.impl.synthesis.accelerationManager.collectLoopsToFunctions
import de.tu_darmstadt.rs.riscv.impl.synthesis.insnPatching.RvKernelPatcher
import de.tu_darmstadt.rs.riscv.simulator.api.IRvSystem
import de.tu_darmstadt.rs.riscv.simulator.impl.builder.rvSystem
import de.tu_darmstadt.rs.riscv.simulator.impl.configuration.configureRv32imfOperationsWithCgraTiming
import de.tu_darmstadt.rs.simulator.api.SimulatorFramework
import de.tu_darmstadt.rs.simulator.api.energy.estimateEnergyUsage
import de.tu_darmstadt.rs.util.kotlin.hex
import de.tu_darmstadt.rs.util.kotlin.logging.slf4j
import rs.rs2.cgra.cgraConfigurations.PerformanceFocused
import rs.rs2.cgra.optConfig.blacklistedFromAcceleration
import java.nio.file.Path
import java.nio.file.Paths

@Parameters(commandNames = ["speedup"], commandDescription = "run 2 times, once without acceleration, once with. Report speedup")
class Rs2SpeedupCommand: BaseRunnerCommand(), Runnable {

    @Parameter(names = ["--noAutoAcc"], description = "disables any automatic kernel selection. Only kernels manually mentioned with --kernel will by synthesized")
    var noAutoAcc: Boolean = false

    fun buildSystem(exPath: Path, argsWithoutProg: List<String>, accelerationRun: Boolean, referenceILoopProfiler: ILoopProfiler?): IRvSystem {

        val disasmType = getDisasmType()

        return rvSystem(exPath, disasmType) {

            configureEnvAndStdio(argsWithoutProg)
            configureMemory()
            heapAllocator {
                reserveMemory(RvKernelPatcher.PATCH_ALLOCATOR_ID, RvKernelPatcher.PATCH_REGION_DEFAULT_SIZE)
            }
            twoLevelCacheHierarchy {
                useDragonCoherency {
                    forceCoherencyRequestEvenIfNoSiblings = true
                }
            }
            core {
                directCoreFrontEnd()
                pipelinedScalarDispatcher()
                configureRv32imfOperationsWithCgraTiming()
                detectUndefinedReturnAsProgramExit()
                if (!accelerationRun) {
                    enableLoopProfiling()
                }
            }
            configureCgraIfNeeded(cgraAccalerationOptions, accelerate = accelerationRun, alwaysAttachCgra = true, loopProfiler = referenceILoopProfiler)
        }
    }

    override fun run() {
        check(!cgraAccalerationOptions.cgraVcdOutput) { "--cgraVcd unsupported in speedup-mode!" }
        check(!cgraAccalerationOptions.accelerateAot) { "--aot unsupported in speedup-mode!" }

        val (exPath, argsWithoutProg) = collectExPathAndArgs()

        val refSystem = buildSystem(exPath, argsWithoutProg, accelerationRun = false, referenceILoopProfiler = null)

        val refOutputDir = debugOutputDir?.resolve("reference")
        val refSim = SimulatorFramework.createSimulator(refSystem, refOutputDir ?: Paths.get("."))

        configureLogging(refSim, refSystem, false)

        System.err.println("======== Start Of Reference Simulation ==========")
        val refStartTime = System.currentTimeMillis()

        try {
            refSim.runSimulatorPossiblyWithGdb(refSystem)
        } finally {
            refSystem.memory.tracer?.closeTraceFile()
            refSystem.stdio.flush()

            System.err.println()
            System.err.println("========= End Of Reference Simulation ===========")
            val refTicks = refSim.currentTick
            System.err.println("Tick Count: $refTicks")
            val totalEnergy = refSystem.estimateEnergyUsage(refTicks)
            System.err.println("Energy Used: $totalEnergy")
            val stopTime = System.currentTimeMillis()
            val passedTime = (stopTime - refStartTime).toFloat() / 1000
            System.err.println("Real Time: $passedTime s")

            refSystem.elaborateEnergyUsage(refTicks, refOutputDir)

            refSystem.printLoopProfilesIfPresent()
        }

        System.err.println()
        System.err.println("======== Preparing Acceleration Simulation ==========")
        val refLoopProfiler = refSystem.core.dispatcher.profiler ?: error("Should always have a LoopProfiler")
        val accSystem = buildSystem(exPath, argsWithoutProg, accelerationRun = true, referenceILoopProfiler = refLoopProfiler)

        val accOutputDir = debugOutputDir?.resolve("acceleration")
        val accSim = SimulatorFramework.createSimulator(accSystem, accOutputDir ?: Paths.get("."))

        configureLogging(accSim, accSystem, false)

        System.err.println("======== Start Of Acceleration Simulation ==========")
        val accStartTime = System.currentTimeMillis()

        try {
            accSim.runSimulatorPossiblyWithGdb(accSystem)
        } finally {
            accSystem.memory.tracer?.closeTraceFile()
            accSystem.stdio.flush()

            System.err.println()
            System.err.println("========= End Of Acceleration Simulation ===========")
            val accTicks = accSim.currentTick
            System.err.println("Tick Count: $accTicks")
            val totalEnergy = accSystem.estimateEnergyUsage(accTicks)
            System.err.println("Energy Used: $totalEnergy")
            val accStopTime = System.currentTimeMillis()
            val accPassedTime = (accStopTime - accStartTime).toFloat() / 1000
            System.err.println("Real Time: $accPassedTime s")
            System.err.println()
            val speedup = refSim.currentTick.toFloat() / accTicks
            System.err.println("Achieved Whole-Program-Speedup: $speedup")

            accSystem.elaborateEnergyUsage(accTicks, accOutputDir)

            accSystem.printCgraExecutionsIfPresent()

            accSystem.printCgraProfilesIfPresent()
        }

    }

    override fun configureAcceleration(mgmt: IAccelerationManagerBuilder<RvArchInfo>, manuallySelectedKernels: Collection<NativeKernelDescriptor>, loopProfiler: ILoopProfiler?) {
        super.configureAcceleration(mgmt, manuallySelectedKernels, loopProfiler)

        check(loopProfiler != null) { "require loopProfiler!" }


        if (!noAutoAcc) {
            mgmt.aotAcceleration {
                autoAccelerate(loopProfiler)
            }
        }
    }

    companion object {
        internal val logger = slf4j<Rs2SpeedupCommand>()
    }

}

fun INativeAccelerationManager.autoAccelerate(loopProfiler: ILoopProfiler) {
    val functions = collectLoopsToFunctions(loopProfiler)
    val candidates = functions.asSequence()
        .filter { it.profiledTicks > 10000 }
        .filter { it.id !in blacklistedFromAcceleration }
        .onEach {
            Rs2SpeedupCommand.logger.info("Picking {}:{} for acceleration. Saw {} ticks in loops", it.entryAddr.hex(), it.id, it.profiledTicks)
        }
    val success = accelerateMostRelevantFunctions(3, candidates)

    if (!success) {
        Rs2SpeedupCommand.logger.error("Ran out of kernel-candidates. None of the candidates was eligible or synthesizable: {}", functions)
    }
}