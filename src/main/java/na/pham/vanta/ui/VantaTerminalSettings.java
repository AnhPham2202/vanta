package na.pham.vanta.ui;

import com.techsenger.jeditermfx.core.TerminalColor;
import com.techsenger.jeditermfx.core.TextStyle;
import com.techsenger.jeditermfx.ui.settings.DefaultSettingsProvider;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

final class VantaTerminalSettings extends DefaultSettingsProvider {
    private final VantaTerminalPane theme;

    VantaTerminalSettings(VantaTerminalPane theme) {
        this.theme = theme;
    }

    @Override
    public Font getTerminalFont() {
        return Font.font("JetBrains Mono", getTerminalFontSize());
    }

    @Override
    public float getTerminalFontSize() {
        return 13.0f;
    }

    @Override
    public TerminalColor getDefaultForeground() {
        return terminalColor(theme::terminalForeground);
    }

    @Override
    public TerminalColor getDefaultBackground() {
        return terminalColor(theme::terminalBackground);
    }

    @SuppressWarnings("deprecation")
    @Override
    public TextStyle getDefaultStyle() {
        return new TextStyle(getDefaultForeground(), getDefaultBackground());
    }

    @Override
    public TextStyle getSelectionColor() {
        return new TextStyle(
                terminalColor(theme::terminalSelectionForeground),
                terminalColor(theme::terminalSelectionBackground));
    }

    @Override
    public boolean useInverseSelectionColor() {
        return false;
    }

    @Override
    public int getBufferMaxLinesCount() {
        return 10_000;
    }

    @Override
    public boolean copyOnSelect() {
        return true;
    }

    @Override
    public boolean scrollToBottomOnTyping() {
        return true;
    }

    private static TerminalColor terminalColor(ColorSupplier colorSupplier) {
        return new TerminalColor(() -> toTerminalColor(colorSupplier.get()));
    }

    private static com.techsenger.jeditermfx.core.Color toTerminalColor(Color color) {
        return new com.techsenger.jeditermfx.core.Color(channel(color.getRed()),
                channel(color.getGreen()), channel(color.getBlue()));
    }

    private static int channel(double value) {
        return (int) Math.round(value * 255.0);
    }

    @FunctionalInterface
    private interface ColorSupplier {
        Color get();
    }
}
