package collabfiltermodels.ALSFactory

import factories.{CollabFilterModel, CollabFilterModelFactory}
import factories.util.{ParamType, CollabDashParameters, FactoryParameters, Param}
import org.apache.mahout.cf.taste.eval.RecommenderBuilder
import org.apache.mahout.cf.taste.impl.recommender.svd.{ALSWRFactorizer, SVDRecommender}
import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.Recommender

import scala.collection.immutable.WrappedString

/**
 * Created by robert on 29/09/2014.
 */
object ALSFactory extends CollabFilterModelFactory {
  override val name: String = "AlternatingLeastSquares"

  override protected def baseBuild(fp: FactoryParameters, cdp: CollabDashParameters): CollabFilterModel = {
    val dataId = fp.getParam(0).getIntValue
    val numFeatures = fp.getParam(1).getIntValue
    val lambda = fp.getParam(2).getDoubleValue
    val numIterations = fp.getParam(3).getIntValue
    val desc = new WrappedString("ALS:%d:%f:%d").format(numFeatures, lambda, numIterations)

    new CollabFilterModel(desc, 4517346567378L, dataId) {
      override def getBuilder: RecommenderBuilder = {
        new RecommenderBuilder {
          override def buildRecommender(p1: DataModel): Recommender = {
            val factorizer = new ALSWRFactorizer(p1, numFeatures ,lambda, numIterations)
            new SVDRecommender(p1, factorizer)
          }
        }
      }
    }
  }

  override val description: String = "CF via alternating least squares"
  override protected val params: Array[Param] = Array(
    new Param(
      "Data source instance number",
      "The integer id of the data source instance",
      ParamType.DataSource),
    new Param(
      "Number of features",
      "",
      ParamType.Int
    ),
    new Param(
      "Lambda",
      "",
      ParamType.Float
    ),
    new Param(
      "NumIterations",
      "",
      ParamType.Int
    )
  )
}
