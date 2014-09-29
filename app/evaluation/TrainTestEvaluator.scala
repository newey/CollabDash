package evaluation

import databases.CollabDB
import evaluation.util.{TrainTestData, ScoreType, TrainTestSplitter}
import factories._
import factories.util.{ParamType, CollabDashParameters, FactoryParameters, Param}
import utilities.Timing

import scala.collection.immutable.WrappedString

/**
 * Created by robert on 27/09/2014.
 */
object TrainTestEvaluator extends EvaluationFactory {
  override val name: String = "Train Test set evaluator"
  override val description: String = "Computes the RMSE, MAE, and MeanAbsGuess of a model with some proportion split"

  override protected def baseBuild(fp: FactoryParameters, cdp: CollabDashParameters): EvaluationGroup = {
    val modelNums = fp.getParam(0).getIntArrayValue
    val testProp = fp.getParam(1).getDoubleValue
    val model = CollabDB.getCollabFilterModel(modelNums(0))
    val dataSource = model.dataStore
    val sets = new TrainTestSplitter(dataSource.getPreferences, testProp, dataSource.getNumUsers)
    val descfmt = new WrappedString("test score on %f")
    val description = descfmt.format(testProp)
    val evals = modelNums.map(modelNum => {
      val ((scores, lost), tobuild) = Timing(getEvaluation(CollabDB.getCollabFilterModel(modelNum), sets))
      Evaluation(modelNum, scores, tobuild, lost)
    }).toList
    EvaluationGroup(evals, description, sets.testSize, sets.trainSize, model.getDataSourceId)
  }

  private def getEvaluation(model: CollabFilterModel, sets: TrainTestData): (List[Score], Int) = {
    val recom = model.getBuilder.buildRecommender(sets.getTrainData)
    val estims = sets.getTestData.map[Double, Array[Double]](
      pref => try {
        recom.estimatePreference(pref.user, pref.item)
      } catch {
        case blah => {Double.NaN}
      })
    val data = (estims, sets.getTestData).zipped.filter((est, pref) => !est.isNaN())
      .zipped.map[(Double, Double), Array[(Double, Double)]]((est, pref) => (est, pref.rating))
    val shrunkSize = data.size

    printf("Lost %d to NaN\n", sets.getTestData.size-shrunkSize)
    def valUnlessEmpty(v: => Double) = if (shrunkSize == 0) {Double.NaN} else {v}
    val maeval = valUnlessEmpty(data.map(dat => Math.abs(dat._1-dat._2)).reduce(_+_)/shrunkSize)
    val rmsval = valUnlessEmpty(Math.sqrt(data.map(dat => Math.pow(dat._1-dat._2, 2)).reduce(_+_)/shrunkSize))
    val meanGuess = valUnlessEmpty(data.map(_._1).reduce(_+_)/shrunkSize)
    val varGuess = valUnlessEmpty(data.map(x=>Math.pow(x._1-meanGuess,2)).reduce(_+_)/shrunkSize)

    (List(
      Score(ScoreType.MAE, maeval),
      Score(ScoreType.RMSE, rmsval),
      Score(ScoreType.GuessMean, meanGuess),
      Score(ScoreType.GuessVariance, varGuess)
    ), sets.testSize-shrunkSize)
  }

  override protected val params = Array(
    new Param(
      "Model instance numbers",
      "The space separated integer ids of the model instances",
      ParamType.CFModelList),
    new Param(
      "Test data proportion",
      "The decimal value of the proportion of the data to use for the test set",
      ParamType.Float
    )
  )
}
