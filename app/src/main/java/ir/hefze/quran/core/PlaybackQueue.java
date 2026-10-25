package ir.hefze.quran.core;

/** Single-threaded state machine. All times use a monotonic clock; callbacks carry a generation. */
public final class PlaybackQueue {
    public enum State { IDLE, LOADING, PLAYING, GAP, PAUSED, FINISHED, ERROR }
    public interface Port {
        void load(int ayah, long generation);
        void start();
        void pause();
        void release();
        void changed();
        void completed(int ayah);
    }
    private final Port port;
    private final int[] repeats = new int[84], gaps = new int[84];
    private State state = State.IDLE, resumeState = State.IDLE;
    private int ayah = 1, iteration = 1, first = 1, last = 83;
    private long generation, deadline, remaining;
    public PlaybackQueue(Port port) { this.port = port; defaults(3, 3); }
    public void defaults(int repeat, int gap) { for (int i=1;i<=83;i++) configure(i,repeat,gap); }
    public void configure(int verse, int repeat, int gap) {
        if (verse<1 || verse>83 || repeat<1 || repeat>10 || gap<0 || gap>30) throw new IllegalArgumentException();
        repeats[verse]=repeat; gaps[verse]=gap;
    }
    public State state() { return state; }
    public int ayah() { return ayah; }
    public int iteration() { return iteration; }
    public int repeatCount() { return repeats[ayah]; }
    public int gapSeconds() { return gaps[ayah]; }
    public int first() { return first; }
    public int last() { return last; }
    public long generation() { return generation; }
    public boolean running() { return state==State.PLAYING || state==State.GAP || state==State.LOADING; }
    public long gapRemaining(long now) { return state==State.PAUSED ? remaining : Math.max(0, deadline-now); }
    public void range(int start, int end) {
        if (start<1 || end>83 || end<start) throw new IllegalArgumentException();
        stop(); first=start; last=end; ayah=start; iteration=1; port.changed();
    }
    public void select(int verse, boolean autoplay) {
        if (verse<first || verse>last) throw new IllegalArgumentException();
        stop(); ayah=verse; iteration=1;
        if (autoplay) load(); else port.changed();
    }
    public void move(int delta) { select(Math.max(first,Math.min(last,ayah+delta)), running()); }
    public void replay() { select(ayah,true); }
    public void play(long now) {
        if (state==State.PAUSED) {
            state=resumeState;
            if (state==State.GAP) deadline=now+remaining;
            if (state==State.PLAYING) port.start();
            port.changed();
        } else if (!running()) { iteration=1; load(); }
    }
    public void pause(long now) {
        if (!running()) return;
        resumeState=state;
        if (state==State.GAP) remaining=Math.max(0,deadline-now);
        if (state==State.PLAYING) port.pause();
        state=State.PAUSED; port.changed();
    }
    public void stop() { generation++; port.release(); state=State.IDLE; remaining=0; }
    private void load() { generation++; state=State.LOADING; port.changed(); port.load(ayah,generation); }
    public void ready(long token) {
        if (token!=generation) return;
        if (state==State.LOADING) { state=State.PLAYING; port.start(); port.changed(); }
        else if (state==State.PAUSED && resumeState==State.LOADING) resumeState=State.PLAYING;
    }
    public void ended(long token,long now) {
        if (token!=generation || state!=State.PLAYING) return;
        port.completed(ayah); port.release(); state=State.GAP;
        remaining=gaps[ayah]*1000L; deadline=now+remaining; port.changed(); tick(now);
    }
    public void tick(long now) {
        if (state!=State.GAP || now<deadline) return;
        if (iteration<repeats[ayah]) { iteration++; load(); }
        else if (ayah<last) { ayah++; iteration=1; load(); }
        else { state=State.FINISHED; port.changed(); }
    }
    public void failed(long token) {
        if (token!=generation) return;
        port.release(); state=State.ERROR; port.changed();
    }
}
