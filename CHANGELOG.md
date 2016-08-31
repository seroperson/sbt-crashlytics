## [0.2] - 2016-08-31
- Initial support for uploading apk distribution to beta
  - Use `crashlyticsUploadDistributionRelease` to package release apk and send it to beta.
  Make sure, that it signed and zipaligned.
- Automatically adding crashlytics library dependency
- `propertiesFile` key renamed to `fabricPropertiesFile`
- `fabricApiKey` now `String` and must be necessarily initialized unlike before

## [0.1] - 2016-05-12
- Injecting fabric api key into the generated manifest
- Injecting crashlytics build id into the resources
- Generating *assets/crashlytics-build.properties* file
