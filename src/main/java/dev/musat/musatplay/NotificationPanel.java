package dev.musat.musatplay;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JPanel;

public final class NotificationPanel extends JPanel {
    public static final int WIDTH = 320;
    public static final int HEIGHT = 116;
    private static final Color ACTIVE = new Color(255, 30, 70);
    private final boolean dark;
    private final EnumMap<PlayerAction, Control> controls = new EnumMap<>(PlayerAction.class);
    private MediaState state = MediaState.EMPTY;
    private BufferedImage artwork;

    public NotificationPanel(boolean dark, Consumer<PlayerAction> action) {
        this.dark = dark;
        setLayout(null);
        setOpaque(false);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        int i = 0;
        for (PlayerAction item : PlayerAction.values()) {
            Control button = new Control(item);
            button.setBounds(44 + i++ * 50, 78, 32, 24);
            button.addActionListener(e -> action.accept(item));
            controls.put(item, button);
            add(button);
        }
        setState(MediaState.EMPTY);
    }

    public void setState(MediaState state) {
        this.state = state;
        for (Control button : controls.values()) button.setEnabled(state.hasTrack());
        label(PlayerAction.PLAY_PAUSE, state.playing() ? Language.text("Pause") : Language.text("Play"));
        label(PlayerAction.SHUFFLE, state.shuffle() ? Language.text("Shuffle on") : Language.text("Shuffle off"));
        label(PlayerAction.REPEAT, switch (state.repeat()) {
            case NONE -> Language.text("Repeat off"); case ALL -> Language.text("Repeat all"); case ONE -> Language.text("Repeat one");
        });
        getAccessibleContext().setAccessibleName(state.title() + ", " + state.artist());
        repaint();
    }

    private void label(PlayerAction action, String label) {
        Control control = controls.get(action);
        control.setToolTipText(label);
        control.getAccessibleContext().setAccessibleName(label);
    }

    public void setArtwork(BufferedImage image) { artwork = image; repaint(); }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        quality(g);
        Shape panel = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, 28, 28);
        g.setColor(dark ? new Color(33, 33, 37, 239) : new Color(242, 242, 246, 239));
        g.fill(panel);
        g.setColor(dark ? new Color(255, 255, 255, 30) : new Color(0, 0, 0, 24));
        g.draw(panel);
        Shape tile = new RoundRectangle2D.Double(12, 14, 56, 56, 16, 16);
        if (artwork == null) {
            g.setColor(dark ? new Color(80, 80, 85) : new Color(210, 210, 215));
            g.fill(tile);
        } else {
            Shape old = g.getClip();
            g.clip(tile);
            g.drawImage(artwork, 12, 14, 56, 56, null);
            g.setClip(old);
        }
        g.setFont(new Font("Segoe UI", Font.BOLD, 13));
        g.setColor(foreground());
        g.drawString(fit(state.title(), g.getFontMetrics(), 228), 80, 35);
        g.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g.setColor(secondary());
        g.drawString(fit(state.artist(), g.getFontMetrics(), 228), 80, 53);
        g.dispose();
    }

    private Color foreground() { return dark ? new Color(246, 246, 248) : new Color(28, 28, 31); }
    private Color secondary() { return dark ? new Color(172, 172, 180) : new Color(109, 109, 117); }

    static String fit(String text, FontMetrics metrics, int width) {
        if (metrics.stringWidth(text) <= width) return text;
        int low = 0, high = text.codePointCount(0, text.length());
        while (low < high) {
            int mid = (low + high + 1) / 2;
            String value = text.substring(0, text.offsetByCodePoints(0, mid)) + "…";
            if (metrics.stringWidth(value) <= width) low = mid;
            else high = mid - 1;
        }
        return text.substring(0, text.offsetByCodePoints(0, low)) + "…";
    }

    private static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    private final class Control extends JButton {
        private final PlayerAction action;
        Control(PlayerAction action) {
            this.action = action;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setToolTipText(action.label);
            getAccessibleContext().setAccessibleName(action.label);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            quality(g);
            if (!isEnabled()) g.setComposite(AlphaComposite.SrcOver.derive(0.4f));
            if (getModel().isRollover()) {
                g.setColor(dark ? new Color(255, 255, 255, 24) : new Color(0, 0, 0, 14));
                g.fillRoundRect(0, 0, 32, 24, 8, 8);
            }
            boolean active = action == PlayerAction.SHUFFLE && state.shuffle()
                    || action == PlayerAction.REPEAT && state.repeat() != MediaState.Repeat.NONE;
            g.setColor(active ? ACTIVE : (action == PlayerAction.SHUFFLE || action == PlayerAction.REPEAT ? secondary() : foreground()));
            g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (action) {
                case PLAY_PAUSE -> {
                    if (state.playing()) { g.fillRoundRect(10, 5, 4, 14, 1, 1); g.fillRoundRect(18, 5, 4, 14, 1, 1); }
                    else triangle(g, 11, 5, 23, 12, 11, 19);
                }
                case PREVIOUS -> { triangle(g, 16, 5, 6, 12, 16, 19); triangle(g, 26, 5, 16, 12, 26, 19); }
                case NEXT -> { triangle(g, 6, 5, 16, 12, 6, 19); triangle(g, 16, 5, 26, 12, 16, 19); }
                case SHUFFLE -> {
                    Path2D p = new Path2D.Double();
                    p.moveTo(6, 6); p.lineTo(10, 6); p.curveTo(16, 6, 16, 18, 22, 18);
                    p.moveTo(6, 18); p.lineTo(10, 18); p.curveTo(16, 18, 16, 6, 22, 6);
                    p.moveTo(19, 3); p.lineTo(22, 6); p.lineTo(19, 9);
                    p.moveTo(19, 15); p.lineTo(22, 18); p.lineTo(19, 21); g.draw(p);
                }
                case REPEAT -> {
                    Path2D p = new Path2D.Double();
                    p.moveTo(7, 10); p.curveTo(7, 6, 9, 6, 12, 6); p.lineTo(25, 6);
                    p.moveTo(22, 3); p.lineTo(25, 6); p.lineTo(22, 9);
                    p.moveTo(25, 14); p.curveTo(25, 18, 23, 18, 20, 18); p.lineTo(7, 18);
                    p.moveTo(10, 15); p.lineTo(7, 18); p.lineTo(10, 21); g.draw(p);
                    if (state.repeat() == MediaState.Repeat.ALL) g.fillOval(14, 10, 4, 4);
                    if (state.repeat() == MediaState.Repeat.ONE) {
                        g.setFont(new Font("Segoe UI", Font.BOLD, 9));
                        g.drawString("1", 13, 15);
                    }
                }
            }
            g.dispose();
        }
    }

    private static void triangle(Graphics2D g, int x1, int y1, int x2, int y2, int x3, int y3) {
        Path2D p = new Path2D.Double();
        p.moveTo(x1, y1); p.lineTo(x2, y2); p.lineTo(x3, y3); p.closePath(); g.fill(p);
    }
}
