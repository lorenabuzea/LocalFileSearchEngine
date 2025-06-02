package widget;

import model.SearchResult;

import javax.swing.*;
import java.util.List;

public interface ContextAwareWidget {
    boolean isRelevant(String query, List<SearchResult> results);
    JPanel render();
}
