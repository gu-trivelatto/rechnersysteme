package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.matrixStarInterconnect
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.fp.FloatTrigonometryOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.PeCube
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.cgraConfigurator
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider
import rs.rs2.cgra.cgraConfigurations.SharedCgraConfig.applyCommonConfig
import rs.rs2.cgra.operatorCollections.all32BitIntegerOperators
import rs.rs2.cgra.operatorCollections.defaultSinglePrecisionFloatOperators
import rs.rs2.cgra.operatorCollections.memoryOperators

class Std2x2x2MatrixStar2Mem1CondPe256r4096cFloat : ICgraSchedulerModelProvider {
    override val name: String
        get() = "2x2x2_MatrixStar_2memPorts_1condPe_256r_4096c_float"

    override fun invoke(): ICgraSchedulerModel {
        val cube = PeCube(2, 2, 2)

        cube.matrixStarInterconnect()

        return cube.cgraConfigurator(name) {

            operatorsForAllDataPes {
                all32BitIntegerOperators()
                defaultSinglePrecisionFloatOperators()

                +FloatTrigonometryOperations
            }

            // Memory PEs
            operatorsFor(cube[0, 0, 0], cube[1, 1, 1]) {
                memoryOperators()
            }

            useCondPEs {
                condPeCount = 1
            }
            setDefaultDataPeRegFileSize(256)
            allLcus {
                memorySize = 4096
            }

            applyCommonConfig()
        }
    }

    override fun getNativeWrapperMemoryInfo() = SharedCgraConfig.buildWrapperConfig()
}