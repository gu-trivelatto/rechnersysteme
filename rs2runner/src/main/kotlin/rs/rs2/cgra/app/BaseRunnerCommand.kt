package rs.rs2.cgra.app

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.ParametersDelegate
import com.beust.jcommander.converters.PathConverter
import de.tu_darmstadt.rs.cgra.api.components.loopProfiling.print
import de.tu_darmstadt.rs.cgra.api.components.printKernelStats
import de.tu_darmstadt.rs.riscv.impl.synthesis.energy.energyConsumptionTreeWithPeActivity
import de.tu_darmstadt.rs.cgra.impl.components.loopProfiling.commonLoopProfiler
import de.tu_darmstadt.rs.cgra.impl.memory.ZeroLatencyByteAddressedCgraMemoryPort
import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.CgraModelLoader
import de.tu_darmstadt.rs.cgra.simulator.impl.testing.loggingConfigs.configureStdCgraLogging
import de.tu_darmstadt.rs.cgra.synthesis.kernel.WriteLocCompression
import de.tu_darmstadt.rs.cgra.synthesis.testing.enableScarAssertions
import de.tu_darmstadt.rs.disasm.executable.IExecutableBinary
import de.tu_darmstadt.rs.disasm.executable.VariableInsnLengthElfLoader
import de.tu_darmstadt.rs.jcommander.validators.DirectoryExistsValidator
import de.tu_darmstadt.rs.memoryTracer.MemoryTracer
import de.tu_darmstadt.rs.nativeSim.components.accelerationManager.NativeKernelDescriptor
import de.tu_darmstadt.rs.nativeSim.components.accelerationManager.parseToKernelDescriptor
import de.tu_darmstadt.rs.nativeSim.components.debugging.GdbSimulationDriver
import de.tu_darmstadt.rs.nativeSim.components.profiling.ILoopProfiler
import de.tu_darmstadt.rs.nativeSim.components.profiling.printLoops
import de.tu_darmstadt.rs.nativeSim.components.sysCalls.BaseSyscallHandler
import de.tu_darmstadt.rs.nativeSim.synthesis.accelerationManager.IAccelerationManagerBuilder
import de.tu_darmstadt.rs.nativeSim.synthesis.patchingStrategy.builder.KernelSelection
import de.tu_darmstadt.rs.riscv.RvArchInfo
import de.tu_darmstadt.rs.riscv.impl.synthesis.builder.cgraAcceleration
import de.tu_darmstadt.rs.riscv.simulator.api.IRvSystem
import de.tu_darmstadt.rs.riscv.simulator.impl.builder.RvSystemBuilder
import de.tu_darmstadt.rs.riscv.simulator.impl.components.syscalls.ECallExecutor
import de.tu_darmstadt.rs.riscv.simulator.impl.debugging.runFullProgramWithDebugging
import de.tu_darmstadt.rs.simulator.api.ISystemSimulator
import de.tu_darmstadt.rs.simulator.api.SimulatorFramework
import de.tu_darmstadt.rs.simulator.api.clock.ITicker
import de.tu_darmstadt.rs.simulator.api.energy.estimateEnergyUsage
import de.tu_darmstadt.rs.util.kotlin.logging.slf4j
import rs.rs2.cgra.cgraConfigurations.PerformanceFocused
import rs.rs2.cgra.optConfig.configureKernelOptimization
import java.io.PrintStream
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.outputStream

abstract class BaseRunnerCommand {

    @Parameter(variableArity = true, description = "executable [args]")
    var programAndArgs: List<String> = mutableListOf()

    @Parameter(names = ["-E"], description = "Sets an additional Env-Var", hidden = true)
    var envVars: List<String> = mutableListOf()

    @Parameter(names = ["-g"], description = "Await attaching of Debugger on this port")
    var debuggerPort: Int? = null

    @Parameter(names = ["-strace"], description = "Sets LogLevel for syscalls to trace", hidden = true)
    var logSyscalls: Boolean = false

