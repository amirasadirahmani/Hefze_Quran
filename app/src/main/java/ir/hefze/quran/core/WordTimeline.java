package ir.hefze.quran.core;

public final class WordTimeline {
    public final int[] starts, ends;
    public WordTimeline(int[] starts, int[] ends, int duration) {
        if(starts.length!=ends.length || starts.length==0 || duration<=0) throw new IllegalArgumentException("Invalid timeline");
        this.starts=starts.clone(); this.ends=ends.clone();
        for(int i=0;i<starts.length;i++) {
            if(starts[i]<0 || ends[i]<=starts[i] || ends[i]>duration || (i>0 && starts[i]<ends[i-1]))
                throw new IllegalArgumentException("Unordered word boundaries");
        }
    }
    public int active(int position) {
        for(int i=0;i<starts.length;i++) if(position>=starts[i] && position<ends[i]) return i;
        return -1;
    }
    public int readThrough(int position) {
        int result=-1;
        for(int i=0;i<ends.length;i++) { if(ends[i]<=position) result=i; else break; }
        return result;
    }
}
