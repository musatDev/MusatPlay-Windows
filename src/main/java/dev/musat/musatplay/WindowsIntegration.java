package dev.musat.musatplay;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

public final class WindowsIntegration implements AutoCloseable {
    private static final Set<String> DESKTOP_CLASSES = Set.of("Progman", "WorkerW", "Shell_TrayWnd", "Shell_SecondaryTrayWnd");
    private final AtomicBoolean closed = new AtomicBoolean();
    private Thread thread;
    private volatile int nativeThread;

    private interface Dwm extends StdCallLibrary {
        int DwmGetWindowAttribute(HWND window, int attribute, RECT value, int size);
        int DwmSetWindowAttribute(HWND window, int attribute, IntByReference value, int size);
    }
    private static final class DwmHolder {
        static final Dwm API = Native.load("dwmapi", Dwm.class, W32APIOptions.DEFAULT_OPTIONS);
    }

    public CompletableFuture<List<String>> registerHotkeys(Consumer<PlayerAction> action) {
        CompletableFuture<List<String>> result = new CompletableFuture<>();
        if (!Platform.isWindows()) { result.complete(List.of()); return result; }
        thread = Thread.ofPlatform().daemon().name("musatplay-hotkeys").start(() -> {
            var registered = new ArrayList<Integer>();
            try {
                WinUser.MSG msg = new WinUser.MSG();
                User32.INSTANCE.PeekMessage(msg, null, 0, 0, 0);
                nativeThread = Kernel32.INSTANCE.GetCurrentThreadId();
                List<String> unavailable = new ArrayList<>();
                for (PlayerAction item : PlayerAction.values()) {
                    int id = item.ordinal() + 1;
                    if (User32.INSTANCE.RegisterHotKey(null, id, 0x4003, item.virtualKey)) registered.add(id);
                    else unavailable.add(item.label);
                }
                result.complete(List.copyOf(unavailable));
                while (!closed.get()) {
                    int status = User32.INSTANCE.GetMessage(msg, null, 0, 0);
                    if (status <= 0) break;
                    if (msg.message == 0x0312) {
                        int index = msg.wParam.intValue() - 1;
                        if (index >= 0 && index < PlayerAction.values().length)
                            SwingUtilities.invokeLater(() -> action.accept(PlayerAction.values()[index]));
                    }
                }
            } catch (Throwable e) {
                result.completeExceptionally(e);
            } finally {
                for (int id : registered) User32.INSTANCE.UnregisterHotKey(null, id);
                nativeThread = 0;
            }
        });
        return result;
    }

    public static boolean anotherAppFullscreen() {
        if (!Platform.isWindows()) return false;
        HWND window = User32.INSTANCE.GetForegroundWindow();
        if (window == null) return false;
        IntByReference pid = new IntByReference();
        User32.INSTANCE.GetWindowThreadProcessId(window, pid);
        if (pid.getValue() == ProcessHandle.current().pid()) return false;
        char[] className = new char[256];
        User32.INSTANCE.GetClassName(window, className, className.length);
        if (DESKTOP_CLASSES.contains(Native.toString(className))) return false;
        var monitor = User32.INSTANCE.MonitorFromWindow(window, 2);
        WinUser.MONITORINFO info = new WinUser.MONITORINFO();
        if (!User32.INSTANCE.GetMonitorInfo(monitor, info).booleanValue()) return false;
        RECT bounds = new RECT();
        if (DwmHolder.API.DwmGetWindowAttribute(window, 9, bounds, bounds.size()) != 0
                && !User32.INSTANCE.GetWindowRect(window, bounds)) return false;
        return fills(bounds.left, bounds.top, bounds.right, bounds.bottom,
                info.rcMonitor.left, info.rcMonitor.top, info.rcMonitor.right, info.rcMonitor.bottom);
    }

    static boolean fills(int left, int top, int right, int bottom, int screenLeft, int screenTop, int screenRight, int screenBottom) {
        return Math.abs(left - screenLeft) <= 2 && Math.abs(top - screenTop) <= 2
                && Math.abs(right - screenRight) <= 2 && Math.abs(bottom - screenBottom) <= 2;
    }

    public static void styleNotification(Window window) {
        if (!Platform.isWindows()) return;
        try {
            HWND handle = new HWND(Native.getWindowPointer(window));
            DwmHolder.API.DwmSetWindowAttribute(handle, 33, new IntByReference(2), 4);
            DwmHolder.API.DwmSetWindowAttribute(handle, 38, new IntByReference(3), 4);
        } catch (RuntimeException | LinkageError ignored) {}
    }

    public static void styleMainWindow(Window window) {
        if (!Platform.isWindows()) return;
        try {
            HWND handle = new HWND(Native.getWindowPointer(window));
            DwmHolder.API.DwmSetWindowAttribute(handle, 20, new IntByReference(1), 4);
            DwmHolder.API.DwmSetWindowAttribute(handle, 35, new IntByReference(0x1a1717), 4);
            DwmHolder.API.DwmSetWindowAttribute(handle, 36, new IntByReference(0xf4f4f4), 4);
        } catch (RuntimeException | LinkageError ignored) {}
    }

    @Override public void close() {
        if (!closed.compareAndSet(false, true)) return;
        int id = nativeThread;
        if (id != 0) User32.INSTANCE.PostThreadMessage(id, 0x0012, null, null);
        if (thread != null && thread != Thread.currentThread()) {
            try { thread.join(2_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
