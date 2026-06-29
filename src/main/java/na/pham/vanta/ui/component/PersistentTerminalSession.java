package na.pham.vanta.ui.component;

import com.techsenger.jeditermfx.core.Color;
import com.techsenger.jeditermfx.core.CursorShape;
import com.techsenger.jeditermfx.core.RequestOrigin;
import com.techsenger.jeditermfx.core.TerminalDisplay;
import com.techsenger.jeditermfx.core.TerminalExecutorServiceManager;
import com.techsenger.jeditermfx.core.TerminalMode;
import com.techsenger.jeditermfx.core.TerminalStarter;
import com.techsenger.jeditermfx.core.TtyBasedArrayDataStream;
import com.techsenger.jeditermfx.core.TtyConnector;
import com.techsenger.jeditermfx.core.emulator.mouse.MouseFormat;
import com.techsenger.jeditermfx.core.emulator.mouse.MouseMode;
import com.techsenger.jeditermfx.core.model.JediTermDebouncerImpl;
import com.techsenger.jeditermfx.core.model.JediTerminal;
import com.techsenger.jeditermfx.core.model.StyleState;
import com.techsenger.jeditermfx.core.model.TerminalSelection;
import com.techsenger.jeditermfx.core.model.TerminalTextBuffer;
import com.techsenger.jeditermfx.core.model.hyperlinks.TextProcessing;
import com.techsenger.jeditermfx.core.typeahead.TerminalTypeAheadManager;
import com.techsenger.jeditermfx.core.typeahead.TypeAheadTerminalModel;
import com.techsenger.jeditermfx.ui.DefaultTerminalExecutorServiceManager;
import com.techsenger.jeditermfx.ui.TerminalPanel;
import com.techsenger.jeditermfx.ui.settings.SettingsProvider;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.Future;

final class PersistentTerminalSession implements AutoCloseable {
    private final SettingsProvider settingsProvider;
    private final TtyConnector connector; // connector btween app and shell like bash, zsh, terminal
    private final StyleState styleState = new StyleState();
    private final TextProcessing textProcessing; // for handling extra text style (like hyperlink)
    private final TerminalTextBuffer textBuffer; // save terminal text state (like cursor, scrollback history, zoom state,...)
    private final DisplayProxy displayProxy = new DisplayProxy(); //  jediterminal and terminalPanel (replace this), same with terminal Panel but custom to not append 1:1 with jedi termianl, so can detach and attach display as we want
    private final JediTerminal terminal;
    private final TerminalExecutorServiceManager executorServiceManager = new DefaultTerminalExecutorServiceManager();
    private final TerminalTypeAheadManager typeAheadManager;
    private final TerminalStarter terminalStarter; // connect all together, manage life cycle, threads, passing data btween connector and terminal
    private Future<?> emulatorFuture;
    private TerminalPanel panel;
    private static final String MODULE_OPEN_OPTIONS = "--add-opens "
            + "com.techsenger.jeditermfx.ui/com.techsenger.jeditermfx.ui=na.pham.vanta "
            + "--add-opens com.techsenger.jeditermfx.core/com.techsenger.jeditermfx.core.model=na.pham.vanta";

    PersistentTerminalSession(SettingsProvider settingsProvider, TtyConnector connector, int columns, int rows) {
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
        this.connector = Objects.requireNonNull(connector, "connector");
        styleState.setDefaultStyle(settingsProvider.getDefaultStyle());
        textProcessing = new TextProcessing(settingsProvider.getHyperlinkColor(),
                settingsProvider.getHyperlinkHighlightingMode());
        textBuffer = new TerminalTextBuffer(columns, rows, styleState,
                settingsProvider.getBufferMaxLinesCount(), textProcessing);
        textProcessing.setTerminalTextBuffer(textBuffer);
        terminal = new JediTerminal(displayProxy, textBuffer, styleState);
        terminal.setModeEnabled(TerminalMode.AltSendsEscape, settingsProvider.altSendsEscape());
        typeAheadManager = new TerminalTypeAheadManager(new NoTypeAheadModel(textBuffer, displayProxy));
        JediTermDebouncerImpl typeAheadDebouncer = new JediTermDebouncerImpl(
                typeAheadManager::debounce,
                TerminalTypeAheadManager.MAX_TERMINAL_DELAY,
                executorServiceManager);
        typeAheadManager.setClearPredictionsDebouncer(typeAheadDebouncer);
        terminalStarter = new TerminalStarter(terminal, connector,
                new TtyBasedArrayDataStream(connector, typeAheadManager::onTerminalStateChanged),
                typeAheadManager,
                executorServiceManager);
    }

