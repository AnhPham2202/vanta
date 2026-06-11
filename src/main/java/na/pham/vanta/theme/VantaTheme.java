package na.pham.vanta.theme;

import javafx.scene.Scene;

import java.util.Objects;

public final class VantaTheme {
    private VantaTheme() {
    }

    public static void apply(Scene scene) {
        String stylesheet = Objects.requireNonNull(
                VantaTheme.class.getResource("/na/pham/vanta/theme/vanta-dark.css"),
                "Missing /na/pham/vanta/theme/vanta-dark.css").toExternalForm();
        scene.getStylesheets().add(stylesheet);
    }
}
