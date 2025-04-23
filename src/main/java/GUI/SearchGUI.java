package GUI;
import model.SearchResult;
import search.QueryProcessor;
import search.ResultRetriever;
import search.SnippetGenerator;
import indexer.DBWriter;
import Crawler.FileCrawler;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;

public class SearchGUI extends JFrame {

    private JTextField searchField, insertPathField;
    private JButton searchButton, showAllButton, loadButton, saveButton;
    private JList<String> resultsList;
    private JTextArea snippetArea;
    private JLabel searchLabel, resultLabel, snippetLabel, insertLabel;

    private DefaultListModel<String> listModel;
    private List<SearchResult> currentResults;
    private List<File> loadedFiles = new ArrayList<>();

    private ResultRetriever retriever;
    private SnippetGenerator snippetGenerator;
    private QueryProcessor queryProcessor;
    private DBWriter dbWriter;
    private FileCrawler fileCrawler;

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
        searchButton = new JButton("🔍 Search");
        showAllButton = new JButton("📋 Show All Files");

        insertPathField = new JTextField(30);
        loadButton = new JButton("📂 Load from Path");
        saveButton = new JButton("💾 Save All to DB");

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
    }

    private void setupSearchEngine() {
        dbWriter = new DBWriter();
        Connection conn = dbWriter.getConnection();

        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Could not connect to database.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        retriever = new ResultRetriever(conn);
        snippetGenerator = new SnippetGenerator();
        queryProcessor = new QueryProcessor();
        fileCrawler = new FileCrawler();
    }

    private void handleSearch(ActionEvent e) {
        String query = searchField.getText().trim();
        if (query.isEmpty()) return;

        String parsed = queryProcessor.prepare(query);
        currentResults = retriever.search(parsed);

        updateResultsList();
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

        try {
            String filename = "index_report_" + System.currentTimeMillis() + ".txt";
            File reportFile = new File(filename);
            Files.write(reportFile.toPath(), report);
            JOptionPane.showMessageDialog(this,  success + " files inserted.\n Report saved to: " + reportFile.getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to write report: " + ex.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
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
        if (index >= 0) {
            String content;
            if (!loadedFiles.isEmpty() && index < loadedFiles.size()) {
                try {
                    content = Files.readString(loadedFiles.get(index).toPath());
                } catch (Exception ex) {
                    content = "Unable to read file.";
                }
            } else {
                content = currentResults.get(index).content;
            }
            snippetArea.setText(content);
        }
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i > 0) ? filename.substring(i).toLowerCase() : "";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SearchGUI().setVisible(true);
        });
    }
}
