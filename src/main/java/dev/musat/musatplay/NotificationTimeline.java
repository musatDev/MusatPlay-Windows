package dev.musat.musatplay;

public final class NotificationTimeline {
    public static final long FADE_MILLIS = 750;
    public static final long STAY_MILLIS = 3_500;
    public static final long AFTER_HOVER_MILLIS = 5_000;
    public static final double SLIDE = 20;
    private boolean visible;
    private boolean hovering;
    private long hideAt = Long.MAX_VALUE;
    private Motion motion = new Motion(0, 0, SLIDE, SLIDE, 0, 0);

    public record Frame(boolean visible, float alpha, double offset) {}
    private record Motion(double fromAlpha, double toAlpha, double fromOffset, double toOffset,
                          long started, long duration) {}

    public void show(long now) {
        if (!visible) {
            visible = true;
            hovering = false;
            motion = new Motion(0, 1, SLIDE, 0, now, FADE_MILLIS);
            hideAt = now + FADE_MILLIS + STAY_MILLIS;
        } else {
            restore(now);
            hideAt = hovering ? Long.MAX_VALUE : now + STAY_MILLIS;
        }
    }

    public void hover(boolean value, long now) {
        if (!visible || hovering == value) return;
        hovering = value;
        if (value) {
            restore(now);
            hideAt = Long.MAX_VALUE;
        } else {
            hideAt = now + AFTER_HOVER_MILLIS;
        }
    }

    private void restore(long now) {
        Frame f = sample(now);
        if (f.alpha < 1 || f.offset > 0) motion = new Motion(f.alpha, 1, f.offset, 0, now, 300);
    }

    public Frame tick(long now) {
        if (!visible) return new Frame(false, 0, SLIDE);
        if (!hovering && now >= hideAt && motion.toAlpha != 0) {
            Frame f = sample(now);
            motion = new Motion(f.alpha, 0, f.offset, SLIDE, now, FADE_MILLIS);
        }
        Frame f = sample(now);
        if (motion.toAlpha == 0 && now >= motion.started + motion.duration) {
            reset();
            return new Frame(false, 0, SLIDE);
        }
        return f;
    }

    private Frame sample(long now) {
        double t = motion.duration == 0 ? 1 : Math.clamp((double) (now - motion.started) / motion.duration, 0, 1);
        double ease = t * t * (3 - 2 * t);
        return new Frame(visible, (float) (motion.fromAlpha + (motion.toAlpha - motion.fromAlpha) * ease),
                motion.fromOffset + (motion.toOffset - motion.fromOffset) * ease);
    }

    public void reset() {
        visible = false;
        hovering = false;
        hideAt = Long.MAX_VALUE;
        motion = new Motion(0, 0, SLIDE, SLIDE, 0, 0);
    }
}
