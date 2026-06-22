package com.rudderstack.android.integration.braze;

import com.rudderstack.android.sdk.core.RudderLogger;
import com.rudderstack.android.sdk.core.ecomm.ECommerceEvents;
import com.rudderstack.android.sdk.core.ecomm.ECommerceParamNames;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

// Stateless mapping logic for Braze recommended ecommerce events (gated by
// useRecommendedEcommerceEvents). Every method here is a pure props -> Map transform; the actual
// braze.logCustomEvent call stays in BrazeIntegrationFactory.
class Utils {

    // Braze recommended ecommerce events. action is non-null only for cart_updated
    // (Product Added -> add, Product Removed -> remove).
    enum EcommerceEvent {
        PRODUCT_VIEWED("ecommerce.product_viewed", null),
        PRODUCT_ADDED("ecommerce.cart_updated", "add"),
        PRODUCT_REMOVED("ecommerce.cart_updated", "remove"),
        CHECKOUT_STARTED("ecommerce.checkout_started", null),
        ORDER_COMPLETED("ecommerce.order_placed", null),
        ORDER_REFUNDED("ecommerce.order_refunded", null),
        ORDER_CANCELLED("ecommerce.order_cancelled", null);

        private final String brazeEvent;
        private final String action;

        EcommerceEvent(String brazeEvent, String action) {
            this.brazeEvent = brazeEvent;
            this.action = action;
        }

        String getBrazeEvent() {
            return brazeEvent;
        }
    }

    // source is hardcoded per platform for mobile device mode (no channel derivation).
    private static final String SOURCE_KEY = "source";
    private static final String SOURCE = "android";

    // RudderStack ecommerce source keys. Sourced from the SDK's ECommerceParamNames wherever it
    // defines a constant; the rest exist only as @SerializedName fields on the ecomm model classes
    // (ECommerceProduct / ECommerceOrder) and have no referable constant today.
    private static final String EC_PRODUCT_ID = ECommerceParamNames.PRODUCT_ID;
    private static final String EC_QUANTITY = ECommerceParamNames.QUANTITY;
    private static final String EC_PRICE = ECommerceParamNames.PRICE;
    private static final String EC_CURRENCY = ECommerceParamNames.CURRENCY;
    private static final String EC_PRODUCTS = ECommerceParamNames.PRODUCTS;
    private static final String EC_CART_ID = ECommerceParamNames.CART_ID;
    private static final String EC_CHECKOUT_ID = ECommerceParamNames.CHECKOUT_ID;
    private static final String EC_ORDER_ID = ECommerceParamNames.ORDER_ID;
    private static final String EC_TOTAL = ECommerceParamNames.TOTAL;
    private static final String EC_REVENUE = ECommerceParamNames.REVENUE;
    private static final String EC_DISCOUNT = ECommerceParamNames.DISCOUNT;
    // Not exposed as ECommerceParamNames constants (only @SerializedName on the ecomm models):
    private static final String EC_SKU = "sku";
    private static final String EC_NAME = "name";
    private static final String EC_VARIANT = "variant";
    private static final String EC_IMAGE_URL = "image_url";
    private static final String EC_URL = "url";
    private static final String EC_VALUE = "value";
    private static final String EC_TAX = "tax";
    private static final String EC_SHIPPING = "shipping";
    // cancel_reason has no standard RS field at all (Braze-required gap).
    private static final String EC_CANCEL_REASON = "cancel_reason";

