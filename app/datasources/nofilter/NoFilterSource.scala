package datasources.nofilter

import java.sql.Statement
import java.util

import datasources.ResultSetDataModelBuilder
import play.api.Play.current
import factories.DataSource
import org.apache.mahout.cf.taste.model.DataModel
import org.apache.mahout.math.{SparseMatrix, Matrix}
import play.api.db.DB

import scala.collection.immutable.WrappedString
import scala.collection.mutable
import scala.collection.immutable

private class NoFilterSource extends DataSource {
  private var jobsAreItems: Boolean = false
  private var wordType: Int = 0
  private var wordsList: Array[String] = null
  private var numWords: Int = 0
  private var numItems: Int = 0
  private var numUsers: Int = 0
  private var corpus: Array[immutable.HashMap[Int, Int]] = null
  private var dataModel: DataModel = null
  override val uuid: Long = NoFilterSource.uuid
  // wordTable => The table which associates terms and items
  // wordTableItemCol => The real name for the column which refers to items
  // wordTableTermType => The part of the table name for the term type
  // wordTableItemType => The part of the table name for the users/jobs which are the items
  // ratingsTableItemCol => Real name for column which refers to items
  // ratingsTableUserCol => Real name for column which refers to users


  // Used to select the column for items out of the wordTable
  private def wordTableItemCol = {
    val select = Map(false->"userid", true->"jobid")
    select(jobsAreItems)
  }
  // Used to select the column for items out of the ratings table
  private def ratingsTableItemCol  = {
    val select = Map(false->"user_id", true->"job_id")
    select(jobsAreItems)
  }
  // Used to select the column for items out of the ratings table (inverse of line above)
  private def ratingsTableUserCol = {
    val select = Map(false->"user_id", true->"job_id")
    select(!jobsAreItems)
  }

  // Gets the table name to draw words from
  private def wordTableName  = {
    val wordTableTermTypeMapping = Array("NOSTEM", "STEM", "COMP")
    val wordTableItemTypeMapping = Map(false->"PEOPLE_SUM", true->"JOBS")
    val wordTableTermType = wordTableTermTypeMapping(wordType)
    val wordTableItemType = wordTableItemTypeMapping(jobsAreItems)
    val fmt = new WrappedString("job_recom.ln2_%s_term_%s")
    fmt.format(wordTableItemType, wordTableTermType)
  }

  // Query to insert all the words into the wordindex table
  private def wordIndexInsertQuery = {
    val fmt = new WrappedString(
      "INSERT INTO wordindex\n" +
      "  SELECT TERM, ROWNUM-1 ident FROM (SELECT DISTINCT(TERM) FROM %s)")
    fmt.format(wordTableName)
  }

  // Query to insert all user ids into the userindex table
  private def itemIndexInsertQuery  = {
    val fmt = new WrappedString(
      "INSERT INTO itemindex\n" +
      "  SELECT item_id, ROWNUM-1 ident FROM (SELECT DISTINCT(ITEM_ID) item_id FROM\n" +
      "  (SELECT %s item_id FROM JOB_RECOM.USER_STUDY_U2J_RATING_FINAL\n" +
      "  UNION\n" +
      "  SELECT %s item_id FROM %s))")
    val ratingsItemCol = ratingsTableItemCol
    val wordsItemCol = wordTableItemCol
    val wordTable = wordTableName
    fmt.format(ratingsItemCol, wordsItemCol, wordTable)
  }