    @Parameter(names = ["--trace-gdb"], description = "Sets LogLevel for GdbSimulationDriver to trace", hidden = true)
    var traceGdb: Boolean = false

    @Parameter(
        names = ["-v", "--virtRoot"],
        description = "similar to chroot, this becomes '/' within the simulation",
        converter = PathConverter::class,
        validateValueWith = [DirectoryExistsValidator::class],
        hidden = true
    )
    var virtRoot: Path? = null

    @Parameter(names = ["--logPerf"], description = "Measure Simulation-Performance and print it to Console in intervals")
    var logPerformance: Boolean = false

    @Parameter(names = ["-t", "--timeout"], description = "Set a Timeout for the Simulation. When reached the simulator will terminate.")
    var timeout: Long? = null

    @Parameter(
        names = ["--parseEager"],
        description = "Parse the code of the given Binary entirely before starting the simulation. Requires more memory, but is faster",
        hidden = true
    )
    var parseEager: Boolean = false

    @Parameter(names = ["--simOut"], description = "Directory into which to pack simulator framework and synthesis outputs.")
    var debugOutputDir: Path? = Paths.get("simOut")

    @Parameter(names = ["--vcd"], description = "Generate Waveform for RISC-V")
    var vcdOutput: Boolean = false

    @Parameter(names = ["--trace-memory"], description = "Write a trace file for all memory accesses", converter = PathConverter::class)
    var traceMemory: Path? = null

    @Parameter(names = ["--energy-report"], description = "Provide a detailed report about the energy consumption of the system")
    var elaborateEnergy: Boolean = false

    @Parameter(names = ["-h", "--help"], description = "Print this Help to Console")
    var help: Boolean = false

    @ParametersDelegate
    var cgraAccalerationOptions: CgraAccelerationOptions = CgraAccelerationOptions(
        defaultCgraName = PerformanceFocused().name
    )


    protected fun configureLogging(sim: ISystemSimulator, system: IRvSystem, excludeCgra: Boolean) {
        if (logSyscalls) {
            (slf4j<BaseSyscallHandler>() as Logger).level = Level.toLevel("trace")
            (slf4j<ECallExecutor>() as Logger).level = Level.toLevel("trace")
        }

        val vcdOutput = vcdOutput

        sim.logManager {
            if (logPerformance) {
                measureSimulatorPerformanceContinuously = true
            }
        }
        if (vcdOutput) {
            sim.addLoggingSink(SimulatorFramework.vcdLogger)
            sim.logManager {
                logTicks(logPauses = true)
                ticker {
                    +ITicker.SIM_PAUSE_REASON
                }
                container("rvSystem") {
                    container("Core") {
                        +"PC"
                        container("Dispatcher") {
                            logAll()
                        }
                        container("registerFile") {
                            +"gpRegs"
                            +"fpRegs"
                        }
                    }
                    if (system.cgra != null && !excludeCgra) {
                        container("cgra") {
                            configureStdCgraLogging()
                        }
                    }
                }
                allElements {
                    if (this.elem is ZeroLatencyByteAddressedCgraMemoryPort) {
                        logAll()
                    }
                }
            }
        }
    }

    protected fun getDisasmType() = when (parseEager) {
        true -> VariableInsnLengthElfLoader.DisasmType.Eager
        else -> VariableInsnLengthElfLoader.DisasmType.AsYouGo
    }

    protected fun RvSystemBuilder.configureCgraIfNeeded(options: CgraAccelerationOptions, accelerate: Boolean, alwaysAttachCgra: Boolean = accelerate, loopProfiler: ILoopProfiler? = null) {

        if (alwaysAttachCgra) {
            val cgraName = options.cgra
            val provider = CgraModelLoader.loadSchedulerModelByName(cgraName) ?: throw ParameterException("cgraConfig $cgraName not found!")
            val model = provider()
            model.verifyRs2Constraints()
            if (accelerate) {
                System.err.println("Using Cgra-Config: ${model.name}")
            }
            cgraAcceleration(model) {
                val cgraVcdOut = options.cgraVcdOutput
                configureCgraAcceleration(
                    this@configureCgraIfNeeded.executable,
                    options,
                    simCgraWithHook = cgraVcdOut || options.createCgraRefImage,
                    generateCgraWaveForms = cgraVcdOut,
                    loopProfiler,
                    accelerate
                )
            }
        }
    }

