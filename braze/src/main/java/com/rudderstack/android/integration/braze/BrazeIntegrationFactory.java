package com.rudderstack.android.integration.braze;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.braze.enums.Gender;
import com.braze.enums.Month;
import com.braze.models.outgoing.AttributionData;
import com.braze.Braze;
import com.braze.configuration.BrazeConfig;
import com.braze.models.outgoing.BrazeProperties;
import com.braze.support.BrazeLogger;
import com.braze.ui.inappmessage.BrazeInAppMessageManager;
import com.rudderstack.android.sdk.core.MessageType;
import com.rudderstack.android.sdk.core.RudderClient;
import com.rudderstack.android.sdk.core.RudderConfig;
import com.rudderstack.android.sdk.core.RudderIntegration;
import com.rudderstack.android.sdk.core.RudderLogger;
import com.rudderstack.android.sdk.core.RudderMessage;
import com.rudderstack.android.sdk.core.RudderTraits;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BrazeIntegrationFactory extends RudderIntegration<Braze> {
    class BrazePurchase {
        private String productId;
        private Integer quantity;
        private Double price;
        private Map<String, Object> properties;
        private String currency;

        public String getProductId() {
            return productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public Double getPrice() {
            return price;
        }

        public Map<String, Object> getProperties() {
            return properties;
        }

        public String getCurrency() {
            return currency;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public void setProperties(Map<String, Object> properties) {
            this.properties = properties;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public BrazePurchase() {
            this.quantity = 1;
            this.properties = new HashMap<>();
            this.currency = "USD";
        }
    }

    enum ConnectionMode {
        HYBRID,
        CLOUD,
        DEVICE
    }

    // String constants
    private static final String BRAZE_KEY = "Braze";
    private static final String BRAZE_EXTERNAL_ID_KEY = "brazeExternalId";
    private static final String AUTO_IN_APP_MESSAGE_REGISTER = "auto_in_app_message_register";
    private static final String DEFAULT_CURRENCY_CODE = "USD";
    private static final String CURRENCY_KEY = "currency";
    private static final String PRODUCTS_KEY = "products";
    private static final String DATA_CENTER_KEY = "dataCenter";
    private static final String ANDROID_API_KEY = "androidApiKey";
    private static final String API_KEY = "appKey";
    private static final String USE_PLATFORM_SPECIFIC_API_KEYS = "usePlatformSpecificApiKeys";
    private static final String SUPPORT_DEDUP = "supportDedup";
    private static final String PRODUCT_ID_KEY = "product_id";
    private static final String QUANTITY_KEY = "quantity";
    private static final String PRICE_KEY = "price";
    private static final String TIME_KEY = "time";
    private static final String EVENT_NAME_KEY = "event_name";
    private static final String USE_NATIVE_SDK_TO_SEND = "useNativeSDKToSend";
    private static final String CONNECTION_MODE = "connectionMode";

    // Array constants
    private static final Set<String> MALE_KEYS = new HashSet<>(Arrays.asList("M",
            "MALE"));
    private static final Set<String> FEMALE_KEYS = new HashSet<>(Arrays.asList("F",
            "FEMALE"));
    public static final String BIRTHDAY = "birthday";
    public static final String EMAIL = "email";
    public static final String FIRSTNAME = "firstname";
    public static final String LASTNAME = "lastname";
    public static final String GENDER = "gender";
    public static final String PHONE = "phone";
    public static final String ADDRESS = "address";
    private static final List<String> RESERVED_KEY_SET = Arrays.asList(BIRTHDAY, EMAIL, FIRSTNAME,
            LASTNAME, GENDER, PHONE, ADDRESS);

    private static final String RUDDER_LABEL = "rudder_id";

    // Braze instance
    private Braze braze;

    // Config variables
    private boolean autoInAppMessageRegEnabled;
    private boolean supportDedup = false; // default it to false
    private ConnectionMode connectionMode;

    // Previous identify payload
    private RudderMessage previousIdentifyElement = null;

    // Factory initialization
    public static final Factory FACTORY = new Factory() {
        @Override
        public RudderIntegration<?> create(Object settings, RudderClient client, RudderConfig rudderConfig) {
            return new BrazeIntegrationFactory(settings, client, rudderConfig);
        }

        @Override
        public String key() {
            return BRAZE_KEY;
        }
    };

    private BrazeIntegrationFactory(Object config, RudderClient client, RudderConfig rudderConfig) {
        String apiKey = "";
        Map<String, Object> destinationConfig = (Map<String, Object>) config;
        if (destinationConfig == null) {
            RudderLogger.logError("Invalid api key. Aborting Braze initialization.");
        } else if (RudderClient.getApplication() == null) {
            RudderLogger.logError("RudderClient is not initialized correctly. Application is null. Aborting Braze initialization.");
        } else {
            // Prefer platform-specific key if present and not empty
            if (getBoolean(destinationConfig.get(USE_PLATFORM_SPECIFIC_API_KEYS))
                    && destinationConfig.containsKey(ANDROID_API_KEY)) {
                apiKey = (String) destinationConfig.get(ANDROID_API_KEY);
            }
            // Fallback to default app key if either platform-specific key is not present or is set to false
            if (TextUtils.isEmpty(apiKey) && destinationConfig.containsKey(API_KEY)) {
                if (getBoolean(destinationConfig.get(USE_PLATFORM_SPECIFIC_API_KEYS))) {
                    RudderLogger.logWarn("BrazeIntegration: Configured to use platform-specific API keys but Android API key is not valid. Falling back to the default API key.");
                }
                apiKey = (String) destinationConfig.get(API_KEY);
            }
            if (TextUtils.isEmpty(apiKey)) {
                RudderLogger.logError("Invalid API key. Aborting Braze initialization.");
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
                case "US-07":
                    customEndPoint = "sdk.iad-07.braze.com";
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
                case "AU-01":
                    customEndPoint = "sdk.au-01.braze.com";
                    break;
            }
            if (customEndPoint == null) {
                RudderLogger.logError("CustomEndPointUrl is blank or incorrect. Aborting Braze initialization.");
                return;
            }

            // check for support dedup for identify calls. default false
            if (destinationConfig.containsKey(SUPPORT_DEDUP)) {
                this.supportDedup = getBoolean(destinationConfig.get(SUPPORT_DEDUP));
            }

            this.connectionMode = getConnectionMode(destinationConfig);

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
            setUserAlias(client.getAnonymousId());
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

    private void setUserAlias(String anonymousId) {
        this.braze.getCurrentUser().addAlias(anonymousId, RUDDER_LABEL);
    }

    private void processTrackEvent(RudderMessage element) {
        String event = element.getEventName();
        if (event == null) {
            return;
        }

        Map<String, Object> eventProperties = element.getProperties();
        try {
            if (event.equals("Install Attributed")) {
                if (eventProperties != null && eventProperties.containsKey("campaign") && this.braze.getCurrentUser() != null) {
                    Map<String, Object> campaignProps = (Map<String, Object>) eventProperties.get("campaign");
                    if (campaignProps != null) {
                        this.braze.getCurrentUser().setAttributionData(new AttributionData(
                                (String) campaignProps.get("source"),
                                (String) campaignProps.get("name"),
                                (String) campaignProps.get("ad_group"),
                                (String) campaignProps.get("ad_creative")));
                    }
                } else {
                    logCustomEvent(event, eventProperties);
                }
            } else if (event.equals("Order Completed")) {
                List<BrazePurchase> purchaseList = getPurchaseList(eventProperties);
                if (purchaseList != null) {
                    for (BrazePurchase purchase: purchaseList) {
                        this.braze.logPurchase(purchase.getProductId(), purchase.getCurrency(),
                                BigDecimal.valueOf(purchase.getPrice()), new BrazeProperties(purchase.properties));
                        RudderLogger.logDebug(String.format("Braze logPurchase for purchase %s for %s %s %s", purchase.getProductId(),
                                purchase.getPrice(), purchase.getCurrency(), purchase.properties.toString()));
                    }
                }
            } else {
                logCustomEvent(event, eventProperties);
            }
        } catch (Exception exception) {
            RudderLogger.logError(exception);
        }
    }

    private void logCustomEvent(String eventName, Map<String, Object> eventProperties) {
        if (eventProperties != null) {
            RudderLogger.logDebug(String.format("Braze logCustomEvent for event %s with properties %% %s", eventName, eventProperties.toString()));
            this.braze.logCustomEvent(eventName, new BrazeProperties(eventProperties));
        } else {
            RudderLogger.logDebug(String.format("Braze logCustomEvent for event %s", eventName));
            this.braze.logCustomEvent(eventName);
        }
    }

    @Nullable
    private List<BrazePurchase> getPurchaseList(Map<String, Object> properties) {
        if (properties == null) {
            return null;
        }
        List<Map<String, Object>> productList = (List<Map<String, Object>>)properties.get(PRODUCTS_KEY);
        if (productList == null || productList.size() == 0) {
            return null;
        }

        String currencyCode = String.valueOf(properties.get(CURRENCY_KEY));
        String currency = TextUtils.isEmpty(currencyCode) ? DEFAULT_CURRENCY_CODE : currencyCode;
        List<String> ignoredKeys = Arrays.asList(PRODUCTS_KEY, QUANTITY_KEY, PRICE_KEY, PRODUCTS_KEY,
                TIME_KEY, EVENT_NAME_KEY, CURRENCY_KEY);
        Map<String, Object> otherProperties = new HashMap<>();
        for (Map.Entry<String, Object> entry: properties.entrySet()) {
            if (!ignoredKeys.contains(entry.getKey())){
                otherProperties.put(entry.getKey(), entry.getValue());
            }
        }

        List<BrazePurchase> purchaseList = new ArrayList<>();

        for (Map<String, Object> product: productList) {
            BrazePurchase brazePurchase = new BrazePurchase();
            Map<String, Object> appboyProperties = new HashMap<>(otherProperties);
            for (Map.Entry<String, Object> entry: product.entrySet()) {
                if (entry.getKey().equals(PRODUCT_ID_KEY)) {
                    brazePurchase.setProductId(String.valueOf(entry.getValue()));
                } else if (entry.getKey().equals(QUANTITY_KEY)) {
                    brazePurchase.setQuantity(Integer.getInteger(String.valueOf(entry.getValue())));
                } else if (entry.getKey().equals(PRICE_KEY)) {
                    brazePurchase.setPrice(Double.parseDouble(String.valueOf(entry.getValue())));
                } else {
                    appboyProperties.put(entry.getKey(), entry.getValue());
                }
            }
            brazePurchase.setProperties(appboyProperties);
            brazePurchase.setCurrency(currency);
            if (brazePurchase.productId == null || brazePurchase.price == null) {
                continue;
            }
            purchaseList.add(brazePurchase);
        }
        return purchaseList.size() > 0 ? purchaseList : null;
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
        Date birthday = (Date) this.needUpdate(BIRTHDAY, element);
        if (birthday != null) {
            Calendar birthdayCal = Calendar.getInstance(Locale.US);
            birthdayCal.setTime(birthday);
            braze.getCurrentUser().setDateOfBirth(birthdayCal.get(Calendar.YEAR),
                    Month.values()[birthdayCal.get(Calendar.MONTH)],
                    birthdayCal.get(Calendar.DAY_OF_MONTH));
        }

        // email
        String email = (String) this.needUpdate(EMAIL, element);
        if (!TextUtils.isEmpty(email)) {
            braze.getCurrentUser().setEmail(email);
        }

        // firstName
        String firstName = (String) this.needUpdate(FIRSTNAME, element);
        if (!TextUtils.isEmpty(firstName)) {
            braze.getCurrentUser().setFirstName(firstName);
        }

        // lastName
        String lastName = (String) this.needUpdate(LASTNAME, element);
        if (!TextUtils.isEmpty(lastName)) {
            braze.getCurrentUser().setLastName(lastName);
        }

        // gender
        String gender = (String) this.needUpdate(GENDER, element);
        if (!TextUtils.isEmpty(gender)) {
            if (MALE_KEYS.contains(gender.toUpperCase())) {
                braze.getCurrentUser().setGender(Gender.MALE);
            } else if (FEMALE_KEYS.contains(gender.toUpperCase())) {
                braze.getCurrentUser().setGender(Gender.FEMALE);
            }
        }

        // phone
        String phone = (String) this.needUpdate(PHONE, element);
        if (!TextUtils.isEmpty(phone)) {
            braze.getCurrentUser().setPhoneNumber(phone);
        }

        // address
        RudderTraits.Address address = (RudderTraits.Address) this.needUpdate(ADDRESS, element);
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
            if (connectionMode != ConnectionMode.DEVICE) {
                return;
            }
            if (element.getType() != null) {
                switch (element.getType()) {
                    case MessageType.TRACK:
                        processTrackEvent(element);
                        break;
                    case MessageType.IDENTIFY:
                        processIdentifyEvent(element);
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
    private static @Nullable Date dateFromString(@Nullable String date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            return formatter.parse(date);
        } catch (Exception e) {
            RudderLogger.logError("Error while converting String type value into Date format: " + e);
            return null;
        }
    }

    // compare two address objects and return false if there is a change in address or true otherwise
    @VisibleForTesting
    static boolean compareAddress(@Nullable RudderTraits.Address curr, @Nullable RudderTraits.Address prev) {
        return (curr == null) || // if current address is null, we consider address unchanged
                (prev != null)  // current is non-null, if previous is null will return false
                        && ((curr.getCity() == null || ( // current city is null, consider city unchanged
                        prev.getCity() != null && //current city not null previous city if null, address changed
                                (prev.getCity().equals(curr.getCity())) // match the cities
                )) && (curr.getCountry() == null || ( // current country is null, consider city unchanged
                        prev.getCountry() != null && //current country not null previous country if null, address changed
                                (prev.getCountry().equals(curr.getCountry())) // match the cities
                )));

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

    private static boolean getBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    private ConnectionMode getConnectionMode(Map<String, Object> config) {
        String connectionMode = (config.containsKey(CONNECTION_MODE)) ? (String)config.get(CONNECTION_MODE) : "";
        switch (connectionMode) {
            case "hybrid":
                return ConnectionMode.HYBRID;
            case "device":
                return ConnectionMode.DEVICE;
            default:
                return ConnectionMode.CLOUD;
        }
    }
}
