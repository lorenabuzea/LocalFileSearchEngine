package search;

import model.SearchResult;
import java.util.List;

public interface SearchService {
    List<SearchResult> search(String query);
}
