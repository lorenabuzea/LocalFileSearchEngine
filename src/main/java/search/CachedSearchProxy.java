package search;

import model.SearchResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CachedSearchProxy implements SearchService {

    private final SearchService realSearchService;
    private final Map<String, List<SearchResult>> cache;

    public CachedSearchProxy(SearchService realSearchService) {
        this.realSearchService = realSearchService;
        this.cache = new HashMap<>();
    }

    @Override
    public List<SearchResult> search(String query) {
        if (cache.containsKey(query)) {
            System.out.println("Cache hit for query: " + query);
            return cache.get(query);
        } else {
            System.out.println("Cache miss for query: " + query);
            List<SearchResult> results = realSearchService.search(query);
            cache.put(query, results);
            return results;
        }
    }
}
