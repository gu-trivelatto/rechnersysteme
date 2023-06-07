package rs.rs2.cgra.operatorCollections

import de.tu_darmstadt.rs.cgra.schedulerModel.builder.IOperatorAdder
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
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.ChunkLogicOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.ChunkMuxAndRouteOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.IntegerCoreOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.IntegerDivisionOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.IntegerMultiplyOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.gp.IntegerShiftOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.memory.NativeMemoryOperations
import de.tu_darmstadt.rs.cgra.scheduling.flow.ILegacyOperatorAdder
import model.resources.processing.operator.*
import scar.ScarFormat

fun IOperatorAdder.all32BitIntegerOperators() {
    // Latencies (bufferedRegs, bufferedX) are not allowed to be changed!
    +IntegerCoreOperations() // +, -, ==, !=, <, >=, u<, u>=
    +ChunkMuxAndRouteOperations // Passthrough & Mux(a, b). Needed for architecture to work
    +ChunkLogicOperations // 32bit And, Or, Xor
    +IntegerShiftOperations // <<, >>, >>>
    +IntegerMultiplyOperations() // multiply
    +IntegerDivisionOperations // divide, remainder
}

/**
 * Old, but tested well. Use IEEE 32 Bit Single Precision format for everything
 */
fun IOperatorAdder.defaultSinglePrecisionFloatOperators() {
    +FloatConversions(withUnsigned = true) // int2float, float2int, float2uint, uint2float
    +FloatAddSub // Add, Sub
    +FloatLogic // Absolute, Negate
    +FloatMultiply //  Multiply
    +FloatDivision // divide
    +FloatSqrt // square root
    +FloatComparisons // ==, !=, <, <=
}

/**
 * New, less tested. Uses custom 34 Bit format internally. Requires additional conversion operations converting to and from custom format. If in doubt, use [legacySinglePrecisionFloatOperators]
 */
fun IOperatorAdder.flopocoSinglePrecisionFloatOperators() {
    // Latencies (registerStages, inputBuffer, outputBuffer) are not allowed to be changed!
    +FlopocoAddSub()
    +FlopocoLogic
    +FlopocoMultiply()
    +FlopocoDivision()
    +FlopocoComparisons()
    +FlopocoF2IEEE
    +FlopocoIEEE2F
    +FlopocoF2I(signed = true, unsigned = true)
    +FlopocoI2F(signed = true, unsigned = true)
    +FlopocoSqrt()
    //+FlopocoTrigonometryOperations // add manually if needed, takes up lots of space
}

fun IOperatorAdder.memoryOperators() {
    +NativeMemoryOperations(withBarriers = false, withIntegratedOffset = true)  // load and store operations in signed, unsigned, 32, 16 and 8 bit
    // [withIntegratedOffset]: addition operation can internally do a pointer addition like (arrBase + 12). Without it, saves additional operand & registerPort, uses regular, external addition
    // [withBarriers]: not needed for the memory-model that is used
}

fun ILegacyOperatorAdder.all32BitLegacyIntegerOperators() {
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

fun ILegacyOperatorAdder.legacySinglePrecisionFloatOperators() {
    +I2F() // Convert Integer to Float
    +F2I() // Convert Float to Integer
    +ADDSUB(ScarFormat.FLOAT) // Addition, Subtraction
    +NEG(ScarFormat.FLOAT) // Negate
    +MUL(ScarFormat.FLOAT) // Multiply
    +DIVFLOAT() // Divide
    +SQRTFLOAT() // SquareRoot
    +CMP(ScarFormat.FLOAT, false) // Main Float comparisons (>,>=,<,<=,==,!=)
}

fun ILegacyOperatorAdder.legacyMemoryOperators() {
    +RandomAccessMemory(false, true, 32, true) // load and store operations in signed, unsigned, 32, 16 and 8 bit
}