package crashlytics

import android.Keys._
import sbt.Keys._
import sbt._

object Crashlytics extends AutoPlugin {

  import java.util.Properties

  import Constants._
  import Keys._
  import okhttp3._

  // There is 'io.fabric.tools % gradle' package with a lot of stuff that already implemented in,
  // but it's hard to use in result of couple of reasons.

  override def trigger = allRequirements
  override def requires = android.AndroidPlugin

  object autoImport {
    val crashlyticsBuild = Seq(
      crashlyticsOkHttpClient := new OkHttpClient(),
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

      // Upload deobs file immediately after proguard task
      proguard <<= (proguard, projectLayout, outputLayout, versionName, versionCode, fabricApiKey, crashlyticsBuildId,
        applicationId, streams, crashlyticsOkHttpClient) map (Api.uploadMapping),

      // todo looks shitty
      crashlyticsUploadDistributionRelease <<= (packageRelease, applicationId, crashlyticsBuildId, versionName, versionCode,
        fabricApiKey, fabricApiSecret, crashlyticsReleaseNotesCreator, streams, crashlyticsOkHttpClient) map (Api.uploadDistribution),
      crashlyticsUploadDistributionDebug <<= (packageDebug, applicationId, crashlyticsBuildId, versionName, versionCode,
        fabricApiKey, fabricApiSecret, crashlyticsReleaseNotesCreator, streams, crashlyticsOkHttpClient) map (Api.uploadDistribution),

      // Or redefine the key to enforce some editor for all project members
      crashlyticsReleaseNotesCreator := (version => { // todo return just f0?
        import sbt.IO._

        withTemporaryFile("crashlytics_note_", "") { file =>
          write(file, NOTES_DESCRIPTION_MESSAGE_FORMAT.format(version))
          if (Process(Seq(sys.env.get("EDITOR").getOrElse(fail("Set $EDITOR env variable to edit the release notes")), file.getCanonicalPath)).!< == 0) {
            readLines(file).foldLeft(StringBuilder.newBuilder)((b, l) => b.append(s"\n$l")).tail.toString()
          }
          else {
            fail("Notes aren't saved, editor finished with non-zero code.")
          }
        }
      }),

      // Writing com.crashlytics.android.build_id value directly to values.xml
      resValues <<= (crashlyticsBuildId, resValues) map { (id, seq) => seq :+ ("string", "com.crashlytics.android.build_id", id) },

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
  }

  private def fail(m: String) = throw new MessageOnlyException(m)

