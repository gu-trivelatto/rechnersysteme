package rs.rs2.cgra.operatorCollections

import de.tu_darmstadt.rs.cgra.schedulerModel.builder.IOperatorAdder
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
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.NativeMemoryOperations
import de.tu_darmstadt.rs.cgra.scheduling.flow.ILegacyOperatorAdder
import model.resources.processing.operator.*
import scar.Format
import scar.ScarFormat

fun ILegacyOperatorAdder.all32BitIntegerOperators() {
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
    +I2B() // Convert Integer to Byte
}

fun ILegacyOperatorAdder.defaultSinglePrecisionFloatOperators() {
    +I2F() // Convert Integer to Float
    +F2I() // Convert Float to Integer
    +ADDSUB(ScarFormat.FLOAT) // Addition, Subtraction
    +NEG(ScarFormat.FLOAT) // Negate
    +MUL(ScarFormat.FLOAT) // Multiply
    +DIVFLOAT() // Divide
    +SQRTFLOAT() // SquareRoot
    +CMP(ScarFormat.FLOAT, false) // Main Float comparisons (>,>=,<,<=,==,!=)
}

fun ILegacyOperatorAdder.memoryOperators() {
    +RandomAccessMemory(false, true, 32, true) // load and store operations in signed, unsigned, 32, 16 and 8 bit
}

fun IOperatorAdder.all32BitIntegerOperators() {
    +IntegerCoreOperations(includeReverseComparisons = true)
    +ChunkMuxAndRouteOperations
    +ChunkLogicOperations
    +IntegerShiftOperations
    +IntegerMultiplyOperations(upperResultOps = false)
    +IntegerDivisionOperations
}

fun IOperatorAdder.defaultSinglePrecisionFloatOperators() {
    +FloatConversions(withUnsigned = true)
    +FloatAddSub
    +FloatLogic
    +FloatMultiply
    +FloatDivision
    +FloatSqrt
    +FloatComparisons
}

fun IOperatorAdder.memoryOperators() {
    +NativeMemoryOperations(hasOffsetOperand = true)
}