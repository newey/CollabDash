package factories

import databases.{InstanceType, CollabDB}
import factories.util.InstanceBase
import org.apache.mahout.cf.taste.eval.RecommenderBuilder

/**
 * Created by robert on 26/09/2014.
 */
abstract class CollabFilterModel (description: String, factoryId: Long, dataSourceInstance: Int)
  extends InstanceBase(description, factoryId, InstanceType.cfModel) {
  def getDataSourceId = dataSourceInstance
  def dataStore = CollabDB.getDataSource(dataSourceInstance)
  def getBuilder: RecommenderBuilder
}