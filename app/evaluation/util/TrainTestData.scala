package evaluation.util

import factories.Preference
import org.apache.mahout.cf.taste.model.DataModel

/**
 * Created by robert on 26/09/2014.
 */
trait TrainTestData {
  def getTestData: Array[Preference]
  def getTrainData: DataModel
  def testSize: Int
  def trainSize: Int
}
