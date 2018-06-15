lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.4",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "alpakka-cassandra-sample",
    libraryDependencies ++= Seq(
      "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % "0.19",
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )
  )
