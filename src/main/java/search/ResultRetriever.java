package search;

import model.SearchResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResultRetriever implements SearchService{

    private final Connection connection;

    public ResultRetriever(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<SearchResult> search(String rawQuery) {
        QueryParser parser = new QueryParser();
        Map<String, String> terms = parser.parse(rawQuery);

        StringBuilder sql = new StringBuilder("SELECT path, content, score FROM files WHERE 1=1");
        List<String> params = new ArrayList<>();

        if (terms.containsKey("path")) {
            for (String word : terms.get("path").split("\\s+")) {
                sql.append(" AND LOWER(path) LIKE ?");
                params.add("%" + word.toLowerCase() + "%");
            }
        }

        if (terms.containsKey("content")) {
            for (String word : terms.get("content").split("\\s+")) {
                sql.append(" AND LOWER(content) LIKE ?");
                params.add("%" + word.toLowerCase() + "%");
            }
        }

        sql.append(" ORDER BY score DESC");

        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setString(i + 1, params.get(i));
            }

            ResultSet rs = stmt.executeQuery();
            List<SearchResult> results = new ArrayList<>();
            while (rs.next()) {
                results.add(new SearchResult(rs.getString("path"), rs.getString("content")));
            }
            return results;

        } catch (SQLException e) {
            System.err.println("Search error: " + e.getMessage());
            return List.of();
        }
    }
}