    private fun IAccelerationManagerBuilder<RvArchInfo>.configureCgraAcceleration(
        elf: IExecutableBinary<*, *>,
        options: CgraAccelerationOptions,
        simCgraWithHook: Boolean,
        generateCgraWaveForms: Boolean,
        loopProfiler: ILoopProfiler?,
        accelerate: Boolean
    ) {
        synthOutputPath = debugOutputDir

        val manuallySelectedKernels = options.kernels.map { kernelId ->
            kernelId.parseToKernelDescriptor(elf)
        }

        cfgOpt {
            dumpSerCfg = options.createCgraRefImage

            configureKernelOptimization()
        }

        if (simCgraWithHook) {
            useCgraHookExecution {

                cgraLoopProfiling = true
                kernelSelection = KernelSelection.KernelWithIntegratedGlobalSpeculation
                printLoopLengths = true
                dumpCgraSchedule = true
                dumpReferenceLiveValues = true

                if (options.createCgraRefImage) {
                    manuallySelectedKernels
                        .filterIsInstance<NativeKernelDescriptor.FunctionKernelDescriptor>()
                        .forEach {
                            applyMemoryTracer(
                                it.id,
                                MemoryTracer(dumpRefImage = debugOutputDir?.resolve("${it.id}.refMemory.json"))
                            )
                        }
                }

                if (generateCgraWaveForms) {
                    loggingConfig {
                        addLoggingSink(SimulatorFramework.vcdLogger)
                        logManager {
                            configureStdCgraLogging()
                            allElements {
                                if (this.elem is ZeroLatencyByteAddressedCgraMemoryPort) {
                                    logAll()
                                }
                            }
                        }
                    }
                }
            }
        } else {
            useMMIOSpeculativePatchingWithCheckOffload() {

                cgraLoopProfiling = true

                dumpAsapSchedule = true
                dumpCgraSchedule = true
                dumpUtilization = true

                printPatchInsns = true

                writeLocCompression = WriteLocCompression.All

                onEachKernel { kernel ->
//                        result(kernelInfo) {
//                            kernel.schedulerStatistics?.let { import(it) }
//                        }
                }

                enableScarAssertions()
            }
        }

        throwSynthesisErrors = false

        if (accelerate) {
            configureAcceleration(this, manuallySelectedKernels, loopProfiler)
        }
    }

    protected open fun configureAcceleration(
        mgmt: IAccelerationManagerBuilder<RvArchInfo>,
        manuallySelectedKernels: Collection<NativeKernelDescriptor>,
        loopProfiler: ILoopProfiler?
    ) {

        mgmt.apply {

            loopProfiler?.let {
                mgmt.importLoopProfiles(it)
            }

            mgmt.forceUseSlowKernels = manuallySelectedKernels.isNotEmpty()

            mgmt.aotAcceleration {
                manuallySelectedKernels.forEach {
                    this.accelerate(it)
                }
            }

        }
    }

    protected fun RvSystemBuilder.configureMemory() {
        unlimitedPagedMemory {
            perWordStaticEnergy = 25000.0

            additionalInitialization {
                val traceFile = this@BaseRunnerCommand.traceMemory
                if (traceFile != null) {
                    tracer = MemoryTracer(dumpTrace = traceFile)
                }
            }
        }
    }

