package rs.rs2.cgra

import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider
import de.tu_darmstadt.rs.cgra.synthesis.kernel.WriteLocCompression
import de.tu_darmstadt.rs.cgra.synthesis.testing.enableScarAssertions
import de.tu_darmstadt.rs.nativeSim.components.profiling.ILoopProfiler
import de.tu_darmstadt.rs.riscv.impl.synthesis.builder.cgraAcceleration
import de.tu_darmstadt.rs.riscv.impl.synthesis.insnPatching.RvKernelPatcher
import de.tu_darmstadt.rs.riscv.simulator.api.IRvSystem
import de.tu_darmstadt.rs.riscv.simulator.impl.builder.rvSystem
import de.tu_darmstadt.rs.riscv.simulator.impl.configuration.configureRv32imfOperationsWithCgraTiming
import de.tu_darmstadt.rs.simulator.api.SimulatorFramework
import de.tu_darmstadt.rs.simulator.api.energy.estimateEnergyUsage
import io.kotest.matchers.collections.shouldEndWith
import io.kotest.matchers.string.shouldNotMatch
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import rs.rs2.cgra.app.autoAccelerate
import rs.rs2.cgra.app.printCgraExecutionsIfPresent
import rs.rs2.cgra.app.printCgraProfilesIfPresent
import rs.rs2.cgra.app.printLoopProfilesIfPresent
import rs.rs2.cgra.app.verifyRs2Constraints
import rs.rs2.cgra.cgraConfigurations.EnergyFocused
import rs.rs2.cgra.cgraConfigurations.PerformanceFocused
import rs.rs2.cgra.optConfig.configureKernelOptimization
import java.nio.file.Path
import java.nio.file.Paths

@Execution(ExecutionMode.CONCURRENT)
class GpsAcquisitionTests {

    val outputDir = Paths.get("build/testOutput/gpsAcquisition")
    val programFile = Paths.get("../gpsAcquisition/build/acquisition.rv32imfc.O3.elf")

