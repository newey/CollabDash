package collabfiltermodels.UserSimilarityFactory

import factories.{CollabFilterModel, CollabFilterModelFactory}
import factories.util.{ParamType, CollabDashParameters, FactoryParameters, Param}
import org.apache.mahout.cf.taste.eval.RecommenderBuilder
import org.apache.mahout.cf.taste.impl.neighborhood.{ThresholdUserNeighborhood, NearestNUserNeighborhood}
import org.apache.mahout.cf.taste.impl.recommender.{GenericUserBasedRecommender, GenericItemBasedRecommender}
import org.apache.mahout.cf.taste.impl.similarity._
import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.similarity.{UserSimilarity, ItemSimilarity}

import scala.collection.immutable.WrappedString

/**
 * Created by robert on 29/09/2014.
 */
object UserSimilarityFactory extends CollabFilterModelFactory {
  override val name: String = "UserSimilarityCF"

  override protected def baseBuild(fp: FactoryParameters, cdp: CollabDashParameters): CollabFilterModel = {
    val simTypes = Array[String]("CityBlock", "Euclidean", "LogLikelihood", "PearsonCorrelation", "Tanimoto", "UncenteredCosine", "Spearman")
    val threshTypes = Array[String]("NearestNUsers", "SimilarityThreshold")
    val simNum = fp.getParam(1).getIntValue
    val dataId = fp.getParam(0).getIntValue
    val threshType = fp.getParam(2).getIntValue
    val threshVal: Double = fp.getParam(3).getDoubleValue
    val desc = new WrappedString("UserBasedCF:%s:%s:%f").format(simTypes(simNum), threshTypes(threshType), threshVal)

    new CollabFilterModel(desc, 65648656383L, dataId) {
      override def getBuilder: RecommenderBuilder = {
        new RecommenderBuilder {
          override def buildRecommender(data: DataModel): Recommender = {
            val sim: UserSimilarity = if (simNum == 0) {
              new CityBlockSimilarity(data)
            } else if (simNum == 1) {
              new EuclideanDistanceSimilarity(data)
            } else if (simNum == 2) {
              new LogLikelihoodSimilarity(data)
            } else if (simNum == 3) {
              new PearsonCorrelationSimilarity(data)
            } else if (simNum == 4) {
              new TanimotoCoefficientSimilarity(data)
            } else if (simNum == 5) {
              new UncenteredCosineSimilarity(data)
            } else if (simNum == 6) {
              new SpearmanCorrelationSimilarity(data)
            } else {
              new EuclideanDistanceSimilarity(data)
            }
            val thresh = if (threshType == 0) {
              new NearestNUserNeighborhood(threshVal.toInt, sim, data)
            } else {
              new ThresholdUserNeighborhood(threshVal, sim, data)
            }

            new GenericUserBasedRecommender(data, thresh, sim)
          }
        }
      }
    }
  }

  override val description: String = "Creates a GenericUserBasedRecommender"


  override protected val params = Array(
    new Param(
      "Data source instance number",
      "The integer id of the data source instance",
      ParamType.DataSource),
    new Param(
      "Similarity type",
      "(0) CityBlock, (1) Euclidean, (2) LogLikelihood, (3) PearsonCorrelation, (4) Tanimoto, (5) UncenteredCosine, (6) SpearmanCorrelation",
      ParamType.Int
    ),
    new Param(
      "Neighbourhood type",
      "(0) Nearest N Users, (1) Similarity Threshold",
      ParamType.Int
    ),
    new Param(
      "Neighbourhood param",
      "N users / Sim threshold",
      ParamType.Float
    )
  )
}
