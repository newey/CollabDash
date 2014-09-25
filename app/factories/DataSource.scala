package factories

import java.util

import org.apache.mahout.cf.taste.impl.common.FastByIDMap
import org.apache.mahout.cf.taste.impl.model.{GenericDataModel, GenericPreference}
import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.cf.taste.model


class DataSource (description: String, factoryId: Long,
                  users: Array[String], items: Array[String], terms: Array[String],
                  documents: Array[Document], preferences: Array[Preference]) extends Serializable {
  private val numUsers = users.size
  private val numItems = items.size
  private val numTerms = terms.size

  def getDescription: String = description
  def getNumUsers: Int =  numUsers
  def getNumItems: Int = numItems
  def getNumWords: Int = numTerms
  def getNumPreferences: Int = preferences.size
  def getTermStr(termIndex: Int): String = terms(termIndex)
  def getItemStr(itemIndex: Int): String = items(itemIndex)
  def getUserStr(userIndex: Int): String = users(userIndex)
  def getCorpus: Array[Document] = documents
  def getFactoryId: Long = factoryId

  def getDataModel: DataModel = {
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
    x.zipWithIndex.foreach(x => newCollection.set(x._2, x._1))
    newCollection
  }
}



case class Preference(user: Int, item: Int, rating: Float) extends Serializable

case class Term(term: Int, count: Int) extends Serializable

case class Document(index: Int, terms: Array[Term]) extends Serializable