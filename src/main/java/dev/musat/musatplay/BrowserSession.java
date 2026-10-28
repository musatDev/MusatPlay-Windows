package dev.musat.musatplay;

import ca.weblite.webview.WebViewPopupHandler;
import ca.weblite.webview.swing.WebViewComponent;
import com.google.gson.JsonParser;
import java.awt.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class BrowserSession implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(BrowserSession.class.getName());
    private final WebViewComponent browser;
    private final Timer polling;
    private final String bridge;
    private final Consumer<MediaState> state;
    private final Consumer<String> title;
    private final Runnable stopped;
    private boolean closed, pending, reportedFailure;
    private int failures;
    private String previous = "";

    public BrowserSession(Consumer<MediaState> state, Consumer<String> title,
                          Runnable quit, Runnable rendererStopped) throws IOException {
        this.state = state;
        this.title = title;
        this.stopped = rendererStopped;
        this.quitAction = quit;
        WebViewPlatform.initialize();
        try (var stream = BrowserSession.class.getResourceAsStream("/player-bridge.js")) {
            if (stream == null) throw new IOException("Missing player bridge");
            bridge = "return " + new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        browser = WebViewComponent.create(WebViewComponent.Mode.HEAVYWEIGHT);
        browser.setPasswordManagerEnabled(false);
        browser.setNavigationHandler(event -> BrowserPolicy.canNavigate(event.url())
                || "about:blank".equals(event.url()) || "about:srcdoc".equals(event.url()));
        browser.setPopupHandler(new WebViewPopupHandler() {
            @Override public boolean popupRequested(ca.weblite.webview.WebViewPopupEvent event) {
                if (BrowserPolicy.canNavigate(event.targetUrl()))
                    SwingUtilities.invokeLater(() -> { if (!closed) browser.setUrl(event.targetUrl()); });
                return false;
            }
        });
        browser.setUrl(BrowserPolicy.HOME);
        polling = new Timer(500, event -> poll());
        polling.start();
    }

    private void poll() {
        if (closed || pending || !browser.isDisplayable()) return;
        pending = true;
        browser.evalAsync(bridge).orTimeout(5, TimeUnit.SECONDS).whenComplete((json, error) ->
            SwingUtilities.invokeLater(() -> {
                pending = false;
                if (closed) return;
                if (error != null) {
                    if (++failures >= 20 && !reportedFailure) {
                        reportedFailure = true;
                        LOG.warning("WebView2 did not respond to player queries");
                        stopped.run();
                    }
                    return;
                }
                failures = 0;
                reportedFailure = false;
                if (json == null || json.equals(previous)) return;
                previous = json;
                try {
                    if ("null".equals(json)) { state.accept(MediaState.EMPTY); return; }
                    var message = JsonParser.parseString(json).getAsJsonObject();
                    if (!BrowserPolicy.isMusic(message.get("url").getAsString())) {
                        state.accept(MediaState.EMPTY);
                        return;
                    }
                    title.accept(message.get("pageTitle").getAsString());
                    state.accept(MediaState.parse(message.get("state").toString()));
                    if (message.has("quit") && message.get("quit").getAsBoolean()) quitOnEdt();
                } catch (RuntimeException invalid) { LOG.fine("Ignored invalid player state"); }
            }));
    }

    private Runnable quitAction;
    private void quitOnEdt() { if (quitAction != null) quitAction.run(); }
    public void setQuitAction(Runnable action) { quitAction = action; }
    public Component component() { return browser; }
    public void perform(PlayerAction action) {
        if (!closed) browser.eval("if (location.origin === 'https://music.youtube.com') { " + action.script() + " }");
    }
    public void reload() { if (!closed) { failures = 0; reportedFailure = false; browser.eval("location.reload()"); } }
    public void signIn() {
        if (!closed) browser.setUrl("https://accounts.google.com/ServiceLogin?service=youtube&continue=https%3A%2F%2Fmusic.youtube.com%2F");
    }
    @Override public void close() { closed = true; polling.stop(); browser.dispose(); }
}
