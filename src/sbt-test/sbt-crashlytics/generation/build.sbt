androidBuild
platformTarget := "android-23"

import crashlytics.Keys._
crashlyticsBuild
fabricApiKey := Some("no-key")

import android.Keys._
TaskKey[Unit]("check-injected-api-key") <<= (projectLayout, outputLayout, fabricApiKey) map { (layout, converter, key) =>
  import scala.xml.XML
  val metadata = (XML.loadFile(converter.apply(layout).processedManifest) \ "application" \ "meta-data").head
  val attributes = metadata.attributes
  if(Seq("value" -> key.get, "name" -> "io.fabric.ApiKey")
      .exists(v => !attributes.get(android.Resources.ANDROID_NS, metadata, v._1).head
        .exists(_.text == v._2))) {
    sys.error("failed")
  }
}

//TaskKey[Boolean]("check-injected-build-id") := {
//}
