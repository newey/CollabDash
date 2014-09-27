package evaluation

import databases.CollabDB
import evaluation.util.{ScoreType, TrainTestSplitter, ResultGroup}
import factories.EvaluationFactory
import factories.util.{ParamType, CollabDashParameters, FactoryParameters, Param}

import scala.collection.immutable.WrappedString
import scala.collection.mutable.ListBuffer

/**
 * Created by robert on 27/09/2014.
 */
object TrainTestEvaluator extends EvaluationFactory {
  override def buildEvaluation(fp: FactoryParameters, cdp: CollabDashParameters): ResultGroup = {
    val modelNum = fp.getParam(0).getIntValue
    val testProp = fp.getParam(1).getDoubleValue
    val model = CollabDB.getCollabFilterModel(modelNum)
    val dataSource = model.dataStore
    val sets = new TrainTestSplitter(dataSource.getPreferences, testProp, dataSource.getNumUsers)
    val recom = model.getBuilder.buildRecommender(sets.getTrainData)
    val testSize = sets.getTestData.size
    def average(l: List[Double]) = l.sum/l.size
    val estims = sets.getTestData.map[Double, Array[Double]](pref => recom.estimatePreference(pref.user, pref.item))
    val data = (estims, sets.getTestData).zipped.filter((est, pref) => !est.isNaN())
      .zipped.map[(Double, Double), Array[(Double, Double)]]((est, pref) => (est, pref.rating))
    val shrunkSize = data.size
    val mae = (dat: (Double, Double)) => Math.abs(dat._1-dat._2)/shrunkSize
    val rms = (dat: (Double, Double)) => Math.pow(dat._1-dat._2, 2)/shrunkSize
    val mag = (dat: (Double, Double)) => Math.abs(dat._1)/shrunkSize

    printf("Lost %d to NaN\n", testSize-shrunkSize)
    val (maeval, rmsval, magval)  =data
      .map(dat => (mae(dat), rms(dat), mag(dat)))
      .reduce((x, y) => (x._1 + y._1, x._2 + y._2, x._3 + y._3))

    val descfmt = new WrappedString("test score on %f of data for model %s")


    val scoretypes = List(
      ScoreType.MAE,
      ScoreType.RMS,
      ScoreType.MeanAbsGuess
    )
    val scores = List(
      List[Double](maeval),
      List[Double](rmsval),
      List[Double](magval)
    )
    val description = descfmt.format(testProp, model.getDescription)

    ResultGroup(modelNum, scoretypes, scores, description, sets.testSize, sets.trainSize, testSize-shrunkSize)
  }

  override def parameters(): FactoryParameters = new FactoryParameters(params)

  override def description(): String = "Computes the RMSE, MAE, and MeanAbsGuess of a model with some proportion split"

  override def name(): String = "Train Test set evaluator"

  private val params = Array(
    new Param(
      "Model instance number",
      "The integer id of the model instance",
      ParamType.CFModel),
    new Param(
      "Test data proportion",
      "The decimal value of the proportion of the data to use for the test set",
      ParamType.Float
    )
  )
}
