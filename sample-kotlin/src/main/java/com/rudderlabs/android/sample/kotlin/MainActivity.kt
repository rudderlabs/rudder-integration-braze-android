package com.rudderlabs.android.sample.kotlin

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.rudderlabs.android.sample.kotlin.MainApplication.Companion.rudderClient
import com.rudderstack.android.sdk.core.RudderMessageBuilder
import com.rudderstack.android.sdk.core.RudderOption
import com.rudderstack.android.sdk.core.RudderTraits
import com.rudderstack.android.sdk.core.TrackPropertyBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private var count = 10
    private val tests = arrayOf(
        "initial_identify",
        "test_track_pre_identify",
        "external_id",
        "user_id",
        "birthday",
        "email",
        "firstname",
        "lastname",
        "gender",
        "phone",
        "address",
        "boolean",
        "integer",
        "double",
        "float",
        "long",
        "date",
        "string",
        "reset",
        "finish"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btn.setOnClickListener { sendEvents() }
    }

    private fun sendEvents() {
        val rem = this.count % 20
        textView.text = this.tests[rem]
        when (rem) {
            0 -> {
                // initial track call
                rudderClient.track("test_track_pre_identify")
            }
            1 -> {
                // initial identify call
                rudderClient.identify(
                    "test_user_id",
                    RudderTraits()
                        .putFirstName("First Name"),
                    RudderOption()
                        .putExternalId("brazeExternalId", "2d31d085-4d93-4126-b2b3-94e651810673")
                )
            }
            2 -> {
                // external id update
                rudderClient.identify(
                    RudderTraits()
                        .putId("test_user_id"),
                    RudderOption()
                        .putExternalId("brazeExternalId", "2d31d085-4d93-4126-b2b3-94e651810674")
                )
            }
            3 -> {
                // user id update
                rudderClient.identify(RudderTraits().putId("test_user_id_update"))
            }
            4 -> {
                // birthday update
                rudderClient.identify(RudderTraits().putBirthday("2012-06-04"))
            }
            5 -> {
                // email update
                rudderClient.identify(RudderTraits().putEmail("example@org.com"))
            }
            6 -> {
                // firstName update
                rudderClient.identify(RudderTraits().putFirstName("Angelina"))
            }
            7 -> {
                // lastName update
                rudderClient.identify(RudderTraits().putLastName("Jolie"))
            }
            8 -> {
                // gender update
                rudderClient.identify(RudderTraits().putGender("F"))
            }
            9 -> {
                // phone update
                rudderClient.identify(RudderTraits().putPhone("+1987543639"))
            }
            10 -> {
                // address update
                rudderClient.identify(
                    RudderTraits().putAddress(
                        RudderTraits.Address()
                            .putCity("Palo Alto")
                            .putCountry("USA")
                            .putPostalCode("98754")
                            .putState("SF")
                            .putStreet("94th Street")
                    )
                )
            }
            11 -> {
                // boolean
                rudderClient.identify(RudderTraits().put("boolKey", true))
            }
            12 -> {
                // integer
                rudderClient.identify(RudderTraits().put("intKey", 4))
            }
            13 -> {
                // double
                rudderClient.identify(RudderTraits().put("doubleKey", 5.6))
            }
            14 -> {
                // float
                rudderClient.identify(RudderTraits().put("floatKey", 5.6f))
            }
            15 -> {
                // long
                rudderClient.identify(RudderTraits().put("longKey", 5L))
            }
            16 -> {
                // Date
                rudderClient.identify(RudderTraits().put("dateKey", Date()))
            }
            17 -> {
                // String
                rudderClient.identify(RudderTraits().put("strKey", "test_string"))
            }
            18 -> {
                rudderClient.reset()
            }
            19 -> {
                textView.text = "Finished testing"
            }
        }

        this.count += 1
    }
}
