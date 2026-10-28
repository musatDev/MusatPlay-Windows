package dev.musat.musatplay;

import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import javax.swing.*;

final class UpdateDialog extends JDialog {
    private final UpdateService service = new UpdateService();
    private final JLabel status = new JLabel("Checking for updates…");
    private final JProgressBar progress = new JProgressBar(0, 100);
    private final JButton action = new JButton("Download update");
    private final Runnable quit;
    private SwingWorker<?, ?> worker;
    private UpdateService.Release release;
    private Path installer;

    UpdateDialog(Frame owner, Runnable quit) {
        super(owner, "Software updates", true);
        this.quit = quit;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBorder(BorderFactory.createEmptyBorder(24, 26, 22, 26));
        JLabel heading = new JLabel("Keep MusatPlay up to date");
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 19));
        JPanel text = new JPanel(new GridLayout(0, 1, 0, 10));
        text.add(heading);
        JLabel current = new JLabel("Installed version " + UpdateService.currentVersion());
        current.setForeground(new Color(160, 160, 168));
        text.add(current); text.add(status);
        content.add(text, BorderLayout.NORTH);
        progress.setIndeterminate(true);
        content.add(progress, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton close = new JButton("Close");
        close.addActionListener(event -> dispose());
        action.setVisible(false);
        action.addActionListener(event -> {
            if (installer == null) download();
            else {
                try {
                    new ProcessBuilder(installer.toString()).directory(installer.getParent().toFile()).start();
                    dispose(); quit.run();
                } catch (Exception error) { showError("Could not open the installer. Please try again."); }
            }
        });
        buttons.add(close); buttons.add(action);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);
        setSize(485, 260);
        setLocationRelativeTo(owner);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { if (worker != null) worker.cancel(true); }
        });
        check();
    }

    private void check() {
        var check = new SwingWorker<Optional<UpdateService.Release>, Void>() {
            protected Optional<UpdateService.Release> doInBackground() throws Exception { return service.check(); }
            protected void done() {
                if (!isDisplayable()) return;
                progress.setVisible(false);
                try {
                    var found = get();
                    if (found.isEmpty()) status.setText("No newer release is available.");
                    else {
                        release = found.get();
                        status.setText("Version " + release.version() + " is available.");
                        action.setVisible(true);
                    }
                } catch (CancellationException ignored) { }
                catch (Exception error) { showError(message(error)); }
            }
        };
        worker = check; check.execute();
    }

    private void download() {
        action.setEnabled(false);
        progress.setVisible(true); progress.setIndeterminate(false); progress.setStringPainted(true);
        status.setText("Downloading version " + release.version() + "…");
        var download = new SwingWorker<Path, Long>() {
            protected Path doInBackground() throws Exception {
                return service.download(release, AppPaths.create().root().resolve("updates"), count -> publish(count));
            }
            protected void process(List<Long> values) {
                if (isDisplayable()) progress.setValue((int) (100L * values.getLast() / release.size()));
            }
            protected void done() {
                if (!isDisplayable()) return;
                try {
                    installer = get();
                    progress.setValue(100);
                    status.setText("<html>Ready to install. MusatPlay will close.<br>Your account stays saved.</html>");
                    action.setText("Install update"); action.setEnabled(true);
                } catch (CancellationException ignored) { }
                catch (Exception error) { action.setEnabled(true); showError(message(error)); }
            }
        };
        worker = download; download.execute();
    }
    private void showError(String text) {
        progress.setVisible(false);
        status.setText("<html>" + text.replace("&", "&amp;").replace("<", "&lt;") + "</html>");
    }
    private static String message(Exception error) {
        Throwable cause = error.getCause() == null ? error : error.getCause();
        return cause.getMessage() == null ? "Update check failed. Please try again." : cause.getMessage();
    }
}
