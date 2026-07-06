package com.rudderstack.android.integration.braze;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.rudderstack.android.integration.braze.Utils.EcommerceEvent;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Unit tests for the recommended-ecommerce mapping in Utils. Pure-Map assertions need no Android
// runtime (RudderLogger is a no-op at the default log level); the JSON-preservation test uses the
// real org.json pulled in as a test dependency. The JSON test specifically guards the fix for the
// bug where BrazeProperties(Map) dropped List values (products / discounts / type).
public class UtilsTest {

    // ----- helpers -----

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((String) pairs[i], pairs[i + 1]);
        }
        return m;
    }

    private static List<Object> list(Object... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private static Map<String, Object> buildOrderCompleted(Map<String, Object> props) {
        return Utils.buildEcommerceProperties(EcommerceEvent.ORDER_COMPLETED, props);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstProduct(Map<String, Object> out) {
        List<Map<String, Object>> products = (List<Map<String, Object>>) out.get("products");
        return products.get(0);
    }

    // ----- resolveEcommerceEvent -----

    @Test
    public void resolveEcommerceEvent_mapsKnownEventsCaseInsensitively() {
        assertEquals(EcommerceEvent.ORDER_COMPLETED, Utils.resolveEcommerceEvent("Order Completed"));
        assertEquals(EcommerceEvent.ORDER_COMPLETED, Utils.resolveEcommerceEvent("order completed"));
        assertEquals(EcommerceEvent.ORDER_COMPLETED, Utils.resolveEcommerceEvent("  Order Completed  "));
        assertEquals(EcommerceEvent.PRODUCT_VIEWED, Utils.resolveEcommerceEvent("Product Viewed"));
        assertEquals(EcommerceEvent.PRODUCT_ADDED, Utils.resolveEcommerceEvent("Product Added"));
        assertEquals(EcommerceEvent.PRODUCT_REMOVED, Utils.resolveEcommerceEvent("Product Removed"));

        assertEquals("ecommerce.order_placed", EcommerceEvent.ORDER_COMPLETED.getBrazeEvent());
        assertEquals("ecommerce.cart_updated", EcommerceEvent.PRODUCT_ADDED.getBrazeEvent());
    }

    @Test
    public void resolveEcommerceEvent_returnsNullForUnmappedOrNull() {
        // Product Clicked and Cart Viewed intentionally stay generic custom events.
        assertNull(Utils.resolveEcommerceEvent("Product Clicked"));
        assertNull(Utils.resolveEcommerceEvent("Cart Viewed"));
        assertNull(Utils.resolveEcommerceEvent("Some Random Event"));
        assertNull(Utils.resolveEcommerceEvent(null));
    }

    // ----- Order Completed -> ecommerce.order_placed mapping -----

    @Test
    public void orderCompleted_mapsCoreFieldsAndProducts() {
        Map<String, Object> out = buildOrderCompleted(map(
                "order_id", "O-100",
                "total", 200,
                "currency", "USD",
                "products", list(map(
                        "product_id", "P1",
                        "name", "Mug",
                        "price", 12.5,
                        "quantity", 3))));

        assertEquals("O-100", out.get("order_id"));
        assertEquals("USD", out.get("currency"));
        assertEquals(200, ((Number) out.get("total_value")).intValue());
        assertEquals("android", out.get("source"));

        Map<String, Object> product = firstProduct(out);
        assertEquals("P1", product.get("product_id"));
        assertEquals("Mug", product.get("product_name"));
        // variant_id falls back to sku -> product_id when absent.
        assertEquals("P1", product.get("variant_id"));
        assertEquals(12.5, ((Number) product.get("price")).doubleValue(), 0.0);
        assertEquals(3, ((Number) product.get("quantity")).intValue());
    }

    @Test
    public void orderCompleted_totalValueFallsBackTotalThenRevenueThenValue() {
        assertEquals(200, ((Number) buildOrderCompleted(map("total", 200, "revenue", 1, "value", 2))
                .get("total_value")).intValue());
        assertEquals(50, ((Number) buildOrderCompleted(map("revenue", 50, "value", 2))
                .get("total_value")).intValue());
        assertEquals(30, ((Number) buildOrderCompleted(map("value", 30))
                .get("total_value")).intValue());
    }

    // ----- metadata pass-through (D6) -----

    @Test
    @SuppressWarnings("unchecked")
    public void metadata_unmappedKeysRoutedToMetadataNotTopLevel() {
        Map<String, Object> out = buildOrderCompleted(map(
                "order_id", "O-100",
                "affiliation", "web-store",            // unmapped top-level
                "products", list(map(
                        "product_id", "P1",
                        "color", "blue"))));            // unmapped product key

        // Unmapped top-level key must not leak to the top level; it lives under metadata.
        assertFalse(out.containsKey("affiliation"));
        Map<String, Object> metadata = (Map<String, Object>) out.get("metadata");
        assertEquals("web-store", metadata.get("affiliation"));

        Map<String, Object> product = firstProduct(out);
        assertFalse(product.containsKey("color"));
        Map<String, Object> productMetadata = (Map<String, Object>) product.get("metadata");
        assertEquals("blue", productMetadata.get("color"));
    }

    // ----- type coercion -----

    @Test
    public void coercion_numericStringsBecomeNumbersAndNumbersBecomeStrings() {
        Map<String, Object> out = buildOrderCompleted(map(
                "total", "199.99",                      // FLOAT field, numeric string -> double
                "products", list(map(
                        "product_id", 1001,             // STRING field, number -> string
                        "price", "12.5",                // FLOAT field, numeric string -> double
                        "quantity", "3"))));            // INTEGER field, numeric string -> long

        assertEquals(199.99, ((Number) out.get("total_value")).doubleValue(), 0.0);

        Map<String, Object> product = firstProduct(out);
        assertTrue(product.get("product_id") instanceof String);
        assertEquals("1001", product.get("product_id"));
        assertEquals(12.5, ((Number) product.get("price")).doubleValue(), 0.0);
        assertEquals(3, ((Number) product.get("quantity")).intValue());
    }

    @Test
    public void coercion_unparsableValueLeftAsIs() {
        // "free" cannot be coerced to a FLOAT price; it is sent verbatim (warned, never dropped).
        Map<String, Object> product = firstProduct(buildOrderCompleted(map(
                "order_id", "O-100",
                "products", list(map("product_id", "P1", "price", "free")))));
        assertEquals("free", product.get("price"));
    }

    // ----- empty products (fix #2) -----

    @Test
    @SuppressWarnings("unchecked")
    public void emptyProducts_areOmittedNotSentAsEmptyObject() {
        // A products array of only-empty maps must be treated as "no products" (key omitted),
        // never sent as products: [ {} ].
        Map<String, Object> out = buildOrderCompleted(map(
                "order_id", "O-100",
                "products", list(new HashMap<String, Object>())));
        assertFalse(out.containsKey("products"));

        // A mix keeps only the non-empty product.
        Map<String, Object> mixed = buildOrderCompleted(map(
                "order_id", "O-100",
                "products", list(new HashMap<String, Object>(), map("product_id", "P1"))));
        List<Map<String, Object>> products = (List<Map<String, Object>>) mixed.get("products");
        assertEquals(1, products.size());
        assertEquals("P1", products.get(0).get("product_id"));
    }

    // ----- toBrazeJson preserves arrays (fix #1 guard) -----

    @Test
    public void toBrazeJson_preservesProductsAndDiscountsAsJsonArrays() throws Exception {
        Map<String, Object> out = buildOrderCompleted(map(
                "order_id", "O-100",
                "total", 200,
                "currency", "USD",
                "affiliation", "web-store",            // unmapped top-level -> metadata
                "discounts", list(map("code", "WELCOME", "amount", 15)),
                "products", list(
                        map("product_id", "P1", "name", "Mug", "color", "blue"),
                        map("product_id", "P2", "name", "Saucer"))));

        JSONObject json = Utils.toBrazeJson(out);

        // Scalars survive unchanged.
        assertEquals("O-100", json.get("order_id"));
        assertEquals("USD", json.get("currency"));

        // The arrays that BrazeProperties(Map) used to drop are now real JSONArrays.
        assertTrue(json.get("products") instanceof JSONArray);
        JSONArray products = json.getJSONArray("products");
        assertEquals(2, products.length());
        assertTrue(products.get(0) instanceof JSONObject);
        assertEquals("P1", products.getJSONObject(0).get("product_id"));

        assertTrue(json.get("discounts") instanceof JSONArray);
        assertEquals(1, json.getJSONArray("discounts").length());

        // Nested objects (top-level + product metadata) become JSONObjects, not raw Maps.
        assertTrue(json.get("metadata") instanceof JSONObject);
        assertTrue(products.getJSONObject(0).get("metadata") instanceof JSONObject);
        assertEquals("blue", products.getJSONObject(0).getJSONObject("metadata").get("color"));
    }

    @Test
    public void toBrazeJson_preservesStringArrayTypeField() throws Exception {
        // product_viewed's "type" is a STRING_ARRAY; it must survive as a JSONArray.
        Map<String, Object> out = Utils.buildEcommerceProperties(EcommerceEvent.PRODUCT_VIEWED, map(
                "product_id", "PV1",
                "type", list("shoe", "running")));

        JSONObject json = Utils.toBrazeJson(out);
        assertTrue(json.get("type") instanceof JSONArray);
        JSONArray type = json.getJSONArray("type");
        assertEquals(2, type.length());
        assertEquals("shoe", type.get(0));
        assertEquals("running", type.get(1));
    }

    // ----- malformed products -> metadata (never dropped) -----

    @Test
    @SuppressWarnings("unchecked")
    public void malformedProductsValue_flowsToMetadataNotDropped() {
        // A non-list products value must not be silently dropped; it is preserved under metadata.
        Map<String, Object> out = buildOrderCompleted(map(
                "order_id", "O-100",
                "products", "not-a-list"));
        assertFalse(out.containsKey("products"));
        Map<String, Object> metadata = (Map<String, Object>) out.get("metadata");
        assertEquals("not-a-list", metadata.get("products"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void mixedProductsArray_flowsToMetadataNotPartiallyMapped() {
        // A list containing a non-Map element is treated as malformed (all-or-nothing) and preserved
        // under metadata rather than partially mapped.
        List<Object> mixed = list(map("product_id", "P1"), "junk");
        Map<String, Object> out = buildOrderCompleted(map("order_id", "O-100", "products", mixed));
        assertFalse(out.containsKey("products"));
        Map<String, Object> metadata = (Map<String, Object>) out.get("metadata");
        assertEquals(mixed, metadata.get("products"));
    }

    // ----- cart_updated products[] -----

    @Test
    @SuppressWarnings("unchecked")
    public void cartUpdated_mapsExplicitProductsArrayItemByItem() {
        // An explicit products[] on Product Added is mapped item-by-item (not folded from top-level).
        Map<String, Object> out = Utils.buildEcommerceProperties(EcommerceEvent.PRODUCT_ADDED, map(
                "cart_id", "C-1",
                "products", list(
                        map("product_id", "P1", "name", "Mug"),
                        map("product_id", "P2", "name", "Saucer"))));

        assertEquals("add", out.get("action"));
        List<Map<String, Object>> products = (List<Map<String, Object>>) out.get("products");
        assertEquals(2, products.size());
        assertEquals("P1", products.get(0).get("product_id"));
        assertEquals("Mug", products.get(0).get("product_name"));
        assertEquals("P2", products.get(1).get("product_id"));
        // products[] is consumed, so it is not duplicated into metadata.
        assertFalse(out.containsKey("metadata"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void cartUpdated_fallsBackToTopLevelProductFields() {
        // Without products[], top-level product fields fold into a single-element products array.
        Map<String, Object> out = Utils.buildEcommerceProperties(EcommerceEvent.PRODUCT_REMOVED, map(
                "cart_id", "C-1",
                "product_id", "P1",
                "name", "Mug",
                "price", 9.99));

        assertEquals("remove", out.get("action"));
        List<Map<String, Object>> products = (List<Map<String, Object>>) out.get("products");
        assertEquals(1, products.size());
        assertEquals("P1", products.get(0).get("product_id"));
        assertEquals("Mug", products.get(0).get("product_name"));
    }
}
