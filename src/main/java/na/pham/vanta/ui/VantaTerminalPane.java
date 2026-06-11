package na.pham.vanta.ui;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.PaintConverter;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.List;

final class VantaTerminalPane extends BorderPane {
    private static final Color DEFAULT_FOREGROUND = Color.rgb(207, 215, 227);
    private static final Color DEFAULT_BACKGROUND = Color.rgb(6, 16, 27);
    private static final Color DEFAULT_SELECTION_FOREGROUND = Color.WHITE;
    private static final Color DEFAULT_SELECTION_BACKGROUND = Color.rgb(29, 78, 216);

    private static final CssMetaData<VantaTerminalPane, Paint> TERMINAL_FOREGROUND =
            cssPaint("-vanta-terminal-foreground", pane -> pane.terminalForeground);
    private static final CssMetaData<VantaTerminalPane, Paint> TERMINAL_BACKGROUND =
            cssPaint("-vanta-terminal-background", pane -> pane.terminalBackground);
    private static final CssMetaData<VantaTerminalPane, Paint> TERMINAL_SELECTION_FOREGROUND =
            cssPaint("-vanta-terminal-selection-foreground", pane -> pane.terminalSelectionForeground);
    private static final CssMetaData<VantaTerminalPane, Paint> TERMINAL_SELECTION_BACKGROUND =
            cssPaint("-vanta-terminal-selection-background", pane -> pane.terminalSelectionBackground);
    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA = cssMetaData();

    private final StyleableObjectProperty<Paint> terminalForeground =
            cssPaintProperty("terminalForeground", DEFAULT_FOREGROUND, TERMINAL_FOREGROUND);
    private final StyleableObjectProperty<Paint> terminalBackground =
            cssPaintProperty("terminalBackground", DEFAULT_BACKGROUND, TERMINAL_BACKGROUND);
    private final StyleableObjectProperty<Paint> terminalSelectionForeground =
            cssPaintProperty("terminalSelectionForeground", DEFAULT_SELECTION_FOREGROUND, TERMINAL_SELECTION_FOREGROUND);
    private final StyleableObjectProperty<Paint> terminalSelectionBackground =
            cssPaintProperty("terminalSelectionBackground", DEFAULT_SELECTION_BACKGROUND, TERMINAL_SELECTION_BACKGROUND);

    Color terminalForeground() {
        return color(terminalForeground.get(), DEFAULT_FOREGROUND);
    }

    Color terminalBackground() {
        return color(terminalBackground.get(), DEFAULT_BACKGROUND);
    }

    Color terminalSelectionForeground() {
        return color(terminalSelectionForeground.get(), DEFAULT_SELECTION_FOREGROUND);
    }

    Color terminalSelectionBackground() {
        return color(terminalSelectionBackground.get(), DEFAULT_SELECTION_BACKGROUND);
    }

    private static Color color(Paint paint, Color fallback) {
        return paint instanceof Color color ? color : fallback;
    }

    private StyleableObjectProperty<Paint> cssPaintProperty(
            String name,
            Paint initialValue,
            CssMetaData<VantaTerminalPane, Paint> cssMetaData) {
        return new StyleableObjectProperty<>(initialValue) {
            @Override
            public Object getBean() {
                return VantaTerminalPane.this;
            }

            @Override
            public String getName() {
                return name;
            }

            @Override
            public CssMetaData<? extends Styleable, Paint> getCssMetaData() {
                return cssMetaData;
            }
        };
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_META_DATA;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    @FunctionalInterface
    private interface PaintPropertyAccessor {
        StyleableObjectProperty<Paint> get(VantaTerminalPane pane);
    }

    private static List<CssMetaData<? extends Styleable, ?>> cssMetaData() {
        List<CssMetaData<? extends Styleable, ?>> metadata =
                new ArrayList<>(BorderPane.getClassCssMetaData());
        metadata.add(TERMINAL_FOREGROUND);
        metadata.add(TERMINAL_BACKGROUND);
        metadata.add(TERMINAL_SELECTION_FOREGROUND);
        metadata.add(TERMINAL_SELECTION_BACKGROUND);
        return List.copyOf(metadata);
    }

    private static CssMetaData<VantaTerminalPane, Paint> cssPaint(
            String cssProperty,
            PaintPropertyAccessor propertyAccessor) {
        return new CssMetaData<>(cssProperty, PaintConverter.getInstance()) {
            @Override
            public boolean isSettable(VantaTerminalPane pane) {
                return !propertyAccessor.get(pane).isBound();
            }

            @Override
            public StyleableProperty<Paint> getStyleableProperty(VantaTerminalPane pane) {
                return propertyAccessor.get(pane);
            }
        };
    }
}
