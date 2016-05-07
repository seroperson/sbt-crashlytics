import java.io.File
import java.util.Properties

import android.AndroidPlugin
import android.Keys._
import android.Resources.ANDROID_NS
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._
import scala.xml._

object Crashlytics extends AutoPlugin {

  // There is 'io.fabric.tools % gradle' package with a lot of stuff that already implemented in,
  // but it's hard to use with sbt-android in result of couple of reasons.

  private val PROPERTIES_API_KEY_KEY = "fabric.apiKey"
  private val PROPERTIES_API_SECRET_KEY = "fabric.apiKey"

  object autoImport {
    lazy val propertiesFile = settingKey[File]("Properties file with your fabric stuff in; local.properties by default")
    lazy val fabricApiKey = settingKey[Option[String]]("API Key to identity you at fabric.io")
    lazy val fabricApiSecret = settingKey[Option[String]]("API Secret for the project")
  }

  import autoImport._

  private lazy val crashlyticsBuildId = settingKey[String]("Build ID to determine any build")
  private lazy val properties = settingKey[Map[String, String]]("Properties loaded from propertiesFile")

  override def projectSettings = Seq(
    propertiesFile := new File("local.properties"),
    properties <<= propertiesFile { file =>
      val prop = new Properties
      IO.load(prop, file)
      prop.asScala.toMap
    },
    crashlyticsBuildId := java.util.UUID.randomUUID.toString,
    fabricApiKey <<= properties { _ get PROPERTIES_API_KEY_KEY },
    fabricApiSecret <<= properties { _ get PROPERTIES_API_SECRET_KEY },

    // Writing com.crashlytics.android.build_id value directly to values.xml
    resValues <<= (crashlyticsBuildId, resValues) map { (id, seq) =>
      seq :+ ("string", "com.crashlytics.android.build_id", id)
    },

    // Instead of using io.fabric.tools apiKey manifest injection we inject it by own implementation
    // Because we can inject it only if call resource generation again
    processManifest <<= (fabricApiKey, processManifest) map { (key, file) =>
      val xml = XML.loadFile(file)
      val prefix = xml.scope.getPrefix(ANDROID_NS)
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

    // Generate crashlytics-build.properties
    collectResources <<= (name, applicationId, crashlyticsBuildId, versionName, versionCode, collectResources) map { (name, packageName, id, verName, verCode, v) =>
      val assets = v._1
      val prop = new Properties
      Seq("app_name" -> name, "package_name" -> packageName,
        "build_id" -> id, "version_name" -> verName.get,
        "version_code" -> verCode.get.toString) foreach (v => prop.put(v._1, v._2))
      IO.write(prop, "Auto-generated properties file for crashlytics", assets / "crashlytics-build.properties")
      v
    })

  override def requires = AndroidPlugin

}
