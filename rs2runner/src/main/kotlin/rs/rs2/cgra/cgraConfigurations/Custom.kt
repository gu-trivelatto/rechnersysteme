 package rs.rs2.cgra.cgraConfigurations

import de.tu_darmstadt.rs.cgra.schedulerModel.ICgraSchedulerModel
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider
import de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.INativeWrapperModel
import de.tu_darmstadt.rs.cgra.scheduling.flow.SampleCgraConfigProvider

 /**
  * You can create new models as you wish to quickly swap between different variants and to try things out.
  * Each must have a unique [name], which is also how you would pick this specific model from the commandLine (´--cgra custom´ for this one)
  *
  * Must be registerd in `resources/META-INF/services/de.tu_darmstadt.rs.cgra.schedulerModel.serviceLoader.ICgraSchedulerModelProvider`
  */
 class Custom: ICgraSchedulerModelProvider {
    override val name: String
        get() = "custom"

    override fun invoke(): ICgraSchedulerModel {
        TODO("No model defined for Model $name")
    }

    override fun getNativeWrapperMemoryInfo(): INativeWrapperModel {
        return SharedCgraConfig.buildWrapperConfig()
    }
}