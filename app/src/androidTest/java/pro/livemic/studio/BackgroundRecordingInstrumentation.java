package pro.livemic.studio;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import java.io.File;

/** Small dependency-free device regression test; run on a disposable emulator/test device. */
public final class BackgroundRecordingInstrumentation extends Instrumentation {
    interface Condition {boolean satisfied();}
    void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    void await(Condition condition,long ms,String message){long until=SystemClock.elapsedRealtime()+ms;while(!condition.satisfied()&&SystemClock.elapsedRealtime()<until)SystemClock.sleep(50);check(condition.satisfied(),message+"; recording="+RecordingManager.active+"; last="+(RecordingManager.lastResult!=null)+"; service="+AudioService.active+"; error="+AudioService.error);}
    void command(String action){Context c=getTargetContext();runOnMainSync(()->c.startService(new Intent(c,AudioService.class).setAction(action)));}
    volatile MainActivity resumed;
    @Override public void callActivityOnResume(Activity activity){super.callActivityOnResume(activity);if(activity instanceof MainActivity)resumed=(MainActivity)activity;}
    @Override public void callActivityOnPause(Activity activity){if(resumed==activity)resumed=null;super.callActivityOnPause(activity);}
    MainActivity open(){if(resumed!=null)return resumed;Intent i=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);runOnMainSync(()->getTargetContext().startActivity(i));await(()->resumed!=null,60000,"activity did not resume");return resumed;}
    void report(String text){Bundle b=new Bundle();b.putString("stream",text+"\n");sendStatus(0,b);}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        Context c=getTargetContext();AudioRouting.prefs(c).edit().putInt("mode",0).commit();getUiAutomation().grantRuntimePermission(c.getPackageName(),Manifest.permission.RECORD_AUDIO);
        MainActivity activity=open();
        if(!BuildConfig.PLAYBACK_CAPTURE){
            PackageInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),PackageManager.GET_PERMISSIONS);
            for(String p:info.requestedPermissions)check(!p.equals("android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION"),"Public APK contains capture permission");
            check(!CaptureSupport.available(),"Public APK exposes capture");report("PASS: distribution variant excludes playback capture permission and support");
        }
        runOnMainSync(()->c.startForegroundService(new Intent(c,AudioService.class).setAction(AudioService.RECORD).putExtra("delay",3).putExtra("bpm",96)));
        await(()->AudioService.waiting()||RecordingManager.active,20000,"reservation did not arm: "+AudioService.error);
        runOnMainSync(()->activity.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)));
        await(()->RecordingManager.active,20000,"recording did not begin while backgrounded: "+AudioService.error);
        SystemClock.sleep(900);command(AudioService.FINISH);
        await(()->!RecordingManager.active&&RecordingManager.lastResult!=null,60000,"recording did not save");
        RecordingManager.Result saved=RecordingManager.lastResult;TakeStore store=new TakeStore(c);
        check(store.file(saved.id,".wav").length()>44,"WAV is empty");check(store.file(saved.id,".mid").length()>22,"MIDI missing");check(store.file(saved.id,".json").isFile(),"take metadata missing");
        report("PASS: 3-second service countdown, app switched to Home, background WAV/MIDI save");
        open();String lastId=RecordingManager.id;
        runOnMainSync(()->c.startForegroundService(new Intent(c,AudioService.class).setAction(AudioService.RECORD).putExtra("delay",5)));
        await(AudioService::waiting,10000,"second reservation did not arm");command(AudioService.CANCEL);
        await(()->!AudioService.waiting(),10000,"cancel did not clear countdown");SystemClock.sleep(5500);
        check(!RecordingManager.active&&lastId.equals(RecordingManager.id),"cancelled countdown created a take");report("PASS: cancellation remains cancelled after the old deadline");
        command(AudioService.STOP);await(()->!AudioService.active,10000,"microphone did not stop");
        open();AudioRouting.prefs(c).edit().putInt("mode",1).commit();AudioService.error="";
        runOnMainSync(()->c.startForegroundService(new Intent(c,AudioService.class)));
        await(()->AudioService.active||!AudioService.error.isEmpty(),20000,"speaker route did not resolve");
        if(AudioService.active){float[] speaker=NativeAudio.stats();check(speaker[19]==1&&speaker[20]<=.181f,"unsafe speaker defaults");report("PASS: phone speaker route opened at low gain");command(AudioService.STOP);await(()->!AudioService.active,10000,"speaker route did not stop");}
        else report("PASS: unsupported speaker route failed closed: "+AudioService.error);
        AudioRouting.prefs(c).edit().putInt("mode",0).commit();
        result.putString("stream","All background recording regressions passed.\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable e){try{command(AudioService.STOP);}catch(Throwable ignored){}result.putString("stream",android.util.Log.getStackTraceString(e));finish(Activity.RESULT_CANCELED,result);}}
}
