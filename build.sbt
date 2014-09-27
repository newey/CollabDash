import play.Project._

name := "CollabDash"

version := "1.0"

libraryDependencies += jdbc

libraryDependencies += "postgresql" % "postgresql" % "9.1-901-1.jdbc4"

libraryDependencies += "org.apache.mahout" % "mahout-core" % "0.9"


playScalaSettings