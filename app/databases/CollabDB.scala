package databases

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}
import java.sql.{Connection, Statement}

import databases.InstanceType.InstanceType
import factories.util.InstanceBase
import factories._
import play.api.Play.current
import play.api.db._

import scala.collection.mutable.ListBuffer
import scala.collection.mutable

object CollabDB {
  val lazyInstanceStore = mutable.Map[Int, AnyRef]()

  def addInstance(obj: InstanceBase): Int = {
    val conn = DB.getConnection()
    try {
      val stmt = conn.prepareStatement(
        "INSERT INTO instances (description, serialized, factoryid, instancetype, buildtime) " +
          "VALUES (?, ?, ?, CAST(? AS instancetype), ?)", Statement.RETURN_GENERATED_KEYS)

      val bos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(bos)

      oos.writeObject(obj)
      oos.flush()
      oos.close()
      bos.close()

      val dataBytes = bos.toByteArray

      stmt.setString(1, obj.getDescription)
      stmt.setBytes(2, dataBytes)
      stmt.setLong(3, obj.getFactoryId)
      stmt.setString(4, obj.getInstanceType.toString)
      stmt.setLong(5, obj.getComputeTime)

      val numRows = stmt.executeUpdate()

      val genKeys = stmt.getGeneratedKeys
      genKeys.next()
      val instanceId = genKeys.getInt(1)
      lazyInstanceStore(instanceId) = obj
      instanceId
    } finally {
      conn.close()
    }
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

  def addResultGroup(rg: EvaluationGroup): Unit = {
    val conn = DB.getConnection()
    try {
      val evalGroupId = addInstance(rg)
      val insertEvalQuery = "INSERT INTO evaluationgroups (evalgroupid, dataid, testsetsize, trainsetsize) " +
        "VALUES (?, ?, ?, ?)"
      val insertEvalGrpStmt = conn.prepareStatement(insertEvalQuery)
      val numtypes = rg.evaluations.size
      insertEvalGrpStmt.setInt(1, evalGroupId)
      insertEvalGrpStmt.setInt(2, rg.dataId)
      insertEvalGrpStmt.setInt(3, rg.testSize)
      insertEvalGrpStmt.setInt(4, rg.trainSize)
      insertEvalGrpStmt.execute()

      addEvaluations(conn, rg.evaluations, evalGroupId)

    } finally {
      conn.close()
    }
  }

  private def addEvaluations(conn: Connection, evals: List[Evaluation], evalGroupId: Int) = {
    val insertResultsQuery = "INSERT INTO evaluations (evalgroupid, buildtime, lost, modelid)" +
      "VALUES (?, ?, ?, ?)"
    val insertResultsStmt = conn.prepareStatement(insertResultsQuery, Statement.RETURN_GENERATED_KEYS)
    insertResultsStmt.setInt(1, evalGroupId)

    evals.foreach(eval => {
      insertResultsStmt.setLong(2, eval.evalTime)
      insertResultsStmt.setInt(3, eval.lost)
      insertResultsStmt.setInt(4, eval.modelId)

      insertResultsStmt.executeUpdate()
      val rs = insertResultsStmt.getGeneratedKeys()
      rs.next()
      val evalid = rs.getInt(1)
      addScores(conn, eval.scores, evalid)
    })
  }

  private def addScores(conn: Connection, scores: List[Score], evalId:Int): Unit = {
    val insertScoreQuery = "INSERT INTO results (evalid, score, scoretype) " +
      "VALUES (?, ?, CAST(? AS scoretype))"
    val insertScoreStmt = conn.prepareStatement(insertScoreQuery)
    insertScoreStmt.setInt(1, evalId)
    scores.foreach(sc => {
      insertScoreStmt.setDouble(2, sc.scoreVal)
      insertScoreStmt.setString(3, sc.scoreType.toString)
      insertScoreStmt.executeUpdate()
    })
  }

  def getResultGroups(): List[EvaluationGroup] = {
    val conn = DB.getConnection()
    try {
      val rgs = ListBuffer[EvaluationGroup]()
      val rs = conn.prepareStatement(
        "SELECT instanceid " +
          "FROM instances " +
          "WHERE instancetype=CAST('evaluation' AS instancetype)").executeQuery()
      while (rs.next()) {
        val eval: EvaluationGroup = getInstance(rs.getInt(1))
        rgs += eval
      }

      rgs.toList
    } finally {
      conn.close()
    }
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

