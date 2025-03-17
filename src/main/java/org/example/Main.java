package org.example;
import Crawler.FileCrawler;
import java.io.File;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String rootPath = "/Users/lorenabuzea/Desktop"; // ✅ Update this to your actual folder
        FileCrawler crawler = new FileCrawler();

        System.out.println("Scanning: " + rootPath);
        List<File> files = crawler.getTextFiles(rootPath);

        if (files.isEmpty()) {
            System.out.println("No valid .txt files found.");
        } else {
            System.out.println("Found " + files.size() + " file(s):");
            for (File file : files) {
                System.out.println(" - " + file.getAbsolutePath());
            }
        }
    }
}
