package com.rudderlabs.android.sample.kotlin

import android.app.Application
import com.rudderstack.android.integration.braze.BrazeIntegrationFactory
import com.rudderstack.android.sdk.core.RudderClient
import com.rudderstack.android.sdk.core.RudderConfig
import com.rudderstack.android.sdk.core.RudderLogger

class MainApplication : Application() {
    companion object {
        lateinit var rudderClient: RudderClient
        const val WRITE_KEY = "1xXCubSHWXbpBI2h6EpCjKOsxmQ"
        const val DATA_PLANE_URL = "https://rudderstacgwyx.dataplane.rudderstack.com"
    }

    override fun onCreate() {
        super.onCreate()
        rudderClient = RudderClient.getInstance(
            this,
            WRITE_KEY,
            RudderConfig.Builder()
                .withDataPlaneUrl(DATA_PLANE_URL)
                .withLogLevel(RudderLogger.RudderLogLevel.VERBOSE)
                .withFactory(BrazeIntegrationFactory.FACTORY)
                .withTrackLifecycleEvents(true)
                .withRecordScreenViews(true)
                .build()
        )
    }
}