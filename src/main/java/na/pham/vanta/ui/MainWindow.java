package na.pham.vanta.ui;

import com.techsenger.jeditermfx.core.TtyConnector;
import com.techsenger.jeditermfx.core.util.TermSize;
import com.techsenger.jeditermfx.ui.JediTermFxWidget;
import com.pty4j.PtyProcess;
import javafx.application.Platform;
import javafx.scene.Parent;
import na.pham.vanta.terminal.TerminalSession;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class MainWindow implements AutoCloseable {
    private final VantaTerminalPane pane = new VantaTerminalPane();
    private final JediTermFxWidget terminal;
    private boolean terminalStarted;

    public MainWindow() {
        pane.getStyleClass().add("terminal-pane");

        terminal = new JediTermFxWidget(120, 30, new VantaTerminalSettings(pane));
        terminal.getTerminalPanel().getPane().getStyleClass().add("terminal-widget");

        pane.setCenter(terminal.getPane());
    }

    public Parent pane() {
        return pane;
    }

    public void startTerminal() {
        pane.applyCss();
        terminal.setTtyConnector(new LocalPtyConnector());
        terminal.start();
        terminalStarted = true;
        Platform.runLater(() -> terminal.getPreferredFocusableNode().requestFocus());
    }

    @Override
    public void close() {
        if (terminalStarted) {
            terminal.close();
        }
    }

    private static final class LocalPtyConnector implements TtyConnector {
        private final PtyProcess process;
        private final InputStreamReader output;
        private final OutputStream input;

        private LocalPtyConnector() {
            try {
                this.process = TerminalSession.spawnShellProcess(Path.of(System.getProperty("user.home")));
                this.output = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
                this.input = process.getOutputStream();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to start terminal", e);
            }
        }

        @Override
        public int read(char[] buffer, int offset, int length) throws IOException {
            return output.read(buffer, offset, length);
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            input.write(bytes);
            input.flush();
        }

        @Override
        public void write(String string) throws IOException {
            write(string.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean isConnected() {
            return process.isAlive();
        }

        @Override
        public void resize(TermSize termSize) {
            process.setWinSize(new com.pty4j.WinSize(termSize.getColumns(), termSize.getRows()));
        }

        @Override
        public int waitFor() throws InterruptedException {
            return process.waitFor();
        }

        @Override
        public boolean ready() throws IOException {
            return output.ready();
        }

        @Override
        public String getName() {
            return "local";
        }

        @Override
        public void close() {
            try {
                input.close();
            } catch (IOException ignored) {
            }
            process.destroy();
        }
    }
}
