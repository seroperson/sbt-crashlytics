package crashlytics

import android.Keys._
import sbt.Keys._
import sbt._

// There is no way to use autoplugin with sbt-android
object Crashlytics extends Plugin {

  import java.util.Properties

  import Constants._
  import Keys._

  private def fail(m: String) = throw new MessageOnlyException(m)

  // There is 'io.fabric.tools % gradle' package with a lot of stuff that already implemented in,
  // but it's hard to use in result of couple of reasons.

  lazy val crashlyticsBuild = Seq(
    fabricPropertiesFile := new File("local.properties"),
    crashlyticsProperties <<= fabricPropertiesFile { file =>
      import scala.collection.JavaConverters._

      val prop = new Properties
      IO.load(prop, file)
      prop.asScala.toMap
    },

    fabricApiKey <<= crashlyticsProperties { _ getOrElse(PROPERTIES_API_KEY_KEY, fail("You need to set fabric.apiKey property")) },
    // Api secret isn't needed just for building application, so it's optional
    fabricApiSecret <<= crashlyticsProperties { _ get PROPERTIES_API_SECRET_KEY },

    // The following library downloads all others as dependencies (beta, answers etc ...)
    crashlyticsLibraries := Seq("com.crashlytics.sdk.android" % "crashlytics" % DEPENDENCY_CRASHLYTICS_VERSION),

    // TODO it must be generated after each successful packaging
    // XmlBuildWriter#updateBuildId
    crashlyticsBuildId := java.util.UUID.randomUUID.toString,

    // Adding fabric maven repository because there is nothing on mavencentral
    resolvers += "fabric" at DEPENDENCY_FABRIC_MAVEN,

    // Automatically adding crashlytics library dependency
    libraryDependencies <++= crashlyticsLibraries,

    // todo looks shitty
    crashlyticsUploadDistributionRelease <<= (packageRelease, applicationId, crashlyticsBuildId, versionName, versionCode,
      fabricApiKey, fabricApiSecret, crashlyticsReleaseNotesCreator, streams) map(uploadDistribution),
    crashlyticsUploadDistributionDebug <<= (packageDebug, applicationId, crashlyticsBuildId, versionName, versionCode,
      fabricApiKey, fabricApiSecret, crashlyticsReleaseNotesCreator, streams) map(uploadDistribution),

    // Or redefine the key to enforce some editor for all project members
    crashlyticsReleaseNotesCreator := (version => { // todo return just f0?
      import sbt.IO._

      withTemporaryFile("crashlytics_note_", "") { file =>
        write(file, NOTES_DESCRIPTION_MESSAGE_FORMAT.format(version))
        if(Process(Seq(sys.env.get("EDITOR").getOrElse(fail("Set $EDITOR env variable to edit the release notes")), file.getCanonicalPath)).!< == 0) {
          readLines(file).foldLeft(StringBuilder.newBuilder)((b, l) => b.append(s"\n$l")).tail.toString()
        }
        else {
          fail("Notes aren't saved, editor finished with non-zero code.")
        }
      }
    }),

    // Writing com.crashlytics.android.build_id value directly to values.xml
    resValues <<= (crashlyticsBuildId, resValues) map { (id, seq) => seq :+("string", "com.crashlytics.android.build_id", id) },

    // Injecting apiKey into the manifest
    processManifest <<= (fabricApiKey, processManifest) map { (key, file) =>
      import scala.xml._

      val xml = XML.loadFile(file)
      val prefix = xml.scope.getPrefix(android.Resources.ANDROID_NS)
      val application = xml \ "application"
      XML.save(
        filename = file.getAbsolutePath,
        node = xml.copy(
          child = xml.child.updated(xml.child.indexOf(application.head),
            application.head.asInstanceOf[Elem].copy(
              child = application.head.child ++ new Elem(null, "meta-data", Null, TopScope, true) %
                new PrefixedAttribute(prefix, "value", key, Null) %
                new PrefixedAttribute(prefix, "name", "io.fabric.ApiKey", Null)))),
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
        "version_name" -> verName.getOrElse(DEFAULT_VERSION_NAME),
        "version_code" -> verCode.getOrElse(DEFAULT_VERSION_CODE).toString) foreach (v => prop.put(v._1, v._2))
      IO.write(prop, ASSET_CRASHLYTICS_BUILD_DESC, assets / "crashlytics-build.properties")
      v
    })

