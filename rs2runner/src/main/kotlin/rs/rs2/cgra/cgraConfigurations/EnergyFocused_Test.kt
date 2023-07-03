package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.hdlModel.api.ICgraHdlGenerationModel
import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.INativeWrapperModel
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.PeGrid
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.cgraConfigurator
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.matrixStarInterconnect
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
import model.resources.processing.operator.*
import rs.rs2.cgra.cgraConfigurations.SharedCgraConfig.applyCommonConfig
import scar.ScarFormat

/**
 * Use this CGRA-Config for the energy efficient variant. Use of this "efficiency" config is already configured in all tests.
 *
 * You can copy from or compare with the Std CGRAs as you wish. Handed in results WILL use this class with the exact names for the energy-build
 */
class EnergyFocused: ICgraSchedulerModelProvider {
    override val name: String
        get() = "energy"

    override fun invoke(): ICgraSchedulerModel {
        val grid = PeGrid(3, 1)

        grid.matrixStarInterconnect();
        // grid.matrixInterconnect();

        return grid.cgraConfigurator(name) {

            val evenColumnPEs = grid.filterByCoordinate { pe, x, y ->
                x % 2 == 0
            }

            val unevenColumnPEs = grid.filterByCoordinate { pe, x, y ->
                x % 2 != 0
            }

            // All standard operators for uneven colums (half the PEs)
            operatorsFor(unevenColumnPEs) {
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
            }

            // only some operators for the other half
            operatorsFor(evenColumnPEs) {
                // contents of all32BitOperators()
                // Latencies (bufferedRegs, bufferedX) are not allowed to be changed!
                +IntegerCoreOperations() // +, -, ==, !=, <, >=, u<, u>=
                +ChunkMuxAndRouteOperations // Passthrough & Mux(a, b). Needed for architecture to work
                +ChunkLogicOperations // 32bit And, Or, Xor
//                +IntegerShiftOperations // <<, >>, >>>
                +IntegerMultiplyOperations() // multiply
//                +IntegerDivisionOperations // divide, remainder
                // ------------ OR ----------------
//                all32BitIntegerOperators() // could be used instead of typing up above operators manually


                // contents of defaultSinglePrecisionFloatOperators()
//                +FloatConversions(withUnsigned = true) // int2float, float2int, float2uint, uint2float
                +FloatAddSub // Add, Sub
                +FloatLogic // Absolute, Negate
                +FloatMultiply //  Multiply
//                +FloatDivision // divide
//                +FloatSqrt // square root
                +FloatComparisons // ==, !=, <, <=
                // ------------ OR ----------------
//                defaultSinglePrecisionFloatOperators() // could be used instead of typing up above operators manually
            }

            // only 2 single Sin/Cos unit on specific PEs
            operatorsFor(grid[1,0], grid[2,0]) {
                +FloatTrigonometryOperations
            }

            // Memory PEs
            // operatorsFor(grid[2, 0], grid[1, 3]) {
            operatorsFor(grid[0, 0]) {
                +NativeMemoryOperations(withBarriers = false, withIntegratedOffset = true)  // load and store operations in signed, unsigned, 32, 16 and 8 bit
                // [withIntegratedOffset]: addition operation can internally do a pointer addition like (arrBase + 12). Without it, saves additional operand & registerPort, uses regular, external addition
                // [withBarriers]: not needed for the memory-model that is used
            }

            useCondPEs {
                regFileSize = 64
                evalBlockCount = 1
            }
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
