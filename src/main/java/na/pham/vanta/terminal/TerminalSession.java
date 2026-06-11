package na.pham.vanta.terminal;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class TerminalSession implements AutoCloseable {
    private final String name;
    private final PtyProcess process;
    private final OutputStream shellInput; // zsh -> java
    private final InputStream shellOutput; // java -> zsh
    private final Consumer<String> onOutput;

    private TerminalSession(String name, PtyProcess process, Consumer<String> onOutput) {
        this.name = Objects.requireNonNull(name, "name");
        this.process = Objects.requireNonNull(process, "process");
        this.shellInput = process.getOutputStream();
        this.shellOutput = process.getInputStream();
        this.onOutput = Objects.requireNonNull(onOutput, "onOutput");
    }

    public static TerminalSession spawnShell(String name, Consumer<String> onOutput) throws IOException {
        return spawnShell(name, Path.of(System.getProperty("user.home")), onOutput);
    }

    public static TerminalSession spawnShell(String name, Path workingDirectory, Consumer<String> onOutput) throws IOException {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(onOutput, "onOutput");
        return new TerminalSession(name, spawnShellProcess(workingDirectory), onOutput);
    }

    public static PtyProcess spawnShellProcess(Path workingDirectory) throws IOException {
        Objects.requireNonNull(workingDirectory, "workingDirectory");

        String shell = System.getenv().getOrDefault("SHELL", "/bin/zsh");
        Map<String, String> environment = new HashMap<>(System.getenv());
        environment.putIfAbsent("TERM", "xterm-256color");
        environment.putIfAbsent("TERMINAL_EMULATOR", "JetBrains-JediTerm");

        return new PtyProcessBuilder()
                .setCommand(new String[] { shell, "-l" })
                .setEnvironment(Map.copyOf(environment))
                .setDirectory(workingDirectory.toString())
                .setRedirectErrorStream(true)
                .setInitialColumns(120)
                .setInitialRows(30)
                .start();
    }

    public void resize(int columns, int rows) {
        process.setWinSize(new WinSize(columns, rows));
    }

    public void write(String in) throws IOException {
        shellInput.write(in.getBytes(UTF_8));
        shellInput.flush();
    }

    public void read() throws IOException {
        byte[] buffer = new byte[4096];
        while (process.isAlive()) {
            int read = shellOutput.read(buffer);
            if (read > 0) {
                onOutput.accept(new String(buffer, 0, read, UTF_8));
            }
        }
    }

    @Override
    public void close() throws IOException {
        shellInput.close();
        process.destroy();
    }
}
