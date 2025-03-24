package indexer;

import Crawler.FileCrawler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Timestamp;
import java.util.List;

public class Indexer {

    private final FileCrawler crawler;
    private final DBWriter dbWriter;

    public Indexer() {
        this.crawler = new FileCrawler();
        this.dbWriter = new DBWriter();
    }

    public void indexDirectory(String rootPath) { //further optimization-multi thread usage
        List<File> files = crawler.getTextFiles(rootPath);

        if (files.isEmpty()) {
            System.out.println("No .txt files found.");
            return;
        }

        for (File file : files) {
            try {
                String content = Files.readString(file.toPath());

                dbWriter.insert(
                        file.getAbsolutePath(),
                        file.getName(),
                        content,
                        getExtension(file.getName()),
                        new Timestamp(file.lastModified()),
                        file.length()
                );
            } catch (IOException e) {
                System.err.println("Failed to read file: " + file.getAbsolutePath());
            }
        }

        dbWriter.close();
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i > 0) ? filename.substring(i).toLowerCase() : "";
    }
}
