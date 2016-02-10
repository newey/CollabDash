package datasources

import java.util

import factories.Preference
import org.apache.mahout.cf.taste.impl.common.FastByIDMap
import org.apache.mahout.cf.taste.impl.model.{GenericDataModel, GenericPreference}
import org.apache.mahout.cf.taste.model
import org.apache.mahout.cf.taste.model.DataModel

/**
 * Created by robert on 26/09/2014.
 */
object PreferenceArrayDataModelBuilder {
  def build(preferences: Array[Preference], numUsers: Int): DataModel = {
    val userPreferenceLists = new FastByIDMap[util.Collection[model.Preference]](numUsers)


    val actualPreferences = preferences.map[model.Preference, Array[model.Preference]](x => new GenericPreference(x.user, x.item, x.rating))

    val groupedPreferences = actualPreferences.groupBy(_.getUserID)
      .mapValues[util.Collection[model.Preference]](arrayToCollection(_))

    groupedPreferences.toTraversable.foreach(x => userPreferenceLists.put(x._1, x._2))


    val userPreferences = GenericDataModel.toDataMap(userPreferenceLists, true)
    new GenericDataModel(userPreferences)
  }

  private def arrayToCollection[B] (x: Array[B]): util.Collection[B] = {
    val newCollection = new util.ArrayList[B](x.size)
    x.foreach(newCollection.add(_))
    newCollection
  }
}
