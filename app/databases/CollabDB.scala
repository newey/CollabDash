package databases

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import java.sql.Statement
import java.sql.Connection

import databases.InstanceType.InstanceType
import evaluation.util.ScoreType
import evaluation.util.ScoreType.ScoreType
import evaluation.util.{ScoreType, ResultGroup}
import factories.{CollabFilterModel, TopicModel, DataSource}
import play.api.Play.current
import play.api.db._

import scala.collection.mutable.ListBuffer
import scala.collection.mutable

object CollabDB {
  val lazyInstanceStore = mutable.Map[Int, AnyRef]()

  private def addInstance(description: String, obj: AnyRef, factoryId: Long, instanceType: InstanceType): Unit = {
    val conn = DB.getConnection()
    try {
      val stmt = conn.prepareStatement(
        "INSERT INTO instances (description, serialized, factoryid, instancetype) " +
          "VALUES (?, ?, ?, CAST(? AS instancetype))")

      val bos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(bos)

      oos.writeObject(obj)
      oos.flush()
      oos.close()
      bos.close()

      val dataBytes = bos.toByteArray

      stmt.setString(1, description)
      stmt.setBytes(2, dataBytes)
      stmt.setLong(3, factoryId)
      stmt.setString(4, instanceType.toString)

      stmt.execute()

      val getidstmt = conn.prepareStatement(
        "SELECT instanceid FROM instances WHERE description=? AND factoryid=? AND instancetype=CAST(? AS instancetype)"
      )
      getidstmt.setString(1, description)
      getidstmt.setLong(2, factoryId)
      getidstmt.setString(3, instanceType.toString)

      val rs = getidstmt.executeQuery()
      rs.next()

      val instanceId = rs.getInt(1)

      lazyInstanceStore(instanceId) = obj

    } finally {
      conn.close()
    }
  }
  def addInstance(dataSource: DataSource): Unit = {
    addInstance(dataSource.getDescription, dataSource, dataSource.getFactoryId, InstanceType.dataSource)
  }
  def addInstance(topicm: TopicModel): Unit = {
    addInstance(topicm.getDescription, topicm, topicm.getFactoryId, InstanceType.topicModel)
  }
  def addInstance(collab: CollabFilterModel): Unit = {
    addInstance(collab.getDescription, collab, collab.getFactoryId, InstanceType.cfModel)
  }

  private def getInstance[B](instanceIndex: Int): B = {
    val first = if (lazyInstanceStore.contains(instanceIndex)) {
      lazyInstanceStore(instanceIndex)
    } else {
      val conn = DB.getConnection()
      try {
        val stmt = conn.prepareStatement("SELECT serialized FROM instances WHERE instanceid = ?")
        stmt.setInt(1, instanceIndex)
        val rs = stmt.executeQuery()

        rs.next()

        val bais = new ByteArrayInputStream(rs.getBytes(1))
        val ins = new ObjectInputStream(bais)

        val obj = ins.readObject()
        lazyInstanceStore(instanceIndex) = obj
        obj
      } finally {
        conn.close()
      }
    }

    first match {
      case g2: B => g2
      case _ => throw new ClassCastException
    }
  }
  def getDataSource(instanceIndex: Int): DataSource = {
    getInstance[DataSource](instanceIndex)
  }
  def getTopicModel(instanceIndex: Int): TopicModel = {
    getInstance[TopicModel](instanceIndex)
  }
  def getCollabFilterModel(instanceIndex: Int): CollabFilterModel = {
    getInstance[CollabFilterModel](instanceIndex)
  }

