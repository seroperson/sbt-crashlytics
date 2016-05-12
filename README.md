Description
===========
sbt-crashlytics - it's a sbt plugin that helps you to get deal with [crashlytics](https://fabric.io/kits/android/crashlytics/) and [sbt-android](https://github.com/scala-android/sbt-android). For now this plugin just do initializing configuration to build working application that uses crashlytics.

Usage
=====
First of all, you need to include your [api key](https://fabric.io/settings/organizations/) in **local.properties** (by default):

```
fabric.apiKey=my-api-key
```

Now the action goes to **build.sbt** where you need to apply crashlytics settings to your android project:

```
androidBuild
minSdkVersion := "8"
targetSdkVersion := "23"
platformSdk := "android-23"
// ...
crashlyticsBuild
```

And add the crashlytics sdk dependency (like you any other library - via libraryDependencies key).
Basically it's all what you need.

License
=======

```
The MIT License (MIT)

Copyright (c) 2016 Daniil Sivak

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```
