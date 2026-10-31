package pro.livemic.studio;

import android.content.Context;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

/** JNI and audio hardware are simulated; persistence and session logic are real. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, instrumentedPackages={"pro.livemic.studio"}, shadows={StudioScreenshotTest.Audio.class,StudioScreenshotTest.Composer.class,StudioScreenshotTest.Harmony.class})
@LooperMode(LooperMode.Mode.PAUSED)
public class HarmonySessionTest {
 Context context; File dir;
 void reset() throws Exception {Field f=HarmonySession.class.getDeclaredField("instance");f.setAccessible(true);HarmonySession old=(HarmonySession)f.get(null);if(old!=null)old.worker.shutdownNow();f.set(null,null);}
 void drain(HarmonySession s)throws Exception{s.worker.submit(()->{}).get();ShadowLooper.idleMainLooper();}
 @Before public void setup()throws Exception{context=RuntimeEnvironment.getApplication();reset();dir=new File(context.getFilesDir(),"vocal-harmony");delete(dir);StudioScreenshotTest.Harmony.current=new double[]{0,0,0,0,-1,-1,0,0,-1,-1,-1,0,0,-1};StudioScreenshotTest.Harmony.failSave=false;StudioScreenshotTest.Harmony.loadedWhilePlaying=false;}
 @After public void cleanup()throws Exception{reset();delete(dir);AudioService.active=false;}
 static void delete(File f){if(f.isDirectory()){File[] children=f.listFiles();if(children!=null)for(File c:children)delete(c);}f.delete();}
 @Test public void completedRecordingPersistsCancelKeepsOldAndUndoRestores()throws Exception{
  HarmonySession s=HarmonySession.get(context);drain(s);assertTrue(s.error,s.ready);
  StudioScreenshotTest.Harmony.current[6]=1;StudioScreenshotTest.Harmony.current[13]=0;s.poll();drain(s);String first=s.files[0];assertNotNull(first);assertTrue(new File(dir,first).exists());
  s.cancel();assertEquals(first,s.files[0]);assertTrue(new File(dir,first).exists());
  StudioScreenshotTest.Harmony.current[6]=2;s.poll();drain(s);String second=s.files[0];assertNotEquals(first,second);assertEquals(first,s.previous[0]);
  s.undo(0);drain(s);assertEquals(first,s.files[0]);assertEquals(second,s.previous[0]);
  s.auto=true;s.liveOn=true;s.names[1]="긴 화음 이름 테스트";s.changed();drain(s);reset();s=HarmonySession.get(context);drain(s);assertTrue(s.error,s.ready);assertFalse(s.liveOn);assertEquals("긴 화음 이름 테스트",s.names[1]);assertEquals(first,s.files[0]);assertTrue(new File(dir,s.files[0]).exists());
 }
 @Test public void metadataChangesDuringSaveKeepCommittedLayer()throws Exception{
  HarmonySession s=HarmonySession.get(context);drain(s);StudioScreenshotTest.Harmony.current[6]=1;StudioScreenshotTest.Harmony.current[13]=0;s.poll();assertTrue(s.busy);s.volume[0]=.35f;s.levels(0);drain(s);drain(s);
  JSONObject json=new JSONObject(new String(Files.readAllBytes(new File(dir,"session.json").toPath()),"UTF-8"));assertEquals(s.files[0],json.getJSONArray("layers").getJSONObject(0).getString("file"));assertEquals(.35,json.getJSONArray("layers").getJSONObject(0).getDouble("volume"),.001);
 }
 @Test public void undoAndFailedSaveStopBeforeReload()throws Exception{
  HarmonySession s=HarmonySession.get(context);drain(s);StudioScreenshotTest.Harmony.current[6]=1;StudioScreenshotTest.Harmony.current[13]=0;s.poll();drain(s);String first=s.files[0];StudioScreenshotTest.Harmony.current[6]=2;s.poll();drain(s);
  StudioScreenshotTest.Harmony.current[0]=1;s.undo(0);drain(s);assertFalse(StudioScreenshotTest.Harmony.loadedWhilePlaying);assertEquals(first,s.files[0]);
  StudioScreenshotTest.Harmony.current[0]=1;StudioScreenshotTest.Harmony.failSave=true;StudioScreenshotTest.Harmony.current[6]=3;s.poll();drain(s);assertFalse(StudioScreenshotTest.Harmony.loadedWhilePlaying);assertEquals(first,s.files[0]);assertFalse(s.error.isEmpty());
 }
 @Test public void atomicBackupIsRecovered()throws Exception{
  assertTrue(dir.mkdirs());Files.write(new File(dir,"session.json.bak").toPath(),new JSONObject().put("version",1).put("bpm",120).toString().getBytes("UTF-8"));
  HarmonySession s=HarmonySession.get(context);drain(s);assertTrue(s.error,s.ready);assertEquals(120,s.bpm);
 }
 @Test public void nativeErrorsAreReportedOnceUntilChanged()throws Exception{
  HarmonySession s=HarmonySession.get(context);drain(s);StudioScreenshotTest.Harmony.current[12]=3;s.poll();int rev=s.revision;assertTrue(s.error.contains("3"));s.poll();assertEquals(rev,s.revision);StudioScreenshotTest.Harmony.current[12]=0;s.poll();StudioScreenshotTest.Harmony.current[12]=3;s.poll();assertTrue(s.revision>rev);
 }
 @Test public void invalidMetadataIsRejected()throws Exception{
  assertTrue(dir.mkdirs());JSONObject json=new JSONObject().put("version",1).put("bpm",999).put("meter",0).put("bars",4);Files.write(new File(dir,"session.json").toPath(),json.toString().getBytes("UTF-8"));
  HarmonySession s=HarmonySession.get(context);drain(s);assertFalse(s.ready);assertFalse(s.error.isEmpty());
 }
}