    void start() {
        if (emulatorFuture != null) {
            return;
        }
        emulatorFuture = executorServiceManager.getUnboundedExecutorService().submit(terminalStarter::start);
    }

    Pane attachView() {
        if (panel == null) {
            panel = new TerminalPanel(settingsProvider, textBuffer, styleState);
            setTypeAheadManager(panel);
            panel.init();
            panel.addTerminalMouseListener(terminal);
            panel.setCoordAccessor(terminal);
            panel.setTerminalStarter(terminalStarter);
            panel.getCanvas().setCache(false);
            displayProxy.attach(panel);
            panel.repaint();
        }
        return panel.getPane();
    }

    void detachView() {
        if (panel == null) {
            return;
        }
        displayProxy.detach(panel);
        panel.dispose();
        panel = null;
        clearPanelBufferListeners();
    }

    Node focusableNode() {
        return panel == null ? null : panel.getPane();
    }

    Canvas canvas() {
        return panel == null ? null : panel.getCanvas();
    }

    boolean hasView() {
        return panel != null;
    }

    @Override
    public void close() {
        detachView();
        terminalStarter.requestEmulatorStop();
        terminalStarter.close();
        connector.close();
        if (emulatorFuture != null) {
            emulatorFuture.cancel(true);
        }
        executorServiceManager.shutdownWhenAllExecuted();
    }

    private void setTypeAheadManager(TerminalPanel panel) {
        try {
            Method method = TerminalPanel.class.getDeclaredMethod("setTypeAheadManager", TerminalTypeAheadManager.class);
            method.setAccessible(true);
            method.invoke(panel, typeAheadManager);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw new IllegalStateException("Unable to configure TerminalPanel type-ahead manager. "
                    + "Add these VM options to the run configuration: " + MODULE_OPEN_OPTIONS, exception);
        }
    }

    @SuppressWarnings("unchecked")
    private void clearPanelBufferListeners() {
        clearListField("myListeners");
        clearListField("myTypeAheadListeners");
        clearListField("myHistoryBufferListeners");
    }

