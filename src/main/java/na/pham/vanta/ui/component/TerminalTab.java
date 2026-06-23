package na.pham.vanta.ui.component;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

final class TerminalTab {
    private final String title;
    private final HBox tabNode = new HBox();
    private final Button selectButton;
    private final Button closeButton = new Button("x");
    private PersistentTerminalSession session;
    private boolean started;
    private long lastSelectedSequence;

    TerminalTab(PersistentTerminalSession session, String title) {
        this.session = session;
        this.title = title;
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

    PersistentTerminalSession session() {
        return session;
    }

    boolean hasSession() {
        return session != null;
    }

    void installSession(PersistentTerminalSession session) {
        this.session = session;
        this.started = false;
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

    void markSelected(long selectionSequence) {
        lastSelectedSequence = selectionSequence;
    }

    long lastSelectedSequence() {
        return lastSelectedSequence;
    }

    void setSelected(boolean selected) {
        tabNode.getStyleClass().remove("selected");
        if (selected) {
            tabNode.getStyleClass().add("selected");
        }
    }

    void attachView(Pane host) {
        if (session == null) {
            return;
        }
        Pane pane = session.attachView();
        pane.setVisible(true);
        pane.setManaged(true);
        host.getChildren().setAll(pane);
        logViewState("attach", host);
        Platform.runLater(() -> logViewState("attach-after-pulse", host));
    }

    void detachView(Pane host) {
        if (session == null) {
            return;
        }
        Pane pane = session.attachView();
        host.getChildren().remove(pane);
        pane.setVisible(false);
        pane.setManaged(false);
        session.detachView();
        logViewState("detach", host);
        Platform.runLater(() -> logViewState("detach-after-pulse", host));
    }

    void closeSession(Pane host) {
        if (session == null) {
            return;
        }
        session.close();
        session = null;
        started = false;
    }

    private void logViewState(String action, Pane host) {
        if (session == null) {
            System.out.printf("terminal-tab[%s] %s: unloaded hostChildren=%d%n",
                    title,
                    action,
                    host.getChildren().size());
            return;
        }
        Canvas canvas = session.canvas();
        System.out.printf(
                "terminal-tab[%s] %s: view=%s hostChildren=%d canvas=%s cache=%s%n",
                title,
                action,
                session.hasView() ? "attached" : "detached",
                host.getChildren().size(),
                canvas == null ? "none" : String.format("%.1fx%.1f", canvas.getWidth(), canvas.getHeight()),
                canvas != null && canvas.isCache());
    }
}
