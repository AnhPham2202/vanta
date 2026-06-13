package na.pham.vanta.terminal;

import com.techsenger.jeditermfx.core.TtyConnector;
import com.techsenger.jeditermfx.core.util.TermSize;
import com.pty4j.PtyProcess;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class LocalPtyConnector implements TtyConnector {
    private final PtyProcess process;
    private final InputStreamReader output;
    private final OutputStream input;

    public LocalPtyConnector() {
        try {
            this.process = TerminalSession.spawnShellProcess(Path.of(System.getProperty("user.home")));
            this.output = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
            this.input = process.getOutputStream();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to start terminal", e);
        }
    }

    @Override
    public int read(char[] buffer, int offset, int length) throws IOException {
        return output.read(buffer, offset, length);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        input.write(bytes);
        input.flush();
    }

    @Override
    public void write(String string) throws IOException {
        write(string.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isConnected() {
        return process.isAlive();
    }

    @Override
    public void resize(TermSize termSize) {
        process.setWinSize(new com.pty4j.WinSize(termSize.getColumns(), termSize.getRows()));
    }

    @Override
    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }

    @Override
    public boolean ready() throws IOException {
        return output.ready();
    }

    @Override
    public String getName() {
        return "local";
    }

    @Override
    public void close() {
        try {
            input.close();
        } catch (IOException ignored) {
        }
        process.destroy();
    }
}
