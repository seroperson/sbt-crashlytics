androidBuild
platformTarget := "android-23"

import crashlytics.Keys._
crashlyticsBuild
fabricApiKey := "no-key"

import android.Keys._
import scala.xml.XML.loadFile
TaskKey[Unit]("check-injected-api-key") <<= (projectLayout, outputLayout, fabricApiKey) map { (layout, converter, key) =>
  val metadata = (loadFile(converter.apply(layout).processedManifest) \ "application" \ "meta-data").head
  val attributes = metadata.attributes
  if(Seq("value" -> key, "name" -> "io.fabric.ApiKey")
      .exists(v => !attributes.get(android.Resources.ANDROID_NS, metadata, v._1).head
        .exists(_.text == v._2))) {
    sys.error("failed")
  }
}

TaskKey[Unit]("check-injected-build-id") <<= (projectLayout, outputLayout, crashlyticsBuildId) map { (layout, converter, id) =>
  if((loadFile(converter.apply(layout).mergedRes / "values/values.xml") \ "string")
      .filter(_.attribute("name")
        .exists(_.text == "com.crashlytics.android.build_id")).text != id) {
    sys.error("failed")
  }
}
