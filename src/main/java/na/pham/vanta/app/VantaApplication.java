package na.pham.vanta.app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import na.pham.vanta.theme.VantaTheme;
import na.pham.vanta.ui.MainWindow;

public final class VantaApplication extends Application {
    private MainWindow mainWindow;

    @Override
    public void start(Stage stage) {
        mainWindow = new MainWindow();
        Scene scene = new Scene(mainWindow.pane(), 1180, 760);
        VantaTheme.apply(scene);
        mainWindow.startTerminal();

        stage.setTitle("Vanta");
        stage.setMinWidth(960);
        stage.setMinHeight(620);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (mainWindow != null) {
            mainWindow.close();
        }
    }
}
