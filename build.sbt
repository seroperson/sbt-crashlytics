val crashlytics = Project(id = "sbt-crashlytics", base = file("."))

sbtPlugin := true
name := "sbt-crashlytics"
description := "Unofficial plugin that provides support for building android applications that uses crashlytics"
organization := "com.seroperson"
version := "0.1"
licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/MIT"))

addSbtPlugin("org.scala-android" % "sbt-android" % "1.6.1")

ScriptedPlugin.scriptedSettings