package com.rudderlabs.android.sample.kotlin

import android.app.Application
import com.rudderlabs.android.integration.braze.BrazeIntegrationFactory
import com.rudderlabs.android.sdk.core.RudderClient
import com.rudderlabs.android.sdk.core.RudderConfig
import com.rudderlabs.android.sdk.core.RudderLogger

class MainApplication : Application() {
    companion object {
        lateinit var rudderClient: RudderClient
    }

    override fun onCreate() {
        super.onCreate()
        rudderClient = RudderClient.getInstance(
            this,
            BuildConfig.WRITE_KEY,
            RudderConfig.Builder()
                .withEndPointUri(BuildConfig.END_POINT_URL)
                .withLogLevel(RudderLogger.RudderLogLevel.DEBUG)
                .withFactory(BrazeIntegrationFactory.FACTORY)
                .build()
        )
    }
}