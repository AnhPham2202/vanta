module na.pham.vanta {
    requires javafx.controls;
    requires java.management;
    requires com.techsenger.jeditermfx.core;
    requires com.techsenger.jeditermfx.ui;
    requires pty4j;

    exports na.pham.vanta.app;
    exports na.pham.vanta.ui;
    exports na.pham.vanta.terminal;
    exports na.pham.vanta.ssh;
    exports na.pham.vanta.logviewer;
    exports na.pham.vanta.theme;
    exports na.pham.vanta.util;
}
