val crashlytics = Project(id = "sbt-crashlytics", base = file("."))

// Unsorted settings
sbtPlugin := true

// Dependency settings
addSbtPlugin("org.scala-android" % "sbt-android" % "1.6.3")
libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.3.0"

// Scripted testing settings
ScriptedPlugin.scriptedSettings

// Publishing-related settings
name := "sbt-crashlytics"
description := "Unofficial plugin that provides crashlytics support for android applications that was built via sbt"
organization := "com.seroperson"
licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/MIT"))
version := "0.2"

publishMavenStyle := false
bintrayReleaseOnPublish := false
bintrayOrganization := None
bintrayVcsUrl := Some("https://github.com/seroperson/sbt-crashlytics")
