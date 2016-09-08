## [0.3] - 2016-09-08
- Initial support for deobs files uploading.
- API requests now handles by okhttp and you can redefine `crashlyticsOkHttpClient`
  to set your own `OkHttpClient`.
- `sbt-android` was updated to `1.6.5`.

## [0.2] - 2016-08-31
- Initial support for uploading apk distribution to beta.
  - Use `crashlyticsUploadDistributionRelease` to package release apk and send it to beta.
  Make sure, that it's signed (gradle plugin requires it, but seems like
  it's optional).
- Automatically adding crashlytics library dependency.
- `propertiesFile` key renamed to `fabricPropertiesFile`.
- `fabricApiKey` now `String` and must be necessarily initialized unlike before.

## [0.1] - 2016-05-12
- Injecting fabric api key into the generated manifest.
- Injecting crashlytics build id into the resources.
- Generating *assets/crashlytics-build.properties* file.
