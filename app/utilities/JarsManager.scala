package utilities

import play.api.libs._
import play.api.Play.current
import play.api.Play
import java.io.File

import databases._

object JarsManager {
  val folder = new File(Play.configuration.getString("collabdash.jarsfolder").get)

  def addJar (jar: Files.TemporaryFile, fileName: String, description: String): Unit = {
    val newf = File.createTempFile("cd-", fileName, folder)
    val actualPath = newf.getAbsolutePath()
    jar.moveTo(newf, true)

    CollabDB.addJarDescription(actualPath, description)
  }
}
