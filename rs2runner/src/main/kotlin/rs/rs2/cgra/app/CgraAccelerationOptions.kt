package rs.rs2.cgra.app

import com.beust.jcommander.Parameter
import de.tu_darmstadt.rs.cgra.resourceModel.predefinedConfigs.Std4x4MatrixStar2MemCombBss256r4096cFloat

class CgraAccelerationOptions(
    defaultCgraName: String = Std4x4MatrixStar2MemCombBss256r4096cFloat().name
) {

    @Parameter(names = ["--cgraVcd"], description = "Generate Waveform for CGRA only. Will exclude CGRA from --vcd if also enabled.")
    var cgraVcdOutput: Boolean = false

    @Parameter(names = ["--kernel"], description = "Define Kernels manually for synthesis. Can be loop or function notation, symbolic or absolute")
    var kernels: MutableList<String> = mutableListOf()

    @Parameter(names = ["--aot"], description = "Synthesize and enable all kernels that are already known immediately. Make known with --kernel")
    var accelerateAot: Boolean = false

    @Parameter(names = ["--cgra"], description = "Configure the type of cgra")
    var cgra: String = defaultCgraName

    @Parameter(names = ["--createCgraRefImage"], description = "Output all the required files to do standalone cgra-kernel simulations for every function-kernel. Switches to cgraHook")
    var createCgraRefImage: Boolean = false

    @Parameter(names = ["--cfgSim"], description = "use CfgSim instead of cgra. Bypasses scheduling, cgra & coherency simulation to check if the CFG was already incorrect. Will use hooks to insert CFG simulation.")
    var cfgSim: Boolean = false

    @Parameter(names = ["--cgraHook"], description = "use CgraHook instead of builtin cgra. Bypasses patches and coherency simulation.")
    var cgraHook: Boolean = false
}