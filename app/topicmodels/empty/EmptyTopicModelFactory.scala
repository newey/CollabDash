package topicmodels.empty

import factories._
import factories.util.{ParamType, Param, FactoryParameters, CollabDashParameters}
import utilities.Timing

/**
 * Created by robert on 25/09/2014.
 */
object EmptyTopicModelFactory extends TopicModelFactory {
  override val name: String = "Empty-Topic-Model"

  override val description: String = "A topic model with no topics"

  override protected val params = Array(
    new Param(
      "Data source instance number",
      "The integer id of the data source instance",
      ParamType.DataSource)
  )

  override protected def baseBuild(fp: FactoryParameters, cdp: CollabDashParameters): TopicModel = {
    val dsNum = fp.getParam(0).getIntValue
    val (model, micros) = Timing(
      new TopicModel("emptyTopicModel-dataSource:" +dsNum.toString, 7705736575635L, dsNum, null))
    model.setComputeTime(micros)
    model
  }
}
