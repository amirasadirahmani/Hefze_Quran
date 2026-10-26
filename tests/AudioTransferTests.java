import ir.hefze.quran.core.AudioTransfer;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AudioTransferTests {
    private static int assertions;
    private static void check(boolean ok,String label){assertions++;if(!ok)throw new AssertionError(label);}
    interface Attempt {void run() throws Exception;}
    private static void fails(Attempt attempt,String label) throws Exception {boolean failed=false;try{attempt.run();}catch(IOException e){failed=true;}check(failed,label);}
    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory("yasin-audio-test");File target=dir.resolve("036-001.mp3").toFile();File part=dir.resolve("036-001.mp3.part").toFile();
        byte[] original={1,2,3,4};AtomicBoolean cancel=new AtomicBoolean();
        AudioTransfer.Progress progress=(n,total)->{};
        AudioTransfer.Validator validator=f->{if(Files.readAllBytes(f.toPath())[0]!=1)throw new IOException("Decoder rejected data");};
        try{
            AudioTransfer.save(new ByteArrayInputStream(original),4,target,cancel::get,progress,validator);
            check(Arrays.equals(original,Files.readAllBytes(target.toPath())),"completed file published");check(!part.exists(),"no partial after success");
            fails(()->AudioTransfer.save(new ByteArrayInputStream(new byte[]{1,2}),4,target,cancel::get,progress,validator),"truncated response rejected");
            check(Arrays.equals(original,Files.readAllBytes(target.toPath())),"failed retry preserves previous audio");check(!part.exists(),"truncated partial removed");
            fails(()->AudioTransfer.save(new ByteArrayInputStream(new byte[]{9,9,9}),3,target,cancel::get,progress,validator),"decoder failure rejected");check(!part.exists(),"decoder failure removes partial");
            cancel.set(true);fails(()->AudioTransfer.save(new ByteArrayInputStream(original),4,target,cancel::get,progress,validator),"cancellation rejected");cancel.set(false);check(!part.exists(),"cancel partial removed");
            fails(()->AudioTransfer.save(new ByteArrayInputStream(original),AudioTransfer.MAX_BYTES+1,target,cancel::get,progress,validator),"oversized response rejected");
            fails(()->AudioTransfer.save(new ByteArrayInputStream(original),5,target,cancel::get,progress,validator,4),"custom declared limit rejected");
            fails(()->AudioTransfer.save(new ByteArrayInputStream(original),-1,target,cancel::get,progress,validator,3),"custom streaming limit rejected");
            fails(()->AudioTransfer.save(new ByteArrayInputStream(new byte[0]),-1,target,cancel::get,progress,validator),"empty unknown-length response rejected");
            AudioTransfer.save(new ByteArrayInputStream(original),-1,target,cancel::get,progress,validator);check(target.length()==4,"chunked response accepted after validation");
            fails(()->AudioTransfer.save(new ByteArrayInputStream(original),4,target,cancel::get,(n,t)->cancel.set(true),validator),"cancellation after final byte rejected");
            check(Arrays.equals(original,Files.readAllBytes(target.toPath())),"cancel preserves completed file");check(!part.exists(),"final-byte cancel cleans partial");
        }finally{Files.deleteIfExists(part.toPath());Files.deleteIfExists(target.toPath());Files.deleteIfExists(dir);}
        System.out.println("PASS: "+assertions+" audio transfer assertions");
    }
}
