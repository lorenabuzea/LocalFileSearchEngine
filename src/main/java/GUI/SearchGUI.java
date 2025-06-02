package GUI;

import model.SearchResult;
import observer.SearchHistoryTracker;
import search.CachedSearchProxy;
import search.QueryProcessor;
import search.ResultRetriever;
import search.SnippetGenerator;
import indexer.DBWriter;
import Crawler.FileCrawler;
import util.SpellingCorrector;
import widget.WidgetManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class SearchGUI extends JFrame {

    private JTextField searchField, insertPathField;
    private JButton searchButton, showAllButton, loadButton, saveButton;
    private JList<String> resultsList;
    private JTextArea snippetArea;
    private JLabel searchLabel, resultLabel, snippetLabel, insertLabel;
    private DefaultComboBoxModel<String> suggestionModel;

    private JPopupMenu suggestionPopup;

    private DefaultListModel<String> listModel;
    private List<SearchResult> currentResults;
    private List<File> loadedFiles = new ArrayList<>();

    private CachedSearchProxy retriever;
    private SnippetGenerator snippetGenerator;
    private QueryProcessor queryProcessor;
    private DBWriter dbWriter;
    private FileCrawler fileCrawler;
    private SearchHistoryTracker historyTracker;

    private final WidgetManager widgetManager = new WidgetManager();
    private final JPanel widgetPanelContainer = new JPanel(new FlowLayout());

    private final JPanel metadataPanel = new JPanel();

    private SpellingCorrector corrector;


    public SearchGUI() {
        setTitle("Local File Search Engine");
        setSize(900, 650);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.decode("#F5F5F5"));

        setupUI();
        setupSearchEngine();
    }

    private void setupUI() {
        searchField = new JTextField(30);
        suggestionPopup = new JPopupMenu();
        searchButton = new JButton("🔍 Search");
        showAllButton = new JButton("📋 Show All Files");

        suggestionModel = new DefaultComboBoxModel<>();

        insertPathField = new JTextField(30);
        loadButton = new JButton("📂 Load from Path");
        saveButton = new JButton("💾 Save to DB");

        listModel = new DefaultListModel<>();
        resultsList = new JList<>(listModel);
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        snippetArea = new JTextArea(6, 40);
        snippetArea.setEditable(false);
        snippetArea.setLineWrap(true);
        snippetArea.setWrapStyleWord(true);
        snippetArea.setBackground(Color.WHITE);
        snippetArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JScrollPane listScroll = new JScrollPane(resultsList);
        JScrollPane snippetScroll = new JScrollPane(snippetArea);

        searchLabel = new JLabel("Search:");
        resultLabel = new JLabel("Search Results:");
        snippetLabel = new JLabel("File Preview:");
        insertLabel = new JLabel("Load Files from Directory:");

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setBackground(Color.decode("#F5F5F5"));
        topPanel.add(searchLabel);
        topPanel.add(searchField);
        topPanel.add(searchButton);
        topPanel.add(showAllButton);
        topPanel.add(new JLabel("Suggestions:"));

        JPanel insertPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        insertPanel.setBackground(Color.decode("#F5F5F5"));
        insertPanel.add(insertLabel);
        insertPanel.add(insertPathField);
        insertPanel.add(loadButton);
        insertPanel.add(saveButton);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout(5, 5));
        centerPanel.setBackground(Color.decode("#F5F5F5"));
        centerPanel.add(resultLabel, BorderLayout.NORTH);
        centerPanel.add(listScroll, BorderLayout.CENTER);
        centerPanel.add(metadataPanel, BorderLayout.SOUTH);  //new summary panel

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout(5, 5));
        bottomPanel.setBackground(Color.decode("#F5F5F5"));
        bottomPanel.add(snippetLabel, BorderLayout.NORTH);
        bottomPanel.add(snippetScroll, BorderLayout.CENTER);

        JPanel mainTopPanel = new JPanel(new GridLayout(2, 1));
        mainTopPanel.add(topPanel);
        mainTopPanel.add(insertPanel);

        add(mainTopPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        searchButton.addActionListener(this::handleSearch);
        showAllButton.addActionListener(this::handleShowAll);
        loadButton.addActionListener(this::handleLoadPath);
        saveButton.addActionListener(this::handleBatchSave);
        resultsList.addListSelectionListener(e -> showSnippet());

        add(widgetPanelContainer, BorderLayout.EAST);

    }

    private void setupSearchEngine() {
        dbWriter = new DBWriter();
        Connection conn = dbWriter.getConnection();

        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Could not connect to database.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        ResultRetriever baseRetriever = new ResultRetriever(conn);
        retriever = new CachedSearchProxy(baseRetriever);
        snippetGenerator = new SnippetGenerator();
        queryProcessor = new QueryProcessor();
        fileCrawler = new FileCrawler();
        historyTracker = new SearchHistoryTracker();

        Set<String> allWords = dbWriter.getAllIndexedWords();
        corrector = new SpellingCorrector(allWords);
    }

    private void handleSearch(ActionEvent e) {
        String originalQuery = searchField.getText().trim();
        if (originalQuery.isEmpty()) return;

        String[] tokens = originalQuery.split("\\s+");
        StringBuilder correctedQueryBuilder = new StringBuilder();

        for (String token : tokens) {
            correctedQueryBuilder.append(corrector.correct(token)).append(" ");
        }

        String correctedQuery = correctedQueryBuilder.toString().trim();

        String finalQueryToUse = originalQuery;
        if (!originalQuery.equalsIgnoreCase(correctedQuery)) {
            int choice = JOptionPane.showConfirmDialog(this,
                    "Did you mean: \"" + correctedQuery + "\"?\nRun corrected search?",
                    "Spelling Correction",
                    JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                finalQueryToUse = correctedQuery;
            }
        }

        String parsed = queryProcessor.prepare(finalQueryToUse);
        currentResults = retriever.search(parsed);

        historyTracker.onSearch(finalQueryToUse);
        updateResultsList();
        updateSuggestionsPopup();
        updateWidgets(finalQueryToUse);
    }

    private JPanel getMetadataSummary(List<SearchResult> results){
        Map<String, Long> extentionCounts = results.stream()
                .collect(Collectors.groupingBy(
                        r-> getExtension(r.filePath),
                        Collectors.counting()
                ));

        Map<String, Long> yearCounts = results.stream()
                .collect(Collectors.groupingBy(
                        r -> {
                            try {
                                File f = new File(r.filePath);
                                long mod = f.lastModified();
                                Calendar cal = Calendar.getInstance();
                                cal.setTimeInMillis(mod);
                                return String.valueOf(cal.get(Calendar.YEAR));
                            } catch (Exception ex) {
                                return "Unknown";
                            }
                        },
                        Collectors.counting()));


        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Metadata Summary"));

        if(!extentionCounts.isEmpty()){
            summaryPanel.add(new JLabel("File types:"));
            extentionCounts.forEach(( ext, count) ->{
                summaryPanel.add(new JLabel(" " + ext + ": "+ count));
            });
        }

        if (!yearCounts.isEmpty()) {
            summaryPanel.add(Box.createVerticalStrut(10));
            summaryPanel.add(new JLabel("Modified Years:"));
            yearCounts.forEach((year, count) -> {
                summaryPanel.add(new JLabel("  " + year + ": " + count));
            });
        }

        return summaryPanel;
    }

    private void updateWidgets(String query) {
        widgetPanelContainer.removeAll();
        metadataPanel.removeAll();

        List<JPanel> widgets = widgetManager.getRelevantWidgets(query, currentResults);
        for (JPanel panel : widgets) {
            widgetPanelContainer.add(panel);
        }

        JPanel summary = getMetadataSummary(currentResults);
        metadataPanel.add(summary);

        widgetPanelContainer.revalidate();
        widgetPanelContainer.repaint();

        metadataPanel.revalidate();
        metadataPanel.repaint();
    }


    private void handleShowAll(ActionEvent e) {
        currentResults = retriever.search("");
        updateResultsList();
    }

    private void handleLoadPath(ActionEvent e) {
        String path = insertPathField.getText().trim();
        if (path.isEmpty()) return;

        loadedFiles = fileCrawler.getTextFiles(path);
        listModel.clear();

        for (File file : loadedFiles) {
            listModel.addElement(file.getAbsolutePath());
        }

        if (loadedFiles.isEmpty()) {
            snippetArea.setText("No .txt files found in this directory.");
        } else {
            snippetArea.setText("Select a file to preview. Click 'Save All to DB' to insert all into database.");
        }
    }

    private void handleBatchSave(ActionEvent e) {
        if (loadedFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No files loaded to insert.");
            return;
        }

        int success = 0;
        List<File> successfullyIndexedFiles = new ArrayList<>();
        List<String> report = new ArrayList<>();
        report.add("Indexing Report\n====================");

        for (File file : loadedFiles) {
            try {
                String content = Files.readString(file.toPath());
                if (content.isBlank()) {
                    report.add("Skipped: " + file.getAbsolutePath() + " (Empty content)");
                    continue;
                }

                boolean inserted = dbWriter.insert(
                        file.getAbsolutePath(),
                        file.getName(),
                        content,
                        getExtension(file.getName()),
                        new Timestamp(file.lastModified()),
                        file.length()
                );

                if (inserted) {
                    success++;
                    successfullyIndexedFiles.add(file);
                    report.add("Inserted: " + file.getAbsolutePath());
                } else {
                    report.add("Failed: " + file.getAbsolutePath() + " (DB insert error)");
                }

            } catch (Exception ex) {
                report.add("Error: " + file.getAbsolutePath() + " (" + ex.getMessage() + ")");
            }
        }

        report.add("====================");
        report.add("Total files processed: " + loadedFiles.size());
        report.add("Successfully inserted: " + success);
        report.add("Skipped or failed: " + (loadedFiles.size() - success));

        boolean hasProblems = report.stream().anyMatch(line ->
                line.startsWith("Skipped:") || line.startsWith("Failed:") || line.startsWith("Error:"));

        if (hasProblems) {
            try {
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HHmm").format(new java.util.Date());
                String filename = "index_report_" + timestamp + ".csv";
                File reportFile = new File(filename);
                Files.write(reportFile.toPath(), report);
                JOptionPane.showMessageDialog(this, success + " files inserted.\nReport saved to: " + reportFile.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Failed to write report: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        }

        if (!successfullyIndexedFiles.isEmpty()) {
            dbWriter.generateCSVReport(successfullyIndexedFiles);
        }
    }

    private void updateResultsList() {
        listModel.clear();
        for (SearchResult result : currentResults) {
            listModel.addElement(result.filePath);
        }

        if (currentResults.isEmpty()) {
            snippetArea.setText("No results found.");
        } else {
            snippetArea.setText("");
        }
    }

    private void showSnippet() {
        int index = resultsList.getSelectedIndex();
        if (index >= 0 && index < currentResults.size()) {
            String content = currentResults.get(index).content;
            snippetArea.setText(content);
        }
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i > 0) ? filename.substring(i).toLowerCase() : "";
    }

    private void updateSuggestionsPopup() {
        suggestionPopup.removeAll(); //clear old suggestions

        List<String> suggestions = historyTracker.suggestQueries();
        if (suggestions.isEmpty()) return;

        for (String suggestion : suggestions) {
            JMenuItem item = new JMenuItem(suggestion);
            item.addActionListener(e -> searchField.setText(suggestion)); //when clicked it sets the text
            suggestionPopup.add(item);
        }

        suggestionPopup.show(searchField, 0, searchField.getHeight()); //show dropdown just below
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SearchGUI().setVisible(true);
        });
    }
}
