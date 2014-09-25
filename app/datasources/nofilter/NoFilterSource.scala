package datasources.nofilter

import datasources.SQLFactory

import scala.collection.immutable.WrappedString
import scala.collection.mutable.ArrayBuffer

private class NoFilterSource (jobsAreItems: Boolean, wordType: Int) extends SQLFactory {
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

  override protected def ratingsQuery = {
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

  override protected def wordsQuery  = {
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

  override protected def initQueries: Array[String] = {
    val queries = ArrayBuffer[String]()

    queries += wordIndexInsertQuery
    queries += itemIndexInsertQuery
    queries += userIndexInsertQuery

    queries.toArray
  }

  override protected def description: String = {
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

  override protected val factoryId: Long = 234563238

  override protected def cleanupQueries: Array[String] = new Array(0)
}
