val crashlytics = project.in(file(".")).settings(addSbtPlugin("org.scala-android" % "sbt-android" % "1.6.0"),
  sbtPlugin := true)