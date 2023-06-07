package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.matrixInterconnect
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider
import de.tu_darmstadt.rs.cgra.scheduling.flow.PeCube
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.cgraConfigurator
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.fp.FloatTrigonometryOperations
import rs.rs2.cgra.cgraConfigurations.SharedCgraConfig.applyCommonConfig
import rs.rs2.cgra.operatorCollections.all32BitIntegerOperators
import rs.rs2.cgra.operatorCollections.defaultSinglePrecisionFloatOperators
import rs.rs2.cgra.operatorCollections.memoryOperators

class Std2x2x2Matrix2Mem1CondPe256r4096cFloat : ICgraSchedulerModelProvider {
    override val name: String
        get() = "2x2x2_Matrix_2memPorts_combBss_256r_4096c_float"

    override fun invoke(): ICgraSchedulerModel {
        val cube = PeCube(2, 2, 2)

        cube.matrixInterconnect()

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