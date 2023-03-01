package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.matrixInterconnect
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider
import de.tu_darmstadt.rs.cgra.scheduling.flow.PeCube
import de.tu_darmstadt.rs.cgra.scheduling.flow.cgraConfigurator
import model.resources.processing.operator.Trigonometric
import rs.rs2.cgra.cgraConfigurations.SharedCgraConfig.applyCommonConfig
import rs.rs2.cgra.operatorCollections.all32BitIntegerOperators
import rs.rs2.cgra.operatorCollections.defaultSinglePrecisionFloatOperators
import rs.rs2.cgra.operatorCollections.memoryOperators
import scar.ScarFormat

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

                +Trigonometric.SINCOS(ScarFormat.FLOAT)
            }

            // Memory PEs
            operatorsFor(cube[0, 0, 0], cube[1, 1, 1]) {
                memoryOperators()
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

    override fun getNativeWrapperMemoryInfo() = SharedCgraConfig.buildWrapperConfig()
}