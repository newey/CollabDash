package evaluation.util

import datasources.PreferenceArrayDataModelBuilder
import factories.Preference
import org.apache.mahout.cf.taste.model.DataModel

import scala.util.Random


/**
 * Created by robert on 26/09/2014.
 */
class TrainTestSplitter extends TrainTestData {
  // True is train, false is test
  var train: Option[DataModel] = None
  var test: Option[Array[Preference]] = None
  var trainSetSize: Int = 0
  var testSetSize: Int = 0

  def this (preferences: Array[Preference], testProportion: Double, numUsers: Int) = {
    this()
    val rand = new Random()
    rand.setSeed(0)
    val randAssigned = preferences.map((_, rand.nextDouble()))
    val partitioned = randAssigned.partition(x => x._2 > testProportion)
    val trainSet = partitioned._1.map(x => x._1)
    val testSet = partitioned._2.map(x => x._1)

    trainSetSize = trainSet.size
    testSetSize = testSet.size
    train = Some(PreferenceArrayDataModelBuilder.build(trainSet, numUsers))
    test = Some(testSet)
  }

  override def getTestData: Array[Preference] = test.get

  override def getTrainData: DataModel = train.get

  override def testSize: Int = testSetSize
  override def trainSize: Int = trainSetSize
}
