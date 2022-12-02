package com.rudderstack.android.integration.braze;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.braze.Braze;
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

public class BrazeIntegrationFactory extends RudderIntegration<Braze> {
    // String constants
    private static final String BRAZE_KEY = "Braze";
    private static final String BRAZE_EXTERNAL_ID_KEY = "brazeExternalId";
    private static final String AUTO_IN_APP_MESSAGE_REGISTER = "auto_in_app_message_register";
    private static final String DEFAULT_CURRENCY_CODE = "USD";
    private static final String REVENUE_KEY = "revenue";
    private static final String CURRENCY_KEY = "currency";
    private static final String DATA_CENTER_KEY = "dataCenter";
    private static final String API_KEY = "appKey";
    private static final String SUPPORT_DEDUP = "supportDedup";

    // Array constants
    private static final Set<String> MALE_KEYS = new HashSet<>(Arrays.asList("M",
            "MALE"));
    private static final Set<String> FEMALE_KEYS = new HashSet<>(Arrays.asList("F",
            "FEMALE"));
    private static final List<String> RESERVED_KEY_SET = Arrays.asList("birthday", "email", "firstName",
            "lastName", "gender", "phone", "address");

    // Braze instance
    private Braze braze;

    // Config variables
    private boolean autoInAppMessageRegEnabled;
    private boolean supportDedup = false; // default it to false

    // Previous identify payload
    private RudderMessage previousIdentifyElement = null;

    // Factory initialization
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

