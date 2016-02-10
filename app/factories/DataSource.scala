package factories


import databases.InstanceType
import datasources.PreferenceArrayDataModelBuilder
import factories.util.InstanceBase
import org.apache.mahout.cf.taste.model.DataModel


class DataSource (description: String, factoryId: Long,
                  users: Array[String], items: Array[String], terms: Array[String],
                  documents: Array[Document], preferences: Array[Preference])
  extends InstanceBase(description, factoryId, InstanceType.dataSource) {
  private val numUsers = users.size
  private val numItems = items.size
  private val numTerms = terms.size

  def getNumUsers: Int =  numUsers
  def getNumItems: Int = numItems
  def getNumWords: Int = numTerms
  def getNumPreferences: Int = preferences.size
  def getTermStr(termIndex: Int): String = terms(termIndex)
  def getItemStr(itemIndex: Int): String = items(itemIndex)
  def getUserStr(userIndex: Int): String = users(userIndex)
  def getCorpus: Array[Document] = documents
  def getPreferences: Array[Preference] = preferences
  def getDataModel: DataModel = PreferenceArrayDataModelBuilder.build(preferences, numUsers)
}



case class Preference(user: Int, item: Int, rating: Float) extends Serializable

case class Term(term: Int, count: Int) extends Serializable

case class Document(index: Int, terms: Array[Term]) extends Serializable