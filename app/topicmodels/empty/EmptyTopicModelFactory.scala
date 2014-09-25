package topicmodels.empty

import factories._

/**
 * Created by robert on 25/09/2014.
 */
object EmptyTopicModelFactory extends TopicModelFactory {
  override def name(): String = "Empty-Topic-Model"

  override def parameters(): FactoryParameters = new FactoryParameters(params)

  override def buildTopicModel(fp: FactoryParameters, cdp: CollabDashParameters): TopicModel = {
    val dsNum = fp.getParam(0).getIntValue
    new TopicModel("emptyTopicModel-dataSource:" +dsNum.toString, 7705736575635L, dsNum, null)
  }

  override def description(): String = "A topic model with no topics"

  private val params = Array(
    new Param(
      "Data source instance number",
      "The integer id of the data source instance",
      ParamType.DataSource)
  )
}