    // Braze recommended-event field names (official Braze schema names — destination side).
    private static final String BRAZE_PRODUCT_ID = "product_id";
    private static final String BRAZE_PRODUCT_NAME = "product_name";
    private static final String BRAZE_VARIANT_ID = "variant_id";
    private static final String BRAZE_QUANTITY = "quantity";
    private static final String BRAZE_PRICE = "price";
    private static final String BRAZE_IMAGE_URL = "image_url";
    private static final String BRAZE_PRODUCT_URL = "product_url";
    private static final String BRAZE_CART_ID = "cart_id";
    private static final String BRAZE_ACTION = "action";
    private static final String BRAZE_ORDER_ID = "order_id";
    private static final String BRAZE_CHECKOUT_ID = "checkout_id";
    private static final String BRAZE_TOTAL_VALUE = "total_value";
    private static final String BRAZE_CURRENCY = "currency";
    private static final String BRAZE_PRODUCTS = "products";
    private static final String BRAZE_TAX = "tax";
    private static final String BRAZE_SHIPPING = "shipping";
    private static final String BRAZE_TOTAL_DISCOUNTS = "total_discounts";
    private static final String BRAZE_CANCEL_REASON = "cancel_reason";
    private static final String BRAZE_METADATA = "metadata";

    // Consumed-key sets drive metadata pass-through (D6): any RS source key NOT consumed by a
    // top-level / product mapping flows into metadata / products[].metadata, never left top-level.
    private static final Set<String> PRODUCT_CONSUMED_KEYS = new HashSet<>(Arrays.asList(
            EC_PRODUCT_ID, EC_SKU, EC_NAME, EC_VARIANT, EC_QUANTITY, EC_PRICE, EC_IMAGE_URL, EC_URL));
    private static final Set<String> PRODUCT_VIEWED_CONSUMED_KEYS = new HashSet<>(Arrays.asList(
            EC_PRODUCT_ID, EC_SKU, EC_NAME, EC_VARIANT, EC_PRICE, EC_CURRENCY, EC_IMAGE_URL, EC_URL));
    private static final Set<String> CART_UPDATED_CONSUMED_KEYS = new HashSet<>(Arrays.asList(
            EC_CART_ID, EC_CURRENCY, EC_PRODUCT_ID, EC_SKU, EC_NAME, EC_VARIANT, EC_QUANTITY, EC_PRICE,
            EC_IMAGE_URL, EC_URL));
    private static final Set<String> CHECKOUT_STARTED_CONSUMED_KEYS = new HashSet<>(Arrays.asList(
            EC_CHECKOUT_ID, EC_ORDER_ID, EC_TOTAL, EC_REVENUE, EC_VALUE, EC_CURRENCY, EC_PRODUCTS, EC_TAX,
            EC_SHIPPING));
    private static final Set<String> ORDER_PLACED_CONSUMED_KEYS = new HashSet<>(Arrays.asList(
            EC_ORDER_ID, EC_TOTAL, EC_REVENUE, EC_VALUE, EC_CURRENCY, EC_PRODUCTS, EC_TAX, EC_SHIPPING,
            EC_DISCOUNT));
    private static final Set<String> ORDER_REFUNDED_CONSUMED_KEYS = new HashSet<>(Arrays.asList(
            EC_ORDER_ID, EC_TOTAL, EC_REVENUE, EC_VALUE, EC_CURRENCY, EC_PRODUCTS));
    private static final Set<String> ORDER_CANCELLED_CONSUMED_KEYS = new HashSet<>(Arrays.asList(
            EC_ORDER_ID, EC_TOTAL, EC_CURRENCY, EC_CANCEL_REASON, EC_PRODUCTS, EC_TAX, EC_SHIPPING,
            EC_DISCOUNT));

    // Case-insensitive RudderStack event name -> Braze recommended event.
    private static final Map<String, EcommerceEvent> ECOMMERCE_EVENT_MAPPING = buildEcommerceEventMapping();

    private static Map<String, EcommerceEvent> buildEcommerceEventMapping() {
        // Keys are the RudderStack ecommerce event names (from the SDK's ECommerceEvents),
        // lowercased for case-insensitive lookup.
        Map<String, EcommerceEvent> mapping = new HashMap<>();
        mapping.put(lowerCase(ECommerceEvents.PRODUCT_VIEWED), EcommerceEvent.PRODUCT_VIEWED);
        mapping.put(lowerCase(ECommerceEvents.PRODUCT_ADDED), EcommerceEvent.PRODUCT_ADDED);
        mapping.put(lowerCase(ECommerceEvents.PRODUCT_REMOVED), EcommerceEvent.PRODUCT_REMOVED);
        mapping.put(lowerCase(ECommerceEvents.CHECKOUT_STARTED), EcommerceEvent.CHECKOUT_STARTED);
        mapping.put(lowerCase(ECommerceEvents.ORDER_COMPLETED), EcommerceEvent.ORDER_COMPLETED);
        mapping.put(lowerCase(ECommerceEvents.ORDER_REFUNDED), EcommerceEvent.ORDER_REFUNDED);
        mapping.put(lowerCase(ECommerceEvents.ORDER_CANCELLED), EcommerceEvent.ORDER_CANCELLED);
        return mapping;
    }

