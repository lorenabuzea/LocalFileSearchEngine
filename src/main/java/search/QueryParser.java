package search;

import java.util.HashMap;
import java.util.Map;

public class QueryParser {

    public Map<String, String> parse(String rawQuery) {
        Map<String, String> terms = new HashMap<>();

        for (String token : rawQuery.split("\\s+")) {
            if (token.contains(":")) {
                String[] parts = token.split(":", 2);
                String field = parts[0].toLowerCase();
                String value = parts[1];

                if (!value.isBlank()) {
                    // Combine multiple path: or content: with AND logic
                    terms.merge(field, value, (oldVal, newVal) -> oldVal + " " + newVal);
                }
            } else {
                // default to content
                terms.merge("content", token, (oldVal, newVal) -> oldVal + " " + newVal);
            }
        }

        return terms;
    }
}