    private fun buildRvSystem(outputDir: Path, accelerate: Boolean, cgraConfig: ICgraSchedulerModelProvider, referenceILoopProfiler: ILoopProfiler?, vararg cmdArgs: String): IRvSystem {
        return rvSystem(programFile) {

            env {
                envVar("LC_NUMERIC", "en_US.UTF-8")
                envVar("LANG", "en_US.UTF-8")
                args(*cmdArgs, useProgNameAsArg0 = true)
            }
            stdio {
                writeStdOutAndErrToFiles()
                recordStdOutAndErr()
                passthroughStdOutAndErr = true
            }
            unlimitedPagedMemory {
            }
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
                if (!accelerate) {
                    enableLoopProfiling()
                }
            }
            val cgraModel = cgraConfig.invoke()
            System.err.println("Using Cgra-Config: ${cgraModel.name}")
            cgraModel.verifyRs2Constraints()
            cgraAcceleration(cgraModel) {
                synthOutputPath = outputDir

                cfgOpt {
                    configureKernelOptimization()
                }

                useMMIOSpeculativePatchingWithCheckOffload() {

                    cgraLoopProfiling = true

                    dumpAsapSchedule = true
                    dumpCgraSchedule = true
                    dumpUtilization = true

                    printPatchInsns = false

                    writeLocCompression = WriteLocCompression.All

                    enableScarAssertions()
                }

                if (accelerate) {
                    check(referenceILoopProfiler != null) { "require loopProfiler!" }
                    importLoopProfiles(referenceILoopProfiler)

                    aotAcceleration {
                        autoAccelerate(referenceILoopProfiler)
                    }
                }
            }
        }
    }

    private fun speedupRun(outputDir: Path, config: ICgraSchedulerModelProvider, testId: Int) {
        val refSystem = buildRvSystem(outputDir, false, config, null, "$testId")

        val refSim = SimulatorFramework.createSimulator(refSystem, outputDir.resolve("reference"))

        System.err.println("======== Start Of Reference Simulation ==========")
        val refStartTime = System.currentTimeMillis()

        refSim.runFullProgram(10_000_000_000L)

        refSystem.stdio.flush()
        System.err.println()
        System.err.println("========= End Of Reference Simulation ===========")

        refSystem.verifyPassed()

        val refTicks = refSim.currentTick
        System.err.println("Tick Count: $refTicks")
        val refTotalEnergy = refSystem.estimateEnergyUsage(refTicks)
        System.err.println("Energy Used: $refTotalEnergy")
        val stopTime = System.currentTimeMillis()
        val passedTime = (stopTime - refStartTime).toFloat() / 1000
        System.err.println("Real Time: $passedTime s")
        refSystem.printLoopProfilesIfPresent()

        System.err.println()
        System.err.println("======== Preparing Acceleration Simulation ==========")
        val refLoopProfiler = refSystem.core.dispatcher.profiler ?: error("Should always have a LoopProfiler")
        val accSystem = buildRvSystem(outputDir, true, config, refLoopProfiler, "$testId")

        val accSim = SimulatorFramework.createSimulator(accSystem, outputDir.resolve("acceleration"))

        System.err.println("======== Start Of Acceleration Simulation ==========")
        val accStartTime = System.currentTimeMillis()

        accSim.runFullProgram(10_000_000_000L)

        accSystem.stdio.flush()

        System.err.println()
        System.err.println("========= End Of Acceleration Simulation ===========")

        accSystem.verifyPassed()

        val accTicks = accSim.currentTick
        System.err.println("Tick Count: $accTicks")
        val accTotalEnergy = accSystem.estimateEnergyUsage(accTicks)
        System.err.println("Energy Used: $accTotalEnergy")
        val accStopTime = System.currentTimeMillis()
        val accPassedTime = (accStopTime - accStartTime).toFloat() / 1000
        System.err.println("Real Time: $accPassedTime s")
        System.err.println()
        val speedup = refSim.currentTick.toFloat() / accTicks
        System.err.println("Achieved Whole-Program-Speedup: $speedup")


        accSystem.printCgraExecutionsIfPresent()

        accSystem.printCgraProfilesIfPresent()
    }

    private fun IRvSystem.verifyPassed() {
        //TODO we could hook getTestCase(int id) to detect which case is actually loaded and sniff the required result.
        // we could also hook loadSamplesAndCodes(acquisition_t* acq, const testCase_t* testCase, int32_t nrOfSamples) to find the location of *acq to verify the result externally after execution

        val lines = stdio.recordedStdOutLines

        lines.forEach {
            it shouldNotMatch "Overriding to .* samples"
        }
        lines shouldEndWith listOf(
            "PASSED",
            "",
            ""
        )
    }

    @Test
    fun testPerformanceFocusedCgraConfig() {
        val outputDir = outputDir.resolve("performanceFocusedSpeedup")
        val config = PerformanceFocused()

        speedupRun(outputDir, config, 0)
    }

    @Test
    fun testEnergyFocusedCgraConfig() {
        val outputDir = outputDir.resolve("energyFocusedSpeedup")
        val config = EnergyFocused()

        speedupRun(outputDir, config, 0)

    }

    @Tag("extended")
    @Test
    fun testPerformanceFocusedCgraConfigTest1() {
        val outputDir = outputDir.resolve("performanceFocusedSpeedup_1")
        val config = PerformanceFocused()

        speedupRun(outputDir, config, 1)
    }

    @Tag("extended")
    @Test
    fun testEnergyFocusedCgraConfigTest1() {
        val outputDir = outputDir.resolve("energyFocusedSpeedup_1")
        val config = EnergyFocused()

        speedupRun(outputDir, config, 1)

    }

    @Tag("extended")
    @Test
    fun testPerformanceFocusedCgraConfigTest2() {
        val outputDir = outputDir.resolve("performanceFocusedSpeedup_2")
        val config = PerformanceFocused()

        speedupRun(outputDir, config, 2)
    }

    @Tag("extended")
    @Test
    fun testEnergyFocusedCgraConfigTest2() {
        val outputDir = outputDir.resolve("energyFocusedSpeedup_2")
        val config = EnergyFocused()

        speedupRun(outputDir, config, 2)

    }
}