  private def getInfo(instanceType: InstanceType): List[InstanceInfo] = {
    val conn = DB.getConnection()
    try {
      val stmt = conn.prepareStatement(
        "SELECT instanceid, description" +
        "FROM instances" +
        "WHERE instancetype = CAST(? AS instancetype)")
      stmt.setString(1, instanceType.toString)
      val rs = stmt.executeQuery()
      val instances = ListBuffer[InstanceInfo]()

      while (rs.next()) {
        instances += InstanceInfo(rs.getInt(1), rs.getString(2))
      }
      instances.toList
    } finally {
      conn.close()
    }
  }
  def getDataSourceInfo: List[InstanceInfo] = getInfo(InstanceType.dataSource)
  def getTopicModelInfo: List[InstanceInfo] = getInfo(InstanceType.topicModel)
  def getCfModelInfo: List[InstanceInfo] = getInfo(InstanceType.cfModel)

  def addResultGroup(rg: ResultGroup): Unit = {
    val conn = DB.getConnection()
    try {
      val insertEvalQuery = "INSERT INTO evaluations (modelid, description, testsetsize, trainsetsize, lost) " +
        "VALUES (?, ?, ?, ?, ?)"
      val insertEvalStmt = conn.prepareStatement(insertEvalQuery, Statement.RETURN_GENERATED_KEYS)
      val numtypes = rg.scoreTypes.size
      insertEvalStmt.setInt(1, rg.modelId)
      insertEvalStmt.setString(2, rg.description)
      insertEvalStmt.setInt(3, rg.testSize)
      insertEvalStmt.setInt(4, rg.trainSize)
      insertEvalStmt.setInt(5, rg.lost)

      val numRowsUpdated = insertEvalStmt.executeUpdate()
      printf("[CollabDB] evaluations: Inserted %d rows\n", numRowsUpdated)

      val genKeys = insertEvalStmt.getGeneratedKeys()
      genKeys.next()

      val evalId = genKeys.getInt(1)


      val insertResultsQuery = "INSERT INTO results (score, scoretype, evalid) VALUES (?, CAST(? AS scoretype), ?)"
      val insertResultsStmt = conn.prepareStatement(insertResultsQuery)
      insertResultsStmt.setInt(3, evalId)

      (rg.scores, rg.scoreTypes).zipped.foreach((s, sct) => {
        insertResultsStmt.setString(2, sct.toString)
        s.foreach(r => {insertResultsStmt.setDouble(1, r); insertResultsStmt.execute()})
      })
    } finally {
      conn.close()
    }
  }
  def getResultGroups(): List[ResultGroup] = {
    val conn = DB.getConnection()
    try {
      val rgs = ListBuffer[ResultGroup]()
      val rs = conn.prepareStatement("SELECT evalid, modelid, description, testsetsize, trainsetsize, lost FROM evaluations").executeQuery()
      while (rs.next()) {
        val (types, scores) = getEvalResults(rs.getInt(1), conn)
        val modelId = rs.getInt(2)
        val desc = rs.getString(3)
        val testsize = rs.getInt(4)
        val trainsize = rs.getInt(5)
        val lost = rs.getInt(6)
        rgs += ResultGroup(modelId, types, scores, desc, testsize, trainsize, lost)
      }

      rgs.toList
    } finally {
      conn.close()
    }
  }
  private def getEvalResults(evalId: Int, conn: Connection): (List[ScoreType], List[List[Double]]) = {
    val firstresults = ListBuffer[(ScoreType, Double)]()
    val stmt = conn.prepareStatement("SELECT scoretype, score FROM results WHERE evalid=?")
    stmt.setInt(1, evalId)
    val rs = stmt.executeQuery()
    while (rs.next()){
      firstresults += ((ScoreType.withName(rs.getString(1)), rs.getDouble(2)))
    }
    val secondresults = firstresults.toList.groupBy(_._1)
    val scoretypes = ListBuffer[ScoreType]()
    val scores = ListBuffer[List[Double]]()

    secondresults.keys.foreach(x => {
      scoretypes += x
      scores += secondresults(x).map(_._2)
    })

    (scoretypes.toList, scores.toList)
  }

  def purge(): Unit = {
    val conn = DB.getConnection()
    try {
      val stmt = conn.createStatement
      stmt.execute("DELETE FROM jars")
    } finally {
      conn.close()
    }
  }
}

case class InstanceInfo (index: Int, description: String)

