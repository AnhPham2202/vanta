package na.pham.vanta.ui.component;

import com.techsenger.jeditermfx.ui.JediTermFxWidget;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

final class TerminalTab {
    private final JediTermFxWidget terminal;
    private final HBox tabNode = new HBox();
    private final Button selectButton;
    private final Button closeButton = new Button("x");
    private boolean started;

    TerminalTab(JediTermFxWidget terminal, String title) {
        this.terminal = terminal;
        this.selectButton = new Button(title);

        tabNode.getStyleClass().add("terminal-tab");

        selectButton.getStyleClass().add("terminal-tab-title");
        selectButton.setFocusTraversable(false);
        selectButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(selectButton, Priority.ALWAYS);

        closeButton.getStyleClass().add("terminal-tab-close");
        closeButton.setFocusTraversable(false);

        tabNode.getChildren().addAll(selectButton, closeButton);
    }

    JediTermFxWidget terminal() {
        return terminal;
    }

    Node node() {
        return tabNode;
    }

    Button selectButton() {
        return selectButton;
    }

    Button closeButton() {
        return closeButton;
    }

    boolean isStarted() {
        return started;
    }

    void markStarted() {
        started = true;
    }

    void setSelected(boolean selected) {
        tabNode.getStyleClass().remove("selected");
        if (selected) {
            tabNode.getStyleClass().add("selected");
        }
        terminal.getPane().setVisible(selected);
        terminal.getPane().setManaged(selected);
    }
}
