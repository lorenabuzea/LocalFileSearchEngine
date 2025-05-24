package widget;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WidgetManager {
    private List<Widget> widgets;

    public WidgetManager() {
        widgets = new ArrayList<>();
        widgets.add(new CalculatorWidget());
        // Add other widgets here
    }

    public List<JPanel> getRelevantWidgets(String query) {
        return widgets.stream()
                .filter(widget -> widget.isRelevant(query))
                .map(Widget::render)
                .collect(Collectors.toList());
    }
}
