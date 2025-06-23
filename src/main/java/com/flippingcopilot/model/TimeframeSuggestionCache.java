package com.flippingcopilot.model;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class TimeframeSuggestionCache {
    private final Map<Integer, Suggestion> cache = new HashMap<>();
    private int currentItemId = -1;

    @Inject
    public TimeframeSuggestionCache() {}

    public void cacheSuggestion(int timeframe, Suggestion suggestion) {
        // If the new suggestion is for a different item, clear the old cache
        if (suggestion.getItemId() != currentItemId) {
            clear();
            currentItemId = suggestion.getItemId();
        }
        cache.put(timeframe, suggestion);
    }

    public Suggestion getSuggestionFor(int timeframe) {
        return cache.get(timeframe);
    }

    public void clear() {
        cache.clear();
        currentItemId = -1;
    }
}