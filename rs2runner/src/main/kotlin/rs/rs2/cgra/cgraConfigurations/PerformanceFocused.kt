package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.hdlModel.api.ICgraHdlGenerationModel
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.INativeWrapperModel
import de.tu_darmstadt.rs.cgra.scheduling.flow.PeGrid
import de.tu_darmstadt.rs.cgra.scheduling.flow.cgraConfigurator
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.matrixStarInterconnect
import model.resources.processing.operator.*
import rs.rs2.cgra.cgraConfigurations.SharedCgraConfig.applyCommonConfig
import scar.Format

/**
 * Use this CGRA-Config for the performance focussed variant. Use of this "performance" config is already configured in all tests.
 *
 * You can copy from or compare with the Std CGRAs as you wish.
 */
class PerformanceFocused: ICgraSchedulerModelProvider {
    override val name: String
        get() = "performance"

    override fun invoke(): ICgraHdlGenerationModel {
        val grid = PeGrid(4, 4)
        // PeCube(3, 3, 3)

    //    grid.matrixInterconnect()
        grid.matrixStarInterconnect()
    //    grid.fullInterconnect()
        // see [IrregularInterconnect] for completely custom interconnects

        return grid.cgraConfigurator(name) {

            operatorsForAllDataPes {
                // contents of all32BitOperators()
                +ADDSUB(Format.INT) // Addition, Subtraction
                +NEG(Format.INT) // K2 Negate
                +MUL(Format.INT) // Multiply
                +DIVREMInt() // Divide, Remainder
                +AND(Format.RAW32) // Binary And
                +OR(Format.RAW32) // Binary Or
                +XOR(Format.RAW32) // Binary XOr
                +NOT(Format.RAW32) // Binary Negate
                +CMP(Format.INT, true) // Main Signed Integer comparisons (>,>=,<,<=,==, !=)
                +CMP(Format.INT, false) // More comparisons with 32bit result. Maybe needed for complicated conditions
                +UCMP(Format.UINT, true) //
                +UCMP(Format.UINT, false) // Main Unsigned Integer comparisons (>,>=,<,<=,==, !=)
                +SHL(Format.INT) // Shift left
                +SHR(Format.INT) // Shift Right (arithmetic)
                +I2B() // Convert Integer to Byte
                // ------------ OR ----------------
//                all32BitIntegerOperators() // could be used instead of typing up above operators manually


                // contents of defaultSinglePrecisionFloatOperators()
                +I2F() // Convert Integer to Float
                +F2I() // Convert Float to Integer
                +ADDSUB(Format.FLOAT) // Addition, Subtraction
                +NEG(Format.FLOAT) // Negate
                +MUL(Format.FLOAT) // Multiply
                +DIVFLOAT() // Divide
                +SQRTFLOAT() // SquareRoot
                +CMP(Format.FLOAT, false) // Main Float comparisons (>,>=,<,<=,==,!=)//
                // ------------ OR ----------------
//                defaultSinglePrecisionFloatOperators() // could be used instead of typing up above operators manually

                // SINCOS is not included in defaultSinglePrecisionFloatOperators()
                +Trigonometric.SINCOS(Format.FLOAT)
            }

            // Memory PEs
            operatorsFor(grid[2, 0], grid[1, 3], grid[0, 1], grid [3, 2]) {
                +RandomAccessMemory(false, true, 32, true) // load and store operations in signed, unsigned, 32, 16 and 8 bit
                // ------------ OR ----------------
//                memoryOperators() // could be used instead of typing up above operator manually
            }

            useCBox {
                regFileSize = 64
                evalBlockCount = 2
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
