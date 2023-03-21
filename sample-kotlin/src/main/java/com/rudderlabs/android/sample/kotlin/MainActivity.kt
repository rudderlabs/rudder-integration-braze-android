package com.rudderlabs.android.sample.kotlin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.rudderlabs.android.sample.kotlin.MainApplication.Companion.rudderClient
import com.rudderstack.android.sdk.core.*
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun onButtonTap(view: View) {
        when (view.id) {
            R.id.btn ->
                rudderClient.identify(
                    "test_user_id",
                    RudderTraits()
                        .putBirthday(Date(631172471000))
                        .putAddress(RudderTraits.Address()
                            .putCity("Palo Alto")
                            .putCountry("USA"))
                        .putFirstName("First Name")
                        .putLastName("Last Name")
                        .putGender("Male")
                        .putPhone("0123456789")
                        .putEmail("test@gmail.com")
                        .put("key-1", "value-1")
                        .put("key-2", 1234),
                    RudderOption()
                        .putExternalId("brazeExternalId", "2d31d085-4d93-4126-b2b3-94e651810673")
                )
            R.id.btn2 ->
                rudderClient.track("Install Attributed")
            R.id.btn3 ->
                rudderClient.track(
                    "Install Attributed",
                    RudderProperty()
                        .putValue(mapOf(Pair("campaign", mapOf(
                            Pair("source", "Source value"), Pair("name", "Name value"),
                            Pair("ad_group", "ad_group value"), Pair("ad_creative", "ad_creative value")
                        ))))
                )
            R.id.btn4 ->
                rudderClient.track("Order Completed")
            R.id.btn5 ->
                rudderClient.track("Order Completed", RudderProperty()
                    .putValue(mapOf(Pair("products", listOf(mapOf<String, Any>())))))
            R.id.btn6 ->
                rudderClient.track("Order Completed", RudderProperty()
                    .putValue(
                        mapOf(Pair("products",
                            listOf(mapOf<String, Any>(
                                Pair("product_id", "10011"),
                                Pair("quantity", 11),
                                Pair("price", 100.11),
                                Pair("Product-Key-1", "Product-Value-1"))
                            )
                        ))
                    )
                    .putValue("revenue", 123)
                    .putValue("currency", "INR")
                    .putValue("Key-1", "Value-1"))
            R.id.btn7 ->
                rudderClient.track("Order Completed", RudderProperty()
                    .putValue(
                        mapOf(Pair("products",
                            listOf(mapOf<String, Any>(
                                Pair("product_id", "1002"),
                                Pair("quantity", 12),
                                Pair("price", 100.22))
                            )
                        ))
                    )
                    .putValue("currency", "INR"))
            R.id.btn8 ->
                rudderClient.track("Ecomm track events", RudderProperty()
                    .putValue(
                        mapOf(Pair("custom",
                            listOf(
                                mapOf<String, Any>(
                                    Pair("product_id", "1002"),
                                    Pair("quantity", 12),
                                    Pair("price", 100.22))
                            )
                        ))
                    )
                    .putValue("currency", "INR"))
            R.id.btn9 ->
                rudderClient.track("Order Completed", RudderProperty()
                    .putValue(
                        mapOf(Pair("products",
                            listOf(
                                mapOf<String, Any>(
                                    Pair("product_id", "1002"),
                                    Pair("quantity", 12),
                                    Pair("price", 100.22)),
                                mapOf<String, Any>(
                                    Pair("product_id", "1003"),
                                    Pair("quantity", 5),
                                    Pair("price", 89.50))
                            )
                        ))
                    )
                    .putValue("currency", "INR"))
            R.id.btn10 ->
                rudderClient.track("New Track event", RudderProperty()
                    .putValue("key_1", "value_1")
                    .putValue("key_2", "value_2"))
            R.id.btn11 ->
                rudderClient.track("New Track event")
        }
    }
}
