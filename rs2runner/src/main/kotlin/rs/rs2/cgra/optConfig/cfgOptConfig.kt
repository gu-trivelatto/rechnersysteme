package rs.rs2.cgra.optConfig

import de.tu_darmstadt.rs.cgra.igraph.opt.GenericCfgOptimizationConfig
import de.tu_darmstadt.rs.cgra.synthesis.builder.ICgraKernelSynthesisBuilder
import de.tu_darmstadt.rs.cgra.synthesis.builder.ICgraKernelSynthesisWithDebuggingBuilder
import de.tu_darmstadt.rs.cgra.synthesis.kernel.WriteLocCompression
import de.tu_darmstadt.rs.cgra.synthesis.testing.enableScarAssertions
import de.tu_darmstadt.rs.nativeSim.synthesis.accelerationManager.ICfgManagerBuilder
import de.tu_darmstadt.rs.nativeSim.synthesis.patchingStrategy.builder.IMMIOSpeculativePatchingStrategyWithCheckOffloadBuilder

/**
 * Functions with the following symbolic-names are excluded from profile-based acceleration.
 * I.E. they will not be picked as kernels, even they make up most of the execution time
 *
 * Does not prevent them from being inlined into a surrounding kernel
 */
val blacklistedFromAcceleration = setOf(
    "main", "enterSample", "enterCode", "loadSamplesAndCodes"
)

/**
 *
 */
fun ICfgManagerBuilder<*>.configureKernelOptimization() {
    intrinsics {
        register("cosf")
        register("sinf")
        register("absf")
        // do not add memcpy, memset, because they have no CFG impl. If they are called, we would fail inlining. Only CFGSim works for those right now
    }

    basePassConfig = GenericCfgOptimizationConfig.Default

//    dontInlineCallsTo("neverInline")

//    dropCallsTo("removeCallsInsideKernels")

    unrollingFactors(4, 8)
}

fun IMMIOSpeculativePatchingStrategyWithCheckOffloadBuilder.configureStrategy() {
    cgraLoopProfiling = true

    printPatchInsns = true
}

fun ICgraKernelSynthesisWithDebuggingBuilder.configureCgraSynthesis() {

    // dump reports
    dumpAsapSchedule = true
    dumpCgraSchedule = true
    dumpUtilization = true

    writeLocCompression = WriteLocCompression.All // tries to send register-contents and local variables from processor only once, even when needed in multiple PEs

    contextCompression = true // compress CGRA-Contexts. Can significantly reduce used Contexts, especially if many PEs are not that busy.

    enableScarAssertions() // verifies intermediate data to still be consistent. No influence on performance
}