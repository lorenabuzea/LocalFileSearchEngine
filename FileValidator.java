package Crawler;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class FileValidator {

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(".txt");

    public boolean isValid(File file) {
        if (!file.isFile() || file.isHidden()) return false;

        String name = file.getName().toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (name.endsWith(ext)) return true;
        }
        return false;
    }
}
