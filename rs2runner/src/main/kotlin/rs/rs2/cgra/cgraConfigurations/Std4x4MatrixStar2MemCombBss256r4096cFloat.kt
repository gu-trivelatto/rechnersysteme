package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.schedulerModel.hdl.ICgraHdlGenerationModel
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraHdlGenerationModelProvider
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.INativeWrapperModel
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.WrapperMemoryInfo
import de.tu_darmstadt.rs.cgra.scheduling.flow.PeGrid
import de.tu_darmstadt.rs.cgra.scheduling.flow.SampleCgraConfigProvider
import de.tu_darmstadt.rs.cgra.scheduling.flow.cgraConfigurator
import de.tu_darmstadt.rs.cgra.scheduling.flow.matrixStarInterconnect
import model.resources.processing.operator.Trigonometric
import rs.rs2.cgra.cgraConfigurations.SharedCgraConfig.applyCommonConfig
import rs.rs2.cgra.operatorCollections.all32BitIntegerOperators
import rs.rs2.cgra.operatorCollections.defaultSinglePrecisionFloatOperators
import rs.rs2.cgra.operatorCollections.memoryOperators
import scar.Format

class Std4x4MatrixStar2MemCombBss256r4096cFloat : ICgraHdlGenerationModelProvider {
    override val name: String
        get() = "4x4_MatrixStar_2memPorts_combBss_256r_4096c_float"

    override fun invoke(): ICgraHdlGenerationModel {
        val grid = PeGrid(4, 4)

        grid.matrixStarInterconnect()

        return grid.cgraConfigurator(name) {

            operatorsForAllDataPes {
                all32BitIntegerOperators()
                defaultSinglePrecisionFloatOperators()

                +Trigonometric.SINCOS(Format.FLOAT)
            }

            // Memory PEs
            operatorsFor(grid[2, 0], grid[1, 3]) {
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