package search;

public class SnippetGenerator {

    public String getSnippet(String content, int lines) {
        if (content == null) return "";
        String[] split = content.split("\\R");
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < Math.min(lines, split.length); i++) {
            sb.append(split[i]).append("\n");
        }

        return sb.toString();
    }
}
