package pro.livemic.studio;
import android.content.Context;
import java.io.*;
import java.text.*;
import java.util.*;
public final class RecordingManager {
    public static boolean active=false,paused=false;public static String id=null;public static int bpm=78;
    private static Context context;
    public static synchronized boolean start(Context c,int tempo){
        if(active||NativeAudio.stats()[10]<.5f)return false;context=c.getApplicationContext();bpm=tempo;
        id=new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.US).format(new Date());TakeStore store=new TakeStore(context);
        paused=false;active=NativeAudio.beginRecord(store.file(id,".wav").getAbsolutePath());return active;
    }
    public static synchronized void pause(boolean value){if(active){NativeAudio.pauseRecord(value);paused=value;}}
    public static synchronized Result stop()throws Exception{
        if(!active)return null;NativeAudio.endRecord();active=false;paused=false;float[] s=NativeAudio.stats();double clock=NativeAudio.recordClock();
        TakeStore store=new TakeStore(context);boolean midi=NativeAudio.exportMidi(store.file(id,".mid").getAbsolutePath(),bpm,true);
        String warning=s[13]>.5?"오디오 버퍼 또는 저장 오류가 감지되었습니다. 재생 상태를 확인하세요.":!midi?"MIDI 파일을 저장하지 못했습니다.":"";
        store.save(id,"Take "+new SimpleDateFormat("MM.dd HH:mm",Locale.KOREA).format(new Date()),s[8],false,warning);
        return new Result(id,s[8],clock,warning);
    }
    public static class Result {public final String id,warning;public final double seconds,clock;Result(String i,double s,double c,String w){id=i;seconds=s;clock=c;warning=w;}}
}
