package com.rudderstack.android.integration.braze;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.appboy.Appboy;
import com.braze.configuration.BrazeConfig;
import com.appboy.enums.Gender;
import com.appboy.enums.Month;
import com.braze.models.outgoing.BrazeProperties;
import com.appboy.models.outgoing.AttributionData;
import com.braze.support.BrazeLogger;
import com.braze.ui.inappmessage.BrazeInAppMessageManager;
import com.rudderstack.android.sdk.core.MessageType;
import com.rudderstack.android.sdk.core.RudderClient;
import com.rudderstack.android.sdk.core.RudderConfig;
import com.rudderstack.android.sdk.core.RudderIntegration;
import com.rudderstack.android.sdk.core.RudderLogger;
import com.rudderstack.android.sdk.core.RudderMessage;
import com.rudderstack.android.sdk.core.RudderTraits;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BrazeIntegrationFactory extends RudderIntegration<Appboy> {
    private static final String BRAZE_KEY = "Braze";
    private static final String BRAZE_EXTERNAL_ID_KEY = "brazeExternalId";
    private Appboy appBoy;

    public static Factory FACTORY = new Factory() {
        @Override
        public RudderIntegration<?> create(Object settings, RudderClient client, RudderConfig rudderConfig) {
            return new BrazeIntegrationFactory(settings, client, rudderConfig);
        }

        @Override
        public String key() {
            return BRAZE_KEY;
        }
    };

    private static final Set<String> MALE_KEYS = new HashSet<>(Arrays.asList("M",
            "MALE"));
    private static final Set<String> FEMALE_KEYS = new HashSet<>(Arrays.asList("F",
            "FEMALE"));
    private static final List<String> RESERVED_KEY_SET = Arrays.asList("birthday", "email", "firstName",
            "lastName", "gender", "phone", "address");

    private static final String AUTO_IN_APP_MESSAGE_REGISTER = "auto_in_app_message_register";

    private static final String DEFAULT_CURRENCY_CODE = "USD";

    private static final String REVENUE_KEY = "revenue";
    private static final String CURRENCY_KEY = "currency";
    private static final String DATA_CENTER_KEY = "dataCenter";
    private static final String API_KEY = "appKey";

    private boolean autoInAppMessageRegEnabled;

    private BrazeIntegrationFactory(Object config, final RudderClient client, RudderConfig rudderConfig) {
        String apiKey = "";
        Map<String, Object> destinationConfig = (Map<String, Object>) config;
        if (destinationConfig == null) {
            RudderLogger.logError("Invalid api key. Aborting Braze initialization.");
        } else if (client.getApplication() == null) {
            RudderLogger.logError("RudderClient is not initialized correctly. Application is null. Aborting Braze initialization.");
        } else {
            // get apiKey and return if null or blank
            if (destinationConfig.containsKey(API_KEY)) {
                apiKey = (String) destinationConfig.get(API_KEY);
            }
            if (TextUtils.isEmpty(apiKey)) {
                RudderLogger.logError("Invalid api key. Aborting Braze initialization.");
                return;
            }

            // check for auto in app message register. default false
            if (destinationConfig.containsKey(AUTO_IN_APP_MESSAGE_REGISTER)) {
                String autoInAppMessageRegEnabledStr = (String) destinationConfig.get(AUTO_IN_APP_MESSAGE_REGISTER);
                if (autoInAppMessageRegEnabledStr != null) {
                    this.autoInAppMessageRegEnabled = Boolean.getBoolean(autoInAppMessageRegEnabledStr);
                }
            }

            // get endpoint
            String endPoint = null, customEndPoint = null;
            if (destinationConfig.containsKey(DATA_CENTER_KEY)) {
                endPoint = (String) destinationConfig.get(DATA_CENTER_KEY);
            }

            if (endPoint == null || endPoint.isEmpty()) {
                RudderLogger.logError("EndPointUrl is empty. Aborting Braze initialization.");
                return;
            } else {
                endPoint = endPoint.trim();
            }

            switch (endPoint) {
                case "US-01":
                    customEndPoint = "sdk.iad-01.braze.com";
                    break;
                case "US-02":
                    customEndPoint = "sdk.iad-02.braze.com";
                    break;
                case "US-03":
                    customEndPoint = "sdk.iad-03.braze.com";
                    break;
                case "US-04":
                    customEndPoint = "sdk.iad-04.braze.com";
                    break;
                case "US-05":
                    customEndPoint = "sdk.iad-05.braze.com";
                    break;
                case "US-06":
                    customEndPoint = "sdk.iad-06.braze.com";
                    break;
                case "US-08":
                    customEndPoint = "sdk.iad-08.braze.com";
                    break;
                case "EU-01":
                    customEndPoint = "sdk.fra-01.braze.eu";
                    break;
                case "EU-02":
                    customEndPoint = "sdk.fra-02.braze.eu";
                    break;
            }
            if (customEndPoint == null) {
                RudderLogger.logError("CustomEndPointUrl is blank or incorrect. Aborting Braze initialization.");
                return;
            }

            // all good. initialize braze sdk
            BrazeConfig.Builder builder =
                    new BrazeConfig.Builder()
                            .setApiKey(apiKey)
                            .setCustomEndpoint(customEndPoint);
            BrazeLogger.setLogLevel(
                    rudderConfig.getLogLevel() >= RudderLogger.RudderLogLevel.DEBUG ?
                            Log.VERBOSE : Log.ERROR
            );
            Appboy.configure(RudderClient.getApplication().getApplicationContext(), builder.build());
            this.appBoy = Appboy.getInstance(RudderClient.getApplication());
            RudderLogger.logInfo("Configured Braze + Rudder integration and initialized Braze.");

            RudderClient.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(@NonNull Activity activity, Bundle bundle) {
                    // nothing to implement
                }

                @Override
                public void onActivityStarted(@NonNull Activity activity) {
                    if (appBoy != null) {
                        appBoy.openSession(activity);
                    }
                }

                @Override
                public void onActivityResumed(@NonNull Activity activity) {
                    if (autoInAppMessageRegEnabled) {
                        BrazeInAppMessageManager.getInstance().registerInAppMessageManager(activity);
                    }
                }

                @Override
                public void onActivityPaused(@NonNull Activity activity) {
                    if (autoInAppMessageRegEnabled) {
//                        AppboyInAppMessageManager.getInstance().unregisterInAppMessageManager(activity);
                        BrazeInAppMessageManager.getInstance().unregisterInAppMessageManager(activity);
                    }
                }

                @Override
                public void onActivityStopped(@NonNull Activity activity) {
                    if (appBoy != null) {
                        appBoy.closeSession(activity);
                    }
                }

                @Override
                public void onActivitySaveInstanceState(@NonNull Activity activity, @Nullable Bundle bundle) {
                    // nothing to implement
                }

                @Override
                public void onActivityDestroyed(@NonNull Activity activity) {
                    // nothing to implement
                }
            });
        }
    }

    private void processRudderEvent(RudderMessage element) {
        if (element.getType() != null) {
            switch (element.getType()) {
                case MessageType.TRACK:
                    String event = element.getEventName();
                    if (event == null) {
                        return;
                    }
                    Map<String, Object> eventProperties = element.getProperties();
                    try {
                        if (element.getEventName().equals("Install Attributed") && eventProperties != null && eventProperties.containsKey("campaign")) {
                            Map<String, Object> campaignProps = (Map<String, Object>) eventProperties.get("campaign");
                            if (campaignProps != null && appBoy.getCurrentUser() != null) {
                                appBoy.getCurrentUser().setAttributionData(new AttributionData(
                                        (String) campaignProps.get("source"),
                                        (String) campaignProps.get("name"),
                                        (String) campaignProps.get("ad_group"),
                                        (String) campaignProps.get("ad_creative")));
                            }
                            return;
                        }
                    } catch (Exception exception) {
                        RudderLogger.logError(exception);
                    }
                    if (eventProperties == null || eventProperties.size() == 0) {
                        RudderLogger.logDebug("Braze event has no properties");
                        appBoy.logCustomEvent(element.getEventName());
                        return;
                    }
                    JSONObject propertiesJson = new JSONObject(eventProperties);

                    if (eventProperties.containsKey(REVENUE_KEY)) {
                        double revenue = Double.parseDouble(String.valueOf(eventProperties.get(REVENUE_KEY)));
                        String currency = String.valueOf(eventProperties.get(CURRENCY_KEY));
                        if (revenue != 0) {
                            String currencyCode = TextUtils.isEmpty(currency) ? DEFAULT_CURRENCY_CODE
                                    : currency;
                            propertiesJson.remove(REVENUE_KEY);
                            propertiesJson.remove(CURRENCY_KEY);

                            if (propertiesJson.length() == 0) {
                                RudderLogger.logDebug("Braze logPurchase for purchase " + element.getEventName() + " for " + revenue + " " + currencyCode + " with no"
                                        + " properties.");
                                appBoy.logPurchase(element.getEventName(), currencyCode, new BigDecimal(revenue));
                            } else {
                                RudderLogger.logDebug("Braze logPurchase for purchase " + element.getEventName() + " for " + revenue + " " + currencyCode + " " + propertiesJson.toString());
                                appBoy.logPurchase(event, currencyCode, new BigDecimal(revenue),
                                        new BrazeProperties(propertiesJson));
                            }
                        }
                    } else {
                        RudderLogger.logDebug("Braze logCustomEvent for event " + element.getEventName() + " with properties % " + propertiesJson.toString());
                        appBoy.logCustomEvent(event, new BrazeProperties(propertiesJson));
                    }

                    break;
                case MessageType.IDENTIFY:
                    List<Map<String, Object>> externalIds = element.getContext().getExternalIds();
                    String externalId = null;
                    for (int index = 0; externalIds != null && index < externalIds.size(); index++) {
                        Map<String, Object> externalIdMap = externalIds.get(index);
                        String typeKey = (String) externalIdMap.get("type");
                        if (typeKey != null && typeKey.equals(BRAZE_EXTERNAL_ID_KEY)) {
                            externalId = (String) externalIdMap.get("id");
                        }
                    }

                    if (externalId != null && !externalId.isEmpty()) {
                        appBoy.changeUser(externalId);
                    } else {
                        String userId = element.getUserId();
                        if (!TextUtils.isEmpty(userId)) {
                            appBoy.changeUser(userId);
                        }
                    }

                    Map<String, Object> traitsMap = element.getTraits();
                    if (traitsMap == null) {
                        return;
                    }

                    if (appBoy.getCurrentUser() == null) {
                        return;
                    }
                    Date birthday = dateFromString(RudderTraits.getBirthday(traitsMap));
                    if (birthday != null) {
                        Calendar birthdayCal = Calendar.getInstance(Locale.US);
                        birthdayCal.setTime(birthday);
                        appBoy.getCurrentUser().setDateOfBirth(birthdayCal.get(Calendar.YEAR),
                                Month.values()[birthdayCal.get(Calendar.MONTH)],
                                birthdayCal.get(Calendar.DAY_OF_MONTH));
                    }

                    String email = RudderTraits.getEmail(traitsMap);
                    if (!TextUtils.isEmpty(email)) {
                        appBoy.getCurrentUser().setEmail(email);
                    }

                    String firstName = RudderTraits.getFirstname(traitsMap);
                    if (!TextUtils.isEmpty(firstName)) {
                        appBoy.getCurrentUser().setFirstName(firstName);
                    }

                    String lastName = RudderTraits.getLastname(traitsMap);
                    if (!TextUtils.isEmpty(lastName)) {
                        appBoy.getCurrentUser().setLastName(lastName);
                    }

                    String gender = RudderTraits.getGender(traitsMap);
                    if (!TextUtils.isEmpty(gender)) {
                        if (MALE_KEYS.contains(gender.toUpperCase())) {
                            appBoy.getCurrentUser().setGender(Gender.MALE);
                        } else if (FEMALE_KEYS.contains(gender.toUpperCase())) {
                            appBoy.getCurrentUser().setGender(Gender.FEMALE);
                        }
                    }

                    String phone = RudderTraits.getPhone(traitsMap);
                    if (!TextUtils.isEmpty(phone)) {
                        appBoy.getCurrentUser().setPhoneNumber(phone);
                    }

                    RudderTraits.Address address = RudderTraits.Address.fromString(RudderTraits.getAddress(traitsMap));
                    if (address != null) {
                        String city = address.getCity();
                        if (!TextUtils.isEmpty(city)) {
                            appBoy.getCurrentUser().setHomeCity(city);
                        }
                        String country = address.getCountry();
                        if (!TextUtils.isEmpty(country)) {
                            appBoy.getCurrentUser().setCountry(country);
                        }
                    }

                    for (String key : traitsMap.keySet()) {
                        if (RESERVED_KEY_SET.contains(key)) {
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
                            RudderLogger.logDebug("Braze can't map rudder value for custom Braze user "
                                    + "attribute with key " + key + "and value " + value);
                        }
                    }
                    break;
                case MessageType.SCREEN:
                    RudderLogger.logWarn("BrazeIntegrationFactory: MessageType is not supported");
                    break;
                default:
                    RudderLogger.logWarn("BrazeIntegrationFactory: MessageType is not specified");
                    break;
            }
        }
    }

    @Override
    public void flush() {
        super.flush();
        RudderLogger.logDebug("Braze requestImmediateDataFlush().");
        appBoy.requestImmediateDataFlush();
    }

    @Override
    public void reset() {
        // nothing to do here for Braze
    }

    @Override
    public void dump(@NonNull RudderMessage element) {
        if (appBoy != null) {
            processRudderEvent(element);
        }
    }

    @Override
    public Appboy getUnderlyingInstance() {
        return appBoy;
    }

    private static Date dateFromString(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            return formatter.parse(date);
        } catch (Exception e) {
            return null;
        }
    }
}