    protected fun RvSystemBuilder.configureEnvAndStdio(argsWithoutProg: List<String>) {
        env {
            if (!workingDirectory.toString().startsWith('/') || this@BaseRunnerCommand.virtRoot != null) {
                virtRoot = this@BaseRunnerCommand.virtRoot ?: workingDirectory.parent?.parent?.parent ?: workingDirectory
            } // default behavoir of EnvBuilder does the right thing for linux/unix, using '/' as virtRoot
            envVar("LC_NUMERIC", "en_US.UTF-8")
            envVar("LANG", "en_US.UTF-8")
            this@BaseRunnerCommand.envVars.forEach {
                val split = it.split('=')
                check(split.size == 2) { "env '$it' does not hold to format ENVNAME=value" }
                envVar(split[0], split[1])
            }
            args(argsWithoutProg, useProgNameAsArg0 = true)
        }
        stdio {
            passthroughStdOutAndErr = true
        }
    }

    protected fun collectExPathAndArgs(): Pair<Path, List<String>> {
        val executable = programAndArgs.firstOrNull() ?: throw ParameterException("No executable was given!")
        val exPath = Paths.get(executable)
        if (!Files.isRegularFile(exPath)) {
            throw ParameterException("The executable '$exPath' does not exist!")
        }

        val argsWithoutProg = programAndArgs.drop(1)
        return Pair(exPath, argsWithoutProg)
    }

    protected fun ISystemSimulator.runSimulatorPossiblyWithGdb(system: IRvSystem) {
        val debuggerPort = debuggerPort
        if (debuggerPort != null) {
            if (traceGdb) {
                (slf4j<GdbSimulationDriver>() as Logger).level = Level.toLevel("trace")
            }
            runFullProgramWithDebugging(system, timeout, port = debuggerPort, waitForDebugger = true)
        } else {
            runFullProgram(timeout)
        }
    }

    protected fun IRvSystem.elaborateEnergyUsage(ticks: Long, outputDir: Path?) {
        if (elaborateEnergy) {
            System.err.println()
            System.err.println("Energy Report")
            System.err.println("=======================================")
            System.err.println()
            this.energyConsumptionTreeWithPeActivity(ticks, output = System.err)
        }
        if (outputDir != null) {
            outputDir.createDirectories()
            val name = if (this.cgra != null) {
                cgraAccalerationOptions.cgra
            } else {
                "noCgra"
            }
            PrintStream(outputDir.resolve("energyReport.${name}.txt").outputStream().buffered()).use { stream ->
                val total = estimateEnergyUsage(ticks)
                stream.println("Total Energy: $total")
                stream.println()
                this.energyConsumptionTreeWithPeActivity(ticks, output = stream)
            }
        }
    }
}

fun IRvSystem.printCgraProfilesIfPresent() {
    cgra?.commonLoopProfiler?.recordedProfiles?.forEach { (kernel, profiles) ->
        System.err.println()
        System.err.println("CGRA Loop Profiles:")
        System.err.println("=======================================")
        System.err.println("Loops in $kernel")
        System.err.println("---------------------------------------")
        profiles.print(System.err)
    }
}

fun IRvSystem.printLoopProfilesIfPresent() {
    core.dispatcher.profiler?.let { profiler ->
        System.err.println()
        System.err.println("Loop Profiles:")
        System.err.println("=======================================")
        System.err.println()
        val loops = profiler.collectPostProcessedLoops().filter { it.directParent == null }.sortedByDescending { it.spentTicks }
        loops.printLoops(System.err)
    }
}

fun IRvSystem.printCgraExecutionsIfPresent() {
    val cgra = cgra
    val execStats = cgra?.kernelStats
    if (execStats?.isNotEmpty() == true) {
        System.err.println()
        System.err.println("CGRA Execution Stats:")
        System.err.println("=======================================")
        cgra.printKernelStats(System.err)
    }
}

fun ICgraSchedulerModel.verifyRs2Constraints() {
    check(bitWidth == 32)
    check(this.dataPes.count { it.hasMemoryAccess } <= 4) { "Cannot use more than 4 Memory Ports" }

    val contextCount = this.getLcuForComponent(0).contextCount ?: error("CtxCount missing!")
    if (contextCount > 8192) {
        System.err.println("WARNING: Excessive Context Count of $contextCount: Please check whether the total amount of required memory is still reasonable and justify this in your report!")
    }
}