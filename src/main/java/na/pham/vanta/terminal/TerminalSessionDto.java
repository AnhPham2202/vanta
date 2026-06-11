package na.pham.vanta.terminal;

import java.util.List;

public record TerminalSessionDto(String name, List<String> lines) {
    public static TerminalSessionDto demo() {
        return new TerminalSessionDto("gateway-01", List.of(
                "$ ssh na@gateway-01",
                "Welcome to Ubuntu 24.04 LTS",
                "Last login: Sat Jun  6 10:42:13 2026 from 10.0.0.21",
                "",
                "$ cd /srv/vanta",
                "$ git status --short",
                " M src/main/java/na/pham/vanta/app/VantaApplication.java",
                "?? src/main/java/na/pham/vanta/ui/",
                "",
                "$ tail -f logs/app.log",
                "10:43:01 INFO  api.health - probe ok",
                "10:43:04 INFO  ssh.session - channel opened gateway-01",
                "10:43:05 WARN  logviewer.stream - backpressure threshold 72%",
                "10:43:08 INFO  terminal.renderer - frame committed",
                "",
                "$ "));
    }
}
