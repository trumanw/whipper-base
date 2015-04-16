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
    "org.xerial.snappy" % "snappy-java" % "1.1.1.3",
    "net.liftweb" %% "lift-json" % "2.5+",
    "eu.henkelmann" % "actuarius_2.10.0" % "0.2.6",
    "com.typesafe.play.plugins" %% "play-plugins-redis" % "2.3.1"
)

resolvers ++= Seq(
  "pk11 repo" at "http://pk11-scratch.googlecode.com/svn/trunk",
  "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
)

scalacOptions += "-feature"