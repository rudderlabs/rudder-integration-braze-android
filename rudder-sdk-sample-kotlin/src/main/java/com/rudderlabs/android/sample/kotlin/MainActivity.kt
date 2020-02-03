package com.rudderlabs.android.sample.kotlin

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.rudderlabs.android.sdk.core.RudderMessageBuilder
import com.rudderlabs.android.sdk.core.TrackPropertyBuilder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sendEvents()
    }

    private fun sendEvents() {
        MainApplication.rudderClient.track(
            RudderMessageBuilder()
                .setEventName("daily_rewards_claim")
                .setProperty(
                    TrackPropertyBuilder()
                        .setCategory("test_category")
                        .build()
                )
                .setUserId("test_user_id")
        )

        MainApplication.rudderClient.identify("developer_user_id")

        MainApplication.rudderClient.track(
            RudderMessageBuilder()
                .setEventName("level_up")
                .setProperty(
                    TrackPropertyBuilder()
                        .setCategory("test_category")
                        .build()
                )
                .setUserId("test_user_id")
        )

        MainApplication.rudderClient.reset()

        val revenueProperty = TrackPropertyBuilder()
            .setCategory("test_category")
            .build()
        revenueProperty.put("total", 4.99)
        revenueProperty.put("currency", "USD")
        MainApplication.rudderClient.track(
            RudderMessageBuilder()
                .setEventName("revenue")
                .setProperty(revenueProperty)
                .setUserId("test_user_id")
        )
    }
}