    private void clearListField(String fieldName) {
        try {
            var field = TerminalTextBuffer.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(textBuffer);
            if (value instanceof java.util.List<?> listeners) {
                listeners.clear();
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            throw new IllegalStateException("Unable to clear TerminalTextBuffer listener field " + fieldName
                    + ". Add these VM options to the run configuration: " + MODULE_OPEN_OPTIONS, exception);
        }
    }

    private static final class NoTypeAheadModel implements TypeAheadTerminalModel {
        private final TerminalTextBuffer textBuffer;
        private final DisplayProxy displayProxy;

        private NoTypeAheadModel(TerminalTextBuffer textBuffer, DisplayProxy displayProxy) {
            this.textBuffer = textBuffer;
            this.displayProxy = displayProxy;
        }

        @Override
        public void insertCharacter(char ch, int index) {
        }

        @Override
        public void removeCharacters(int from, int count) {
        }

        @Override
        public void moveCursor(int index) {
        }

        @Override
        public void forceRedraw() {
        }

        @Override
        public void clearPredictions() {
        }

        @Override
        public void lock() {
            textBuffer.lock();
        }

        @Override
        public void unlock() {
            textBuffer.unlock();
        }

        @Override
        public boolean isUsingAlternateBuffer() {
            return textBuffer.isUsingAlternateBuffer();
        }

        @Override
        public LineWithCursorX getCurrentLineWithCursor() {
            return new LineWithCursorX(new StringBuffer(), displayProxy.cursorX());
        }

        @Override
        public int getTerminalWidth() {
            return textBuffer.getWidth();
        }

        @Override
        public boolean isTypeAheadEnabled() {
            return false;
        }

        @Override
        public long getLatencyThreshold() {
            return Long.MAX_VALUE;
        }

        @Override
        public ShellType getShellType() {
            return ShellType.Unknown;
        }
    }

    private static final class DisplayProxy implements TerminalDisplay {
        private TerminalPanel panel;
        private String windowTitle = "Terminal";
        private boolean cursorVisible = true;
        private int cursorX;
        private int cursorY = 1;
        private CursorShape cursorShape;
        private MouseMode mouseMode = MouseMode.MOUSE_REPORTING_NONE;
        private MouseFormat mouseFormat = MouseFormat.MOUSE_FORMAT_XTERM;
        private boolean bracketedPasteMode;

        void attach(TerminalPanel panel) {
            this.panel = panel;
            panel.setWindowTitle(windowTitle);
            panel.setCursor(cursorX, cursorY);
            panel.setCursorShape(cursorShape);
            panel.setCursorVisible(cursorVisible);
            panel.terminalMouseModeSet(mouseMode);
            panel.setMouseFormat(mouseFormat);
            panel.setBracketedPasteMode(bracketedPasteMode);
        }

        void detach(TerminalPanel panel) {
            if (this.panel == panel) {
                this.panel = null;
            }
        }

        int cursorX() {
            return cursorX;
        }

        @Override
        public void setCursor(int x, int y) {
            cursorX = x;
            cursorY = y;
            if (panel != null) {
                panel.setCursor(x, y);
            }
        }

        @Override
        public void setCursorShape(CursorShape cursorShape) {
            this.cursorShape = cursorShape;
            if (panel != null) {
                panel.setCursorShape(cursorShape);
            }
        }

        @Override
        public void beep() {
            if (panel != null) {
                panel.beep();
            }
        }

        @Override
        public void onResize(com.techsenger.jeditermfx.core.util.TermSize newTermSize, RequestOrigin origin) {
            if (panel != null) {
                panel.onResize(newTermSize, origin);
            }
        }

        @Override
        public void scrollArea(int scrollRegionTop, int scrollRegionSize, int dy) {
            if (panel != null) {
                panel.scrollArea(scrollRegionTop, scrollRegionSize, dy);
            }
        }

        @Override
        public void setCursorVisible(boolean isCursorVisible) {
            cursorVisible = isCursorVisible;
            if (panel != null) {
                panel.setCursorVisible(isCursorVisible);
            }
        }

        @Override
        public void useAlternateScreenBuffer(boolean useAlternateScreenBuffer) {
            if (panel != null) {
                panel.useAlternateScreenBuffer(useAlternateScreenBuffer);
            }
        }

        @Override
        public String getWindowTitle() {
            return windowTitle;
        }

        @Override
        public void setWindowTitle(String windowTitle) {
            this.windowTitle = windowTitle;
            if (panel != null) {
                panel.setWindowTitle(windowTitle);
            }
        }

        @Override
        public TerminalSelection getSelection() {
            return panel == null ? null : panel.getSelection();
        }

        @Override
        public void terminalMouseModeSet(MouseMode mouseMode) {
            this.mouseMode = mouseMode;
            if (panel != null) {
                panel.terminalMouseModeSet(mouseMode);
            }
        }

        @Override
        public void setMouseFormat(MouseFormat mouseFormat) {
            this.mouseFormat = mouseFormat;
            if (panel != null) {
                panel.setMouseFormat(mouseFormat);
            }
        }

        @Override
        public boolean ambiguousCharsAreDoubleWidth() {
            return panel != null && panel.ambiguousCharsAreDoubleWidth();
        }

        @Override
        public void setBracketedPasteMode(boolean bracketedPasteModeEnabled) {
            bracketedPasteMode = bracketedPasteModeEnabled;
            if (panel != null) {
                panel.setBracketedPasteMode(bracketedPasteModeEnabled);
            }
        }

        @Override
        public Color getWindowForeground() {
            return panel == null ? null : panel.getWindowForeground();
        }

        @Override
        public Color getWindowBackground() {
            return panel == null ? null : panel.getWindowBackground();
        }
    }
}
