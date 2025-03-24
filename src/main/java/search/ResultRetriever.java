package search;

import model.SearchResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResultRetriever {

    private final Connection connection;

    public ResultRetriever(Connection connection) {
        this.connection = connection;
    }

    public List<SearchResult> search(String rawQuery) {
        List<SearchResult> results = new ArrayList<>();

        // Split input into multiple keywords
        String[] keywords = rawQuery.trim().split("\\s+");

        // Build dynamic SQL query
        StringBuilder sql = new StringBuilder("SELECT path, content FROM files WHERE ");
        for (int i = 0; i < keywords.length; i++) {
            if (i > 0) sql.append(" AND ");
            sql.append("(")
                    .append("LOWER(path) LIKE ? OR ")
                    .append("LOWER(name) LIKE ? OR ")
                    .append("LOWER(content) LIKE ?")
                    .append(")");
        }

        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            for (String word : keywords) {
                String pattern = "%" + word.toLowerCase() + "%";
                stmt.setString(paramIndex++, pattern); //path
                stmt.setString(paramIndex++, pattern); //name
                stmt.setString(paramIndex++, pattern); //content
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String path = rs.getString("path");
                String content = rs.getString("content");
                results.add(new SearchResult(path, content));
            }
        } catch (SQLException e) {
            System.err.println("SEARCH ERROR: " + e.getMessage());
        }

        return results;
    }
}
