package com.rudderlabs.android.sample.kotlin

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.rudderlabs.android.sample.kotlin.MainApplication.Companion.rudderClient
import com.rudderstack.android.sdk.core.RudderMessageBuilder
import com.rudderstack.android.sdk.core.RudderTraits
import com.rudderstack.android.sdk.core.TrackPropertyBuilder

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sendEvents()
    }

    private fun sendEvents() {
        val traits = RudderTraits()
        traits.putEmail("test_nana@gmail.com")
        traits.putFirstName("test_nana")
        traits.putPhone("9876543210")
        val address = RudderTraits.Address()
        address.putCity("city")
        address.putCountry("KSA")
        traits.putAddress(address)
        rudderClient.identify("userModel.userId", traits, null)

//        MainApplication.rudderClient.track(
//            RudderMessageBuilder()
//                .setEventName("daily_rewards_claim")
//                .setProperty(
//                    TrackPropertyBuilder()
//                        .setCategory("test_category")
//                        .build()
//                )
//                .setUserId("test_user_id")
//        )
//
//        MainApplication.rudderClient.identify("developer_user_id")
//
//        MainApplication.rudderClient.track(
//            RudderMessageBuilder()
//                .setEventName("level_up")
//                .setProperty(
//                    TrackPropertyBuilder()
//                        .setCategory("test_category")
//                        .build()
//                )
//                .setUserId("test_user_id")
//        )
//
//        MainApplication.rudderClient.reset()
//
//        val revenueProperty = TrackPropertyBuilder()
//            .setCategory("test_category")
//            .build()
//        revenueProperty.put("total", 4.99)
//        revenueProperty.put("currency", "USD")
//        MainApplication.rudderClient.track(
//            RudderMessageBuilder()
//                .setEventName("revenue")
//                .setProperty(revenueProperty)
//                .setUserId("test_user_id")
//        )
    }
}
