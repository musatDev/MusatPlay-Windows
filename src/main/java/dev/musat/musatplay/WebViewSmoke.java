package dev.musat.musatplay;

import ca.weblite.webview.swing.WebViewComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;


final class WebViewSmoke {
    static void run(Path output) throws Exception {
        WebViewPlatform.initialize();
        Path profile = output.toAbsolutePath().getParent().resolve("smoke-profile");
        Files.createDirectories(profile);
        com.sun.jna.platform.win32.Kernel32.INSTANCE.SetEnvironmentVariable("WEBVIEW2_USER_DATA_FOLDER", profile.toString());
        SwingUtilities.invokeAndWait(() -> {
            var browser = WebViewComponent.create(WebViewComponent.Mode.HEAVYWEIGHT);
            browser.setPasswordManagerEnabled(false);
            browser.setUrl("about:blank");
            JFrame frame = new JFrame("MusatPlay WebView2 check");
            frame.add(browser);
            frame.setSize(400, 250);
            frame.setLocation(-3000, -3000);
            frame.setVisible(true);
            final boolean[] pending = {false};
            Timer poll = new Timer(250, null);
            poll.addActionListener(event -> {
                if (pending[0]) return;
                pending[0] = true;
                browser.evalAsync("return {engine: 'WebView2', userAgent: navigator.userAgent, sum: 2 + 3}")
                    .whenComplete((json, error) -> SwingUtilities.invokeLater(() -> {
                        pending[0] = false;
                        if (error != null) return;
                        try {
                            var result = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
                            result.addProperty("appVersion", UpdateService.currentVersion());
                            result.add("runtimeLanguages", new com.google.gson.Gson().toJsonTree(
                                java.util.Arrays.stream(java.util.Locale.getAvailableLocales())
                                    .map(java.util.Locale::getLanguage).filter(value -> !value.isEmpty()).distinct().sorted().toList()));
                            Files.writeString(output, result.toString());
                            poll.stop(); browser.dispose(); frame.dispose(); System.exit(0);
                        } catch (Exception failure) { failure.printStackTrace(); System.exit(1); }
                    }));
            });
            poll.start();
            Timer timeout = new Timer(30_000, event -> System.exit(1));
            timeout.setRepeats(false); timeout.start();
        });
    }
}