    private static String lowerCase(String value) {
        return value.toLowerCase(Locale.US);
    }

    // Resolves a RudderStack event name to its Braze recommended event (case-insensitive).
    // Returns null for events without a recommended-event counterpart.
    static EcommerceEvent resolveEcommerceEvent(String eventName) {
        if (eventName == null) {
            return null;
        }
        return ECOMMERCE_EVENT_MAPPING.get(eventName.trim().toLowerCase(Locale.US));
    }

    // Builds the Braze custom-event property map for a recommended ecommerce event.
    // Send-anyway posture (D5): build from an empty map when properties are absent so the event is
    // still logged (with required-field warnings) rather than dropped.
    static Map<String, Object> buildEcommerceProperties(EcommerceEvent ecommerceEvent, Map<String, Object> properties) {
        Map<String, Object> props = (properties != null) ? properties : new HashMap<>();
        switch (ecommerceEvent) {
            case PRODUCT_VIEWED:
                return buildProductViewed(props);
            case PRODUCT_ADDED:
            case PRODUCT_REMOVED:
                return buildCartUpdated(props, ecommerceEvent.action);
            case CHECKOUT_STARTED:
                return buildCheckoutStarted(props);
            case ORDER_COMPLETED:
                return buildOrderPlaced(props);
            case ORDER_REFUNDED:
                return buildOrderRefunded(props);
            case ORDER_CANCELLED:
                return buildOrderCancelled(props);
            default:
                return new HashMap<>();
        }
    }

    // ecommerce.product_viewed — flat, single-product event (no products array, no quantity).
    private static Map<String, Object> buildProductViewed(Map<String, Object> props) {
        String brazeEvent = EcommerceEvent.PRODUCT_VIEWED.brazeEvent;
        Map<String, Object> out = new HashMap<>();

        Object productId = firstNonNull(props, EC_PRODUCT_ID, EC_SKU);
        Object productName = firstNonNull(props, EC_NAME);
        Object variantId = firstNonNull(props, EC_VARIANT, EC_SKU, EC_PRODUCT_ID);
        Object price = firstNonNull(props, EC_PRICE);
        Object currency = firstNonNull(props, EC_CURRENCY);

        warnIfMissing(brazeEvent, BRAZE_PRODUCT_ID, productId);
        warnIfMissing(brazeEvent, BRAZE_PRODUCT_NAME, productName);
        warnIfMissing(brazeEvent, BRAZE_VARIANT_ID, variantId);
        warnIfMissing(brazeEvent, BRAZE_PRICE, price);
        warnIfMissing(brazeEvent, BRAZE_CURRENCY, currency);

        putIfPresent(out, BRAZE_PRODUCT_ID, productId);
        putIfPresent(out, BRAZE_PRODUCT_NAME, productName);
        putIfPresent(out, BRAZE_VARIANT_ID, variantId);
        putIfPresent(out, BRAZE_PRICE, price);
        putIfPresent(out, BRAZE_CURRENCY, currency);
        putIfPresent(out, BRAZE_IMAGE_URL, firstNonNull(props, EC_IMAGE_URL));
        putIfPresent(out, BRAZE_PRODUCT_URL, firstNonNull(props, EC_URL));
        out.put(SOURCE_KEY, SOURCE);

        putMetadata(out, props, PRODUCT_VIEWED_CONSUMED_KEYS);
        return out;
    }

