val crashlytics = Project(id = "sbt-crashlytics", base = file("."))
  .settings(addSbtPlugin("org.scala-android" % "sbt-android" % "1.6.0"),
    sbtPlugin := true)