package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.WrapperMemoryInfo
import de.tu_darmstadt.rs.cgra.scheduling.flow.BaseCgraResourceModelConfigurator

object SharedCgraConfig {

    fun buildWrapperConfig() = WrapperMemoryInfo(512, 512, 128)

    fun BaseCgraResourceModelConfigurator.applyCommonConfig() {

        applyGlobalConfiguration {
            cBox.isCombinatorialBSS = true
        }
    }
}