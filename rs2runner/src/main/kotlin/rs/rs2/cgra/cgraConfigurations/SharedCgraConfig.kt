package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.schedulerModel.builder.ICgraModelConfigurator
import de.tu_darmstadt.rs.cgra.schedulerModel.pureImpl.kernelMgmt.WrapperMemoryInfo

object SharedCgraConfig {

    fun buildWrapperConfig() = WrapperMemoryInfo(512, 512, 128)

    fun ICgraModelConfigurator<*, *>.applyCommonConfig() {

        useCBox {
            isCombinatorialBSS = true
        }
    }
}