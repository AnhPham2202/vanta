package na.pham.vanta.ui;

import com.techsenger.jeditermfx.ui.JediTermFxWidget;
import javafx.scene.Parent;
import na.pham.vanta.terminal.LocalPtyConnector;
import na.pham.vanta.ui.component.TerminalWorkspace;

public final class MainWindow implements AutoCloseable {
    private final VantaTerminalPane pane = new VantaTerminalPane();
    private final TerminalWorkspace terminalWorkspace = new TerminalWorkspace(this::createTerminal, LocalPtyConnector::new);

    public MainWindow() {
        pane.getStyleClass().add("terminal-pane");
        pane.setCenter(terminalWorkspace);
    }

    public Parent pane() {
        return pane;
    }

    public void startTerminal() {
        pane.applyCss();
        terminalWorkspace.startTerminals();
    }

    @Override
    public void close() {
        terminalWorkspace.close();
    }

    private JediTermFxWidget createTerminal() {
        return new JediTermFxWidget(120, 30, new VantaTerminalSettings(pane));
    }
}
