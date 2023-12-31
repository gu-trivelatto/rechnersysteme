package rs.rs2.cgra.app

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.beust.jcommander.Parameter
import com.beust.jcommander.ParameterException
import com.beust.jcommander.ParametersDelegate
import com.beust.jcommander.converters.PathConverter
import de.tu_darmstadt.rs.cgra.api.components.printKernelStats
import de.tu_darmstadt.rs.riscv.impl.synthesis.energy.energyConsumptionTreeWithPeActivity
import de.tu_darmstadt.rs.cgra.impl.components.loopProfiling.commonLoopProfiler
import de.tu_darmstadt.rs.cgra.impl.components.loopProfiling.printLoopTable
import de.tu_darmstadt.rs.cgra.impl.memory.ZeroLatencyByteAddressedCgraMemoryPort
import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.hdlModel.serviceLoader.CgraModelLoader
import de.tu_darmstadt.rs.cgra.schedulerModel.TS
import de.tu_darmstadt.rs.cgra.schedulerModel.operator.IOperationSchedulerModel
import de.tu_darmstadt.rs.cgra.simulator.impl.testing.loggingConfigs.configureStdCgraLogging
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
import de.tu_darmstadt.rs.riscv.BaseRvArchDescription
import de.tu_darmstadt.rs.riscv.impl.synthesis.builder.cgraAcceleration
import de.tu_darmstadt.rs.riscv.simulator.api.IRvSystem
import de.tu_darmstadt.rs.riscv.simulator.impl.builder.RvSystemBuilder
import de.tu_darmstadt.rs.riscv.simulator.impl.components.syscalls.ECallExecutor
import de.tu_darmstadt.rs.riscv.simulator.impl.debugging.runFullProgramWithDebugging
import de.tu_darmstadt.rs.simulator.api.ISystemSimulator
import de.tu_darmstadt.rs.simulator.api.SimulatorFramework
import de.tu_darmstadt.rs.simulator.api.clock.ITicker
import de.tu_darmstadt.rs.simulator.api.energy.estimateEnergyUsage
import de.tu_darmstadt.rs.simulator.components.memoryModel.caches.dataless.BaseSuspendingDatalessLatencyModel
import de.tu_darmstadt.rs.simulator.components.memoryModel.coherencyManagement.BaseCoherencyManager
import de.tu_darmstadt.rs.util.kotlin.logging.slf4j
import rs.rs2.cgra.cgraConfigurations.PerformanceFocused
import rs.rs2.cgra.optConfig.configureCgraSynthesis
import rs.rs2.cgra.optConfig.configureKernelOptimization
import rs.rs2.cgra.optConfig.configureStrategy
import rs.rs2.cgra.optConfig.disableMemoryAliasingDetection
import scar.Operation
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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

    @Parameter(names = ["--correctnessOnly"], description = "Ignore memory and compute latencies to simulate faster. Clock cycles will be unrealistic")
    var fast: Boolean = false

    @Parameter(names = ["--noCoherencyModel"], description = "Disable full coherency model for simulation. Avoids memory consistency issues, but reduced execution times, as memory access will be unnaturally fast")
    var noCoherencyModel: Boolean = false

    @Parameter(names = ["--postedWrites"], description = "Allow CGRA to continue execution without waiting for memory-writes to actually complete. Increases speed, but may cause memory consistency issues with current CGRA Synthesis. Recommended off, unless you can verify correct operation!")
    var cgraPostedWrites: Boolean = false

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
                    when (this.elem) {
                        is BaseSuspendingDatalessLatencyModel<*,*> -> {
                            logAll()
                        }
                        is BaseCoherencyManager<*,*,*> -> {
                            logAll()
                        }
                    }

                }
            }
        }
    }

    protected fun getDisasmType() = when (parseEager) {
        true -> VariableInsnLengthElfLoader.DisasmType.Eager
        else -> VariableInsnLengthElfLoader.DisasmType.AsYouGo
    }

    protected fun RvSystemBuilder.configureCgraIfNeeded(options: CgraAccelerationOptions, accelerate: Boolean, alwaysAttachCgra: Boolean = accelerate, loopProfiler: ILoopProfiler? = null, kernelTraceConfig: KernelTraceConfig? = null) {

        if (alwaysAttachCgra) {
            val cgraName = options.cgra
            val provider = CgraModelLoader.loadSchedulerModelByName(cgraName) ?: throw ParameterException("cgraConfig $cgraName not found!")
            val model = provider()
            cgra {
                postedWrites = this@BaseRunnerCommand.cgraPostedWrites
            }
            model.verifyRs2Constraints()
            if (accelerate) {
                System.err.println("Using Cgra-Config: ${model.name}")
            }
            cgraAcceleration(model) {
                val cgraVcdOut = options.cgraVcdOutput
                configureCgraAcceleration(
                    this@configureCgraIfNeeded.executable,
                    options,
                    simCgraWithHook = options.cgraHook || cgraVcdOut || options.createCgraRefImage || kernelTraceConfig != null,
                    cfgSim = options.cfgSim,
                    generateCgraWaveForms = cgraVcdOut,
                    loopProfiler = loopProfiler,
                    accelerate = accelerate,
                    kernelTraceConfig = kernelTraceConfig
                )
            }
        }
    }

    private fun IAccelerationManagerBuilder<BaseRvArchDescription>.configureCgraAcceleration(
        elf: IExecutableBinary<*, *, *>,
        options: CgraAccelerationOptions,
        simCgraWithHook: Boolean,
        cfgSim: Boolean,
        generateCgraWaveForms: Boolean,
        loopProfiler: ILoopProfiler?,
        accelerate: Boolean,
        kernelTraceConfig: KernelTraceConfig?
    ) {
        synthOutputPath = debugOutputDir

        val manuallySelectedKernels = options.kernels.map { kernelId ->
            kernelId.parseToKernelDescriptor(elf)
        }

        cfgOpt {
            dumpSerCfg = options.createCgraRefImage

            configureKernelOptimization()
        }

        if (cfgSim) {
            useCfgSimOnly {
                dumpReferenceLiveValues = true
                loopProfiling = true

                if (kernelTraceConfig != null) {
                    applyMemoryTracer(kernelTraceConfig.kernel.id, kernelTraceConfig.createVerifier(debugOutputDir))
                }

                if (options.createCgraRefImage) {
                    manuallySelectedKernels
                        .filterIsInstance<NativeKernelDescriptor.FunctionKernelDescriptor>()
                        .forEach {
                            applyMemoryTracer(
                                it.id,
                                MemoryTracer(
                                    dumpTrace = debugOutputDir?.resolve("${it.id}.refMemory.trace"),
                                    dumpRefImage = debugOutputDir?.resolve("${it.id}.refMemory.json")
                                )
                            )
                        }
                }
            }
        } else if (simCgraWithHook) {
            useCgraHookExecution {

                cgraLoopProfiling = true
                kernelSelection = if (disableMemoryAliasingDetection) {
                    KernelSelection.Safe
                } else {
                    KernelSelection.KernelWithIntegratedGlobalSpeculation
                }
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

                if (kernelTraceConfig != null) {
                    applyMemoryTracer(kernelTraceConfig.kernel.id, kernelTraceConfig.createVerifier(debugOutputDir))
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
            if (disableMemoryAliasingDetection) {
                useMMIOPatching {
                    configureStrategy()
                    configureCgraSynthesis()
                }
            } else {
                useMMIOSpeculativePatchingWithCheckOffload() {

                    configureStrategy()
                    configureCgraSynthesis()
                }
            }
        }

        throwSynthesisErrors = false

        if (accelerate) {
            configureAcceleration(this, manuallySelectedKernels, loopProfiler)
        }
    }

    protected open fun configureAcceleration(
        mgmt: IAccelerationManagerBuilder<BaseRvArchDescription>,
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
            staticEnergyPerTick = 25.0

            additionalInitialization {
                val traceFile = this@BaseRunnerCommand.traceMemory
                if (traceFile != null) {
                    tracer = MemoryTracer(dumpTrace = traceFile)
                }
            }
        }
    }

    protected fun RvSystemBuilder.configureMemoryLatencyModel() = configureMemoryLatencyModel(noCoherencyModel)

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
    val recordedProfiles = cgra?.commonLoopProfiler?.recordedProfiles
    if (recordedProfiles?.isNotEmpty() == true) {
        System.err.println()
        System.err.println("CGRA Loop Profiles:")
        System.err.println("=======================================")
        recordedProfiles.forEach { (kernel, profiles) ->
            System.err.println("Loops in $kernel")
            System.err.println("---------------------------------------")
            profiles.printLoopTable(System.err)
        }
    }
}

