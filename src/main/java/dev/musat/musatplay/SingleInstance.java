package dev.musat.musatplay;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SingleInstance implements AutoCloseable {
    private final FileChannel channel;
    private final FileLock lock;
    private final ServerSocket server;
    private final Path endpoint;
    private final AtomicBoolean closed = new AtomicBoolean();

    private SingleInstance(FileChannel channel, FileLock lock, ServerSocket server, Path endpoint) {
        this.channel = channel;
        this.lock = lock;
        this.server = server;
        this.endpoint = endpoint;
    }

    public static Optional<SingleInstance> acquire(Path root, Runnable activate) throws IOException {
        Files.createDirectories(root);
        Path endpoint = root.resolve("instance.endpoint");
        FileChannel channel = FileChannel.open(root.resolve("instance.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock lock;
        try { lock = channel.tryLock(); }
        catch (OverlappingFileLockException e) { lock = null; }
        if (lock == null) {
            channel.close();
            for (int attempt = 0; attempt < 15; attempt++) {
                try {
                    var lines = Files.readAllLines(endpoint);
                    try (Socket socket = new Socket()) {
                        socket.connect(new java.net.InetSocketAddress(InetAddress.getLoopbackAddress(), Integer.parseInt(lines.get(0))), 300);
                        new PrintWriter(socket.getOutputStream(), true).println(lines.get(1));
                    }
                    return Optional.empty();
                } catch (IOException | RuntimeException e) {
                    try { Thread.sleep(100); }
                    catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); break; }
                }
            }
            throw new IOException("MusatPlay is already running but its window could not be activated.");
        }
        ServerSocket server = null;
        try {
            server = new ServerSocket(0, 8, InetAddress.getLoopbackAddress());
            byte[] random = new byte[24];
            new SecureRandom().nextBytes(random);
            String secret = HexFormat.of().formatHex(random);
            Files.writeString(endpoint, server.getLocalPort() + "\n" + secret + "\n");
            SingleInstance instance = new SingleInstance(channel, lock, server, endpoint);
            Thread.ofPlatform().daemon().name("musatplay-activation").start(() -> instance.listen(secret, activate));
            return Optional.of(instance);
        } catch (IOException e) {
            if (server != null) server.close();
            lock.release();
            channel.close();
            throw e;
        }
    }

    private void listen(String secret, Runnable activate) {
        while (!closed.get()) {
            try (Socket socket = server.accept()) {
                socket.setSoTimeout(1_000);
                byte[] bytes = socket.getInputStream().readNBytes(secret.length() + 1);
                String value = new String(bytes, java.nio.charset.StandardCharsets.US_ASCII).strip();
                if (secret.equals(value)) activate.run();
            } catch (IOException ignored) {}
        }
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        try { server.close(); } catch (IOException ignored) {}
        try { Files.deleteIfExists(endpoint); } catch (IOException ignored) {}
        try { lock.release(); } catch (IOException ignored) {}
        try { channel.close(); } catch (IOException ignored) {}
    }
}
