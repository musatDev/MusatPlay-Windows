package dev.musat.musatplay;

import com.sun.jna.Platform;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.*;
import javax.imageio.ImageIO;
import javax.swing.*;




public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) throws Exception {
        if (args.length >= 1 && args[0].equals("--check-updates")) {
            var update = new UpdateService().check();
            String result = update.map(value -> "Update available: " + value.version()).orElse("No newer release is available.");
            if (args.length >= 2) java.nio.file.Files.writeString(Path.of(args[1]), result);
            System.out.println(result);
            return;
        }
        if (args.length >= 2 && args[0].equals("--render-menu-preview")) {
            AppTheme.renderMenu(Path.of(args[1]));
            return;
        }
        if (args.length >= 2 && args[0].equals("--webview2-smoke")) {
            WebViewSmoke.run(Path.of(args[1]));
            return;
        }
        if (args.length >= 2 && args[0].equals("--write-icon")) {
            AppImages.writeWindowsIcon(Path.of(args[1]));
            return;
        }
        if (args.length >= 2 && args[0].equals("--render-preview")) {
            renderPreview(Path.of(args[1]), Arrays.asList(args).contains("--dark"));
            return;
        }
        if (!Platform.isWindows() && !Arrays.asList(args).contains("--allow-non-windows")) {
            System.err.println("This edition targets Windows. Use --allow-non-windows for development only.");
            return;
        }
        AppPaths paths = AppPaths.create();
        AtomicReference<MainWindow> window = new AtomicReference<>();
        var acquired = SingleInstance.acquire(paths.root(), () -> SwingUtilities.invokeLater(() -> {
            if (window.get() != null) window.get().show();
        }));
        if (acquired.isEmpty()) return;
        SingleInstance instance = acquired.get();
        Runtime.getRuntime().addShutdownHook(new Thread(instance::close, "musatplay-shutdown"));
        FileHandler log = new FileHandler(paths.root().resolve("application-%g.log").toString(), 1_048_576, 2, true);
        log.setFormatter(new SimpleFormatter());
        Logger.getLogger("").addHandler(log);
        AppTheme.install();
        JFrame loading = new JFrame("MusatPlay");
        JLabel progress = new JLabel(Language.text("Preparing browser engine…"), SwingConstants.CENTER);
        SwingUtilities.invokeAndWait(() -> {
            loading.setIconImage(AppImages.icon());
            loading.add(progress);
            loading.setSize(360, 120);
            loading.setLocationRelativeTo(null);
            loading.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            loading.setVisible(true);
        });
        try {
            if (Platform.isWindows()) {
                com.sun.jna.platform.win32.Kernel32.INSTANCE.SetEnvironmentVariable("WEBVIEW2_USER_DATA_FOLDER", paths.profile().toString());
                com.sun.jna.platform.win32.Kernel32.INSTANCE.SetEnvironmentVariable("WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS", "--lang=" + Language.currentTag());
            }
            JPopupMenu.setDefaultLightWeightPopupEnabled(false);
            ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
            SwingUtilities.invokeAndWait(() -> {
                try { window.set(new MainWindow()); loading.dispose(); }
                catch (Exception | LinkageError e) { failure(loading, paths, e); instance.close(); System.exit(1); }
            });
        } catch (Exception | LinkageError e) {
            SwingUtilities.invokeAndWait(() -> failure(loading, paths, e));
            instance.close();
            System.exit(1);
        }
    }

    private static void failure(JFrame loading, AppPaths paths, Throwable error) {
        Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, "Startup failed", error);
        loading.dispose();
        JOptionPane.showMessageDialog(null, Language.text("MusatPlay could not start.\nLogs: ") + paths.root(), "MusatPlay", JOptionPane.ERROR_MESSAGE);
    }

    private static void renderPreview(Path path, boolean dark) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            NotificationPanel panel = new NotificationPanel(dark, action -> {});
            panel.setState(new MediaState("preview", "MusatPlay", "YouTube Music", null, true, true, MediaState.Repeat.ONE));
            panel.setArtwork(AppImages.icon());
            panel.setSize(NotificationPanel.WIDTH, NotificationPanel.HEIGHT);
            BufferedImage image = new BufferedImage(960, 348, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.scale(3, 3);
            panel.paint(g);
            g.dispose();
            try { ImageIO.write(image, "png", path.toFile()); }
            catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        });
    }
}