fun RvSystemBuilder.configureMemoryLatencyModel(noCoherencyModel: Boolean = false) {
    if (!noCoherencyModel) {
        twoLevelCacheHierarchy {
            useDragonCoherency {
                forceCoherencyRequestEvenIfNoSiblings = true
            }
        }
    } else {
        staticMemoryLatency(1)
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
    check(liveInOutDataWidth == 32)
    check(constantTableDataWidth == 32)
    check(dataInterconnectWidth <= 34)

    check(this.dataPes.count { it.hasMemoryAccess } <= 4) { "Cannot use more than 4 Memory Ports" }

    val contextCount = this.getLcuForComponent(0).contextCount ?: error("CtxCount missing!")
    if (contextCount > 8192) {
        System.err.println("WARNING: Excessive Context Count of $contextCount: Please check whether the total amount of required memory is still reasonable and justify this in your report!")
    }

    verifyOperationNotQuickerThan(Operation.MUL_int, 1)
    verifyOperationNotQuickerThan(Operation.ADD_flopoco, 3)
    verifyOperationNotQuickerThan(Operation.SUB_flopoco, 3)
    verifyOperationNotQuickerThan(Operation.MUL_flopoco, 2)
    verifyOperationNotQuickerThan(Operation.DIV_flopoco, 6)
    verifyOperationNotQuickerThan(Operation.IF_EQ_flopoco, 1)
    verifyOperationNotQuickerThan(Operation.IF_NE_flopoco, 1)
    verifyOperationNotQuickerThan(Operation.IF_LT_flopoco, 1)
    verifyOperationNotQuickerThan(Operation.IF_LE_flopoco, 1)
    verifyOperationNotQuickerThan(Operation.Flopoco2I, 1)
    verifyOperationNotQuickerThan(Operation.Flopoco2UI, 1)
    verifyOperationNotQuickerThan(Operation.I2Flopoco, 1)
    verifyOperationNotQuickerThan(Operation.UI2Flopoco, 1)
    verifyOperationNotQuickerThan(Operation.SQRT_flopoco, 6)
}

private fun ICgraSchedulerModel.verifyOperationNotQuickerThan(operation: Operation, shortestTs: TS) {
    val illegal = this.getOperationModels(operation).filter { it.getActionPlan().timeSteps <= shortestTs }.toList()

    check(illegal.isEmpty()) {
        "Illegal operation: $operation was configured to be faster than $shortestTs timeSteps, which is below the default-value: ${illegal.joinToString() { describeOp(it) }}"
    }
}

private fun ICgraSchedulerModel.describeOp(op: IOperationSchedulerModel): String {
    val peID = op.peID
    val pipe = dataPes[peID].pipelines?.find { it.opcodes.any { it.scarOperation == op.scarOperation } }
    return "${pipe?.builder?.simpleName ?: "[unknown pipeline]"} on PE$peID"
}