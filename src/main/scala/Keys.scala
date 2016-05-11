package crashlytics

import java.io.File

import sbt._

object Keys {

  val propertiesFile = settingKey[File]("Properties file with your fabric stuff in; local.properties by default")
  val fabricApiKey = settingKey[Option[String]]("API Key to identity you at fabric.io")
  val fabricApiSecret = settingKey[Option[String]]("API Secret for the project")

  private[crashlytics] val crashlyticsBuildId = settingKey[String]("Build ID to determine any build")
  private[crashlytics] val crashlyticsProperties = settingKey[Map[String, String]]("Properties loaded from propertiesFile")

}