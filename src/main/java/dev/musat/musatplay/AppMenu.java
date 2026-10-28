package dev.musat.musatplay;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.*;

final class AppMenu {
    static JMenuBar create(Runnable signIn, Runnable reload, Runnable quit, Runnable shortcuts, Runnable updates) {
        JMenuBar bar = new JMenuBar();
        bar.setOpaque(true);
        bar.setBackground(new java.awt.Color(23, 23, 26));
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new java.awt.Color(42, 42, 46)),
            BorderFactory.createEmptyBorder(3, 8, 3, 8)));
        JMenu app = new JMenu("MusatPlay");
        app.add(item("Sign in to Google", signIn));
        JMenuItem refresh = item("Reload page", reload);
        refresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        app.add(refresh);
        app.addSeparator();
        JMenuItem exit = item("Quit", quit);
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        app.add(exit);
        JMenu help = new JMenu("Help");
        help.add(item("Keyboard shortcuts", shortcuts));
        help.add(item("Check for updates…", updates));
        bar.add(app);
        bar.add(help);
        return bar;
    }
    private static JMenuItem item(String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(event -> action.run());
        return item;
    }
}
