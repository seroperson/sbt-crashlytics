Description
===========
sbt-crashlytics - it's a sbt plugin that helps you to get deal with [crashlytics](https://fabric.io/kits/android/crashlytics/)
and [sbt-android](https://github.com/scala-android/sbt-android). For now this plugin
just do initializing configuration to build working application that uses crashlytics.

Usage
=====
First of all, you need to include your [api key](https://fabric.io/settings/organizations/)
into the **local.properties** (by default):

```scala
fabric.apiKey=my-api-key
```

Now the action goes to **build.sbt** where you need to apply crashlytics settings to
your android project:

```scala
androidBuild
minSdkVersion := "8"
targetSdkVersion := "23"
platformSdk := "android-23"
// ...
crashlyticsBuild
```

Crashlytics dependency will be added automatically.
So, that is basically all what you need.

Tips
====
While uploading apk to beta, you need to write release notes. By default, `$EDITOR`
will be opened for this stuff. But if you too lazy for writing it by yourself, you can
redefine `crashlyticsReleaseNotesCreator` key to implement automatic generation.
For example, you want to post your commit history since last tag along with distribution.
It will be like that (*code isn't excellent and it's here just to show you how
flexible is the feature*):

```scala
crashlyticsReleaseNotesCreator := (v => {
  import scala.util.{Success, Try}

  Process(Seq("git", "log", "--oneline", (Try(Process(Seq("git", "describe", "--abbrev=0", "--tags")).!!) match {
    case Success(v) => s"$v.."
    case _ => ""
  }) + "HEAD")).lines.mkString("\n")
})
```

Download
========
Just include the following line in your **project/plugins.sbt**

```scala
addSbtPlugin("com.seroperson" % "sbt-crashlytics" % "0.2")
```

Be sure that you also included the latest version of sbt-android plugin.

License
=======

```
The MIT License (MIT)

Copyright (c) 2016 Daniil Sivak

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```
