package Crawler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileCrawler {

    private final DirectoryWalker walker = new DirectoryWalker();
    private final FileValidator validator = new FileValidator();

    public List<File> getTextFiles(String rootPath) {
        List<File> allFiles = walker.walk(rootPath);
        List<File> validFiles = new ArrayList<>();

        for (File file : allFiles) {
            if (validator.isValid(file)) {
                validFiles.add(file);
            }
        }
        return validFiles;
    }
}

