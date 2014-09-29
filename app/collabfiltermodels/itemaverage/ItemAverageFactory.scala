package collabfiltermodels.itemaverage

import factories._
import factories.util._
import org.apache.mahout.cf.taste.eval.RecommenderBuilder
import org.apache.mahout.cf.taste.impl.recommender.ItemAverageRecommender
import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.Recommender

/**
 * Created by robert on 26/09/2014.
 */
object ItemAverageFactory extends CollabFilterModelFactory {
  val name: String = "Item Average Model"
  val description: String = "Gives the model which always guesses the items average"

  protected def baseBuild(fp: FactoryParameters, cdp: CollabDashParameters): CollabFilterModel = {
    val factId: Long = 4188313047372L
    val dataId = fp.getParam(0).getIntValue
    val descr = "ItemAverageModel-DataModel:"+dataId.toString
    new CollabFilterModel(descr, factId, dataId) {
      override def getBuilder: RecommenderBuilder = new RecommenderBuilder {
        override def buildRecommender(p1: DataModel): Recommender = {
          new ItemAverageRecommender(p1)
        }
      }
    }
  }

  override protected val params = Array(
    new Param(
      "Data source instance number",
      "The integer id of the data source instance",
      ParamType.DataSource)
  )
}