  // Query to insert all user ids into the userindex table
  private def userIndexInsertQuery  = {
    val fmt = new WrappedString(
      "INSERT INTO userindex\n" +
      " SELECT user_id, ROWNUM-1 ident FROM (SELECT DISTINCT(%s) user_id FROM JOB_RECOM.USER_STUDY_U2J_RATING_FINAL)")
    val ratingsUserCol = ratingsTableUserCol
    fmt.format(ratingsUserCol)
  }
  private def ratingsQuery = {
    val fmt = new WrappedString(
      "select userNum, itemNum, avg(rate_list) rating from\n" +
      "  (select userindex.ident userNum, itemindex.ident itemNum, rate_list\n" +
      "    from job_recom.user_study_u2j_rating_final\n" +
      "    inner join userindex\n" +
      "      on userindex.user_id=job_recom.user_study_u2j_rating_final.%s\n" +
      "    inner join itemindex\n" +
      "      on itemindex.item_id=job_recom.user_study_u2j_rating_final.%s)\n" +
      "  group by userNum, itemNum"
    )
    val ratingsItemCol = ratingsTableItemCol
    val ratingsUserCol = ratingsTableUserCol
    fmt.format(ratingsUserCol, ratingsItemCol)
  }
  private def wordsQuery  = {
    val fmt = new WrappedString(
      "select itemNum, wordNum, count(wordNum) cnt from\n" +
      "(select itemindex.ident itemNum, wordindex.ident wordNum\n" +
      "from %s\n" +
      "inner join itemindex\n" +
      "  on itemindex.item_id=%s.%s\n" +
      "inner join wordindex\n" +
      "  on wordindex.term=%s.term)\n" +
      "group by itemNum, wordNum\n" +
      "order by itemNum, wordNum"
    )
    val wordTable = wordTableName
    val wordsItemCol = wordTableItemCol
    fmt.format(wordTable, wordTable, wordsItemCol, wordTable)
  }

  private def initTables(stmt: Statement): Unit = {
    stmt.execute(wordIndexInsertQuery)
    stmt.execute(itemIndexInsertQuery)
    stmt.execute(userIndexInsertQuery)
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

  private def getNumWords(stmt: Statement): Int = getNumBase(stmt, "wordindex")
  private def getNumUsers(stmt: Statement): Int = getNumBase(stmt, "userindex")
  private def getNumItems(stmt: Statement): Int = getNumBase(stmt, "itemindex")

  private def getWordsList(stmt: Statement): Array[String] =
    getListBase(stmt, "wordindex", "term", numWords)

  private def getUsersList(stmt: Statement): Array[String] =
    getListBase(stmt, "userindex", "user_id", numUsers)

  private def getItemsList(stmt: Statement): Array[String] =
    getListBase(stmt, "itemindex", "item_id", numItems)

  private def retrieveCorpus(stmt: Statement): Array[immutable.HashMap[Int, Int]] = {
    val corpus = Array.range(0, numItems).map(x => immutable.HashMap.newBuilder[Int, Int])

    val rs = stmt.executeQuery(wordsQuery)

    while (rs.next()) {
      val item = rs.getInt(1)
      val wordNum = rs.getInt(2)
      val count = rs.getInt(3)
      corpus(item).+=((wordNum, count))
    }
    corpus.map(_.result())
  }

  private def retrieveDataModel(stmt: Statement): DataModel = {
    val rs = stmt.executeQuery(ratingsQuery)
    ResultSetDataModelBuilder.makeDataModel(rs, numUsers)
  }

  def this (jobsAreItems: Boolean, wordType: Int) = {
    this()
    this.jobsAreItems = jobsAreItems
    this.wordType = wordType

    val conn = DB.getConnection("jobs")
    try {
      val stmt = conn.createStatement()
      stmt.setFetchSize(10000)
      initTables(stmt)
      numWords = getNumWords(stmt)
      numUsers = getNumUsers(stmt)
      numItems = getNumItems(stmt)
      wordsList = getWordsList(stmt)
      corpus = retrieveCorpus(stmt)
      dataModel = retrieveDataModel(stmt)
    } finally {
      conn.close()
    }
  }

  override def getDataModel: DataModel = dataModel

  override def getNumUsers: Int = numUsers

  override def getNumItems: Int = numItems

  override def getNumWords: Int = numWords

  override def description: String = {
    val items = if (jobsAreItems) {
      "Jobs"
    } else {
      "Users"
    }
    val wordTableTermTypeMapping = Array("NOSTEM", "STEM", "COMP")
    val wordTypeStr = wordTableTermTypeMapping(wordType)
    val fmt = new WrappedString("Nofilter-Items_%s-Terms_%s")
    fmt.format(items, wordTypeStr)
  }

  override def getWord(wordIndex: Int): String = wordsList(wordIndex)

  override def getCorpus: Array[immutable.HashMap[Int, Int]] = corpus
}

object NoFilterSource {
  val uuid: Long = 123433234
}
