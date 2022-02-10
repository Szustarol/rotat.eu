name := """deeplearning_test"""
organization := "org.szustarol"

version := "1.0-SNAPSHOT"

scalaVersion := "2.13.8"

val nd4jVersion = "1.0.0-M1.1"

libraryDependencies += "org.deeplearning4j" % "deeplearning4j-core" % nd4jVersion
libraryDependencies += "org.deeplearning4j" % "deeplearning4j-modelimport" % nd4jVersion
libraryDependencies += "org.nd4j" % "nd4j-native-platform" % nd4jVersion
libraryDependencies += "org.nd4j" % "nd4j-common" % nd4jVersion
