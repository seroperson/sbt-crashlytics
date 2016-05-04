import java.io.File
import java.util.Properties

import android.Keys._
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._
import scala.xml._

object Crashlytics {

  private val PROPERTIES_API_KEY_KEY = "fabric.apiKey"
  private val PROPERTIES_API_SECRET_KEY = "fabric.apiKey"

  val fabricApiKey = settingKey[Option[String]]("API Key to identity you at fabric.io")
  val fabricApiSecret = settingKey[Option[String]]("API Secret for the project")

  private val crashlyticsBuildId = settingKey[String]("Build ID to determine any build")
  private val localProperties = settingKey[Map[String, String]]("Properties loaded from local.properties")

  val crashlyticsSettings: Seq[Setting[_]] = Seq(
    localProperties := {
      val prop = new Properties
      IO.load(prop, new File("local.properties"))
      prop.asScala.toMap
    },
    crashlyticsBuildId := java.util.UUID.randomUUID.toString,
    fabricApiKey <<= localProperties { _ get PROPERTIES_API_KEY_KEY },
    fabricApiSecret <<= localProperties { _ get PROPERTIES_API_SECRET_KEY },

    resValues <<= (crashlyticsBuildId, resValues) map { (id, seq) =>
      seq :+ ("string", "com.crashlytics.android.build_id", id)
    },
    processManifest <<= (fabricApiKey, processManifest) map { (key, file) =>
      val xml = XML.loadFile(file)
      val prefix = xml.scope.getPrefix(android.Resources.ANDROID_NS)
      val application = xml \ "application"
      XML.save(
        filename = file.getAbsolutePath,
        node = xml.copy(
          child = xml.child.updated(xml.child.indexOf(application.head),
            application.head.asInstanceOf[Elem].copy(
              child = application.head.child ++ new Elem(null, "meta-data", Null, TopScope, true) %
                new PrefixedAttribute(prefix, "value", key.get, Null) %
                new PrefixedAttribute(prefix, "name", "io.fabric.ApiKey", Null)))),
        enc = "UTF-8",
        xmlDecl = true)
      file
    },

    collectResources <<= (name, applicationId, crashlyticsBuildId, versionName, versionCode, collectResources) map { (name, packageName, id, verName, verCode, v) =>
      val prop = new Properties
      Seq("app_name" -> name, "package_name" -> packageName,
        "build_id" -> id, "version_name" -> verName.get,
        "version_code" -> verCode.get.toString) foreach (v => prop.put(v._1, v._2))
      IO.write(prop, "Auto-generated properties file for crashlytics", v._1 / "crashlytics-build.properties")
      v
    })

}
