package databases

import java.io.{ObjectInputStream, ByteArrayInputStream, ObjectOutputStream, ByteArrayOutputStream}

import databases.InstanceType.InstanceType
import factories.{CollabFilterModel, TopicModel, DataSource}
import play.api.Play.current
import play.api.db._

import scala.collection.mutable.ListBuffer
import scala.collection.mutable

object CollabDB {
  val lazyInstanceStore = mutable.Map[Int, AnyRef]()
  def getJarDescriptions = {
    var outstring = ""
    val conn = DB.getConnection()
    try {
      val stmt = conn.createStatement
      val rs = stmt.executeQuery("SELECT * FROM jars")
      while (rs.next()) {
        outstring += rs.getString("description") + "\n"
      }
    } finally {
      conn.close()
    }
    outstring
  }

  def addJarDescription (path :String, description :String): Unit = {
    val conn = DB.getConnection()
    try {
      val insertStatement = conn.prepareStatement("INSERT INTO jars (jarpath, description) VALUES (?, ?)")
      insertStatement.setString(1, path)
      insertStatement.setString(2, description)
      insertStatement.execute()
    } finally {
      conn.close()
    }
  }

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

