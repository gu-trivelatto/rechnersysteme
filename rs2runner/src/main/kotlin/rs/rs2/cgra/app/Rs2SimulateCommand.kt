package rs.rs2.cgra.app

import com.beust.jcommander.Parameter
import com.beust.jcommander.Parameters
import com.beust.jcommander.ParametersDelegate
import de.tu_darmstadt.rs.memoryTracer.MemoryTracer
import de.tu_darmstadt.rs.riscv.impl.synthesis.insnPatching.RvKernelPatcher
import de.tu_darmstadt.rs.riscv.simulator.api.IRvSystem
import de.tu_darmstadt.rs.riscv.simulator.impl.builder.rvSystem
import de.tu_darmstadt.rs.riscv.simulator.impl.configuration.configureAllOperationsZeroCycle
import de.tu_darmstadt.rs.riscv.simulator.impl.configuration.configureRv32imfOperationsWithCgraTiming
import de.tu_darmstadt.rs.simulator.api.SimulatorFramework
import de.tu_darmstadt.rs.simulator.api.energy.estimateEnergyUsage
import de.tu_darmstadt.rs.util.kotlin.logging.slf4j
import rs.rs2.cgra.cgraConfigurations.PerformanceFocused
import java.nio.file.Path
import java.nio.file.Paths

@Parameters(commandNames = ["simulate"], commandDescription = "simulate a binary. has options to enable acceleration, choose kernels or")
class Rs2SimulateCommand : BaseRunnerCommand(), Runnable {

    @Parameter(names = ["--profile"], description = "Profile the execution and output the profiles after")
    var profile: Boolean = false

    fun buildSystem(exPath: Path, argsWithoutProg: List<String>): IRvSystem {
        val disasmType = getDisasmType()

        return rvSystem(exPath, disasmType) {

            configureEnvAndStdio(argsWithoutProg)
            configureMemory()
            heapAllocator {
                reserveMemory(RvKernelPatcher.PATCH_ALLOCATOR_ID, RvKernelPatcher.PATCH_REGION_DEFAULT_SIZE)
            }

            if (fast) {
                System.err.println("Warning: --correctnessOnly Mode. Operation and Memory Latencies artificially shortened. Tick Count not realistic!")
                staticMemoryLatency(1)
                core {
                    directCoreFrontEnd()
                    configureAllOperationsZeroCycle()
                }
            } else {
                twoLevelCacheHierarchy {
                    useDragonCoherency {
                        forceCoherencyRequestEvenIfNoSiblings = true
                    }
                }
                core {
                    directCoreFrontEnd()
                    pipelinedScalarDispatcher()
                    configureRv32imfOperationsWithCgraTiming()
                }
            }
            core {
                detectUndefinedReturnAsProgramExit()
                if (profile) {
                    enableLoopProfiling()
                }
            }
            configureCgraIfNeeded(cgraAccalerationOptions, accelerate = cgraAccalerationOptions.accelerateAot)
        }
    }

    override fun run() {
        val (exPath, argsWithoutProg) = collectExPathAndArgs()

        val system = buildSystem(exPath, argsWithoutProg)

        val sim = SimulatorFramework.createSimulator(system, debugOutputDir ?: Paths.get("."))

        configureLogging(sim, system, cgraAccalerationOptions.cgraVcdOutput)

        System.err.println("======== Start Of Simulation ==========")
        val startTime = System.currentTimeMillis()

        try {
            sim.runSimulatorPossiblyWithGdb(system)
        } finally {
            system.memory.tracer?.closeTraceFile()
            system.stdio.flush()

            System.err.println()
            System.err.println("========= End Of Simulation ===========")
            val simTicks = sim.currentTick
            System.err.println("Tick Count: $simTicks")
            val totalEnergy = system.estimateEnergyUsage(simTicks)
            System.err.println("Energy Used: $totalEnergy")
            val stopTime = System.currentTimeMillis()
            val passedTime = (stopTime - startTime).toFloat() / 1000
            System.err.println("Real Time: $passedTime s")

            system.elaborateEnergyUsage(simTicks, debugOutputDir)

            system.printLoopProfilesIfPresent()

            system.printCgraExecutionsIfPresent()

            system.printCgraProfilesIfPresent()
        }
    }

    companion object {
        private val logger = slf4j<Rs2SimulateCommand>()
    }
}
