import ir.hefze.quran.core.PageIndex;
import ir.hefze.quran.core.RecitationCycle;
import static ir.hefze.quran.core.RecitationCycle.Action.*;

public final class ReaderTests {
    static int checks;
    static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("Check " + checks); }
    public static void main(String[] args) {
        int[] pages=new int[604]; for(int i=0;i<604;i++)pages[i]=i+1;
        PageIndex index=new PageIndex(pages); pages[0]=440;
        check(index.restoreAyah(false,440)==1);
        check(index.restoreAyah(true,440)==440);
        check(index.adjacent(1,-1)==1 && index.adjacent(604,1)==604);
        for(int i=1;i<=604;i++) check(index.pageOf(index.firstAyah(i))==i);
        RecitationCycle c=new RecitationCycle(3,2);
        c.completed(false,true,100); check(c.takeDue(2099)==NONE); check(c.takeDue(2100)==REPEAT);
        c.completed(false,true,3000); c.pause(3500); check(c.remaining(9000)==1500);
        check(c.takeDue(10000)==NONE); c.resume(10000); check(c.takeDue(11499)==NONE); check(c.takeDue(11500)==REPEAT);
        c.completed(false,true,12000); check(c.takeDue(14000)==NEXT);
        c.completed(false,false,15000); check(c.takeDue(17000)==REPEAT);
        c.completed(false,false,18000); check(c.takeDue(20000)==REPEAT);
        c.completed(false,false,21000); check(!c.pending());
        c.completed(true,false,24000); check(c.takeDue(26000)==REPEAT);
        c.completed(true,true,28000); c.reset(); check(c.takeDue(99999)==NONE);
        System.out.println("PASS: " + checks + " reader checks (including 604 page roundtrips)");
    }
}
