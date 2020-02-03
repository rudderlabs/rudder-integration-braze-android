package com.rudderlabs.android.integration.braze;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;


import com.appboy.Appboy;
import com.appboy.configuration.AppboyConfig;
import com.appboy.enums.Gender;
import com.appboy.enums.Month;
import com.appboy.models.outgoing.AppboyProperties;
import com.appboy.models.outgoing.AttributionData;
import com.appboy.support.StringUtils;
import com.appboy.ui.inappmessage.AppboyInAppMessageManager;
import com.rudderstack.android.sdk.core.MessageType;
import com.rudderstack.android.sdk.core.RudderClient;
import com.rudderstack.android.sdk.core.RudderConfig;
import com.rudderstack.android.sdk.core.RudderIntegration;
import com.rudderstack.android.sdk.core.RudderMessage;
import com.rudderstack.android.sdk.core.RudderTraits;
import com.rudderstack.android.sdk.core.util.Utils;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BrazeIntegrationFactory extends RudderIntegration<Appboy> {
    private static final String APPBOY_KEY = "Braze";
    private static final String LOG_KEY = "RudderSDK-Braze";
    private Appboy appBoy;
    private Map<String, String> eventMap = new HashMap<>();

    public static Factory FACTORY = new Factory() {
        @Override
        public RudderIntegration<?> create(Object settings, RudderClient client, RudderConfig rudderConfig) {
            return new BrazeIntegrationFactory(settings, client, rudderConfig);
        }

        @Override
        public String key() {
            return APPBOY_KEY;
        }
    };


    private static final Set<String> MALE_KEYS = new HashSet<String>(Arrays.asList("M",
            "MALE"));
    private static final Set<String> FEMALE_KEYS = new HashSet<String>(Arrays.asList("F",
            "FEMALE"));

    private static final String AUTO_IN_APP_MESSAGE_REGISTER =
            "auto_in_app_message_register";

    private static final String DEFAULT_CURRENCY_CODE = "USD";


    private static final String CUSTOM_ENDPOINT_KEY = "customEndpoint";
    private static final String REVENUE_KEY = "revenue";
    private static final String CURRENCY_KEY = "currency";

    private static final List<String> RESERVED_KEYSET = Arrays.asList("birthday", "email", "firstName",
            "lastName", "gender", "phone", "address");



    private static final String API_KEY  = "appKey";

    private boolean autoInAppMessageRegEnabled;

    private BrazeIntegrationFactory(Object config, final RudderClient client, RudderConfig rudderConfig) {


        String apiKey = "";
        Map<String, Object> destinationConfig = (Map<String, Object>) config;
        if (destinationConfig != null )
        {
            if(destinationConfig.containsKey(API_KEY)) {
                apiKey = (String) destinationConfig.get(API_KEY);
            }
            if(destinationConfig.containsKey(AUTO_IN_APP_MESSAGE_REGISTER)) {
                autoInAppMessageRegEnabled =
                        Boolean.getBoolean((String)destinationConfig.get(AUTO_IN_APP_MESSAGE_REGISTER ));
            }
        }
        if (StringUtils.isNullOrBlank(apiKey)) {
            Log.w(LOG_KEY,"Braze integration not initialized due to invalid api key.");
            return;
        }


        AppboyConfig.Builder builder = new AppboyConfig.Builder()
                .setApiKey(apiKey);
        /*if (!StringUtils.isNullOrBlank(customEndpoint)) {
            builder.setCustomEndpoint(customEndpoint);
        }*/

        Appboy.configure(client.getApplication().getApplicationContext(), builder.build());
        this.appBoy = Appboy.getInstance(client.getApplication()) ;

        Log.i(LOG_KEY,"Configured Braze + Rudder integration and initialized Braze.");



        if (client.getApplication() != null) {
            client.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(Activity activity, Bundle bundle) {

                }

                @Override
                public void onActivityStarted(Activity activity) {
                    appBoy.openSession(activity);


                }

                @Override
                public void onActivityResumed(Activity activity) {
                    if (autoInAppMessageRegEnabled) {
                        AppboyInAppMessageManager.getInstance().registerInAppMessageManager(activity);
                    }
                }

                @Override
                public void onActivityPaused(Activity activity) {
                    if (autoInAppMessageRegEnabled) {
                        AppboyInAppMessageManager.getInstance().unregisterInAppMessageManager(activity);
                    }
                }

                @Override
                public void onActivityStopped(Activity activity) {
                    appBoy.closeSession(activity);

                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

                }

                @Override
                public void onActivityDestroyed(Activity activity) {

                }
            });
        }
    }

    private void processRudderEvent(RudderMessage element) {
        if (element != null && element.getType() != null) {
            switch (element.getType()) {
                case MessageType.TRACK:
                    if (element == null) {
                        return;
                    }
                    String event =  element.getEventName() ;
                    if (event == null) {
                        return;
                    }
                    Map<String, Object> eventProperties = element.getProperties();
                    try {
                        if (element.getEventName().equals("Install Attributed")) {

                            Map<String, Object> campaignProps =  (Map<String, Object>)eventProperties.get("campaign");

                            if (campaignProps != null) {
                                appBoy.getCurrentUser().setAttributionData(new AttributionData(
                                        (String) campaignProps.get("source"),
                                        (String)campaignProps.get("name"),
                                        (String) campaignProps.get("ad_group"),
                                        (String) campaignProps.get("ad_creative")));
                            }
                            return;
                        }
                    } catch (Exception exception) {
                        Log.v(LOG_KEY, "Check the format of Install Attributed event . Caught "+ exception);
                    }
                    if (eventProperties == null || eventProperties.size() == 0) {
                        Log.v(LOG_KEY,"appboy event has no properties");
                        appBoy.logCustomEvent(element.getEventName());
                        return;
                    }
                    JSONObject propertiesJson = new JSONObject( eventProperties);


                    if (eventProperties.containsKey(REVENUE_KEY) ) {

                        double revenue = Double.parseDouble(String.valueOf(eventProperties.get(REVENUE_KEY)));

                        String currency = String.valueOf(eventProperties.get(CURRENCY_KEY));
                        if (revenue != 0) {
                            String currencyCode = StringUtils.isNullOrBlank(currency) ? DEFAULT_CURRENCY_CODE
                                    : currency;
                            propertiesJson.remove(REVENUE_KEY);
                            propertiesJson.remove(CURRENCY_KEY);
                            if (propertiesJson.length() == 0) {
                                Log.v(LOG_KEY,"appboy logPurchase for purchase "+element.getEventName()+" for "+revenue+" "+ currencyCode+" with no"
                                        + " properties." );
                                appBoy.logPurchase(element.getEventName(), currencyCode, new BigDecimal(revenue));
                            } else {
                                Log.v(LOG_KEY,"appboy logPurchase for purchase "+element.getEventName()+" for "+revenue+" "+ currencyCode+" "+propertiesJson.toString());
                                appBoy.logPurchase(event, currencyCode, new BigDecimal(revenue),
                                        new AppboyProperties(propertiesJson));
                            }
                        }
                    }else {
                        Log.v(LOG_KEY,"appBoy logCustomEvent for event "+element.getEventName()+" with properties % "+ propertiesJson.toString());
                        appBoy.logCustomEvent(event, new AppboyProperties(propertiesJson));
                    }

                    break;
                case MessageType.IDENTIFY:

                    String userId = element.getUserId();
                    if (!StringUtils.isNullOrBlank(userId)) {
                        appBoy.changeUser(userId);
                    }

                    Map<String,Object> traitsMap = element.getTraits();

                    if (traitsMap == null) {
                        return;
                    }

                    Date birthday = Utils.dateFromString(RudderTraits.getBirthday(traitsMap));
                    if (birthday != null) {
                        Calendar birthdayCal = Calendar.getInstance(Locale.US);
                        birthdayCal.setTime(birthday);
                        appBoy.getCurrentUser().setDateOfBirth(birthdayCal.get(Calendar.YEAR),
                                Month.values()[birthdayCal.get(Calendar.MONTH)],
                                birthdayCal.get(Calendar.DAY_OF_MONTH));
                    }

                    String email = RudderTraits.getEmail(traitsMap);
                    if (!StringUtils.isNullOrBlank(email)) {
                        appBoy.getCurrentUser().setEmail(email);
                    }

                    String firstName = RudderTraits.getFirstname(traitsMap);
                    if (!StringUtils.isNullOrBlank(firstName)) {
                        appBoy.getCurrentUser().setFirstName(firstName);
                    }

                    String lastName = RudderTraits.getLastname(traitsMap);
                    if (!StringUtils.isNullOrBlank(lastName)) {
                        appBoy.getCurrentUser().setLastName(lastName);
                    }

                    String gender = RudderTraits.getGender(traitsMap);
                    if (!StringUtils.isNullOrBlank(gender)) {
                        if (MALE_KEYS.contains(gender.toUpperCase())) {
                            appBoy.getCurrentUser().setGender(Gender.MALE);
                        } else if (FEMALE_KEYS.contains(gender.toUpperCase())) {
                            appBoy.getCurrentUser().setGender(Gender.FEMALE);
                        }
                    }

                    String phone = RudderTraits.getPhone(traitsMap);
                    if (!StringUtils.isNullOrBlank(phone)) {
                        appBoy.getCurrentUser().setPhoneNumber(phone);
                    }

                    RudderTraits.Address address = RudderTraits.Address.fromString(RudderTraits.getAddress(traitsMap));
                    if (address != null) {
                        String city = address.getCity();
                        if (!StringUtils.isNullOrBlank(city)) {
                            appBoy.getCurrentUser().setHomeCity(city);
                        }
                        String country = address.getCountry();
                        if (!StringUtils.isNullOrBlank(country)) {
                            appBoy.getCurrentUser().setCountry(country);
                        }
                    }

                    for (String key : traitsMap.keySet()) {
                        if (RESERVED_KEYSET.contains(key)) {
                            continue;
                        }
                        Object value = traitsMap.get(key);
                        if (value instanceof Boolean) {
                            appBoy.getCurrentUser().setCustomUserAttribute(key, (Boolean) value);
                        } else if (value instanceof Integer) {
                            appBoy.getCurrentUser().setCustomUserAttribute(key, (Integer) value);
                        } else if (value instanceof Double) {
                            appBoy.getCurrentUser().setCustomUserAttribute(key, (Double) value);
                        } else if (value instanceof Float) {
                            appBoy.getCurrentUser().setCustomUserAttribute(key, (Float) value);
                        } else if (value instanceof Long) {
                            appBoy.getCurrentUser().setCustomUserAttribute(key, (Long) value);
                        } else if (value instanceof Date) {
                            long secondsFromEpoch = ((Date) value).getTime() / 1000L;
                            appBoy.getCurrentUser().setCustomUserAttributeToSecondsFromEpoch(key, secondsFromEpoch);
                        } else if (value instanceof String) {
                            appBoy.getCurrentUser().setCustomUserAttribute(key, (String) value);
                        } else {
                            Log.d(LOG_KEY,"Appboy can't map rudder value for custom Appboy user "
                                    + "attribute with key "+  key + "and value "+value);
                        }
                    }

                    break;
                case MessageType.SCREEN:
                    Log.w(LOG_KEY,"BrazeIntegrationFactory: MessageType is not supported");
                    break;
                default:
                    Log.w(LOG_KEY,"BrazeIntegrationFactory: MessageType is not specified");
                    break;
            }
        }
    }

    @Override
    public void flush() {
        super.flush();
        Log.w(LOG_KEY," appboy requestImmediateDataFlush().");
        appBoy.requestImmediateDataFlush();
    }


     @Override
    public void reset() {
        //this.adjust.resetSessionPartnerParameters();
    }


    @Override
    public void dump(RudderMessage element) {

        if(appBoy != null)
            processRudderEvent(element);
    }

    @Override
    public Appboy getUnderlyingInstance() {
        return appBoy;
    }
}
