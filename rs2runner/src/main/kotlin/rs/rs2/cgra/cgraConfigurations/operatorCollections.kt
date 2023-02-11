package rs.rs2.cgra.operatorCollections

import de.tu_darmstadt.rs.cgra.scheduling.flow.ILegacyOperatorAdder
import model.resources.processing.operator.*
import scar.Format

fun ILegacyOperatorAdder.all32BitIntegerOperators() {
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
}

fun ILegacyOperatorAdder.defaultSinglePrecisionFloatOperators() {
    +I2F() // Convert Integer to Float
    +F2I() // Convert Float to Integer
    +ADDSUB(Format.FLOAT) // Addition, Subtraction
    +NEG(Format.FLOAT) // Negate
    +MUL(Format.FLOAT) // Multiply
    +DIVFLOAT() // Divide
    +SQRTFLOAT() // SquareRoot
    +CMP(Format.FLOAT, false) // Main Float comparisons (>,>=,<,<=,==,!=)
}

fun ILegacyOperatorAdder.memoryOperators() {
    +RandomAccessMemory(false, true, 32, true) // load and store operations in signed, unsigned, 32, 16 and 8 bit
}