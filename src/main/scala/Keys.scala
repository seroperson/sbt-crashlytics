package crashlytics

import java.io.File

import okhttp3.OkHttpClient
import sbt._

object Keys {

  val fabricPropertiesFile = settingKey[File]("Properties file with your fabric stuff in - local.properties by default")
  val fabricApiKey = settingKey[String]("API Key to identity you at fabric.io")
  val fabricApiSecret = settingKey[Option[String]]("API Secret for the project")
  val crashlyticsLibraries = settingKey[Seq[ModuleID]]("By default there is the set of crashlytics libraries that will be added after settings applied. Overwrite if you need to change library version or something else")
  val crashlyticsBuildId = settingKey[String]("Build ID to determine any build")
  val crashlyticsOkHttpClient = settingKey[OkHttpClient]("Client that performs API requests")
  // Names according to gradle plugin
  val crashlyticsUploadDistributionDebug = taskKey[Unit]("Upload debug apk file to Beta")
  val crashlyticsUploadDistributionRelease = taskKey[Unit]("Upload release apk file to Beta")
  val crashlyticsReleaseNotesCreator = taskKey[(String) => String]("Function that returns release notes for the given version")

  private[crashlytics] val crashlyticsProperties = settingKey[Map[String, String]]("Properties loaded from propertiesFile")

}