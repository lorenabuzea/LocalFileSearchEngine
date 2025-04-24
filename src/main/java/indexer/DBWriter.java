package indexer;

import java.io.File;
import java.sql.*;
import java.util.List;

public class DBWriter {

    private static final String DB_URL = "jdbc:sqlite:/Users/lorenabuzea/Desktop/UTCN/AN3/SEM2/SD/SearchEngineProject/src/main/resources/search_engine.db";
    private static final List<String> KEYWORDS = List.of("project", "report", "final", "search");
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

        String lowerPath = path.toLowerCase();

        //ranking on path. shorter path is better. looking for fewer folders as well
        int pathLength = lowerPath.split("[/\\\\]").length;
        score+= Math.max(0,15-pathLength);


        //ranking based on extensions
        if (ext.equals(".md")) score += 15;
        if (ext.equals(".txt")) score += 10;
        if (ext.equals(".log")) score -= 5;

        //ranking on keyword presence in path
        for ( String keyword : KEYWORDS){
            if ( lowerPath.contains(keyword)){
                score +=20;
            }
        }

        //ranking on file size. smaller is better
        if (size < 10_000) score += 10;
        else if ( size < 100_000) score +=5;

        //ranking on last accessed files. if modified the last 7 days bring up score
        long now = System.currentTimeMillis();
        long age = now - new File(path).lastModified();
        if (age < 7L * 24 * 60 * 60 * 1000) score += 10;

        return score;
    }

    public void updateAllScores(){
        String selectSQL = "SELECT path, extension, size FROM files";
        String updateSQL = "UPDATE files SET score = ? WHERE path = ?";

        int updated  = 0;

        try(
                PreparedStatement selectStmt = connection.prepareStatement(selectSQL);
                PreparedStatement updateStmt = connection.prepareStatement(updateSQL);
                ResultSet rs = selectStmt.executeQuery()
                ) {
                while (rs.next()) {
                    String path = rs.getString("path");
                    String ext = rs.getString("extension");
                    long size = rs.getLong("size");

                    int score = computeScore(path, ext, size);

                    updateStmt.setInt(1,score);
                    updateStmt.setString(2,path);
                    updateStmt.addBatch();
                    updated++;

                }
                updateStmt.executeBatch();
            System.out.println("Added scores for " + updated + "files");

        }catch ( SQLException e){
            System.err.println("Failed to update scores " + e.getMessage());

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