    // ecommerce.cart_updated — Product Added/Removed; single top-level product wrapped into a
    // 1-element products array. action is the only difference between add and remove.
    private static Map<String, Object> buildCartUpdated(Map<String, Object> props, String action) {
        String brazeEvent = EcommerceEvent.PRODUCT_ADDED.brazeEvent; // ecommerce.cart_updated
        Map<String, Object> out = new HashMap<>();

        Object cartId = firstNonNull(props, EC_CART_ID);
        Object currency = firstNonNull(props, EC_CURRENCY);
        Map<String, Object> product = buildProductFields(props);

        warnIfMissing(brazeEvent, BRAZE_CART_ID, cartId);
        warnIfMissing(brazeEvent, BRAZE_CURRENCY, currency);
        if (product.isEmpty()) {
            warnIfMissing(brazeEvent, BRAZE_PRODUCTS, null);
        }

        putIfPresent(out, BRAZE_CART_ID, cartId);
        putIfPresent(out, BRAZE_CURRENCY, currency);
        out.put(BRAZE_ACTION, action);
        if (!product.isEmpty()) {
            List<Map<String, Object>> products = new ArrayList<>();
            products.add(product);
            out.put(BRAZE_PRODUCTS, products);
        }
        out.put(SOURCE_KEY, SOURCE);

        putMetadata(out, props, CART_UPDATED_CONSUMED_KEYS);
        return out;
    }

    // ecommerce.checkout_started — checkout_id falls back to order_id.
    private static Map<String, Object> buildCheckoutStarted(Map<String, Object> props) {
        String brazeEvent = EcommerceEvent.CHECKOUT_STARTED.brazeEvent;
        Map<String, Object> out = new HashMap<>();

        Object checkoutId = firstNonNull(props, EC_CHECKOUT_ID, EC_ORDER_ID);
        Object totalValue = firstNonNull(props, EC_TOTAL, EC_REVENUE, EC_VALUE);
        Object currency = firstNonNull(props, EC_CURRENCY);
        List<Map<String, Object>> products = buildProducts(props);

        warnIfMissing(brazeEvent, BRAZE_CHECKOUT_ID, checkoutId);
        warnIfMissing(brazeEvent, BRAZE_TOTAL_VALUE, totalValue);
        warnIfMissing(brazeEvent, BRAZE_CURRENCY, currency);
        if (products == null) {
            warnIfMissing(brazeEvent, BRAZE_PRODUCTS, null);
        }

        putIfPresent(out, BRAZE_CHECKOUT_ID, checkoutId);
        putIfPresent(out, BRAZE_TOTAL_VALUE, totalValue);
        putIfPresent(out, BRAZE_CURRENCY, currency);
        if (products != null) {
            out.put(BRAZE_PRODUCTS, products);
        }
        putIfPresent(out, BRAZE_TAX, firstNonNull(props, EC_TAX));
        putIfPresent(out, BRAZE_SHIPPING, firstNonNull(props, EC_SHIPPING));
        out.put(SOURCE_KEY, SOURCE);

        putMetadata(out, props, CHECKOUT_STARTED_CONSUMED_KEYS);
        return out;
    }

