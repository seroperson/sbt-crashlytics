val crashlytics = Project(id = "sbt-crashlytics", base = file("."))

// Unsorted settings
sbtPlugin := true

// Dependency settings
addSbtPlugin("org.scala-android" % "sbt-android" % "1.6.15")
libraryDependencies += "com.squareup.okhttp3" % "okhttp" % "3.4.1"

// Scripted testing settings
ScriptedPlugin.scriptedSettings

// Publishing-related settings
name := "sbt-crashlytics"
description := "Provides unofficial support for crashlytics on sbt"
organization := "com.seroperson"
licenses := Seq("MIT" -> url("http://www.opensource.org/licenses/MIT"))
version := "0.3"

publishMavenStyle := false
bintrayReleaseOnPublish := false
bintrayOrganization := None
bintrayVcsUrl := Some("https://github.com/seroperson/sbt-crashlytics")