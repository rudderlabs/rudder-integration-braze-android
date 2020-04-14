package com.rudderlabs.android.sample.kotlin

import android.app.Application
import com.rudderstack.android.integration.braze.BrazeIntegrationFactory
import com.rudderstack.android.sdk.core.RudderClient
import com.rudderstack.android.sdk.core.RudderConfig
import com.rudderstack.android.sdk.core.RudderLogger

class MainApplication : Application() {
    companion object {
        lateinit var rudderClient: RudderClient
        const val WRITE_KEY = "1ZOVzjHRL0Vpk627qpkmcIYLrv3"
        const val DATA_PLANE_URL = "https://6be9fce2.ngrok.io"
    }

    override fun onCreate() {
        super.onCreate()
        rudderClient = RudderClient.getInstance(
            this,
            WRITE_KEY,
            RudderConfig.Builder()
                .withDataPlaneUrl(DATA_PLANE_URL)
                .withLogLevel(RudderLogger.RudderLogLevel.DEBUG)
                .withFactory(BrazeIntegrationFactory.FACTORY)
                .withTrackLifecycleEvents(true)
                .withRecordScreenViews(true)
                .build()
        )
    }
}