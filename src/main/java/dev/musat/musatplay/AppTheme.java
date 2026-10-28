package dev.musat.musatplay;

import com.formdev.flatlaf.FlatDarkLaf;
import java.awt.*;
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
}
