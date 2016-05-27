package crashlytics

import java.io.File
import java.util.Properties

import android.Keys._
import android.Resources.ANDROID_NS
import sbt.Keys._
import sbt._

import scala.collection.JavaConverters._
import scala.xml._

// There is no way to use autoplugin with sbt-android
object Crashlytics extends Plugin {

  import Constants._
  import Keys._

  object Dependency {
    val crashlytics =
      "com.crashlytics.sdk.android" % "crashlytics" % DEPENDENCY_CRASHLYTICS_VERSION intransitive
    val crashlyticsCore =
      "com.crashlytics.sdk.android" % "crashlytics-core" % DEPENDENCY_CRASHLYTICS_CORE_VERSION intransitive
    val crashlyticsAnswers =
      "com.crashlytics.sdk.android" % "answers" % DEPENDENCY_CRASHLYTICS_ANSWERS_VERSION intransitive
    val crashlyticsBeta =
      "com.crashlytics.sdk.android" % "beta" % DEPENDENCY_CRASHLYTICS_BETA_VERSION intransitive
    val fabric =
      "io.fabric.sdk.android" % "fabric" % DEPENDENCY_FABRIC_VERSION intransitive
  }

  // There is 'io.fabric.tools % gradle' package with a lot of stuff that already implemented in,
  // but it's hard to use in result of couple of reasons.

  lazy val crashlyticsBuild = Seq(
    fabricPropertiesFile := new File("local.properties"),
    crashlyticsProperties <<= fabricPropertiesFile { file =>
      val prop = new Properties
      IO.load(prop, file)
      prop.asScala.toMap
    },
    fabricApiKey <<= crashlyticsProperties { _ getOrElse(PROPERTIES_API_KEY_KEY,
      throw new IllegalStateException("You need to set fabric.apiKey property")) },
    fabricApiSecret <<= crashlyticsProperties { _ get PROPERTIES_API_SECRET_KEY },
    crashlyticsLibraries := {
      import Dependency._
      Seq(crashlytics, crashlyticsCore, crashlyticsAnswers, crashlyticsBeta, fabric)
    },
    // TODO it must be generated after each successful packaging
    crashlyticsBuildId := java.util.UUID.randomUUID.toString,

    // Adding fabric maven repository because there is nothing on mavencentral
    resolvers += "fabric" at DEPENDENCY_FABRIC_MAVEN,

    // Adding the set of predefined crashlytics libraries
    libraryDependencies <++= crashlyticsLibraries,

    // Writing com.crashlytics.android.build_id value directly to values.xml
    resValues <<= (crashlyticsBuildId, resValues) map { (id, seq) =>
      seq :+("string", RESOURCE_FABRIC_BUILD_ID_KEY, id)
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
                new PrefixedAttribute(prefix, "value", key, Null) %
                new PrefixedAttribute(prefix, "name", MANIFEST_FABRIC_META_KEY, Null)))),
        enc = "UTF-8",
        xmlDecl = true)
      file
    },

    // Generate crashlytics-build.properties
    collectResources <<= (name, applicationId, crashlyticsBuildId, versionName, versionCode, collectResources) map { (name, packageName, id, verName, verCode, v) =>
      val assets = v._1
      val prop = new Properties
      Seq("app_name" -> name,
        "package_name" -> packageName,
        "build_id" -> id,
        "version_name" -> verName.getOrElse(ASSET_CRASHLYTICS_DEFAULT_VERSION_NAME),
        "version_code" -> verCode.getOrElse(ASSET_CRASHLYTICS_DEFAULT_VERSION_CODE).toString) foreach (v => prop.put(v._1, v._2))
      IO.write(prop, ASSET_CRASHLYTICS_BUILD_DESC, assets / ASSET_CRASHLYTICS_BUILD_FILENAME)
      v
    })

}
