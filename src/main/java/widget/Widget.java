package widget;

import javax.swing.*;

public interface Widget {
    boolean isRelevant(String query);
    JPanel render();  //panel that can be shown in UI
}

