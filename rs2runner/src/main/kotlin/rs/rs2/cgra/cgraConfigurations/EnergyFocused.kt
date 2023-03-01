package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.hdlModel.api.ICgraHdlGenerationModel
import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.INativeWrapperModel
import de.tu_darmstadt.rs.cgra.scheduling.flow.PeGrid
import de.tu_darmstadt.rs.cgra.scheduling.flow.cgraConfigurator
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.matrixStarInterconnect
import model.resources.processing.operator.*
import rs.rs2.cgra.cgraConfigurations.SharedCgraConfig.applyCommonConfig
import scar.ScarFormat

/**
 * Use this CGRA-Config for the energy efficient variant. Use of this "efficiency" config is already configured in all tests.
 *
 * You can copy from or compare with the Std CGRAs as you wish.
 */
class EnergyFocused: ICgraSchedulerModelProvider {
    override val name: String
        get() = "energy"

    override fun invoke(): ICgraSchedulerModel {
        val grid = PeGrid(2, 4)

        grid.matrixStarInterconnect();
        // grid.matrixInterconnect();

        return grid.cgraConfigurator(name) {

            val evenColumnPEs = grid.filterByCoordinate { pe, x, y ->
                x % 2 == 0
            }

            val unevenColumnPEs = grid.filterByCoordinate { pe, x, y ->
                x % 2 != 0
            }

            operatorsFor(unevenColumnPEs) {
                // contents of all32BitOperators()
                +ADDSUB(ScarFormat.INT) // Addition, Subtraction
                +NEG(ScarFormat.INT) // K2 Negate
                +MUL(ScarFormat.INT) // Multiply
                +DIVREMInt() // Divide, Remainder
                +AND(ScarFormat.RAW32) // Binary And
                +OR(ScarFormat.RAW32) // Binary Or
                +XOR(ScarFormat.RAW32) // Binary XOr
                +NOT(ScarFormat.RAW32) // Binary Negate
                +CMP(ScarFormat.INT, true) // Main Signed Integer comparisons (>,>=,<,<=,==, !=)
                +CMP(ScarFormat.INT, false) // More comparisons with 32bit result. Maybe needed for complicated conditions
                +UCMP(ScarFormat.UINT, true) //
                +UCMP(ScarFormat.UINT, false) // Main Unsigned Integer comparisons (>,>=,<,<=,==, !=)
                +SHL(ScarFormat.INT) // Shift left
                +SHR(ScarFormat.INT) // Shift Right (arithmetic)
                // +I2B() // Convert Integer to Byte
                // ------------ OR ----------------
//                all32BitIntegerOperators() // could be used instead of typing up above operators manually


                // contents of defaultSinglePrecisionFloatOperators()
                +I2F() // Convert Integer to Float
                +F2I() // Convert Float to Integer
                +ADDSUB(ScarFormat.FLOAT) // Addition, Subtraction
                +NEG(ScarFormat.FLOAT) // Negate - almost no float negations needed
                +MUL(ScarFormat.FLOAT) // Multiply
                +DIVFLOAT() // Divide - only a few float divisions, none of them inside a loop
                //+SQRTFLOAT() // SquareRoot
                +CMP(ScarFormat.FLOAT, false) // Main Float comparisons (>,>=,<,<=,==,!=)
                // ------------ OR ----------------
//                defaultSinglePrecisionFloatOperators() // could be used instead of typing up above operators manually
            }

            operatorsFor(evenColumnPEs) {
                // contents of all32BitOperators()
                +ADDSUB(ScarFormat.INT) // Addition, Subtraction
                +NEG(ScarFormat.INT) // K2 Negate
                +MUL(ScarFormat.INT) // Multiply
                +DIVREMInt() // Divide, Remainder
                // +AND(ScarFormat.RAW32) // Binary And
                // +OR(ScarFormat.RAW32) // Binary Or
                // +XOR(ScarFormat.RAW32) // Binary XOr
                // +NOT(ScarFormat.RAW32) // Binary Negate
                // +CMP(ScarFormat.INT, true) // Main Signed Integer comparisons (>,>=,<,<=,==, !=)
                // +CMP(ScarFormat.INT, false) // More comparisons with 32bit result. Maybe needed for complicated conditions
                // +UCMP(ScarFormat.UINT, true) //
                // +UCMP(ScarFormat.UINT, false) // Main Unsigned Integer comparisons (>,>=,<,<=,==, !=)
                +SHL(ScarFormat.INT) // Shift left
                +SHR(ScarFormat.INT) // Shift Right (arithmetic)
                // +I2B() // Convert Integer to Byte
                // ------------ OR ----------------
//                all32BitIntegerOperators() // could be used instead of typing up above operators manually


                // contents of defaultSinglePrecisionFloatOperators()
                +I2F() // Convert Integer to Float
                +F2I() // Convert Float to Integer
                +ADDSUB(ScarFormat.FLOAT) // Addition, Subtraction
                // +NEG(ScarFormat.FLOAT) // Negate - almost no float negations needed
                +MUL(ScarFormat.FLOAT) // Multiply
                // +DIVFLOAT() // Divide - only a few float divisions, none of them inside a loop
                //+SQRTFLOAT() // SquareRoot
                +CMP(ScarFormat.FLOAT, false) // Main Float comparisons (>,>=,<,<=,==,!=)
                // ------------ OR ----------------
//                defaultSinglePrecisionFloatOperators() // could be used instead of typing up above operators manually
            }

            // operatorsFor(evenColumnPEs) {
            //     // contents of all32BitOperators()
            //     +ADDSUB(ScarFormat.INT) // Addition, Subtraction
            //     +NEG(ScarFormat.INT) // K2 Negate
            //     +MUL(ScarFormat.INT) // Multiply
            //     +AND(ScarFormat.RAW32) // Binary And
            //     +OR(ScarFormat.RAW32) // Binary Or
            //     +XOR(ScarFormat.RAW32) // Binary XOr
            //     +NOT(ScarFormat.RAW32) // Binary Negate
            //     +CMP(ScarFormat.INT, true) // Main Signed Integer comparisons (>,>=,<,<=,==, !=)
            //     +CMP(ScarFormat.INT, false) // More comparisons with 32bit result. Maybe needed for complicated conditions
            //     +UCMP(ScarFormat.UINT, true) //
            //     +UCMP(ScarFormat.UINT, false) // Main Unsigned Integer comparisons (>,>=,<,<=,==, !=)
            //     +I2B() // Convert Integer to Byte


            //     +ADDSUB(ScarFormat.FLOAT) // Addition, Subtraction
            //     +NEG(ScarFormat.FLOAT) // Negate
            //     +MUL(ScarFormat.FLOAT) // Multiply
            //     +CMP(ScarFormat.FLOAT, false) // Main Float comparisons (>,>=,<,<=,==,!=)//
            // }

            operatorsFor(grid[1,1], grid[0,2]) {
                +Trigonometric.SINCOS(ScarFormat.FLOAT)
            }

            // Memory PEs
            // operatorsFor(grid[2, 0], grid[1, 3]) {
            operatorsFor(grid[0, 1]) {
                +RandomAccessMemory(false, true, 32, true) // load and store operations in signed, unsigned, 32, 16 and 8 bit
                // ------------ OR ----------------
//                memoryOperators() // could be used instead of typing up above operator manually
            }

            useCBox {
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
