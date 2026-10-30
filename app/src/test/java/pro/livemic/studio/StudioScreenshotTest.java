package pro.livemic.studio;

import android.graphics.*;
import android.app.Dialog;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;

/** Real Android layout and native Skia rendering. Audio hardware alone is shadowed.
 * Fixtures are isolated test data and are never created by the installed application. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, instrumentedPackages={"pro.livemic.studio"},shadows=StudioScreenshotTest.Audio.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
public class StudioScreenshotTest {
 @org.robolectric.annotation.internal.DoNotInstrument
 @Implements(value=NativeAudio.class,isInAndroidSdk=false)
 public static class Audio {
  static double lastSeek=-1;
  @Implementation protected static void __staticInitializer__(){}
  @Implementation protected static void set(int k,float v){}
  @Implementation protected static float[] stats(){float[] f=new float[22];f[2]=-1;f[6]=48000;return f;}
  @Implementation protected static float[] roll(){return new float[0];}
  @Implementation protected static double[] localPlaybackInfo(){return new double[]{0,6};}
  @Implementation protected static void seekLocal(double seconds){lastSeek=seconds;}
  @Implementation protected static boolean loadLocalFile(String p,int r,int c){return true;}
  @Implementation protected static void guideTone(float f){}
  @Implementation protected static void stop(){}
 }
 @Test public void renderStudioScreens()throws Exception {
  File out=new File(System.getProperty("user.dir"),"build/studio-screenshots");out.mkdirs();
  for(int width:new int[]{360,412}){
   int physicalHeight=width==360?800:915,height=physicalHeight-72;
   RuntimeEnvironment.setQualifiers("w"+width+"dp-h"+height+"dp-mdpi");
   MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();a.handler.removeCallbacks(a.tick);a.activityVisible=false;
   assertEquals("Bottom navigation stays within three items",3,a.nav.getChildCount());
   seed(a);a.selectedFile="보컬 연습 01.wav";a.preset(1);a.set(0,.8f);a.handler.removeCallbacks(a.tick);
   a.getResources().getDisplayMetrics().density=1;a.getResources().getDisplayMetrics().scaledDensity=1;a.getResources().getDisplayMetrics().widthPixels=width;a.getResources().getDisplayMetrics().heightPixels=height;
   for(int page:new int[]{0,1,4,5}){
    a.tab=page;a.showTab();a.transport.setVisibility(View.GONE);a.tick.run();a.handler.removeCallbacks(a.tick);a.io.submit(()->{}).get();ShadowLooper.idleMainLooper();a.handler.removeCallbacks(a.tick);
    capture(a,width,height,new File(out,width+"-"+page+"-idle.png"),false);
    if(page==0){
     try{RecordingManager.active=true;a.tick.run();a.handler.removeCallbacks(a.tick);capture(a,width,height,new File(out,width+"-0-recording-fixture.png"),false);assertTrue(a.recButton.getText().toString().contains("저장"));assertFalse(a.recordOptions.isEnabled());}
     finally{RecordingManager.active=false;a.tick.run();a.handler.removeCallbacks(a.tick);}
     a.micButton.setSelected(true);a.micButton.setText("마이크 끄기");a.liveState.setText("● 마이크 켜짐");a.liveState.setTextColor(StudioTheme.ACCENT);a.visual.stats=new float[22];a.visual.stats[10]=1;for(int i=0;i<64;i++)a.visual.sampleInput((float)(.0001+.045*Math.pow(.5+.5*Math.sin(i*.43+.25*Math.sin(i*.15)),2)*(.3+.7*Math.abs(Math.sin(i*1.7)))));a.visual.invalidate();capture(a,width,height,new File(out,width+"-0-fixture.png"),false);a.drawer();capture(a,width,height,new File(out,width+"-drawer.png"),true);a.drawerPopup.dismiss();}
   }
  // Verify the drawn fader retains a real accessible adjustment action.
  a.tab=0;a.showTab();StudioFader f=null;for(int i=0;i<a.content.getChildCount();i++)if(a.content.getChildAt(i) instanceof StudioFader){f=(StudioFader)a.content.getChildAt(i);break;}
  assertNotNull(f);Bundle args=new Bundle();args.putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,.65f);assertTrue(f.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId(),args));assertEquals(.65f,a.values[0],.001f);
  assertEquals(18,StudioMidiPreview.read(a.store.file("render_fixture_1",".mid")).length);
  a.tab=1;a.showTab();assertNotNull(a.playerLine);Bundle seekArgs=new Bundle();seekArgs.putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,.3f);assertTrue(a.playerLine.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId(),seekArgs));assertEquals(1.8,Audio.lastSeek,.001);a.playerLine.setEnabled(false);seekArgs.putFloat(android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,.8f);assertFalse(a.playerLine.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId(),seekArgs));assertEquals(1.8,Audio.lastSeek,.001);
  a.io.shutdown();
  }
 }
 @Test public void selectedOptionAndChoiceRemainAccessible(){
  MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();a.handler.removeCallbacks(a.tick);
  int[] chosen={-1};StudioDialogs.choose(a,"보컬 프리셋",new String[]{"노래방","발라드"},1,i->chosen[0]=i);
  Dialog dialog=ShadowDialog.getLatestDialog();assertNotNull(dialog);Button selected=findButton(dialog.getWindow().getDecorView(),"✓  발라드");assertNotNull(selected);assertTrue(selected.isSelected());assertTrue(selected.getContentDescription().toString().contains("선택됨"));
  Button first=findButton(dialog.getWindow().getDecorView(),"노래방");assertNotNull(first);first.performClick();assertEquals(0,chosen[0]);assertFalse(dialog.isShowing());
  a.drawer();View practice=findDescription(a.drawerPopup.getContentView(),"보컬 연습,");assertNotNull(practice);practice.performClick();assertEquals(5,a.tab);assertNull(a.drawerPopup);assertEquals(3,a.nav.getChildCount());assertTrue(a.nav.getChildAt(0).isSelected());a.io.shutdown();
 }
 @Test @Config(sdk=35) public void galaxyWindowKeepsPrimaryActionsAboveSystemBars()throws Exception {
  File out=new File(System.getProperty("user.dir"),"build/studio-screenshots");out.mkdirs();
  for(float fontScale:new float[]{1f,1.3f})for(int navigationBottom:new int[]{24,48}){
   RuntimeEnvironment.setQualifiers("w412dp-h893dp-mdpi");RuntimeEnvironment.setFontScale(fontScale);
   MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();a.handler.removeCallbacks(a.tick);a.activityVisible=false;
   android.view.WindowInsets insets=new android.view.WindowInsets.Builder().setInsets(android.view.WindowInsets.Type.statusBars(),android.graphics.Insets.of(0,36,0,0)).setInsets(android.view.WindowInsets.Type.navigationBars(),android.graphics.Insets.of(0,0,0,navigationBottom)).build();a.root.dispatchApplyWindowInsets(insets);
   a.tick.run();a.handler.removeCallbacks(a.tick);a.tick.run();a.handler.removeCallbacks(a.tick);
   String prefix="s26u-"+(fontScale==1?"default":"large")+(navigationBottom==48?"-buttons":"");
   capture(a,412,893,new File(out,prefix+"-live.png"),false);
   LinearLayout header=(LinearLayout)a.root.getChildAt(0);assertTrue("Preset must retain a tappable width",a.presetChip.getWidth()>=48);assertTrue("Emergency output control must retain a tappable width",header.getChildAt(header.getChildCount()-1).getWidth()>=48);
   Rect record=bounds(a,a.recButton);assertTrue(record.top>=a.pageScroll.getBottom());assertTrue(record.bottom<=a.nav.getTop());assertTrue(a.nav.getBottom()<=893-navigationBottom);assertTrue(a.root.getPaddingTop()>=36);assertEquals(3,a.nav.getChildCount());
   a.pageScroll.scrollTo(0,a.page.getHeight());assertEquals(record,bounds(a,a.recButton));a.pageScroll.scrollTo(0,0);a.drawer();assertTrue(a.drawerTop+a.drawerPopup.getHeight()<=893-navigationBottom);capture(a,412,893,new File(out,prefix+"-drawer.png"),true);a.drawerPopup.dismiss();
   try{RecordingManager.active=true;a.tick.run();a.handler.removeCallbacks(a.tick);capture(a,412,893,new File(out,prefix+"-recording.png"),false);assertEquals(record,bounds(a,a.recButton));}
   finally{RecordingManager.active=false;a.tick.run();a.handler.removeCallbacks(a.tick);}
   a.chooseTab(5);capture(a,412,893,new File(out,prefix+"-practice.png"),false);Rect stop=bounds(a,a.practiceButton);a.pageScroll.scrollTo(0,a.page.getHeight());assertEquals(stop,bounds(a,a.practiceButton));assertTrue(stop.top>=a.pageScroll.getBottom());assertTrue(stop.bottom<=a.nav.getTop());
   a.chooseTab(0);android.view.WindowInsets keyboard=new android.view.WindowInsets.Builder(insets).setInsets(android.view.WindowInsets.Type.ime(),android.graphics.Insets.of(0,0,0,300)).build();a.root.dispatchApplyWindowInsets(keyboard);capture(a,412,893,new File(out,prefix+"-keyboard.png"),false);assertTrue(a.nav.getBottom()<=893-300);assertTrue(bounds(a,a.recButton).bottom<=a.nav.getTop());
   a.io.shutdown();
  }
  RuntimeEnvironment.setFontScale(1f);
 }
 @Test @Config(sdk=35) public void cameraControlsStayReachableWhenPreviewScrolls()throws Exception {
  RuntimeEnvironment.setQualifiers("w412dp-h893dp-mdpi");RuntimeEnvironment.setFontScale(1.3f);
  org.robolectric.Shadows.shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(android.Manifest.permission.CAMERA);
  MainActivity a=Robolectric.buildActivity(MainActivity.class).setup().get();a.handler.removeCallbacks(a.tick);a.activityVisible=false;
  a.root.dispatchApplyWindowInsets(new WindowInsets.Builder().setInsets(WindowInsets.Type.systemBars(),android.graphics.Insets.of(0,36,0,24)).build());
  a.chooseTab(3);
  try{
   assertNotNull(a.videoButton);File out=new File(System.getProperty("user.dir"),"build/studio-screenshots");capture(a,412,893,new File(out,"s26u-camera-layout.png"),false);
   Rect record=bounds(a,a.videoButton);a.pageScroll.scrollTo(0,a.page.getHeight());assertEquals(record,bounds(a,a.videoButton));assertTrue(record.top>=a.pageScroll.getBottom());assertTrue(record.bottom<=a.nav.getTop());
  }finally{a.chooseTab(0);a.io.shutdown();RuntimeEnvironment.setFontScale(1f);}
 }
 static Rect bounds(MainActivity a,View view){Rect b=new Rect(0,0,view.getWidth(),view.getHeight());a.root.offsetDescendantRectToMyCoords(view,b);return b;}
 static View findDescription(View view,String prefix){if(view.getContentDescription()!=null&&view.getContentDescription().toString().startsWith(prefix))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View match=findDescription(group.getChildAt(i),prefix);if(match!=null)return match;}}return null;}
 static Button findButton(View view,String label){if(view instanceof Button&&label.contentEquals(((Button)view).getText()))return (Button)view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button found=findButton(group.getChildAt(i),label);if(found!=null)return found;}}return null;}
 static void capture(MainActivity a,int w,int h,File output,boolean drawer)throws Exception {
  redraw(a.root);a.root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));a.root.layout(0,0,w,h);
  if(a.tab==0){Rect bounds=new Rect(0,0,a.recButton.getWidth(),a.recButton.getHeight());a.root.offsetDescendantRectToMyCoords(a.recButton,bounds);System.out.println("LAYOUT "+output.getName()+" record="+bounds.bottom+" nav="+a.nav.getTop());assertTrue("Recording button must remain above navigation",bounds.bottom<=a.nav.getTop());}
  Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);a.root.draw(canvas);
  if(drawer){View panel=a.drawerPopup.getContentView();panel.measure(View.MeasureSpec.makeMeasureSpec(a.drawerPopup.getWidth(),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(a.drawerPopup.getHeight(),View.MeasureSpec.EXACTLY));panel.layout(0,0,panel.getMeasuredWidth(),panel.getMeasuredHeight());canvas.save();canvas.translate(a.drawerLeft,a.drawerTop);panel.draw(canvas);canvas.restore();}
  try(FileOutputStream file=new FileOutputStream(output)){bitmap.compress(Bitmap.CompressFormat.PNG,100,file);}bitmap.recycle();
 }
 static void redraw(View v){v.forceLayout();v.invalidate();if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++)redraw(group.getChildAt(i));}}
 static void seed(MainActivity a)throws Exception {
  File dir=new File(a.getCacheDir(),"render-fixtures");dir.mkdirs();String[] titles={"보컬 연습 01.wav","발라드 연습.wav","내 반주.wav"};byte[] pcm=pcm();
  for(int i=2;i>=0;i--){File f=new File(dir,titles[i]);wav(f,pcm);a.recent.remember(android.net.Uri.fromFile(f),titles[i]);}
  org.json.JSONArray history=new org.json.JSONArray();for(org.json.JSONObject item:a.recent.list()){item.put("time",1793355840000L);history.put(item);}a.getSharedPreferences("backing-history",0).edit().putString("items",history.toString()).commit();
  for(int i=0;i<3;i++){String id="render_fixture_"+i;wav(a.store.file(id,".wav"),pcm);a.store.save(id,new String[]{"오늘의 연습","멜로디 스케치","보컬 트레이닝"}[i],6,false,"");org.json.JSONObject info=a.store.read(id);info.put("created",1793355840000L-i*86400000L);if(i==1)info.put("practice",new JSONObject().put("accuracy",92).put("coverage",86).put("meanAbsoluteCents",12).put("complete",true));a.store.update(id,info);try(FileOutputStream mid=new FileOutputStream(a.store.file(id,".mid"))){mid.write(new byte[]{77,84,104,100,0,0,0,6,0,0,0,1,1,(byte)224,77,84,114,107,0,0,0,58,0,(byte)144,60,90,(byte)0x83,0x60,(byte)128,60,0,0,(byte)144,64,90,(byte)0x83,0x60,(byte)128,64,0,0,(byte)144,67,90,(byte)0x83,0x60,(byte)128,67,0,0,(byte)144,65,90,(byte)0x83,0x60,(byte)128,65,0,0,(byte)144,62,90,(byte)0x83,0x60,(byte)128,62,0,0,(byte)144,60,90,(byte)0x83,0x60,(byte)128,60,0,0,(byte)255,47,0});}}
 }
 static byte[] pcm(){ByteBuffer b=ByteBuffer.allocate(48000*6*4).order(ByteOrder.LITTLE_ENDIAN);for(int n=0;n<48000*6;n++){double x=(double)n/48000;short v=(short)(Math.sin(x*2*Math.PI*220)*(.06+.28*Math.pow(Math.sin(x*2.7),4))*32767);b.putShort(v);b.putShort(v);}return b.array();}
 static void wav(File f,byte[] pcm)throws Exception {ByteBuffer b=ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);b.put("RIFF".getBytes("US-ASCII")).putInt(pcm.length+36).put("WAVEfmt ".getBytes("US-ASCII")).putInt(16).putShort((short)1).putShort((short)2).putInt(48000).putInt(192000).putShort((short)4).putShort((short)16).put("data".getBytes("US-ASCII")).putInt(pcm.length);try(FileOutputStream out=new FileOutputStream(f)){out.write(b.array());out.write(pcm);}}
}
