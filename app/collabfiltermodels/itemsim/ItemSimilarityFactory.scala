package collabfiltermodels.itemsim

import factories.{CollabFilterModel, CollabFilterModelFactory}
import factories.util.{ParamType, CollabDashParameters, FactoryParameters, Param}
import org.apache.mahout.cf.taste.eval.RecommenderBuilder
import org.apache.mahout.cf.taste.impl.recommender.GenericItemBasedRecommender
import org.apache.mahout.cf.taste.impl.similarity._
import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.Recommender
import org.apache.mahout.cf.taste.similarity.ItemSimilarity

/**
 * Created by robert on 29/09/2014.
 */
object ItemSimilarityFactory extends CollabFilterModelFactory {
  override val name: String = "ItemSimilarityCF"

  override protected def baseBuild(fp: FactoryParameters, cdp: CollabDashParameters): CollabFilterModel = {
    val simTypes = Array[String]("CityBlock", "Euclidean", "LogLikelihood", "PearsonCorrelation", "Tanimoto", "UncenteredCosine")
    val simNum = fp.getParam(1).getIntValue
    val dataId = fp.getParam(0).getIntValue
    new CollabFilterModel("ItemBasedCF:"+simTypes(simNum), 234561658484L, dataId) {
      override def getBuilder: RecommenderBuilder = {
        new RecommenderBuilder {
          override def buildRecommender(data: DataModel): Recommender = {
            val sim: ItemSimilarity = if (simNum == 0) {
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
            } else {
              new EuclideanDistanceSimilarity(data)
            }
            new GenericItemBasedRecommender(data, sim)
          }
        }
      }
    }
  }

  override val description: String = "Creates a GenericItemBasedRecommender"


  override protected val params = Array(
    new Param(
      "Data source instance number",
      "The integer id of the data source instance",
      ParamType.DataSource),
    new Param(
      "Similarity type",
      "(0) CityBlock, (1) Euclidean, (2) LogLikelihood, (3) PearsonCorrelation, (4) Tanimoto, (5) UncenteredCosine",
      ParamType.Int
    )
  )
}
