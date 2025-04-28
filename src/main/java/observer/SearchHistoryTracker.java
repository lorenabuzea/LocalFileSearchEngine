package observer;

import java.util.*;

public class SearchHistoryTracker implements SearchObserver {

    private final Map<String, Integer> searchFrequency = new HashMap<>();

    @Override
    public void onSearch(String query) {
        query = query.trim().toLowerCase();
        if (!query.isBlank()) {
            searchFrequency.put(query, searchFrequency.getOrDefault(query, 0) + 1);
        }
    }

    public List<String> suggestQueries() {
        return searchFrequency.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue()) // most frequent first
                .map(Map.Entry::getKey)
                .toList();
    }


}
