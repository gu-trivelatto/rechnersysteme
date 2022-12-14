package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.hdlModel.api.ICgraHdlGenerationModel
import de.tu_darmstadt.rs.cgra.hdlModel.serviceLoader.ICgraHdlGenerationModelProvider
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.INativeWrapperModel
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.WrapperMemoryInfo
import de.tu_darmstadt.rs.cgra.scheduling.flow.PeCube
import de.tu_darmstadt.rs.cgra.scheduling.flow.SampleCgraConfigProvider
import de.tu_darmstadt.rs.cgra.scheduling.flow.cgraConfigurator
import de.tu_darmstadt.rs.cgra.scheduling.flow.matrixStarInterconnect
import model.resources.processing.operator.Trigonometric
import rs.rs2.cgra.cgraConfigurations.SharedCgraConfig.applyCommonConfig
import rs.rs2.cgra.operatorCollections.all32BitIntegerOperators
import rs.rs2.cgra.operatorCollections.defaultSinglePrecisionFloatOperators
import rs.rs2.cgra.operatorCollections.memoryOperators
import scar.Format

class Std2x2x2MatrixStar2MemCombBss256r4096cInt : ICgraHdlGenerationModelProvider {
    override val name: String
        get() = "2x2x2_MatrixStar_2memPorts_combBss_256r_4096c_int"

    override fun invoke(): ICgraHdlGenerationModel {
        val cube = PeCube(2, 2, 2)

        cube.matrixStarInterconnect()

        return cube.cgraConfigurator(name) {

            operatorsForAllDataPes {
                all32BitIntegerOperators()
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

    override fun getNativeWrapperMemoryInfo(): INativeWrapperModel {
        return WrapperMemoryInfo(512, 512, 128)
    }
}

