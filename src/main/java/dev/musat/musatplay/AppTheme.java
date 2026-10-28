package dev.musat.musatplay;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import javax.swing.*;

final class AppTheme {
    static void install() {
        java.util.Locale.setDefault(java.util.Locale.US);
        JComponent.setDefaultLocale(java.util.Locale.US);
        FlatDarkLaf.setup();
        UIManager.put("defaultFont", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("MenuBar.background", new Color(23, 23, 26));
        UIManager.put("MenuBar.itemMargins", new Insets(6, 12, 6, 12));
        UIManager.put("MenuItem.minimumWidth", 210);
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("Component.focusColor", new Color(231, 56, 79));
    }
    static void renderMenu(Path output) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            install();
            JMenuBar bar = AppMenu.create(() -> {}, () -> {}, () -> {}, () -> {}, () -> {});
            bar.setSize(900, bar.getPreferredSize().height);
            bar.doLayout();
            BufferedImage image = new BufferedImage(1800, bar.getHeight() * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.scale(2, 2);
            bar.paint(g);
            g.dispose();
            try { ImageIO.write(image, "png", output.toFile()); }
            catch (java.io.IOException error) { throw new java.io.UncheckedIOException(error); }
        });
    }
}
