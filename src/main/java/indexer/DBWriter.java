package indexer;

import java.sql.*;
import java.util.Map;

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
        String sql = "INSERT INTO files (path, name, content, extension, modified_time, size) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, path);
            stmt.setString(2, name);
            stmt.setString(3, content);
            stmt.setString(4, extension);
            stmt.setTimestamp(5, modifiedTime);
            stmt.setLong(6, size);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Insert failed: " + path + " — " + e.getMessage());
            return false;
        }
    }


    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            System.err.println("Failed to close database connection: " + e.getMessage());
        }
    }
}
