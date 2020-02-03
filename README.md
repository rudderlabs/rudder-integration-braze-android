[ ![Download](https://api.bintray.com/packages/rudderlabs-bintray/rudder-sdk-android-core/rudder-integration-braze-android/images/download.svg?version=0.1.1) ](https://bintray.com/rudderlabs-bintray/rudder-sdk-android-core/rudder-integration-braze-android/0.1.1/link)

# What is Rudder?

**Short answer:** 
Rudder is an open-source Segment alternative written in Go, built for the enterprise. .

**Long answer:** 
Rudder is a platform for collecting, storing and routing customer event data to dozens of tools. Rudder is open-source, can run in your cloud environment (AWS, GCP, Azure or even your data-centre) and provides a powerful transformation framework to process your event data on the fly.

Released under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Getting Started with Braze Integration of Android SDK
1. Add [Braze](https://www.braze.com) as a destination in the [Dashboard](https://app.rudderlabs.com/) and define ```apiToken```  

2. Add these lines to your ```app/build.gradle```
```
repositories {
  maven {
    url  "https://dl.bintray.com/rudderlabs-bintray/rudder-sdk-android-core"
  }
}
```
3. Add the dependency under ```dependencies```
```
implementation 'com.rudderlabs.android.sdk:rudder-sdk-core:0.1.4'
implementation 'com.rudderlabs.android.integration.braze:rudder-integration-braze-android:0.1.1'
```

## Initialize ```RudderClient```
```
val rudderClient: RudderClient = RudderClient.getInstance(
    this,
    WRITE_KEY,
    RudderConfig.Builder()
        .withEndPointUri(END_POINT_URI)
        .withLogLevel(RudderLogger.RudderLogLevel.DEBUG)
        .withFactory(BrazeIntegrationFactory.FACTORY)
        .build()
)
```

## Send Events
Follow the steps from [Rudder Android SDK](https://github.com/rudderlabs/rudder-sdk-android)

# Coming Soon
1. Native platform SDK integration support
2. More documentation
3. More destination support