    private BrazeIntegrationFactory(Object config, final RudderClient client, RudderConfig rudderConfig) {
        String apiKey = "";
        Map<String, Object> destinationConfig = (Map<String, Object>) config;
        if (destinationConfig == null) {
            RudderLogger.logError("Invalid api key. Aborting Braze initialization.");
        } else if (RudderClient.getApplication() == null) {
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

            if (TextUtils.isEmpty(endPoint)) {
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

            // check for support dedup for identify calls. default false
            if (destinationConfig.containsKey(SUPPORT_DEDUP)) {
                String supportDedupStr = (String) destinationConfig.get(SUPPORT_DEDUP);
                if (supportDedupStr != null) {
                    this.supportDedup = Boolean.getBoolean(supportDedupStr);
                }
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
            Braze.configure(RudderClient.getApplication().getApplicationContext(), builder.build());
            this.braze = Braze.getInstance(RudderClient.getApplication());
            RudderLogger.logInfo("Configured Braze + Rudder integration and initialized Braze.");

            RudderClient.getApplication().registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                @Override
                public void onActivityCreated(@NonNull Activity activity, Bundle bundle) {
                    // nothing to implement
                }

                @Override
                public void onActivityStarted(@NonNull Activity activity) {
                    if (braze != null) {
                        braze.openSession(activity);
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
                        BrazeInAppMessageManager.getInstance().unregisterInAppMessageManager(activity);
                    }
                }

                @Override
                public void onActivityStopped(@NonNull Activity activity) {
                    if (braze != null) {
                        braze.closeSession(activity);
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

    private void processTrackEvent(RudderMessage element) {
        String event = element.getEventName();
        if (event == null) {
            return;
        }

        Map<String, Object> eventProperties = element.getProperties();
        try {
            if (element.getEventName().equals("Install Attributed") && eventProperties != null && eventProperties.containsKey("campaign")) {
                Map<String, Object> campaignProps = (Map<String, Object>) eventProperties.get("campaign");
                if (campaignProps != null && this.braze.getCurrentUser() != null) {
                    this.braze.getCurrentUser().setAttributionData(new AttributionData(
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
            this.braze.logCustomEvent(element.getEventName());
            return;
        }

        JSONObject propertiesJson = new JSONObject(eventProperties);
        if (eventProperties.containsKey(REVENUE_KEY) && eventProperties.get(REVENUE_KEY) != null) {
            double revenue = Double.parseDouble(String.valueOf(eventProperties.get(REVENUE_KEY)));
            String currency = String.valueOf(eventProperties.get(CURRENCY_KEY));
            String currencyCode = TextUtils.isEmpty(currency) ? DEFAULT_CURRENCY_CODE
                    : currency;
            propertiesJson.remove(REVENUE_KEY);
            propertiesJson.remove(CURRENCY_KEY);

            if (propertiesJson.length() == 0) {
                RudderLogger.logDebug(String.format("Braze logPurchase for purchase %s for %s %s with no properties.", element.getEventName(), revenue, currencyCode));
                this.braze.logPurchase(element.getEventName(), currencyCode, BigDecimal.valueOf(revenue));
            } else {
                RudderLogger.logDebug(String.format("Braze logPurchase for purchase %s for %s %s %s", element.getEventName(), revenue, currencyCode, propertiesJson.toString()));
                this.braze.logPurchase(event, currencyCode, BigDecimal.valueOf(revenue),
                        new BrazeProperties(propertiesJson));
            }
        } else {
            RudderLogger.logDebug(String.format("Braze logCustomEvent for event %s with properties %% %s", element.getEventName(), propertiesJson.toString()));
            this.braze.logCustomEvent(event, new BrazeProperties(propertiesJson));
        }
    }

    private void processIdentifyEvent(RudderMessage element) {
        // externalId or userId
        String externalId = (String) this.needUpdate(BRAZE_EXTERNAL_ID_KEY, element);
        if (!TextUtils.isEmpty(externalId)) {
            this.braze.changeUser(externalId);
        } else {
            String userId = (String) this.needUpdate("userId", element);
            if (!TextUtils.isEmpty(userId)) {
                this.braze.changeUser(userId);
            }
        }

        Map<String, Object> traitsMap = element.getTraits();
        if (traitsMap == null) {
            this.previousIdentifyElement = element;
            return;
        }

        if (braze.getCurrentUser() == null) {
            this.previousIdentifyElement = element;
            return;
        }

        // birthday
        Date birthday = (Date) this.needUpdate("birthday", element);
        if (birthday != null) {
            Calendar birthdayCal = Calendar.getInstance(Locale.US);
            birthdayCal.setTime(birthday);
            braze.getCurrentUser().setDateOfBirth(birthdayCal.get(Calendar.YEAR),
                    Month.values()[birthdayCal.get(Calendar.MONTH)],
                    birthdayCal.get(Calendar.DAY_OF_MONTH));
        }

        // email
        String email = (String) this.needUpdate("email", element);
        if (!TextUtils.isEmpty(email)) {
            braze.getCurrentUser().setEmail(email);
        }

        // firstName
        String firstName = (String) this.needUpdate("firstName", element);
        if (!TextUtils.isEmpty(firstName)) {
            braze.getCurrentUser().setFirstName(firstName);
        }

        // lastName
        String lastName = (String) this.needUpdate("lastName", element);
        if (!TextUtils.isEmpty(lastName)) {
            braze.getCurrentUser().setLastName(lastName);
        }

        // gender
        String gender = (String) this.needUpdate("gender", element);
        if (!TextUtils.isEmpty(gender)) {
            if (MALE_KEYS.contains(gender.toUpperCase())) {
                braze.getCurrentUser().setGender(Gender.MALE);
            } else if (FEMALE_KEYS.contains(gender.toUpperCase())) {
                braze.getCurrentUser().setGender(Gender.FEMALE);
            }
        }

        // phone
        String phone = (String) this.needUpdate("phone", element);
        if (!TextUtils.isEmpty(phone)) {
            braze.getCurrentUser().setPhoneNumber(phone);
        }

        // address
        RudderTraits.Address address = (RudderTraits.Address) this.needUpdate("address", element);
        if (address != null) {
            String city = address.getCity();
            if (!TextUtils.isEmpty(city)) {
                braze.getCurrentUser().setHomeCity(city);
            }
            String country = address.getCountry();
            if (!TextUtils.isEmpty(country)) {
                braze.getCurrentUser().setCountry(country);
            }
        }

        for (String key : traitsMap.keySet()) {
            if (RESERVED_KEY_SET.contains(key)) {
                continue;
            }
            Object value = this.needUpdate(key, element);
            if (value != null) {
                if (value instanceof Boolean) {
                    braze.getCurrentUser().setCustomUserAttribute(key, (Boolean) value);
                } else if (value instanceof Integer) {
                    braze.getCurrentUser().setCustomUserAttribute(key, (Integer) value);
                } else if (value instanceof Double) {
                    braze.getCurrentUser().setCustomUserAttribute(key, (Double) value);
                } else if (value instanceof Float) {
                    braze.getCurrentUser().setCustomUserAttribute(key, (Float) value);
                } else if (value instanceof Long) {
                    braze.getCurrentUser().setCustomUserAttribute(key, (Long) value);
                } else if (value instanceof Date) {
                    long secondsFromEpoch = ((Date) value).getTime() / 1000L;
                    braze.getCurrentUser().setCustomUserAttributeToSecondsFromEpoch(key, secondsFromEpoch);
                } else if (value instanceof String) {
                    braze.getCurrentUser().setCustomUserAttribute(key, (String) value);
                } else {
                    RudderLogger.logDebug(String.format("Braze can't map rudder value for custom Braze user attribute with key %sand value %s", key, value));
                }
            }
        }

        // update the previousIdentifyElement so that we can check against it next time
        this.previousIdentifyElement = element;
    }

    private String getExternalId(RudderMessage element) {
        List<Map<String, Object>> externalIds = element.getContext().getExternalIds();
        for (int index = 0; externalIds != null && index < externalIds.size(); index++) {
            Map<String, Object> externalIdMap = externalIds.get(index);
            String typeKey = (String) externalIdMap.get("type");
            if (typeKey != null && typeKey.equals(BRAZE_EXTERNAL_ID_KEY)) {
                return (String) externalIdMap.get("id");
            }
        }
        return null;
    }

    @Override
    public void flush() {
        super.flush();
        RudderLogger.logDebug("Braze requestImmediateDataFlush().");
        braze.requestImmediateDataFlush();
    }

    @Override
    public void reset() {
        // nothing to do here for Braze
    }

    @Override
    public void dump(@NonNull RudderMessage element) {
        if (braze != null) {
            if (element.getType() != null) {
                switch (element.getType()) {
                    case MessageType.TRACK:
                        processTrackEvent(element);
                        break;
                    case MessageType.IDENTIFY:
                        processIdentifyEvent(element);
                        break;
                    case MessageType.SCREEN:
                        RudderLogger.logWarn("BrazeIntegrationFactory: MessageType is not supported");
                        break;
                    default:
                        RudderLogger.logWarn("BrazeIntegrationFactory: MessageType is not specified");
                        break;
                }
            }
        } else {
            RudderLogger.logWarn("BrazeIntegrationFactory: Braze is not initialized");
        }
    }

    @Override
    public Braze getUnderlyingInstance() {
        return braze;
    }

    //////////////////////////////////////////////////
    // UTIL Methods
    //////////////////////////////////////////////////
    private static Date dateFromString(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            return formatter.parse(date);
        } catch (Exception e) {
            RudderLogger.logError(e);
            return null;
        }
    }

    // compare two address objects and return accordingly
    private static boolean compareAddress(@Nullable RudderTraits.Address curr, @Nullable RudderTraits.Address prev) {
        if (prev == null && curr != null) {
            return true;
        }

        return prev != null && curr != null
                && prev.getCity().equals(curr.getCity())
                && prev.getCountry().equals(curr.getCountry())
                && prev.getPostalCode().equals(curr.getPostalCode())
                && prev.getState().equals(curr.getState())
                && prev.getStreet().equals(curr.getStreet());
    }

    private @Nullable
    Object getValueFromElement(@NonNull String key, @Nullable RudderMessage element) {
        if (element != null) {
            Map<String, Object> traitsMap = element.getTraits();
            switch (key) {
                case BRAZE_EXTERNAL_ID_KEY:
                    return getExternalId(element);
                case "userId":
                    return element.getUserId();
                case "birthday":
                    return dateFromString(RudderTraits.getBirthday(traitsMap));
                case "email":
                    return RudderTraits.getEmail(traitsMap);
                case "firstName":
                    return RudderTraits.getFirstname(traitsMap);
                case "lastName":
                    return RudderTraits.getLastname(traitsMap);
                case "gender":
                    return RudderTraits.getGender(traitsMap);
                case "phone":
                    return RudderTraits.getPhone(traitsMap);
                case "address":
                    return RudderTraits.Address.fromString(RudderTraits.getAddress(traitsMap));
                default:
                    return traitsMap.get(key);
            }
        }
        return null;
    }

    // checks two message objects and returns the value to be updated against the key
    // else returns null
    private @Nullable
    Object needUpdate(@NonNull String key, @NonNull RudderMessage element) {
        // get current value first
        Object currValue = this.getValueFromElement(key, element);

        // get prev value only if supportDedup is ON
        if (this.supportDedup) {
            Object prevValue = this.getValueFromElement(key, this.previousIdentifyElement);
            if (currValue instanceof RudderTraits.Address && prevValue instanceof RudderTraits.Address) {
                RudderTraits.Address currAddress = (RudderTraits.Address) currValue;
                RudderTraits.Address prevAddress = (RudderTraits.Address) prevValue;
                return BrazeIntegrationFactory.compareAddress(currAddress, prevAddress)
                        ? null
                        : currValue;
            }
            return currValue != null && currValue.equals(prevValue)
                    ? null
                    : currValue;
        }

        return currValue;
    }
}
