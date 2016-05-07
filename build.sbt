val crashlytics = Project(id = "sbt-crashlytics", base = file("."))

name := "sbt-crashlytics"
description := "Unofficial plugin that provides support for building android applications that uses crashlytics"
organization := "com.seroperson"
licenses := Seq("MIT License" -> url("http://www.opensource.org/licenses/mit-license.html"))

version := "0.1"

addSbtPlugin("org.scala-android" % "sbt-android" % "1.6.0")

sbtPlugin := true