    // ecommerce.order_placed — Order Completed. One event per order with all products inside
    // products[] (vs. the legacy one-purchase-per-product model).
    private static Map<String, Object> buildOrderPlaced(Map<String, Object> props) {
        String brazeEvent = EcommerceEvent.ORDER_COMPLETED.brazeEvent; // ecommerce.order_placed
        Map<String, Object> out = new HashMap<>();

        Object orderId = firstNonNull(props, EC_ORDER_ID);
        Object totalValue = firstNonNull(props, EC_TOTAL, EC_REVENUE, EC_VALUE);
        Object currency = firstNonNull(props, EC_CURRENCY);
        List<Map<String, Object>> products = buildProducts(props);

        warnIfMissing(brazeEvent, BRAZE_ORDER_ID, orderId);
        warnIfMissing(brazeEvent, BRAZE_TOTAL_VALUE, totalValue);
        warnIfMissing(brazeEvent, BRAZE_CURRENCY, currency);
        if (products == null) {
            warnIfMissing(brazeEvent, BRAZE_PRODUCTS, null);
        }

        putIfPresent(out, BRAZE_ORDER_ID, orderId);
        putIfPresent(out, BRAZE_TOTAL_VALUE, totalValue);
        putIfPresent(out, BRAZE_CURRENCY, currency);
        if (products != null) {
            out.put(BRAZE_PRODUCTS, products);
        }
        putIfPresent(out, BRAZE_TAX, firstNonNull(props, EC_TAX));
        putIfPresent(out, BRAZE_SHIPPING, firstNonNull(props, EC_SHIPPING));
        putIfPresent(out, BRAZE_TOTAL_DISCOUNTS, firstNonNull(props, EC_DISCOUNT));
        out.put(SOURCE_KEY, SOURCE);

        putMetadata(out, props, ORDER_PLACED_CONSUMED_KEYS);
        return out;
    }

    // ecommerce.order_refunded — RS Order Refunded carries only order_id today; total_value,
    // currency and products are RS-spec gaps (mapped if present, warned if absent).
    private static Map<String, Object> buildOrderRefunded(Map<String, Object> props) {
        String brazeEvent = EcommerceEvent.ORDER_REFUNDED.brazeEvent;
        Map<String, Object> out = new HashMap<>();

        Object orderId = firstNonNull(props, EC_ORDER_ID);
        Object totalValue = firstNonNull(props, EC_TOTAL, EC_REVENUE, EC_VALUE);
        Object currency = firstNonNull(props, EC_CURRENCY);
        List<Map<String, Object>> products = buildProducts(props);

        warnIfMissing(brazeEvent, BRAZE_ORDER_ID, orderId);
        warnIfMissing(brazeEvent, BRAZE_TOTAL_VALUE, totalValue);
        warnIfMissing(brazeEvent, BRAZE_CURRENCY, currency);
        if (products == null) {
            warnIfMissing(brazeEvent, BRAZE_PRODUCTS, null);
        }

        putIfPresent(out, BRAZE_ORDER_ID, orderId);
        putIfPresent(out, BRAZE_TOTAL_VALUE, totalValue);
        putIfPresent(out, BRAZE_CURRENCY, currency);
        if (products != null) {
            out.put(BRAZE_PRODUCTS, products);
        }
        out.put(SOURCE_KEY, SOURCE);

        putMetadata(out, props, ORDER_REFUNDED_CONSUMED_KEYS);
        return out;
    }

