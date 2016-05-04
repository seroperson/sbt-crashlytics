import java.io.File
import java.util.Properties

import android.Keys._
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._
import scala.xml._

object Crashlytics {

  val fabricApiKey = settingKey[Option[String]]("")
  val fabricApiSecret = settingKey[Option[String]]("")
  val crashlyticsBuildId = settingKey[String]("")

  private val localProperties = settingKey[Map[String, String]]("")

  val crashlyticsSettings: Seq[Setting[_]] = Seq(
    localProperties := {
      val prop = new Properties
      IO.load(prop, new File("local.properties"))
      prop.asScala.toMap
    },
    crashlyticsBuildId := java.util.UUID.randomUUID.toString,
    fabricApiKey <<= localProperties { _ get "fabric.apiKey" },
    fabricApiSecret <<= localProperties { _ get "fabric.apiSecret" },

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
      val toGen = v._1 / "crashlytics-build.properties"
      if(!toGen.exists()) {
        val prop = new Properties
        prop.put("app_name", name)
        prop.put("package_name", packageName)
        prop.put("build_id", id)
        prop.put("version_name", verName.get)
        prop.put("version_code", verCode.get.toString)
        IO.write(prop, "", toGen)
      }
      v
    })

}
