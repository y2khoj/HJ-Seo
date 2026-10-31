package pro.livemic.studio;
import android.view.*;
import java.io.File;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=35,instrumentedPackages={"pro.livemic.studio"},shadows={StudioScreenshotTest.Audio.class,StudioScreenshotTest.Composer.class,StudioScreenshotTest.Harmony.class}) @GraphicsMode(GraphicsMode.Mode.NATIVE) @LooperMode(LooperMode.Mode.PAUSED)
public class LiveConsoleScreenshotTest {
 @Test public void liveConsoleInBothLanguagesAndLargeText()throws Exception {
  File out=new File(System.getProperty("user.dir"),"build/studio-screenshots");out.mkdirs();java.lang.reflect.Field field=AudioService.class.getDeclaredField("countdown");field.setAccessible(true);RecordingCountdown countdown=(RecordingCountdown)field.get(null);
  for(boolean english:new boolean[]{false,true})for(float scale:new float[]{1f,1.3f})for(int width:new int[]{412,360}){
   if(width==360&&scale==1f)continue;int height=width==412?893:800;RuntimeEnvironment.setQualifiers("w"+width+"dp-h"+height+"dp-mdpi");RuntimeEnvironment.setFontScale(scale);AppLanguage.set(RuntimeEnvironment.getApplication(),english);AudioService.active=false;RecordingManager.active=false;countdown.cancel();
   MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();a.handler.removeCallbacks(a.tick);a.composer.worker.submit(()->{}).get();ShadowLooper.idleMainLooper();a.handler.removeCallbacks(a.tick);a.root.dispatchApplyWindowInsets(new WindowInsets.Builder().setInsets(WindowInsets.Type.systemBars(),android.graphics.Insets.of(0,36,0,24)).build());a.chooseTab(0);String prefix="v34-"+(english?"en":"ko")+"-"+width+(scale==1?"-default":"-large");
   float[] stats=new float[22];StudioScreenshotTest.Audio.supplied=stats;a.tick.run();a.handler.removeCallbacks(a.tick);StudioScreenshotTest.capture(a,width,height,new File(out,prefix+"-idle.png"),false);android.graphics.Rect rec=StudioScreenshotTest.bounds(a,a.recButton);assertTrue(rec.top>=a.pageScroll.getBottom());assertTrue(rec.bottom<=a.nav.getTop());
   a.pageScroll.scrollTo(0,a.page.getHeight());StudioScreenshotTest.capture(a,width,height,new File(out,prefix+"-knobs.png"),false);assertEquals(rec,StudioScreenshotTest.bounds(a,a.recButton));
   a.pageScroll.scrollTo(0,0);AudioService.active=true;RecordingManager.active=true;stats[0]=440;stats[2]=69;stats[3]=.08f;stats[8]=12;stats[10]=1;stats[16]=1;a.tick.run();a.handler.removeCallbacks(a.tick);StudioScreenshotTest.capture(a,width,height,new File(out,prefix+"-recording.png"),false);assertTrue(a.liveScreen.clock.getText().toString().contains("00:12"));
   RecordingManager.active=false;stats[17]=1;a.tick.run();a.handler.removeCallbacks(a.tick);StudioScreenshotTest.capture(a,width,height,new File(out,prefix+"-safety.png"),false);assertTrue(a.liveScreen.route.getText().toString().contains(english?"safety":"보호"));
   stats[17]=0;countdown.arm(5,android.os.SystemClock.elapsedRealtime());a.tick.run();a.handler.removeCallbacks(a.tick);assertTrue(a.liveScreen.clock.getText().toString().contains(english?"START IN":"예약"));countdown.cancel();a.io.shutdown();StudioScreenshotTest.Audio.supplied=null;AudioService.active=false;
  }AppLanguage.set(RuntimeEnvironment.getApplication(),false);RuntimeEnvironment.setFontScale(1f);
 }
}
