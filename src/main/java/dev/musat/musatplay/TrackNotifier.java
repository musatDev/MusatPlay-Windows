package dev.musat.musatplay;

import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class TrackNotifier implements AutoCloseable {
    private final JWindow window = new JWindow();
    private final NotificationPanel panel;
    private final NotificationTimeline timeline = new NotificationTimeline();
    private final ArtworkLoader artwork = new ArtworkLoader();
    private final Supplier<GraphicsConfiguration> screen;
    private final Timer animation;
    private MediaState previous = MediaState.EMPTY;
    private long lastFullscreenCheck;

    public TrackNotifier(boolean dark, Supplier<GraphicsConfiguration> screen, Consumer<PlayerAction> action) {
        this.screen = screen;
        panel = new NotificationPanel(dark, action);
        window.setType(Window.Type.UTILITY);
        window.setFocusableWindowState(false);
        window.setAutoRequestFocus(false);
        window.setAlwaysOnTop(true);
        window.setBackground(new Color(0, 0, 0, 0));
        window.setContentPane(panel);
        window.pack();
        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { timeline.hover(true, now()); animation.setDelay(16); }
            @Override public void mouseExited(MouseEvent e) {
                Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), panel);
                if (!panel.contains(point)) timeline.hover(false, now());
            }
        };
        panel.addMouseListener(hover);
        for (Component child : panel.getComponents()) child.addMouseListener(hover);
        animation = new Timer(16, e -> tick());
    }

    public void accept(MediaState state) {
        boolean changed = state.differentTrack(previous);
        previous = state;
        panel.setState(state);
        artwork.load(state.artwork(), panel::setArtwork);
        if (!state.hasTrack()) { hide(); return; }
        if (!changed) return;
        if (WindowsIntegration.anotherAppFullscreen()) { hide(); return; }
        timeline.show(now());
        animation.setDelay(16);
        tick();
        if (!window.isVisible()) {
            window.setVisible(true);
            WindowsIntegration.styleNotification(window);
        }
        animation.start();
    }

    private void tick() {
        long now = now();
        if (now - lastFullscreenCheck > 250) {
            lastFullscreenCheck = now;
            if (WindowsIntegration.anotherAppFullscreen()) { hide(); return; }
        }
        var frame = timeline.tick(now);
        if (!frame.visible()) { hide(); return; }
        GraphicsConfiguration gc = screen.get();
        Rectangle bounds = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        int x = bounds.x + bounds.width - insets.right - NotificationPanel.WIDTH - 16 + (int) Math.round(frame.offset());
        int y = bounds.y + insets.top + 12;
        if (window.getX() != x || window.getY() != y) window.setLocation(x, y);
        GraphicsDevice device = gc.getDevice();
        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.TRANSLUCENT)
                && Math.abs(window.getOpacity() - frame.alpha()) > 0.001f) window.setOpacity(frame.alpha());
        animation.setDelay(frame.alpha() == 1 && frame.offset() == 0 ? 100 : 16);
    }

    private static long now() { return System.nanoTime() / 1_000_000; }
    private void hide() { timeline.reset(); animation.stop(); window.setVisible(false); }

    @Override public void close() { hide(); artwork.close(); window.dispose(); }
}
