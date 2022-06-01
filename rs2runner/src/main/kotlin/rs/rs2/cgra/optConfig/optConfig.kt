package rs.rs2.cgra.optConfig

import de.tu_darmstadt.rs.cgra.igraph.opt.GenericCfgOptimizationConfig
import de.tu_darmstadt.rs.cgra.synthesis.builder.ICgraKernelSynthesisWithDebuggingBuilder
import de.tu_darmstadt.rs.cgra.synthesis.kernel.WriteLocCompression
import de.tu_darmstadt.rs.cgra.synthesis.testing.enableScarAssertions
import de.tu_darmstadt.rs.nativeSim.synthesis.accelerationManager.ICfgManagerBuilder
import de.tu_darmstadt.rs.nativeSim.synthesis.patchingStrategy.builder.IMMIOPatchingStrategyBuilder
import de.tu_darmstadt.rs.nativeSim.synthesis.patchingStrategy.builder.IMMIOSpeculativePatchingStrategyWithCheckOffloadBuilder

/**
 * Functions with the following symbolic-names are excluded from profile-based acceleration.
 * I.E. they will not be picked as kernels, even if they make up most of the execution time
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
//        register("cosf")
//        register("sinf")
//        register("absf")
//        register("memcpy")
//        register("memset")
        useAll() // useAll includes all of the above!
    }

    basePassConfig = GenericCfgOptimizationConfig.Default

//    dontInlineCallsTo("neverInline")

//    dropCallsTo("removeCallsInsideKernels")

    // Loop-Unroll factors from right to left: deepest loop-level up.
    // see JavaDoc
    unrollingFactors(4, 8)
}

/**
 * In case of crashes or hangs during synthesis. Should only be needed in case of bugs in synthesis
 */
val disableMemoryAliasingDetection: Boolean = false

fun IMMIOPatchingStrategyBuilder.configureStrategy() {
    // track which loop execution on CGRA
    // allows showing how the loops performed on CGRA. Also makes currentLoop and iteration available in Waveform
    cgraLoopProfiling = true

    // Print how the code is patched to activate a CGRA-Kernel
    printPatchInsns = true
}

fun ICgraKernelSynthesisWithDebuggingBuilder.configureCgraSynthesis() {

    scarConversion {
        // attempt to reduce memoryLoads that are detected to load the same data
        mergeRedundantLoads = true
        // attempt to replace memoryLoad after a memoryStore of same address with the data that was stored, to save on memory-accesses
        mergeRedundantStoreLoadPair = true
    }

    // dump reports
    dumpAsapSchedule = true
    dumpCgraSchedule = true
    dumpUtilization = true

    // tries to send register-contents and local variables from processor only once, even when needed in multiple PEs
    writeLocCompression = WriteLocCompression.All

    // compress CGRA-Contexts. Can significantly reduce used Contexts, especially if many PEs are not that busy.
    // With compression, CCNT (like Program Counter in CPU) is no longer shared accross PEs. PE0 could still be in CCNT 0, while PE2 is in CCNT 100.
    // makes it harder to read and debug the Kernel
    // no influence on performance
    contextCompression = true

    enableScarAssertions() // verifies intermediate data to still be consistent. No influence on performance
}