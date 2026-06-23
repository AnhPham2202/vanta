package na.pham.vanta.ui.component;

import com.techsenger.jeditermfx.core.TtyConnector;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import com.techsenger.jeditermfx.ui.settings.SettingsProvider;

public final class TerminalWorkspace extends VBox implements AutoCloseable {
    private static final int MAX_LIVE_VIEWS = Integer.getInteger("vanta.maxLiveViews", 1);

    private final Supplier<SettingsProvider> settingsFactory;
    private final Supplier<TtyConnector> connectorFactory;
    private final HBox tabStrip = new HBox();
    private final HBox tabList = new HBox();
    private final ScrollPane tabScroller = new ScrollPane(tabList);
    private final Button newTabButton = new Button("+");
    private final StackPane terminalHost = new StackPane();
    private final List<TerminalTab> tabs = new ArrayList<>();
    private TerminalTab activeTab;
    private int nextTabNumber = 1;
    private long baselineHeapUsed = -1;
    private long lastHeapUsed = -1;
    private long selectionSequence;

    public TerminalWorkspace(Supplier<SettingsProvider> settingsFactory, Supplier<TtyConnector> connectorFactory) {
        this.settingsFactory = settingsFactory;
        this.connectorFactory = connectorFactory;

        tabStrip.getStyleClass().add("terminal-tab-strip");
        tabList.getStyleClass().add("terminal-tab-list");
        tabScroller.getStyleClass().add("terminal-tab-scroller");
        tabScroller.setFitToHeight(true);
        tabScroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tabScroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tabScroller.setPannable(true);
        HBox.setHgrow(tabScroller, Priority.ALWAYS);

        terminalHost.getStyleClass().add("terminal-host");
        VBox.setVgrow(terminalHost, Priority.ALWAYS);

        newTabButton.getStyleClass().add("terminal-new-tab-button");
        newTabButton.setFocusTraversable(false);
        newTabButton.setOnAction(event -> createTab());

        tabStrip.getChildren().addAll(tabScroller, newTabButton);
        getChildren().addAll(tabStrip, terminalHost);

        createTab();
    }

    public void startTerminals() {
        tabs.forEach(this::startTab);
        focusActiveTab();
    }

    @Override
    public void close() {
        for (TerminalTab tab : tabs) {
            if (tab.hasSession() && tab.isStarted()) {
                tab.session().close();
            }
        }
    }

    private void createTab() {
        TerminalTab tab = createTerminalTab("local " + nextTabNumber++);
        tabs.add(tab);
        tabList.getChildren().add(tab.node());
        startTab(tab);
        selectTab(tab);
        Platform.runLater(() -> tabScroller.setHvalue(1.0));
        Platform.runLater(() -> logResourceUsage("after-create-tab"));
    }

    private TerminalTab createTerminalTab(String title) {
        TerminalTab tab = new TerminalTab(createSession(), title);
        tab.selectButton().setOnAction(event -> selectTab(tab));
        tab.closeButton().setOnAction(event -> closeTab(tab));
        return tab;
    }

    private void closeTab(TerminalTab tab) {
        int closedTabIndex = tabs.indexOf(tab);
        boolean closingActiveTab = tab == activeTab;

        tabs.remove(tab);
        tabList.getChildren().remove(tab.node());
        tab.closeSession(terminalHost);

        if (tabs.isEmpty()) {
            activeTab = null;
            createTab();
            return;
        }

        if (closingActiveTab) {
            int nextTabIndex = Math.min(closedTabIndex, tabs.size() - 1);
            selectTab(tabs.get(nextTabIndex));
        }
        Platform.runLater(() -> logResourceUsage("after-close-tab"));
    }

    private void selectTab(TerminalTab tab) {
        if (activeTab == tab) {
            ensureSession(tab);
            tab.attachView(terminalHost);
            focusActiveTab();
            return;
        }

        if (activeTab != null) {
            activeTab.detachView(terminalHost);
        }

        activeTab = tab;
        ensureSession(tab);
        tab.markSelected(++selectionSequence);
        for (TerminalTab terminalTab : tabs) {
            terminalTab.setSelected(terminalTab == tab);
        }
        tab.attachView(terminalHost);
        focusActiveTab();
        evictInactiveViews();
        Platform.runLater(() -> logResourceUsage("after-select-tab"));
    }

    private void startTab(TerminalTab tab) {
        ensureSession(tab);
        if (tab.isStarted()) {
            return;
        }
        tab.session().start();
        tab.markStarted();
    }

