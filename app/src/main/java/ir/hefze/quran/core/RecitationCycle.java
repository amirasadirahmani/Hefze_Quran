package ir.hefze.quran.core;

/** Pure state for repetitions and a pausable silence interval. Uses a monotonic clock. */
public final class RecitationCycle {
    public enum Action { NONE, REPEAT, NEXT }
    private final int count;
    private final long gap;
    private int iteration = 1;
    private Action action = Action.NONE;
    private long deadline, remaining;
    private boolean paused;
    public RecitationCycle(int count, int seconds) {
        if (count < 1 || count > 10 || seconds < 0 || seconds > 30) throw new IllegalArgumentException("Practice settings");
        this.count = count; gap = seconds * 1000L;
    }
    public void completed(boolean repeatOne, boolean hasNext, long now) {
        action = repeatOne || iteration < count ? Action.REPEAT : hasNext ? Action.NEXT : Action.NONE;
        paused = false; remaining = gap; deadline = now + gap;
    }
    public boolean pending() { return action != Action.NONE; }
    public boolean paused() { return paused; }
    public long remaining(long now) { return paused ? remaining : Math.max(0, deadline - now); }
    public void pause(long now) { if (pending() && !paused) { remaining = remaining(now); paused = true; } }
    public void resume(long now) { if (pending() && paused) { deadline = now + remaining; paused = false; } }
    public Action takeDue(long now) {
        if (!pending() || paused || now < deadline) return Action.NONE;
        Action result = action; action = Action.NONE;
        iteration = result == Action.NEXT ? 1 : Math.min(count, iteration + 1);
        return result;
    }
    public void reset() { action = Action.NONE; iteration = 1; remaining = 0; paused = false; }
}