  // DistributionTasks#uploadDistribution
  private def uploadDistribution(apk: File, packageName: String, id: String,
                                 verName: Option[String], verCode: Option[Int],
                                 apiKey: String, apiSecret: Option[String], editor: (String) => String,
                                 streams: TaskStreams) {
    import java.lang.System._
    import java.nio.file.Files
    import java.util.concurrent.TimeUnit.{NANOSECONDS, SECONDS}

    import scalaj.http.{Http, HttpRequest, HttpResponse, MultiPart}

    val unrolledSecret = apiSecret.getOrElse(fail("You must specify fabric.apiSecret before distribution uploading"))
    // todo apk sign check
    implicit class RichRequest(req: HttpRequest) {
      def crashlyticsHeaders() = req.headers(
        // todo move to constants.scala
        "User-Agent" -> s"Crashlytics Gradle Plugin for ${Option(getProperty("os.name")).getOrElse("unknown os")} / 1.21.7", // =)
        "X-CRASHLYTICS-API-KEY" -> apiKey,
        "X-CRASHLYTICS-BUILD-SECRET" -> unrolledSecret,
        // have no idea what is it and why it's const, but things are not working without this header
        "X-CRASHLYTICS-DEVELOPER-TOKEN" -> API_HEADER_DEVELOPER_TOKEN)
      def doIfSucceeded(f: HttpResponse[String] => Unit) { f.apply(req.asString.throwError) }
    }
    val log = streams.log
    val noteUrl = API_BASE_NOTES_FORMAT.format(packageName, id)
    log.debug(s"Requested note url: ${noteUrl}")
    val displayVersion = verName.getOrElse(DEFAULT_VERSION_NAME)
    val buildVersion = verCode.getOrElse(DEFAULT_VERSION_CODE).toString
    val notesText = editor(displayVersion)
    log.info("Sending release notes:")
    notesText.split('\n').foreach(v => log.info(' ' + v)) // brr
    // RestfulWebApi#performSetReleaseNotes
    Http(noteUrl).crashlyticsHeaders()
      .postForm(Seq("app[display_version]" -> displayVersion,
        "app[build_version]" -> buildVersion,
        // 'markdown' is also available, but seems like it doesn't make any difference
        "release_notes[format]" -> "text",
        "release_notes[body]" -> notesText))
      .method("PUT")
      .doIfSucceeded(r => {
        def stringPart(v: (String, String)) = MultiPart(v._1, v._1, "text/plain", v._2)
        log.debug(s"Release notes updated successfully (${r.code} / ${r.body})")
        val distributionUrl = API_DISTRIBUTION_UPLOAD_FORMAT.format(packageName)
        log.info(s"Sending apk distribution ...") // todo add 'progressbar'
        log.debug(s"Requested distribution url: ${distributionUrl}")
        // RestfulWebApi#createDistribution
        Http(distributionUrl).crashlyticsHeaders()
          // i think there is a lot of redundant parts, but gradle plugin does so
          .postMulti(MultiPart("distribution[file]", apk.getName, "application/octet-stream", Files.readAllBytes(apk.toPath)),
            stringPart("send_notifications" -> API_DISTRIBUTION_NOTIFY_TRUE),
            stringPart("distribution[built_at]" -> SECONDS.convert(nanoTime(), NANOSECONDS).toString),
            stringPart("app[instance_identifier]" -> id), // build_id
            stringPart("app[display_version]" -> displayVersion),
            stringPart("app[build_version]" -> buildVersion),
            stringPart("app[type]" -> "android_app"), // never changes
            stringPart("app[slices][][arch]" -> "java"), // it's too
            stringPart("app[slices][][uuid]" -> id)) // similar to instance_identifier
          .doIfSucceeded(r => { log.debug(s"Upload succeeded (${r.code} / ${r.body})") })
      })
  }

}