    private void ensureSession(TerminalTab tab) {
        if (tab.hasSession()) {
            return;
        }
        tab.installSession(createSession());
        startTab(tab);
    }

    private PersistentTerminalSession createSession() {
        return new PersistentTerminalSession(settingsFactory.get(), connectorFactory.get(), 120, 30);
    }

    private void evictInactiveViews() {
        int liveViews = countLiveViews();
        while (liveViews > MAX_LIVE_VIEWS) {
            TerminalTab evictable = tabs.stream()
                    .filter(tab -> tab != activeTab)
                    .filter(tab -> tab.hasSession() && tab.session().hasView())
                    .min(Comparator.comparingLong(TerminalTab::lastSelectedSequence))
                    .orElse(null);
            if (evictable == null) {
                return;
            }
            System.out.println("detaching inactive terminal view");
            evictable.detachView(terminalHost);
            liveViews--;
        }
    }

    private int countLiveViews() {
        int count = 0;
        for (TerminalTab tab : tabs) {
            if (tab.hasSession() && tab.session().hasView()) {
                count++;
            }
        }
        return count;
    }

    private void focusActiveTab() {
        if (activeTab != null && activeTab.isStarted()) {
            Platform.runLater(() -> {
                if (activeTab.hasSession() && activeTab.session().focusableNode() != null) {
                    activeTab.session().focusableNode().requestFocus();
                }
            });
        }
    }

    private void logResourceUsage(String reason) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        Runtime runtime = Runtime.getRuntime();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        long heapUsed = heap.getUsed();
        if (baselineHeapUsed < 0) {
            baselineHeapUsed = heapUsed;
        }
        long heapDelta = heapUsed - baselineHeapUsed;
        long heapStep = lastHeapUsed < 0 ? 0 : heapUsed - lastHeapUsed;
        lastHeapUsed = heapUsed;

        long totalCanvasPixels = 0;
        int nonZeroCanvases = 0;
        int cachedCanvases = 0;
        int startedTabs = 0;
        int liveViews = 0;
        for (TerminalTab tab : tabs) {
            if (!tab.hasSession()) {
                continue;
            }
            if (tab.session().hasView()) {
                liveViews++;
            }
            if (tab.isStarted()) {
                startedTabs++;
            }
            Canvas canvas = tab.session().canvas();
            if (canvas == null) {
                continue;
            }
            long pixels = Math.round(canvas.getWidth()) * Math.round(canvas.getHeight());
            totalCanvasPixels += pixels;
            if (pixels > 0) {
                nonZeroCanvases++;
            }
            if (canvas.isCache()) {
                cachedCanvases++;
            }
        }
        long estimatedCanvasBytes = totalCanvasPixels * 4;
        long heapDeltaPerTab = tabs.isEmpty() ? 0 : heapDelta / tabs.size();
        long estimatedCanvasBytesPerTab = tabs.isEmpty() ? 0 : estimatedCanvasBytes / tabs.size();

        System.out.printf(
                "resource[%s]: tabs=%d liveViews=%d maxLiveViews=%d started=%d hostChildren=%d canvases=%d nonZeroCanvases=%d cachedCanvases=%d canvasPixels=%.2fM estimatedCanvas=%s estimatedCanvasPerTab=%s heap=%s/%s heapDelta=%s heapDeltaPerTab=%s heapStep=%s nonHeap=%s committedHeap=%s threads=%d processors=%d%n",
                reason,
                tabs.size(),
                liveViews,
                MAX_LIVE_VIEWS,
                startedTabs,
                terminalHost.getChildren().size(),
                liveViews,
                nonZeroCanvases,
                cachedCanvases,
                totalCanvasPixels / 1_000_000.0,
                formatBytes(estimatedCanvasBytes),
                formatBytes(estimatedCanvasBytesPerTab),
                formatBytes(heapUsed),
                formatBytes(heap.getMax()),
                formatSignedBytes(heapDelta),
                formatSignedBytes(heapDeltaPerTab),
                formatSignedBytes(heapStep),
                formatBytes(nonHeap.getUsed()),
                formatBytes(heap.getCommitted()),
                threadBean.getThreadCount(),
                runtime.availableProcessors());
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) {
            return "unknown";
        }
        return String.format("%.1fMB", bytes / 1024.0 / 1024.0);
    }

    private static String formatSignedBytes(long bytes) {
        String sign = bytes >= 0 ? "+" : "-";
        return sign + formatBytes(Math.abs(bytes));
    }
}
