package collabfiltermodels.guesszero

import java.util

import factories._
import factories.util._
import org.apache.mahout.cf.taste.common.Refreshable
import org.apache.mahout.cf.taste.eval.RecommenderBuilder
import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.recommender.{IDRescorer, RecommendedItem, Recommender}

/**
 * Created by robert on 26/09/2014.
 */
object GuessZeroFactory extends CollabFilterModelFactory {
  val name: String = "Guess Zero Model"
  val description: String = "Gives the model which always guesses zero"

  protected def baseBuild(fp: FactoryParameters, cdp: CollabDashParameters): CollabFilterModel = {
    val factId: Long = 346561834632L
    val dataId = fp.getParam(0).getIntValue
    val descr = "GuessZeroModel-DataModel:"+dataId.toString
    new CollabFilterModel(descr, factId, dataId) {
      override def getBuilder: RecommenderBuilder = new RecommenderBuilder {
        override def buildRecommender(dataModel: DataModel): Recommender = {
          new Recommender{
            override def recommend(p1: Long, p2: Int): util.List[RecommendedItem] = ???

            override def recommend(p1: Long, p2: Int, p3: IDRescorer): util.List[RecommendedItem] = ???

            override def removePreference(p1: Long, p2: Long): Unit = ???

            override def getDataModel: DataModel = dataModel

            override def estimatePreference(p1: Long, p2: Long): Float = 0

            override def setPreference(p1: Long, p2: Long, p3: Float): Unit = ???

            override def refresh(p1: util.Collection[Refreshable]): Unit = ???
          }
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
