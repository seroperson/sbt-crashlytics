package crashlytics

object Constants {

  val DEPENDENCY_FABRIC_MAVEN = "https://maven.fabric.io/public"
  val DEPENDENCY_CRASHLYTICS_VERSION = "2.5.5"

  val PROPERTIES_API_KEY_KEY = "fabric.apiKey"
  val PROPERTIES_API_SECRET_KEY = "fabric.apiSecret"

  val DEFAULT_VERSION_NAME = "0.0" // Like in AndroidBuildHandler
  val DEFAULT_VERSION_CODE = 0
  val ASSET_CRASHLYTICS_BUILD_DESC = "Auto-generated properties file for crashlytics"

  val API_HEADER_DEVELOPER_TOKEN = "ed8fc3dc68a7475cc970eb1e9c0cb6603b0a3ea2"
  val API_BASE_ENDPOINT = "https://api.crashlytics.com"
  val API_BASE_NOTES_FORMAT = s"$API_BASE_ENDPOINT/spi/v1/platforms/android/apps/%s/releases/%s/build_server/notes"
  val API_DISTRIBUTION_ENDPOINT = "https://distribution-uploads.crashlytics.com"
  val API_DISTRIBUTION_UPLOAD_FORMAT = s"$API_DISTRIBUTION_ENDPOINT/spi/v1/platforms/android/apps/%s/distributions"
  val API_DISTRIBUTION_NOTIFY_TRUE = "true"
  val API_DISTRIBUTION_NOTIFY_FALSE = "suppress" // ohgod

}
