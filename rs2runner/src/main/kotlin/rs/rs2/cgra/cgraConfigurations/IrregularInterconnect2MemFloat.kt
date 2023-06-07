package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.PeGrid
import de.tu_darmstadt.rs.cgra.schedulerModel.builder.cgraConfigurator
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.dataPe.fp.FloatTrigonometryOperations
import rs.rs2.cgra.cgraConfigurations.SharedCgraConfig.applyCommonConfig
import rs.rs2.cgra.operatorCollections.all32BitIntegerOperators
import rs.rs2.cgra.operatorCollections.defaultSinglePrecisionFloatOperators
import rs.rs2.cgra.operatorCollections.memoryOperators

class IrregularInterconnect2MemFloat : ICgraSchedulerModelProvider {
    override val name: String
        get() = "irregular_interconnect_2mem_float"

    override fun invoke(): ICgraSchedulerModel {
        val grid = PeGrid(2, 3)

        // First row directly connected
        grid[0, 0].addInterconnectInput(grid[1, 0])
        grid[1, 0].addInterconnectInput(grid[0, 0])

        // second row connected to first row, same column and reverse
        grid[0, 1].addInterconnectInput(grid[0, 0])
        grid[1, 1].addInterconnectInput(grid[1, 0])
        grid[0, 0].addInterconnectInput(grid[0, 1])
        grid[1, 0].addInterconnectInput(grid[1, 1])
        // second row connected to each other
        grid[0, 1].addInterconnectInput(grid[1, 1])
        grid[1, 1].addInterconnectInput(grid[0, 1])

        // third row connected to second row, straight & diagonal and reverse
        grid[0, 2].addInterconnectInput(grid[0, 1])
        grid[1, 2].addInterconnectInput(grid[1, 1])
        grid[0, 2].addInterconnectInput(grid[1, 1])
        grid[1, 2].addInterconnectInput(grid[0, 1])
        grid[0, 1].addInterconnectInput(grid[0, 2])
        grid[1, 1].addInterconnectInput(grid[1, 2])
        grid[1, 1].addInterconnectInput(grid[0, 2])
        grid[0, 1].addInterconnectInput(grid[1, 2])

        // third row connected to each other
        grid[0, 2].addInterconnectInput(grid[1, 2])
        grid[1, 2].addInterconnectInput(grid[0, 2])

        val cgraModel = grid.cgraConfigurator(name) {

            operatorsForAllDataPes {
                all32BitIntegerOperators()
                defaultSinglePrecisionFloatOperators()

                +FloatTrigonometryOperations
            }

            // Memory PEs
            operatorsFor(grid[0, 0], grid[1, 2]) {
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

        return cgraModel
    }

    override fun getNativeWrapperMemoryInfo() = SharedCgraConfig.buildWrapperConfig()
}