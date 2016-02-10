package factories

import databases.{InstanceType, CollabDB}
import factories.util.InstanceBase

/**
 * Created by robert on 25/09/2014.
 */
class TopicModel (description: String, factoryId: Long, dataSourceInstance: Int,
                  topics: Array[Array[Double]])
  extends InstanceBase(description, factoryId, InstanceType.topicModel) {

  val numTopics = if (topics == null || topics.size == 0) {0} else {topics(0).size}
  def dataStoreId = dataSourceInstance
  def dataStore = CollabDB.getDataSource(dataSourceInstance)
  val getTopics = topics
}
