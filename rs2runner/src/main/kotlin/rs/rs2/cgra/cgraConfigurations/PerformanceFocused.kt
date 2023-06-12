package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.INativeWrapperModel
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.PeGrid
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.cgraConfigurator
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.matrixStarInterconnect
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.flopoco.FlopocoAddSub
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.flopoco.FlopocoComparisons
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.flopoco.FlopocoDivision
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.flopoco.FlopocoF2I
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.flopoco.FlopocoF2IEEE
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.flopoco.FlopocoI2F
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.flopoco.FlopocoIEEE2F
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.flopoco.FlopocoLogic
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.flopoco.FlopocoMultiply
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.flopoco.FlopocoSqrt
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.flopoco.FlopocoTrigonometryOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.fp.FloatAddSub
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.fp.FloatComparisons
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.fp.FloatConversions
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.fp.FloatDivision
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.fp.FloatLogic
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.fp.FloatMultiply
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.fp.FloatSqrt
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.fp.FloatTrigonometryOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.ChunkLogicOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.ChunkMuxAndRouteOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.IntegerCoreOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.IntegerDivisionOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.IntegerMultiplyOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.IntegerShiftOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.memory.NativeMemoryOperations
import rs.rs2.cgra.cgraConfigurations.SharedCgraConfig.applyCommonConfig

/**
 * Use this CGRA-Config for the performance focussed variant. Use of this "performance" config is already configured in all tests.
 *
 * You can copy from or compare with the Std CGRAs as you wish. Handed in results WILL use this class with the exact names for the performance-build
 */
class PerformanceFocused: ICgraSchedulerModelProvider {
    override val name: String
        get() = "performance"

    override fun invoke(): ICgraSchedulerModel {
        val grid = PeGrid(4, 4)
        // PeCube(3, 3, 3)

    //    grid.matrixInterconnect()
        grid.matrixStarInterconnect()
    //    grid.fullInterconnect()
        // see [IrregularInterconnect] for completely custom interconnects

        return grid.cgraConfigurator(name) {

            operatorsForAllDataPes {
                // contents of all32BitOperators()
                // Latencies (bufferedRegs, bufferedX) are not allowed to be changed!
                +IntegerCoreOperations() // +, -, ==, !=, <, >=, u<, u>=
                +ChunkMuxAndRouteOperations // Passthrough & Mux(a, b). Needed for architecture to work
                +ChunkLogicOperations // 32bit And, Or, Xor
                +IntegerShiftOperations // <<, >>, >>>
                +IntegerMultiplyOperations() // multiply
                +IntegerDivisionOperations // divide, remainder
                // ------------ OR ----------------
//                all32BitIntegerOperators() // could be used instead of typing up above operators manually


                // contents of defaultSinglePrecisionFloatOperators()
                +FloatConversions(withUnsigned = true) // int2float, float2int, float2uint, uint2float
                +FloatAddSub // Add, Sub
                +FloatLogic // Absolute, Negate
                +FloatMultiply //  Multiply
                +FloatDivision // divide
                +FloatSqrt // square root
                +FloatComparisons // ==, !=, <, <=
                // ------------ OR ----------------
//                defaultSinglePrecisionFloatOperators() // could be used instead of typing up above operators manually

                // SINCOS is not included in defaultSinglePrecisionFloatOperators()
                +FloatTrigonometryOperations
            }

            // Memory PEs
            operatorsFor(grid[2, 0], grid[1, 3]) {
                +NativeMemoryOperations(withBarriers = false, withIntegratedOffset = false)  // load and store operations in signed, unsigned, 32, 16 and 8 bit
                // [withIntegratedOffset]: addition operation can internally do a pointer addition like (arrBase + 12). Without it, saves additional operand & registerPort, uses regular, external addition
                // [withBarriers]: not needed for the memory-model that is used
                // ------------ OR ----------------
//                memoryOperators() // could be used instead of typing up above operator manually
            }

            useCondPEs { // universal, can handle any kind of code structure
                condPeCount = 2
            }
//            useCBox { // old alternative to CondPEs. Can technically do more per cycle, but often less well utilized and cannot handle every kind of code structure
//                regFileSize = 64
//                evalBlockCount = 2
//            }
            setDefaultDataPeRegFileSize(256)
            allLcus {
                memorySize = 4096
            }

            applyCommonConfig()
        }
    }

    override fun getNativeWrapperMemoryInfo(): INativeWrapperModel {
        return SharedCgraConfig.buildWrapperConfig()
    }
}
