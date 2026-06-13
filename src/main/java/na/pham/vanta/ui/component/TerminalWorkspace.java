package na.pham.vanta.ui.component;

import com.techsenger.jeditermfx.core.TtyConnector;
import com.techsenger.jeditermfx.ui.JediTermFxWidget;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class TerminalWorkspace extends VBox implements AutoCloseable {
    private final Supplier<JediTermFxWidget> terminalFactory;
    private final Supplier<TtyConnector> connectorFactory;
    private final HBox tabStrip = new HBox();
    private final HBox tabList = new HBox();
    private final ScrollPane tabScroller = new ScrollPane(tabList);
    private final Button newTabButton = new Button("+");
    private final StackPane terminalHost = new StackPane();
    private final List<TerminalTab> tabs = new ArrayList<>();
    private TerminalTab activeTab;
    private int nextTabNumber = 1;

    public TerminalWorkspace(Supplier<JediTermFxWidget> terminalFactory, Supplier<TtyConnector> connectorFactory) {
        this.terminalFactory = terminalFactory;
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
            if (tab.isStarted()) {
                tab.terminal().close();
            }
        }
    }

    private void createTab() {
        TerminalTab tab = createTerminalTab("local " + nextTabNumber++);
        tabs.add(tab);
        tabList.getChildren().add(tab.node());
        terminalHost.getChildren().add(tab.terminal().getPane());
        startTab(tab);
        selectTab(tab);
        Platform.runLater(() -> tabScroller.setHvalue(1.0));
    }

    private TerminalTab createTerminalTab(String title) {
        JediTermFxWidget terminal = terminalFactory.get();
        terminal.getTerminalPanel().getPane().getStyleClass().add("terminal-widget");

        TerminalTab tab = new TerminalTab(terminal, title);
        tab.selectButton().setOnAction(event -> selectTab(tab));
        tab.closeButton().setOnAction(event -> closeTab(tab));
        return tab;
    }

    private void closeTab(TerminalTab tab) {
        int closedTabIndex = tabs.indexOf(tab);
        boolean closingActiveTab = tab == activeTab;

        tabs.remove(tab);
        tabList.getChildren().remove(tab.node());
        terminalHost.getChildren().remove(tab.terminal().getPane());
        tab.terminal().close();

        if (tabs.isEmpty()) {
            activeTab = null;
            createTab();
            return;
        }

        if (closingActiveTab) {
            int nextTabIndex = Math.min(closedTabIndex, tabs.size() - 1);
            selectTab(tabs.get(nextTabIndex));
        }
    }

    private void selectTab(TerminalTab tab) {
        activeTab = tab;
        for (TerminalTab terminalTab : tabs) {
            terminalTab.setSelected(terminalTab == tab);
        }
        focusActiveTab();
    }

    private void startTab(TerminalTab tab) {
        if (tab.isStarted()) {
            return;
        }
        tab.terminal().setTtyConnector(connectorFactory.get());
        tab.terminal().start();
        tab.markStarted();
    }

    private void focusActiveTab() {
        if (activeTab != null && activeTab.isStarted()) {
            Platform.runLater(() -> activeTab.terminal().getPreferredFocusableNode().requestFocus());
        }
    }
}
