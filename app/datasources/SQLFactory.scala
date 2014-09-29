package datasources

import java.sql.Statement

import factories.util.InstanceBase
import factories.{Term, Document, DataSource, Preference}
import play.api.Play.current
import play.api.db.DB
import utilities.Timing

import scala.collection.immutable.WrappedString
import scala.collection.mutable.ArrayBuffer

abstract class SQLFactory {
  protected def ratingsQuery: String
  protected def wordsQuery: String
  protected def initQueries: Array[String]
  protected def cleanupQueries: Array[String]
  protected def description: String
  protected val factoryId: Long

  protected val wordIndex = "wordindex"
  protected val itemIndex = "itemIndex"
  protected val userIndex = "userIndex"

  private var numWords = 0
  private var numItems = 0
  private var numUsers = 0

  def getDataSource: DataSource = {
    val conn = DB.getConnection("jobs")
    try {
      val (dataSource, micros) = Timing({
        val stmt = conn.createStatement()
        stmt.setFetchSize(10000)
        multiQuery(stmt, initQueries)
        numWords = getNumWords(stmt)
        numUsers = getNumUsers(stmt)
        numItems = getNumItems(stmt)
        val words = getWordList(stmt)
        val users = getUserList(stmt)
        val items = getItemList(stmt)
        val corpus = retrieveCorpus(stmt)
        val preferences = retrievePreferences(stmt)


        multiQuery(stmt, cleanupQueries)
        new DataSource(description, factoryId, users, items, words, corpus, preferences)

      })
      dataSource.setComputeTime(micros)
      dataSource
    } finally {
      conn.close()
    }
  }

  private def multiQuery(stmt: Statement, queries: Array[String]) = queries.foreach(stmt.execute(_))

  private def retrieveCorpus(stmt: Statement): Array[Document] = {
    val corpus = Array.range(0, numItems).map(x => ArrayBuffer[Term]())
    val rs = stmt.executeQuery(wordsQuery)
    while (rs.next()) {
      val item = rs.getInt(1)
      val termNum = rs.getInt(2)
      val count = rs.getInt(3)
      corpus(item) += Term(termNum, count)
    }
    corpus.zipWithIndex.map(x => Document(x._2, x._1.toArray))
  }

  private def retrievePreferences(stmt: Statement): Array[Preference] = {
    val rs = stmt.executeQuery(ratingsQuery)
    val preferences = ArrayBuffer[Preference]()

    while (rs.next()) {
      val userNum = rs.getInt(1)
      val itemNum = rs.getInt(2)
      val rating = rs.getFloat(3)
      preferences += Preference(userNum, itemNum, rating)
    }

    preferences.toArray
  }

  private def getNumBase(stmt: Statement, tableName: String): Int = {
    val fmt = new WrappedString("SELECT COUNT(ident) FROM %s")
    val query = fmt.format(tableName)
    val rs = stmt.executeQuery(query)
    rs.next()
    rs.getInt(1)
  }

  private def getListBase(stmt: Statement, tableName: String, colName: String, length: Int): Array[String] = {
    val listingQuery = new WrappedString("SELECT %s FROM %s ORDER BY ident")
    val query = listingQuery.format(colName, tableName)
    val rs = stmt.executeQuery(query)
    val array = new Array[String](length)
    var curNum = 0
    while (rs.next()) {
      array(curNum) = rs.getString(1)
      curNum += 1
    }
    array
  }

  private def getNumWords(stmt: Statement): Int = getNumBase(stmt, wordIndex)
  private def getNumUsers(stmt: Statement): Int = getNumBase(stmt, userIndex)
  private def getNumItems(stmt: Statement): Int = getNumBase(stmt, itemIndex)

  private def getWordList(stmt: Statement): Array[String] =
    getListBase(stmt, wordIndex, "term", numWords)

  private def getUserList(stmt: Statement): Array[String] =
    getListBase(stmt, userIndex, "user_id", numUsers)

  private def getItemList(stmt: Statement): Array[String] =
    getListBase(stmt, itemIndex, "item_id", numItems)
}