    // ecommerce.order_cancelled — total_value is the absolute value of total; cancel_reason has
    // no standard RS source (mapped if present, warned if absent).
    private static Map<String, Object> buildOrderCancelled(Map<String, Object> props) {
        String brazeEvent = EcommerceEvent.ORDER_CANCELLED.brazeEvent;
        Map<String, Object> out = new HashMap<>();

        Object orderId = firstNonNull(props, EC_ORDER_ID);
        Object totalValue = toAbsoluteValue(firstNonNull(props, EC_TOTAL));
        Object currency = firstNonNull(props, EC_CURRENCY);
        Object cancelReason = firstNonNull(props, EC_CANCEL_REASON);
        List<Map<String, Object>> products = buildProducts(props);

        warnIfMissing(brazeEvent, BRAZE_ORDER_ID, orderId);
        warnIfMissing(brazeEvent, BRAZE_TOTAL_VALUE, totalValue);
        warnIfMissing(brazeEvent, BRAZE_CURRENCY, currency);
        warnIfMissing(brazeEvent, BRAZE_CANCEL_REASON, cancelReason);
        if (products == null) {
            warnIfMissing(brazeEvent, BRAZE_PRODUCTS, null);
        }

        putIfPresent(out, BRAZE_ORDER_ID, orderId);
        putIfPresent(out, BRAZE_TOTAL_VALUE, totalValue);
        putIfPresent(out, BRAZE_CURRENCY, currency);
        putIfPresent(out, BRAZE_CANCEL_REASON, cancelReason);
        if (products != null) {
            out.put(BRAZE_PRODUCTS, products);
        }
        putIfPresent(out, BRAZE_TAX, firstNonNull(props, EC_TAX));
        putIfPresent(out, BRAZE_SHIPPING, firstNonNull(props, EC_SHIPPING));
        putIfPresent(out, BRAZE_TOTAL_DISCOUNTS, firstNonNull(props, EC_DISCOUNT));
        out.put(SOURCE_KEY, SOURCE);

        putMetadata(out, props, ORDER_CANCELLED_CONSUMED_KEYS);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> buildProducts(Map<String, Object> props) {
        Object raw = props.get(EC_PRODUCTS);
        if (!(raw instanceof List)) {
            return null;
        }
        List<?> rsProducts = (List<?>) raw;
        List<Map<String, Object>> products = new ArrayList<>();
        for (Object item : rsProducts) {
            if (item instanceof Map) {
                products.add(buildProduct((Map<String, Object>) item));
            }
        }
        return products.isEmpty() ? null : products;
    }

    private static Map<String, Object> buildProduct(Map<String, Object> rsProduct) {
        Map<String, Object> product = buildProductFields(rsProduct);
        putMetadata(product, rsProduct, PRODUCT_CONSUMED_KEYS);
        return product;
    }

    // Maps the shared Braze product object (no metadata). cart_updated routes leftover product
    // keys into event-level metadata, so it uses this directly; array products add per-product
    // metadata via buildProduct.
    private static Map<String, Object> buildProductFields(Map<String, Object> rsProduct) {
        Map<String, Object> product = new HashMap<>();
        putIfPresent(product, BRAZE_PRODUCT_ID, firstNonNull(rsProduct, EC_PRODUCT_ID, EC_SKU));
        putIfPresent(product, BRAZE_PRODUCT_NAME, firstNonNull(rsProduct, EC_NAME));
        putIfPresent(product, BRAZE_VARIANT_ID, firstNonNull(rsProduct, EC_VARIANT, EC_SKU, EC_PRODUCT_ID));
        putIfPresent(product, BRAZE_QUANTITY, firstNonNull(rsProduct, EC_QUANTITY));
        putIfPresent(product, BRAZE_PRICE, firstNonNull(rsProduct, EC_PRICE));
        putIfPresent(product, BRAZE_IMAGE_URL, firstNonNull(rsProduct, EC_IMAGE_URL));
        putIfPresent(product, BRAZE_PRODUCT_URL, firstNonNull(rsProduct, EC_URL));
        return product;
    }

    // D6: any source key not consumed by a mapping flows into metadata (never left top-level).
    private static void putMetadata(Map<String, Object> target, Map<String, Object> props, Set<String> consumedKeys) {
        Map<String, Object> metadata = new HashMap<>();
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (!consumedKeys.contains(entry.getKey())) {
                metadata.put(entry.getKey(), entry.getValue());
            }
        }
        if (!metadata.isEmpty()) {
            target.put(BRAZE_METADATA, metadata);
        }
    }

    // Returns the first non-null, non-empty value among the given keys (fallback chain).
    private static Object firstNonNull(Map<String, Object> props, String... keys) {
        if (props == null) {
            return null;
        }
        for (String key : keys) {
            Object value = props.get(key);
            if (value != null && !(value instanceof String && ((String) value).isEmpty())) {
                return value;
            }
        }
        return null;
    }

    private static void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }

    // Send-anyway validation (D5): a missing Braze-required field is logged but never drops the
    // event. source is intentionally never checked here — it always resolves to the platform.
    private static void warnIfMissing(String brazeEvent, String field, Object value) {
        if (value == null) {
            RudderLogger.logWarn(String.format(
                    "BrazeIntegrationFactory: recommended event %s is missing required field '%s'; sending event anyway.",
                    brazeEvent, field));
        }
    }

    private static Object toAbsoluteValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Math.abs(Double.parseDouble(String.valueOf(value)));
        } catch (NumberFormatException exception) {
            return value;
        }
    }
}
