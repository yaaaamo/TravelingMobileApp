package com.example.traveling;

import java.util.List;
import java.util.Map;

/**
 * Simple static holder to pass a shared journey from GroupDetailsActivity
 * to MainActivity → Maps without Parcelable complexity.
 * Clear after use.
 */
public class SharedJourneyHolder {
    public static List<Map<String, Object>> places = null;
    public static String title = null;

    public static void clear() {
        places = null;
        title  = null;
    }
}