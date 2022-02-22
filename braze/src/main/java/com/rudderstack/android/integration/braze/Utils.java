package com.rudderstack.android.integration.braze;

import com.google.gson.Gson;
import com.rudderstack.android.sdk.core.RudderLogger;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class Utils {


    static String getString(Object object) {
        if (object == null) {
            return null;
        }
        switch (getType(object)) {
            case "Byte":
            case "Short":
            case "Integer":
            case "Long":
            case "Float":
            case "Double":
            case "Boolean":
            case "Character":
            case "ArrayList":
            case "HashMap":
                return object.toString();
            case "String":
                return (String) object;
            case "Array":
                return new Gson().toJson(object);
            default:
                return null;
        }
    }

    static double getDouble(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException ignored) {
                RudderLogger.logDebug("Unable to convert the value: " + value +
                        " to Double, using the defaultValue: " + (double) 0);
            }
        }
        return 0;
    }

    static int getInt(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                RudderLogger.logDebug("Unable to convert the value: " + value +
                        " to Integer, using the defaultValue: " + 0);
            }
        }
        return 0;
    }

    static String getType(Object object) {
        if (object.getClass().isArray()) {
            return "Array";
        }
        return object.getClass().getSimpleName();
    }

    public static boolean isEmpty(Object value) {
        if(value == null){
            return true;
        }
        if (value instanceof String) {
            return (((String) value).trim().isEmpty());
        }
        if (value instanceof JSONArray) {
            return (((JSONArray) value).length() == 0);
        }
        if (value instanceof JSONObject) {
            return (((JSONObject) value).length() == 0);
        }
        if (value instanceof Map) {
            return ((Map<?, ?>) value).size() == 0;
        }
        return false;
    }

    public static Date dateFromString(String date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        try {
            return formatter.parse(date);
        } catch (Exception e) {
            return null;
        }
    }

    static double getRevenue(Map<String, Object> eventProperties, String key) {
        if (eventProperties.containsKey(key)) {
            return getDouble(eventProperties.get(key));
        }
        return 0.0D;
    }
}
