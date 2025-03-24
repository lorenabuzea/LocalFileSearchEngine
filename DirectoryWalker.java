package Crawler;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DirectoryWalker {

    public List<File> walk(String rootPath) {
        List<File> validFiles = new ArrayList<>();
        File root = new File(rootPath);
        crawlRecursive(root, validFiles);
        return validFiles;
    }

    private void crawlRecursive(File dir, List<File> fileList) {
        if (dir == null || !dir.exists()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                crawlRecursive(file, fileList);
            } else {
                fileList.add(file); //All files for now, filtering happens in FileValidator
            }
        }
    }
}
