package widget;

import model.SearchResult;

import javax.swing.*;
import java.util.List;

public class LogAnalyserWidget implements ContextAwareWidget {

    @Override
    public boolean isRelevant(String query, List<SearchResult> results) {
        long count = results.stream()
                .filter(r -> r.filePath.toLowerCase().endsWith(".log"))
                .count();
        return count >= 5;  //triggered for more log files
    }

    @Override
    public JPanel render() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("Log Analyzer"));
        panel.add(new JLabel("Detected log files. Would you like to analyze them?"));
        return panel;
    }
}
