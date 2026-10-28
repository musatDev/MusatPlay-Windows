package dev.musat.musatplay;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;


public final class MainWindow {
    private final JFrame frame = new JFrame("MusatPlay");
    private final WindowsIntegration nativeWindows = new WindowsIntegration();
    private final BrowserSession browser;
    private final TrackNotifier notifier;
    private TrayIcon tray;
    private boolean quitting;

    public MainWindow() throws IOException {
        frame.setIconImage(AppImages.icon());
        frame.setMinimumSize(new Dimension(900, 600));
        frame.setSize(1100, 760);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        browser = new BrowserSession(this::stateChanged, title -> {}, this::quit, this::rendererStopped);
        notifier = new TrackNotifier(darkTheme(), frame::getGraphicsConfiguration, browser::perform);
        frame.add(browser.component(), BorderLayout.CENTER);
        frame.setJMenuBar(menu());
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (tray != null) frame.setVisible(false);
                else frame.setState(Frame.ICONIFIED);
            }
        });
        installTray();
        show();
        nativeWindows.registerHotkeys(browser::perform).whenComplete((missing, error) -> {
            if (error != null || !missing.isEmpty()) SwingUtilities.invokeLater(() -> {
                if (!quitting) JOptionPane.showMessageDialog(frame,
                    error != null ? Language.text("Global shortcuts could not be registered. Use the player or tray controls.")
                        : Language.text("These global shortcuts are already in use: ") + String.join(", ", missing),
                    Language.text("MusatPlay shortcuts"), JOptionPane.WARNING_MESSAGE);
            });
        });
    }

    private JMenuBar menu() {
        return AppMenu.create(browser::signIn, browser::reload, this::quit,
            () -> JOptionPane.showMessageDialog(frame, Language.shortcutsHelp(), "Keyboard shortcuts", JOptionPane.INFORMATION_MESSAGE),
            () -> new UpdateDialog(frame, this::quit).setVisible(true));
    }
    private void installTray() {
        if (!SystemTray.isSupported()) return;
        PopupMenu menu = new PopupMenu();
        MenuItem open = new MenuItem(Language.text("Open MusatPlay"));
        open.addActionListener(e -> SwingUtilities.invokeLater(this::show));
        menu.add(open);
        for (PlayerAction action : PlayerAction.values()) {
            MenuItem item = new MenuItem(action.label);
            item.addActionListener(e -> SwingUtilities.invokeLater(() -> browser.perform(action)));
            menu.add(item);
        }
        menu.addSeparator();
        MenuItem exit = new MenuItem(Language.text("Quit"));
        exit.addActionListener(e -> SwingUtilities.invokeLater(this::quit));
        menu.add(exit);
        TrayIcon candidate = new TrayIcon(AppImages.scaled(AppImages.icon(), 32, 32), "MusatPlay", menu);
        candidate.setImageAutoSize(true);
        candidate.addActionListener(e -> SwingUtilities.invokeLater(this::show));
        candidate.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) SwingUtilities.invokeLater(MainWindow.this::show);
            }
        });
        try { SystemTray.getSystemTray().add(candidate); tray = candidate; }
        catch (AWTException ignored) {}
    }

    public void show() {
        if (quitting) return;
        frame.setExtendedState(frame.getExtendedState() & ~Frame.ICONIFIED);
        frame.setVisible(true);
        WindowsIntegration.styleMainWindow(frame);
        frame.toFront();
        frame.requestFocus();
    }

    private void stateChanged(MediaState state) {
        if (quitting) return;
        notifier.accept(state);
        if (tray != null) tray.setToolTip(state.hasTrack() ? "MusatPlay — " + state.title() : "MusatPlay");
    }

    private void rendererStopped() {
        if (!quitting && JOptionPane.showConfirmDialog(frame,
                Language.text("The browser stopped unexpectedly. Reload YouTube Music?"), "MusatPlay",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) browser.reload();
    }

    public void quit() {
        if (quitting) return;
        quitting = true;
        notifier.close();
        nativeWindows.close();
        if (tray != null) SystemTray.getSystemTray().remove(tray);
        frame.dispose();
        browser.close();
    }

    private static boolean darkTheme() {
        if (!Platform.isWindows()) return false;
        try {
            return Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER,
                "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize", "AppsUseLightTheme") == 0;
        } catch (RuntimeException ignored) { return false; }
    }
}
