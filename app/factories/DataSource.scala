package factories

import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.math.Matrix

import scala.collection.immutable

trait DataSource extends Serializable {
  def getDataModel: DataModel
  def description: String
  def getNumUsers: Int
  def getNumItems: Int
  def getNumWords: Int
  def getCorpus: Array[immutable.HashMap[Int, Int]]
  def getWord(wordIndex: Int): String
  val uuid: Long
}
