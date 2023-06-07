package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.matrixStarInterconnect
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.fp.FloatTrigonometryOperations
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.PeGrid
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.cgraConfigurator
import rs.rs2.cgra.cgraConfigurations.SharedCgraConfig.applyCommonConfig
import rs.rs2.cgra.operatorCollections.all32BitIntegerOperators
import rs.rs2.cgra.operatorCollections.defaultSinglePrecisionFloatOperators
import rs.rs2.cgra.operatorCollections.memoryOperators

class Toroidal4x4MatrixStar2MemFloat : ICgraSchedulerModelProvider {
    override val name: String
        get() = "4x4_toroidal_2memPorts_float"

    override fun invoke(): ICgraSchedulerModel {
        val grid = PeGrid(4, 4)

        grid.matrixStarInterconnect()

        val rightBorder = grid.width-1
        val bottomBorder = grid.height-1
        grid.forEachCoordinate { pe, x, y, id ->
            when {
                x == 0 -> pe.addInterconnectInput(grid[rightBorder, y])
                x == rightBorder -> pe.addInterconnectInput(grid[0, y])
                y == 0 -> pe.addInterconnectInput(grid[x, bottomBorder])
                y == bottomBorder -> pe.addInterconnectInput(grid[x, 0])
            }
        }

        return grid.cgraConfigurator(name) {

            operatorsForAllDataPes {
                all32BitIntegerOperators()
                defaultSinglePrecisionFloatOperators()

                +FloatTrigonometryOperations
            }

            // Memory PEs
            operatorsFor(grid[2, 0], grid[1, 3]) {
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