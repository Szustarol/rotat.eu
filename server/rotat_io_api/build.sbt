name := """rotat_io_api"""
organization := "io.rotat"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.8"

val nd4jVersion = "1.0.0-beta7"

resolvers += "tensorflow-snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "5.0.0" % Test
// libraryDependencies += "org.deeplearning4j" % "deeplearning4j-core" % nd4jVersion
// libraryDependencies += "org.deeplearning4j" % "deeplearning4j-modelimport" % nd4jVersion
// libraryDependencies += "org.nd4j" % "nd4j-native-platform" % nd4jVersion
// libraryDependencies += "org.nd4j" % "nd4j-common" % nd4jVersion
libraryDependencies += "org.tensorflow" % "tensorflow-core-platform" % "0.5.0-SNAPSHOT"
// Adds additional packages into Twirl
//TwirlKeys.templateImports += "io.rotat.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "io.rotat.binders._"
