package indexer;

import java.sql.*;

public class DBWriter {

    private static final String DB_URL = "jdbc:sqlite:/Users/lorenabuzea/Desktop/UTCN/AN3/SEM2/SD/SearchEngineProject/src/main/resources/search_engine.db";

    private Connection connection;

    public DBWriter() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to SQLLite database.");
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return this.connection;
    }

    public boolean insert(String path, String name, String content, String extension, Timestamp modifiedTime, long size) {
        String sql = "INSERT INTO files (path, name, content, extension, modified_time, size, score) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, path);
            stmt.setString(2, name);
            stmt.setString(3, content);
            stmt.setString(4, extension);
            stmt.setTimestamp(5, modifiedTime);
            stmt.setLong(6, size);
            stmt.setInt(7, computeScore(path, extension, size));
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Insert failed: " + path + " — " + e.getMessage());
            return false;
        }
    }

    private int computeScore(String path, String ext, long size) {
        int score = 0;
        if (path.toLowerCase().contains("project")) score += 20;
        if (ext.equalsIgnoreCase(".md")) score += 10;
        if (size < 10000) score += 5;
        score -= path.length(); // shorter paths preferred
        return Math.max(score, 0);
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }
}
