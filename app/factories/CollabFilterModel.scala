package factories

import databases.CollabDB
import org.apache.mahout.cf.taste.eval.RecommenderBuilder

/**
 * Created by robert on 26/09/2014.
 */
abstract class CollabFilterModel (description: String, factoryId: Long, dataSourceInstance: Int) extends Serializable {
  val getDescription = description
  val getFactoryId = factoryId
  def dataStore = CollabDB.getDataSource(dataSourceInstance)
  def getBuilder: RecommenderBuilder
}