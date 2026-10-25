import ir.hefze.quran.core.*;
import static ir.hefze.quran.core.PlaybackQueue.State.*;

public final class CoreTests {
    static int assertions=0;
    static void check(boolean condition,String name){assertions++;if(!condition)throw new AssertionError(name);}
    static final class Fake implements PlaybackQueue.Port {
        int starts,pauses,loads,completed,releases;long token;int ayah;
        public void load(int ayah,long token){this.ayah=ayah;this.token=token;loads++;}
        public void start(){starts++;}public void pause(){pauses++;}public void release(){releases++;}public void changed(){}public void completed(int ayah){completed++;}
    }
    public static void main(String[] args){
        Fake f=new Fake();PlaybackQueue q=new PlaybackQueue(f);q.defaults(2,3);q.range(1,2);q.play(0);
        check(q.state()==LOADING&&f.loads==1,"initial load");q.ready(f.token);check(q.state()==PLAYING&&f.starts==1,"prepared starts");
        long token=f.token;q.ended(token,1000);check(q.state()==GAP&&f.completed==1,"completion enters gap");q.ended(token,1001);check(f.completed==1,"duplicate completion ignored");
        q.tick(1999);check(f.loads==1,"gap not shortened");q.pause(2000);check(q.state()==PAUSED&&q.gapRemaining(9999)==2000,"remaining gap frozen");q.tick(9000);check(f.loads==1,"paused gap cannot advance");
        q.play(10000);q.tick(11999);check(f.loads==1,"resume preserves remainder");q.tick(12000);check(q.iteration()==2&&q.ayah()==1,"second repetition");
        q.ready(f.token);q.ended(f.token,13000);q.tick(16000);check(q.ayah()==2&&q.iteration()==1,"next verse");
        q.ready(f.token);q.ended(f.token,17000);q.tick(20000);q.ready(f.token);q.ended(f.token,21000);q.tick(24000);check(q.state()==FINISHED&&f.completed==4,"range ends after exact completions");
        q.select(1,true);long stale=f.token;q.select(2,true);q.ready(stale);check(q.state()==LOADING,"stale prepared ignored");q.ended(stale,1);check(f.completed==4,"stale completed ignored");q.failed(stale);check(q.state()==LOADING,"stale error ignored");
        q.pause(0);q.ready(f.token);int starts=f.starts;check(q.state()==PAUSED,"prepare while paused remains paused");q.play(20);check(q.state()==PLAYING&&f.starts==starts+1,"resume after prepared starts");
        q.move(-1);check(q.ayah()==1&&q.iteration()==1&&q.state()==LOADING,"navigation cancels old audio");q.ready(f.token);q.pause(40);q.move(1);check(q.state()==IDLE&&q.ayah()==2,"paused navigation does not autoplay");
        q.defaults(1,0);q.range(83,83);q.play(0);q.ready(f.token);q.ended(f.token,1);check(q.state()==FINISHED,"zero gap last verse finishes");
        q.range(1,1);q.play(0);q.failed(f.token);check(q.state()==ERROR,"missing audio terminates queue");q.play(0);check(q.state()==LOADING,"retry after error");q.replay();check(q.iteration()==1,"manual replay resets repetition");
        try{q.configure(1,11,0);throw new AssertionError();}catch(IllegalArgumentException expected){assertions++;}
        try{q.range(4,3);throw new AssertionError();}catch(IllegalArgumentException expected){assertions++;}
        WordTimeline t=new WordTimeline(new int[]{100,400},new int[]{300,600},700);
        check(t.active(99)==-1&&t.active(100)==0&&t.active(299)==0&&t.active(300)==-1,"timeline half-open boundaries");
        check(t.active(400)==1&&t.active(600)==-1,"second word boundary");check(t.readThrough(299)==-1&&t.readThrough(300)==0&&t.readThrough(700)==1,"read-through words");
        try{new WordTimeline(new int[]{0,100},new int[]{200,300},300);throw new AssertionError();}catch(IllegalArgumentException expected){assertions++;}
        try{new WordTimeline(new int[]{0},new int[]{301},300);throw new AssertionError();}catch(IllegalArgumentException expected){assertions++;}
        for(StudyMethod m:StudyMethod.values())check(m.repeats>=1&&m.repeats<=10&&m.gap>=0&&m.gap<=30,"method bounds");
        System.out.println("PASS: "+assertions+" core assertions");
    }
}