  private[this] object Api {
    import java.io.{BufferedOutputStream, File, FileOutputStream}

    import okhttp3.MultipartBody.FORM
    import okhttp3.MultipartBody.Part.createFormData
    import okhttp3.RequestBody.create

    private implicit class RichRequest(req: Request.Builder) {
      import scala.collection.JavaConversions._
      def crashlyticsHeaders(apiKey: String) = req.headers(Headers.of(Map(
        "X-CRASHLYTICS-API-KEY" -> apiKey,
        // have no idea what is it and why it's const, but things are not working without this header
        "X-CRASHLYTICS-DEVELOPER-TOKEN" -> API_HEADER_DEVELOPER_TOKEN)))
      def crashlyticsHeaders(apiKey: String, apiSecret: String): Request.Builder = crashlyticsHeaders(apiKey).header("X-CRASHLYTICS-BUILD-SECRET", apiSecret)
    }
    private implicit class RichResponse(res: Response) {
      def throwIfError() { if(!res.isSuccessful) throw new Exception(s"Request failed with code ${res.code()}") }
    }

    private implicit def tuple2FormPart(v: (String, String)) = MultipartBody.Part.createFormData(v._1, v._2)
    private implicit def str2MediaType(v: String) = MediaType.parse(v)

    def uploadMapping(proguard: Option[File], layout: ProjectLayout, converter: android.BuildOutput.Converter,
                      verName: Option[String], verCode: Option[Int],
                      apiKey: String, id: String, packageName: String,
                      streams: TaskStreams, okHttpClient: OkHttpClient) = {
      if(proguard.isDefined) {
          import java.util.zip.{ZipEntry, ZipOutputStream}
          val log = streams.log
          // sbt-android writes into mappings.txt
          val mapping = converter.apply(layout).proguardOut / "mappings.txt"
          // DataDirDeobsManager#storeDeobfuscationFile
          IO.withTemporaryFile("crashlytics_zipped_mapping_", "") { file =>
            log.debug(s"Zipping mappings.txt file into ${file.getCanonicalPath}")
            val out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)))
            // Crashlytics requires to zip 'mapping.txt' file
            out.putNextEntry(new ZipEntry("mapping.txt"))
            try {
              IO.readBytes(mapping).grouped(2048).foreach(arr => out.write(arr))
              out.flush()
              out.closeEntry()
            }
            finally { out.close() }
            val mappingUrl = API_MAPPING_UPLOAD_URL
            log.info("Sending mapping.txt ...")
            log.debug(s"Requested code mapping url: $mappingUrl")
            okHttpClient.newCall(new Request.Builder().url(mappingUrl).crashlyticsHeaders(apiKey)
              .post(new MultipartBody.Builder().setType(FORM)
                .addPart("code_mapping[type]" -> "proguard")
                .addPart(createFormData("code_mapping[file]", s"$id.zip", create("application/zip", file)))
                .addPart("code_mapping[executables][][arch]" -> "java")
                .addPart("code_mapping[executables][][identifier]" -> id.replace("-", "").toLowerCase)
                .addPart("project[identifier]" -> packageName)
                .addPart("project[build_version]" -> verCode.get.toString)
                .addPart("project[display_version]" -> verName.get)
                .build())
              .build()).execute().throwIfError()
          }
      }
      proguard
    }
    // DistributionTasks#uploadDistribution
    def uploadDistribution(apk: File, packageName: String, id: String,
                           verName: Option[String], verCode: Option[Int],
                           apiKey: String, apiSecret: Option[String],
                           editor: (String) => String, streams: TaskStreams,
                           okHttpClient: OkHttpClient) {
      // - Retrieving release notes via crashlyticsReleaseNotesCreator
      // - Sending notes to Constants.API_BASE_NOTES_FORMAT
      //   Without creating release notes the distribution will be uploaded successfully, but will be 'invisible'
      // - Uploading distribution to Constants.API_DISTRIBUTION_UPLOAD_FORMAT
      // todo apk sign check?
      val unrolledSecret = apiSecret.getOrElse(fail("You must specify fabric.apiSecret before distribution uploading"))
      val log = streams.log
      val displayVersion = verName.getOrElse(DEFAULT_VERSION_NAME)
      val buildVersion = verCode.getOrElse(DEFAULT_VERSION_CODE).toString
      val notesText = editor(displayVersion)
      log.info("Sending release notes:")
      notesText.split('\n').foreach(v => log.info(" " * 1 + v)) // 1 is 'padding level'
      val noteUrl = API_BASE_NOTES_FORMAT.format(packageName, id)
      log.debug(s"Requested note url: $noteUrl")
      // RestfulWebApi#performSetReleaseNotes
      okHttpClient.newCall(new Request.Builder().url(noteUrl).crashlyticsHeaders(apiKey, unrolledSecret)
        .method("PUT", new MultipartBody.Builder().setType(FORM)
          .addPart("app[display_version]" -> displayVersion)
          .addPart("app[build_version]" -> buildVersion)
          // 'markdown' is also available, but seems like it doesn't make any difference
          .addPart("release_notes[format]" -> "text")
          .addPart("release_notes[body]" -> notesText)
          .build())
        .build()).execute().throwIfError()

      import java.lang.System.nanoTime
      import java.util.concurrent.TimeUnit.{NANOSECONDS, SECONDS}
      val distributionUrl = API_DISTRIBUTION_UPLOAD_FORMAT.format(packageName)
      log.info(s"Sending apk distribution ...") // todo add 'progressbar'
      log.debug(s"Requested distribution url: $distributionUrl")
      // RestfulWebApi#createDistribution
      okHttpClient.newCall(new Request.Builder().url(distributionUrl).crashlyticsHeaders(apiKey, unrolledSecret)
        // i think there is a lot of redundant parts, but gradle plugin does so
        .post(new MultipartBody.Builder().setType(FORM)
          .addPart(createFormData("distribution[file]", apk.getName(), create("application/octet-stream", apk)))
          // 'true' and 'suppress' (wow) are available to notify (or not) your testers about new distributions
          .addPart("send_notifications" -> "true")
          .addPart("distribution[built_at]" -> SECONDS.convert(nanoTime(), NANOSECONDS).toString)
          .addPart("app[instance_identifier]" -> id) // build_id
          .addPart("app[display_version]" -> displayVersion)
          .addPart("app[build_version]" -> buildVersion)
          .addPart("app[type]" -> "android_app") // never changes
          .addPart("app[slices][][arch]" -> "java") // it's too
          .addPart("app[slices][][uuid]" -> id) // similar to instance_identifier
          .build())
        .build()).execute().throwIfError()
    }
  }
}