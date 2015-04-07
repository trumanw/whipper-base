name := """wipbase"""

organization := "com.mcookie"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
    jdbc,
  	cache,
  	ws,
  	filters,
  	"com.typesafe.slick" % "slick_2.10" % "2.1.0",
    "com.typesafe.play" %% "play-slick" % "0.8.1",
  	"org.slf4j" % "slf4j-nop" % "1.6.4",
  	"mysql" % "mysql-connector-java" % "5.1.27",
    "com.typesafe.akka" % "akka-actor_2.10" % "2.3.5",
    "org.apache.commons" % "commons-math3" % "3.3",
    "com.rabbitmq" % "amqp-client" % "3.4.4",
    "org.apache.activemq" % "activemq-all" % "5.9.0",
    "org.apache.activemq" % "activemq-pool" % "5.9.0",
    "org.xerial.snappy" % "snappy-java" % "1.1.1.3"
)

scalacOptions += "-feature"