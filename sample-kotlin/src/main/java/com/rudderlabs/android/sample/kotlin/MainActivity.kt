package com.rudderlabs.android.sample.kotlin

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.rudderlabs.android.sample.kotlin.MainApplication.Companion.rudderClient
import com.rudderstack.android.sdk.core.RudderMessageBuilder
import com.rudderstack.android.sdk.core.RudderOption
import com.rudderstack.android.sdk.core.RudderTraits
import com.rudderstack.android.sdk.core.TrackPropertyBuilder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sendEvents()
    }

    private fun sendEvents() {
        rudderClient.track("test_track_pre_identify")
        rudderClient.identify(
            "test_user_id",
            RudderTraits()
                .putFirstName("First Name"),
            RudderOption()
                .putExternalId("brazeExternalId", "2d31d085-4d93-4126-b2b3-94e651810673")
        )
    }
}
