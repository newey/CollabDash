package factories

import databases.CollabDB

/**
 * Created by robert on 25/09/2014.
 */
class TopicModel (description: String, factoryId: Long, dataSourceInstance: Int,
                  topics: Array[Array[Double]]) extends Serializable {
  val getDescription = description
  val getFactoryId = factoryId
  val numTopics = if (topics == null || topics.size == 0) {0} else {topics(0).size}
  def dataStore = CollabDB.getDataSource(dataSourceInstance)
  val getTopics = topics
}
