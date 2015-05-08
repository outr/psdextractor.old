import spray.revolver.RevolverPlugin._

name := "outrutility"

organization := "com.outr"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.6"

seq(Revolver.settings: _*)

libraryDependencies += "org.hyperscala" %% "hyperscala-ui" % "0.10.1-SNAPSHOT"

libraryDependencies += "com.outr.net" %% "outrnet-jetty" % "1.1.5-SNAPSHOT"

libraryDependencies += "com.badlogicgames.gdx" % "gdx" % "1.6.0"