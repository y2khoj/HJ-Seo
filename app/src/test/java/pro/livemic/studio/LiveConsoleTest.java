package pro.livemic.studio;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,instrumentedPackages={"pro.livemic.studio"},shadows={StudioScreenshotTest.Audio.class,StudioScreenshotTest.Composer.class,StudioScreenshotTest.Harmony.class}) @LooperMode(LooperMode.Mode.PAUSED)
public class LiveConsoleTest {
 MainActivity a;
 @Before public void setup()throws Exception{AppLanguage.set(RuntimeEnvironment.getApplication(),true);AudioService.active=false;AudioService.playbackActive=false;RecordingManager.active=false;StudioScreenshotTest.Audio.supplied=null;StudioScreenshotTest.Audio.changes.clear();a=Robolectric.buildActivity(MainActivity.class).setup().get();a.handler.removeCallbacks(a.tick);a.composer.worker.submit(()->{}).get();ShadowLooper.idleMainLooper();a.handler.removeCallbacks(a.tick);a.chooseTab(0);}
 @After public void cleanup(){a.handler.removeCallbacks(a.tick);a.io.shutdown();AudioService.active=false;RecordingManager.active=false;StudioScreenshotTest.Audio.supplied=null;AppLanguage.set(RuntimeEnvironment.getApplication(),false);}
 static void progress(StudioKnob k,float value){Bundle args=new Bundle();args.putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,value);assertTrue(k.performAccessibilityAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId(),args));}
 @Test public void everyKnobControlsItsOwnRealParameterAndSyncDoesNotWrite(){
  int[] ids={2,3,4,5,6,0};float[] target={.4f,3f,.3f,350,.4f,1.2f};StudioScreenshotTest.Audio.changes.clear();
  for(int i=0;i<ids.length;i++){progress(a.liveScreen.knobs[i],target[i]);assertEquals(target[i],a.values[ids[i]],.001);assertEquals(target[i],a.getPreferences(0).getFloat("p"+ids[i],-1),.001);float[] change=StudioScreenshotTest.Audio.changes.get(StudioScreenshotTest.Audio.changes.size()-1);assertEquals(ids[i],(int)change[0]);assertEquals(target[i],change[1],.001);}
  int count=StudioScreenshotTest.Audio.changes.size();a.liveScreen.syncKnobs();assertEquals(count,StudioScreenshotTest.Audio.changes.size());
 }
 @Test public void relativeTouchDoesNotJumpAndSyncCannotStealActiveGesture(){
  List<Float> calls=new ArrayList<>();StudioKnob knob=new StudioKnob(a,"Test",0,1,.5f,v->Math.round(v*100)+"%",calls::add);LinearLayout parent=new LinearLayout(a);parent.addView(knob,new LinearLayout.LayoutParams(200,200));parent.measure(View.MeasureSpec.makeMeasureSpec(200,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(200,View.MeasureSpec.EXACTLY));parent.layout(0,0,200,200);
  knob.onTouchEvent(MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,40,120,0));assertTrue(calls.isEmpty());knob.sync(.1f);assertTrue(calls.isEmpty());knob.onTouchEvent(MotionEvent.obtain(0,1,MotionEvent.ACTION_MOVE,40,70,0));assertTrue(knob.dragging);assertEquals(.75f,calls.get(calls.size()-1),.01);int n=calls.size();knob.sync(.1f);assertEquals(n,calls.size());knob.onTouchEvent(MotionEvent.obtain(0,2,MotionEvent.ACTION_MOVE,40,-1000,0));assertEquals(1,calls.get(calls.size()-1),0);knob.onTouchEvent(MotionEvent.obtain(0,3,MotionEvent.ACTION_UP,40,-1000,0));assertFalse(knob.dragging);knob.sync(.25f);assertEquals(n+1,calls.size());assertTrue(knob.getContentDescription().toString().contains("25%"));
 }
 @Test public void accessibilityAndKeyboardExposeAdjustableRange(){
  List<Float> calls=new ArrayList<>();StudioKnob knob=new StudioKnob(a,"Level",0,2,1,v->Math.round(v*100)+"%",calls::add);AccessibilityNodeInfo node=knob.createAccessibilityNodeInfo();assertEquals(SeekBar.class.getName(),node.getClassName());assertEquals(2,node.getRangeInfo().getMax(),0);progress(knob,4);assertEquals(2,calls.get(calls.size()-1),0);knob.onKeyDown(KeyEvent.KEYCODE_DPAD_DOWN,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN));assertEquals(1.9,calls.get(calls.size()-1),.001);knob.performAccessibilityAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD,null);assertEquals(1.8,calls.get(calls.size()-1),.001);
 }
 @Test public void resetDoesNotStopMicOrRecordingOrTouchOtherParameters(){
  AudioService.active=true;RecordingManager.active=true;a.values[11]=1;a.values[13]=1;a.values[7]=5;StudioScreenshotTest.Audio.changes.clear();a.liveScreen.reset();assertTrue(AudioService.active);assertTrue(RecordingManager.active);assertEquals(1,a.values[11],0);assertEquals(1,a.values[13],0);assertEquals(5,a.values[7],0);Set<Integer> keys=new HashSet<>();for(float[] v:StudioScreenshotTest.Audio.changes)keys.add((int)v[0]);assertEquals(new HashSet<>(Arrays.asList(0,2,3,4,5,6)),keys);
 }
 @Test public void lcdShowsRealInputBackingAndSafetyState(){
  float[] stats=new float[22];stats[0]=440;stats[2]=69;stats[3]=.1f;stats[8]=12;stats[16]=1;stats[17]=1;stats[21]=0;RecordingManager.active=true;a.liveScreen.poll(stats,true);assertTrue(a.liveScreen.mic.getText().toString().contains("MIC ON"));assertTrue(a.liveScreen.route.getText().toString().contains("safety mute"));assertTrue(a.liveScreen.pitch.getText().toString().contains("A"));assertTrue(a.liveScreen.backing.getText().toString().contains("Backing"));assertTrue(a.liveScreen.meter.getProgress()>0);assertTrue(a.liveScreen.clock.getText().toString().contains("00:12"));
  stats[0]=0;stats[3]=0;stats[16]=0;stats[17]=0;RecordingManager.active=false;a.liveScreen.poll(stats,false);assertTrue(a.liveScreen.mic.getText().toString().contains("READY"));assertEquals(0,a.liveScreen.meter.getProgress());assertTrue(a.liveScreen.backing.getText().toString().contains("idle"));
 }
 @Test public void presetAndSafetyMuteKeepRecordingAndMicrophoneActive(){
  AudioService.active=true;RecordingManager.active=true;float[] stats=new float[22];stats[10]=1;stats[17]=1;StudioScreenshotTest.Audio.supplied=stats;StudioScreenshotTest.Audio.changes.clear();a.monitorButton.performClick();assertTrue(AudioService.active);assertTrue(RecordingManager.active);for(float[] change:StudioScreenshotTest.Audio.changes)assertNotEquals(11,(int)change[0]);
  a.preset(1);assertTrue(AudioService.active);assertTrue(RecordingManager.active);assertEquals(.3,a.values[2],.001);assertTrue(a.liveScreen.knobs[0].getContentDescription().toString().contains("30%"));
 }

 @Test public void knobDragThroughScrollViewAndBackgroundScrollBothWork(){
  List<Float> changes=new ArrayList<>();ScrollView scroll=new ScrollView(a);LinearLayout column=new LinearLayout(a);column.setOrientation(LinearLayout.VERTICAL);scroll.addView(column);StudioKnob knob=new StudioKnob(a,"Test",0,1,.5f,v->Math.round(v*100)+"%",changes::add);column.addView(knob,new LinearLayout.LayoutParams(200,200));column.addView(new android.view.View(a),new LinearLayout.LayoutParams(200,500));scroll.measure(View.MeasureSpec.makeMeasureSpec(200,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY));scroll.layout(0,0,200,300);
  scroll.dispatchTouchEvent(MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,100,140,0));scroll.dispatchTouchEvent(MotionEvent.obtain(0,10,MotionEvent.ACTION_MOVE,100,70,0));scroll.dispatchTouchEvent(MotionEvent.obtain(0,20,MotionEvent.ACTION_UP,100,70,0));assertFalse("scroll parent must not steal knob drag",changes.isEmpty());assertEquals(0,scroll.getScrollY());
  scroll.dispatchTouchEvent(MotionEvent.obtain(50,50,MotionEvent.ACTION_DOWN,100,280,0));scroll.dispatchTouchEvent(MotionEvent.obtain(50,60,MotionEvent.ACTION_MOVE,100,170,0));scroll.dispatchTouchEvent(MotionEvent.obtain(50,70,MotionEvent.ACTION_UP,100,170,0));assertTrue("page remains scrollable outside knobs",scroll.getScrollY()>0);
 }

}
