package pro.livemic.studio;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Live Vault native UI and studio controls. */
public class MainActivity extends Activity {
    static final int BG=StudioTheme.BG,PANEL=StudioTheme.SURFACE,RAISED=StudioTheme.RAISED,TEXT=StudioTheme.TEXT,MUTED=StudioTheme.MUTED,LIME=StudioTheme.ACCENT,RED=StudioTheme.DANGER;
    final Handler handler=new Handler(Looper.getMainLooper());final ExecutorService io=Executors.newSingleThreadExecutor();
    final float[] values={1,.55f,.27f,2.2f,.14f,280,.25f,0,0,1,1,0,0,0,0,78,0,0,0,1};
    StudioRootLayout root;StudioPageLayout page;StudioScrollView pageScroll;LinearLayout actionDock;
    LinearLayout content,nav;Button practiceButton,micButton,monitorButton,recButton,videoButton,pauseButton,flipButton,recordOptions;TextView status,pitch,timer,cameraTimer,levelText;StudioVisual visual;
    int sourceTab=1,genre=0,selectedPreset=-1,pendingAction=0; boolean effectsExpanded=false,videoPaused=false,flipResumeAudio=false,cameraFailed=false,awaitingMicPermission=false;
    TextView pitchDetails;PitchMeter pitchMeter;
    ComposerSession composer;ComposerScreen composerScreen;HarmonySession harmony;HarmonyScreen harmonyScreen;int lastHarmonyRevision=-1;int libraryFilter=0,lastComposerRevision=-1;
    RecentBackings recent;String selectedFile="";long actionDeadline=0;
    int recordDelay=0,lastRecordingRevision=-1,lastRouteRevision=-1;boolean captureOtherApps=false,recordPermissionPending=false;float captureGain=.7f;String videoTakeId=null;boolean activityVisible=false;
    Button presetChip,outputButton,globalRecord,globalOptions;TextView globalTimer,liveState,playerTime,recordCaption;StudioPlaybackLine playerLine;
    PopupWindow drawerPopup;int drawerLeft,drawerTop;
    LinearLayout transport;TextView trainingReadout;PracticePlot practicePlot;boolean practiceOwnsMic=false;boolean practiceRecord=false;int practiceRoot=48,practiceBpm=60,practiceMode=0,practicePattern=0;
    int tab=0,inputDevice=0;boolean starting=false,busy=false,wasRunning=false,headphones=false,pendingMonitor=false,videoPreparing=false;
    CameraController camera;TextureView texture;File rawVideo,exportFile,playingFile;TakeStore store;MediaPlayer player;
    final Map<String,StudioButton> takePlayButtons=new HashMap<>();final Map<String,TextView> takePlaybackStatus=new HashMap<>();
    @Override public void onCreate(Bundle state){super.onCreate(state);composer=ComposerSession.get(this);composerScreen=new ComposerScreen(this,composer);harmony=HarmonySession.get(this);harmonyScreen=new HarmonyScreen(this,harmony);store=new TakeStore(this);recent=new RecentBackings(this);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        selectedPreset=getPreferences(0).getInt("preset",-1);recordDelay=getPreferences(0).getInt("recordDelay",0);captureOtherApps=BuildConfig.PLAYBACK_CAPTURE&&getPreferences(0).getBoolean("captureOtherApps",false);captureGain=getPreferences(0).getFloat("captureGain",.7f);
        for(int i=0;i<values.length;i++)if(i!=11&&i!=13&&i!=12)values[i]=getPreferences(0).getFloat("p"+i,values[i]);
        sourceTab=getPreferences(0).getInt("backingSource",1);if(!AudioService.active)for(int i=0;i<values.length;i++)NativeAudio.set(i,values[i]);build();handler.post(tick);
    }
    int dp(float n){return(int)(getResources().getDisplayMetrics().density*n+.5f);}
    GradientDrawable bg(int c,int r){return StudioTheme.background(this,c,r);}
    TextView text(String t,int size,int color){return StudioTheme.text(this,t,size,color);}
    TextView title(String t){TextView v=text(t,26,TEXT);v.setTypeface(StudioTheme.bold(this));v.setPadding(0,dp(6),0,dp(12));return v;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    Button button(String t,Runnable r){return StudioTheme.button(this,t,r);}
    void add(View v){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);content.addView(v,lp);}
    void gap(){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,dp(4)));}
    void note(String t){TextView v=text(t,12,MUTED);v.setPadding(0,dp(4),0,dp(4));add(v);}
    LinearLayout beginCard(){LinearLayout previous=content;LinearLayout card=StudioTheme.card(this);add(card);content=card;return previous;}
    void endCard(LinearLayout previous){content=previous;}
    void weighted(LinearLayout line,View view,int height){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(height),1);if(line.getChildCount()>0)lp.leftMargin=dp(8);line.addView(view,lp);}
    void build(){root=new StudioRootLayout(this);root.setBackgroundColor(BG);
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());int bottom=Math.max(i.bottom,insets.getInsets(WindowInsets.Type.ime()).bottom);root.safeInsets(i.left,i.top,i.right,bottom);}
            else {int left=insets.getSystemWindowInsetLeft(),top=insets.getSystemWindowInsetTop(),right=insets.getSystemWindowInsetRight(),bottom=insets.getSystemWindowInsetBottom();android.view.DisplayCutout cutout=insets.getDisplayCutout();if(cutout!=null){left=Math.max(left,cutout.getSafeInsetLeft());top=Math.max(top,cutout.getSafeInsetTop());right=Math.max(right,cutout.getSafeInsetRight());bottom=Math.max(bottom,cutout.getSafeInsetBottom());}root.safeInsets(left,top,right,bottom);}return insets;
        });setContentView(root);root.requestApplyInsets();
        LinearLayout header=row();header.setMinimumHeight(dp(52));header.addView(StudioTheme.icon(this,"menu","전체 메뉴",this::drawer),new LinearLayout.LayoutParams(dp(48),dp(48)));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.live_vault_mark);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setContentDescription("승인된 Live Vault 원호 마이크 로고");header.addView(logo,new LinearLayout.LayoutParams(dp(30),dp(30)));
        TextView brand=text("Live Vault",19,TEXT);brand.setTypeface(StudioTheme.bold(this));brand.setSingleLine();brand.setEllipsize(android.text.TextUtils.TruncateAt.END);brand.setPadding(dp(8),0,0,0);brand.setGravity(Gravity.CENTER_VERTICAL);header.addView(brand,new LinearLayout.LayoutParams(0,dp(52),1));
        StudioButton mute=StudioTheme.styled(this,"출력 끄기",StudioButton.QUIET,()->{set(11,0);toast("내 목소리 출력만 껐습니다. 녹음은 계속됩니다.");});mute.glyph("mute");mute.setTextSize(11);mute.setMaxLines(1);mute.setEllipsize(android.text.TextUtils.TruncateAt.END);mute.setPadding(dp(8),0,dp(8),0);header.addView(mute,new LinearLayout.LayoutParams(-2,dp(48)));root.addView(header);
        pageScroll=new StudioScrollView(this);page=new StudioPageLayout(this);page.setPadding(0,dp(8),0,dp(8));content=page;pageScroll.addView(page);root.addView(pageScroll,new LinearLayout.LayoutParams(-1,0,1));
        actionDock=column();actionDock.setPadding(0,dp(8),0,dp(8));actionDock.setVisibility(View.GONE);root.addView(actionDock,new LinearLayout.LayoutParams(-1,-2));
        transport=row();transport.setPadding(dp(12),dp(10),dp(12),dp(10));transport.setBackground(bg(PANEL,14));recButton=button("●  오디오 녹음",this::toggleRecord);transport.addView(recButton,new LinearLayout.LayoutParams(0,dp(52),1));recordOptions=button("⋯",this::recordingOptions);recordOptions.setTextSize(22);recordOptions.setContentDescription("녹음 시작 설정 · 즉시, 3초, 5초, 10초");transport.addView(recordOptions,new LinearLayout.LayoutParams(dp(48),dp(52)));LinearLayout clock=column();clock.setPadding(dp(12),0,0,0);timer=text("00:00.0",16,TEXT);timer.setTypeface(StudioTheme.mono(this));clock.addView(timer);clock.addView(text("WAV + MIDI",10,MUTED));transport.addView(clock);transport.setVisibility(View.GONE);root.addView(transport,new LinearLayout.LayoutParams(-1,dp(76)));
        globalRecord=recButton;globalOptions=recordOptions;globalTimer=timer;
        nav=row();nav.setPadding(dp(4),dp(2),dp(4),0);nav.setBackground(bg(PANEL,14));String[] labels={"라이브","반주","보관함"},icons={"mic","music","folder"};for(int i=0;i<labels.length;i++){final int k=new int[]{0,1,4}[i];StudioNavButton button=new StudioNavButton(this,labels[i],icons[i],()->chooseTab(k));nav.addView(button,new LinearLayout.LayoutParams(0,dp(60),1));}LinearLayout.LayoutParams navParams=new LinearLayout.LayoutParams(-1,dp(62));navParams.topMargin=dp(4);navParams.bottomMargin=dp(4);root.addView(nav,navParams);showTab();
    }
    void chooseTab(int k){if(testActive)finishSyncTest(false);if(AudioService.practice!=null&&AudioService.practice.active)AudioService.practice.stop(false);pendingAction=0;if(videoPreparing){toast("카메라를 준비하고 있습니다.");return;}if(camera!=null&&camera.recording){new AlertDialog.Builder(this).setTitle("녹화를 마칠까요?").setMessage("영상을 저장한 뒤 화면을 이동합니다.").setPositiveButton("저장 후 이동",(d,w)->{stopVideo();tab=k;showTab();}).setNegativeButton("계속 녹화",null).show();return;}tab=k;showTab();}
    void showTab(){setVolumeControlStream(AudioRouting.selected(this)==1?AudioManager.STREAM_VOICE_CALL:AudioManager.STREAM_MUSIC);if(camera!=null&&camera.recording)return;if(camera!=null){camera.release();camera=null;}recButton=globalRecord;recordOptions=globalOptions;timer=globalTimer;playerTime=null;playerLine=null;liveState=null;recordCaption=null;presetChip=null;outputButton=null;takePlayButtons.clear();takePlaybackStatus.clear();content=page;page.flexible(null,0,0);content.removeAllViews();actionDock.removeAllViews();actionDock.setVisibility(tab==0||tab==3||tab==5||tab==7||tab==8||tab==9?View.VISIBLE:View.GONE);pageScroll.scrollTo(0,0);visual=null;pitch=null;pitchDetails=null;pitchMeter=null;texture=null;micButton=null;monitorButton=null;status=null;cameraTimer=null;videoButton=null;pauseButton=null;flipButton=null;levelText=null;
        int selected=tab==1||tab==7||tab==8||tab==9?1:tab==4?2:0;for(int i=0;i<nav.getChildCount();i++)nav.getChildAt(i).setSelected(i==selected);

        if(tab==0)studio();else if(tab==1)backing();else if(tab==2)midi();else if(tab==3)cameraPage();else if(tab==5)practicePage();else if(tab==6)audioSettings();else if(tab==7)composerScreen.draw(false);else if(tab==8)composerScreen.draw(true);else if(tab==9)harmonyScreen.draw();else library();updateTransportVisibility();
    }
    void set(int k,float v){if((k>=2&&k<=10)||k==18){selectedPreset=-1;getPreferences(0).edit().putInt("preset",-1).apply();if(presetChip!=null)presetChip.setText("사용자 설정 ▾");}values[k]=v;NativeAudio.set(k,v);if(k==11){if(v==0)NativeComposer.monitor(false);harmony.monitor(v>0);}if(k!=11&&k!=12&&k!=13)getPreferences(0).edit().putFloat("p"+k,v).apply();}
    interface Formatter{String value(float v);}
    void slider(String label,int k,float min,float max,Formatter f){StudioFader control=new StudioFader(this,label,min,max,values[k],f,v->set(k,v));content.addView(control,new LinearLayout.LayoutParams(-1,-2));}
    void toggle(String label,int k){Switch s=new Switch(this);s.setText(label);s.setTextColor(TEXT);s.setTextSize(14);s.setPadding(0,dp(10),0,dp(10));s.setChecked(values[k]>.5);s.setOnCheckedChangeListener((b,on)->{if(k==11&&on&&!AudioService.canMonitor(this)){s.setChecked(false);toast("이어폰을 연결하거나 출력 모드에서 스피커를 선택하세요.");return;}set(k,on?1:0);});add(s);}
    String presetName(){return selectedPreset>=0?new String[]{"노래방","발라드","콘서트 홀","팝 보컬","대성당","생목소리"}[selectedPreset]:"사용자 설정";}
    void presetPicker(){StudioDialogs.choose(this,"보컬 프리셋",new String[]{"노래방","발라드","콘서트 홀","팝 보컬","대성당","생목소리"},selectedPreset,this::preset);}
    void studio(){
        LinearLayout intro=row();TextView headline=text("내 목소리",22,TEXT);headline.setTypeface(StudioTheme.bold(this));headline.setSingleLine();intro.addView(headline,new LinearLayout.LayoutParams(0,-2,1));
        StudioButton preset=StudioTheme.styled(this,presetName()+" ▾",StudioButton.QUIET,this::presetPicker);presetChip=preset;preset.setTextColor(LIME);preset.setTextSize(12);preset.setMaxLines(1);preset.setEllipsize(android.text.TextUtils.TruncateAt.END);intro.addView(preset,new LinearLayout.LayoutParams(-2,dp(48)));add(intro);
        LinearLayout chips=row();micButton=button("마이크 켜기",this::requestMicToggle);weighted(chips,micButton,52);
        StudioButton route=StudioTheme.styled(this,outputName()+" ▾",StudioButton.STANDARD,this::monitoringOptions);outputButton=route;route.glyph("headphones");weighted(chips,route,52);add(chips);
        LinearLayout previous=beginCard();LinearLayout caption=row();caption.addView(text("입력 레벨",13,MUTED),new LinearLayout.LayoutParams(0,-2,1));
        liveState=text("마이크 대기",11,MUTED);caption.addView(liveState);levelText=text("— dB",12,MUTED);levelText.setTypeface(StudioTheme.mono(this));levelText.setPadding(dp(8),0,0,0);caption.addView(levelText);add(caption);
        visual=new StudioVisual(this,false);content.addView(visual,new LinearLayout.LayoutParams(-1,dp(64)));page.flexible(visual,64,168);endCard(previous);
        status=text("",12,MUTED);status.setMinHeight(dp(24));status.setVisibility(View.GONE);status.setOnClickListener(v->monitoringOptions());add(status);
        slider("목소리 크기",0,0,2,v->Math.round(v*100)+"%");slider("리버브",2,0,1,v->Math.round(v*100)+"%");
        monitorButton=button("내 목소리 듣기 · 꺼짐",()->{if(NativeAudio.stats()[17]>.5){monitoringOptions();return;}if(AudioService.active&&AudioService.canMonitor(this)){set(11,values[11]>.5?0:1);}else toast("마이크와 출력 장치를 먼저 켜주세요.");});add(monitorButton);
        add(button("보컬 화음 쌓기 ›",()->chooseTab(9)));StudioButton backing=StudioTheme.styled(this,selectedFile.isEmpty()?"반주 선택":selectedFile,StudioButton.STANDARD,()->chooseTab(1));backing.glyph("music");backing.trailing("arrow");backing.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);backing.setSingleLine();backing.setEllipsize(android.text.TextUtils.TruncateAt.END);add(backing);
        content=actionDock;addModeSelector(false);
        LinearLayout captionRow=row();recordCaption=text("오디오 녹음",12,MUTED);captionRow.addView(recordCaption,new LinearLayout.LayoutParams(0,-2,1));timer=text("",13,RED);timer.setTypeface(StudioTheme.mono(this));captionRow.addView(timer);actionDock.addView(captionRow,new LinearLayout.LayoutParams(-1,dp(24)));
        LinearLayout recordRow=row();recButton=StudioTheme.styled(this,"● 녹음 시작",StudioButton.STANDARD,this::toggleRecord);recButton.setTextColor(BG);recButton.setBackgroundTintList(ColorStateList.valueOf(LIME));recButton.setMinHeight(dp(56));recButton.setPadding(dp(16),dp(12),dp(16),dp(12));recordRow.addView(recButton,new LinearLayout.LayoutParams(0,-2,1));
        recordOptions=button("⋯",this::recordingOptions);recordOptions.setTextSize(22);recordOptions.setContentDescription("녹음 시작 설정 · 즉시, 3초, 5초, 10초");LinearLayout.LayoutParams options=new LinearLayout.LayoutParams(dp(52),dp(56));options.leftMargin=dp(8);recordRow.addView(recordOptions,options);actionDock.addView(recordRow);content=page;
    }
    void addModeSelector(boolean video){LinearLayout media=row();media.setPadding(dp(3),dp(3),dp(3),dp(3));media.setBackground(bg(RAISED,12));StudioButton audio=StudioTheme.styled(this,"오디오",video?StudioButton.QUIET:StudioButton.STANDARD,()->chooseTab(0));audio.glyph("music");audio.setTextColor(video?MUTED:LIME);StudioButton film=StudioTheme.styled(this,"영상",video?StudioButton.STANDARD:StudioButton.QUIET,()->chooseTab(3));film.glyph("camera");film.setTextColor(video?LIME:MUTED);media.addView(audio,new LinearLayout.LayoutParams(0,dp(48),1));media.addView(film,new LinearLayout.LayoutParams(0,dp(48),1));add(media);}
    void drawer(){
        LinearLayout panel=column();panel.setPadding(dp(18),dp(20),dp(18),dp(24));panel.setBackground(bg(PANEL,18));int availableHeight=root.getHeight()-root.getPaddingTop()-root.getPaddingBottom();int h=Math.max(dp(160),availableHeight-dp(64));ScrollView menuScroll=new ScrollView(this);menuScroll.setFillViewport(true);menuScroll.setVerticalScrollBarEnabled(false);menuScroll.addView(panel);PopupWindow popup=new PopupWindow(menuScroll,Math.min(dp(328),root.getWidth()-root.getPaddingLeft()-root.getPaddingRight()),h,true);popup.setBackgroundDrawable(bg(PANEL,18));popup.setElevation(dp(24));popup.setOutsideTouchable(true);drawerPopup=popup;root.setForeground(new android.graphics.drawable.ColorDrawable(0x88000000));popup.setOnDismissListener(()->{root.setForeground(null);drawerPopup=null;});
        LinearLayout close=row();close.setGravity(Gravity.END);StudioButton emergency=StudioTheme.styled(this,"출력 끄기",StudioButton.QUIET,()->{set(11,0);toast("내 목소리 출력만 껐습니다. 녹음은 계속됩니다.");});emergency.glyph("mute");emergency.setTextSize(11);emergency.setMaxLines(1);close.addView(emergency,new LinearLayout.LayoutParams(-2,dp(48)));close.addView(new Space(this),new LinearLayout.LayoutParams(0,1,1));close.addView(StudioTheme.icon(this,"close","메뉴 닫기",popup::dismiss),new LinearLayout.LayoutParams(dp(48),dp(48)));panel.addView(close);
        ImageView mark=new ImageView(this);mark.setImageResource(R.drawable.live_vault_mark);mark.setScaleType(ImageView.ScaleType.FIT_CENTER);LinearLayout.LayoutParams markParams=new LinearLayout.LayoutParams(dp(96),dp(96));markParams.gravity=Gravity.CENTER;markParams.topMargin=dp(4);panel.addView(mark,markParams);TextView name=text("Live Vault",24,TEXT);name.setTypeface(StudioTheme.bold(this));name.setGravity(Gravity.CENTER);panel.addView(name,new LinearLayout.LayoutParams(-1,dp(48)));
        String[] names={"오디오 설정","녹음 설정","보컬 연습","도움말"},subtitles={"출력 · 레이턴시 · 상세 효과","예약 녹음 · 저장 옵션","스케일 · 피치 · MIDI","사용 방법 · 앱 정보"},glyphs={"settings","record","piano","help"};
        for(int i=0;i<4;i++){final int k=i;LinearLayout item=row();item.setPadding(dp(8),dp(14),dp(8),dp(14));ImageView icon=new ImageView(this);icon.setImageDrawable(new StudioGlyph(glyphs[i],i==1?RED:0xffbcc5d4));item.addView(icon,new LinearLayout.LayoutParams(dp(30),dp(30)));LinearLayout copy=column();copy.setPadding(dp(16),0,dp(8),0);TextView title=text(names[i],17,TEXT);title.setTypeface(StudioTheme.bold(this));copy.addView(title);TextView detail=text(subtitles[i],12,MUTED);detail.setPadding(0,dp(4),0,0);copy.addView(detail);item.addView(copy,new LinearLayout.LayoutParams(0,-2,1));ImageView arrow=new ImageView(this);arrow.setImageDrawable(new StudioGlyph("arrow",MUTED));item.addView(arrow,new LinearLayout.LayoutParams(dp(16),dp(16)));item.setFocusable(true);item.setContentDescription(names[i]+", "+subtitles[i]);item.setOnClickListener(v->{popup.dismiss();if(k==0)chooseTab(6);else if(k==1)recordingOptions();else if(k==2)chooseTab(5);else new AlertDialog.Builder(this).setTitle("Live Vault").setMessage("효과와 함께 듣고, 녹음하고, 음정을 연습하세요.\n\n스피커는 낮은 출력에서 시작하세요. 블루투스 전송 지연은 기기마다 다릅니다.\nNoto Sans KR · Roboto Mono / SIL OFL").setPositiveButton("닫기",null).show();});item.setMinimumHeight(dp(82));panel.addView(item,new LinearLayout.LayoutParams(-1,-2));View separator=new View(this);separator.setBackgroundColor(0x193b4657);panel.addView(separator,new LinearLayout.LayoutParams(-1,dp(1)));}
        drawerLeft=Math.max(dp(8),root.getPaddingLeft()-dp(12));drawerTop=root.getPaddingTop()+dp(56);popup.showAtLocation(root,Gravity.TOP|Gravity.START,drawerLeft,drawerTop);
    }
    boolean settingsLocked(){return testActive||RecordingManager.active||AudioService.waiting()||videoPreparing||(AudioService.practice!=null&&AudioService.practice.active);}
    void audioSettings(){add(title("오디오 설정"));add(button("출력: "+outputName()+" ›",this::monitoringOptions));
        add(text("실시간 레이턴시",18,TEXT));note("낮추면 반응이 빨라지고, 높이면 끊김이 줄어듭니다. 블루투스 전송 지연은 남습니다.");
        int mode=AudioRouting.selected(this);int count=AudioRouting.prefs(this).getInt("bursts"+mode,2);TextView latency=text("",14,LIME);add(latency);updateLatency(latency);
        LinearLayout presets=row();String[] names={"반응 우선","균형","안정"};int[] bs={1,2,4};for(int i=0;i<3;i++){final int n=bs[i];weighted(presets,button(names[i],()->applyLatency(mode,n,latency)),48);}add(presets);
        SeekBar buffer=new SeekBar(this);buffer.setMax(7);buffer.setProgress(count-1);buffer.setEnabled(!settingsLocked());buffer.setContentDescription("출력 버퍼 단계 1에서 8");buffer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int p,boolean u){}public void onStopTrackingTouch(SeekBar b){applyLatency(mode,b.getProgress()+1,latency);}});add(buffer);
        note("표시는 실제 적용된 출력 버퍼입니다. 장치가 요청값을 제한할 수 있습니다.");
        add(text("녹음 싱크 보정",18,TEXT));int offset=AudioRouting.prefs(this).getInt("sync"+mode,0);TextView sync=text("보컬 보정: "+offset+" ms",14,TEXT);add(sync);SeekBar shift=new SeekBar(this);shift.setMax(200);shift.setProgress((offset+500)/5);shift.setEnabled(!settingsLocked());shift.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int v,boolean u){if(u&&!settingsLocked()){int ms=v*5-500;AudioRouting.prefs(MainActivity.this).edit().putInt("sync"+mode,ms).apply();sync.setText("보컬 보정: "+ms+" ms");}}});add(shift);note("− 보컬 앞당김 / + 보컬 늦춤. 다음 녹음에 적용하며 실시간 목소리에는 지연을 추가하지 않습니다.");
        add(button("싱크 테스트 · 5초",this::syncTest));
        add(text("상세 효과",18,TEXT));slider("반주 볼륨",1,0,1,v->Math.round(v*100)+"%");slider("리버브 잔향",3,.3f,6,v->String.format(Locale.US,"%.1f s",v));slider("에코 양",4,0,.8f,v->Math.round(v*100)+"%");slider("에코 간격",5,70,700,v->Math.round(v)+" ms");slider("에코 반복",6,0,.7f,v->Math.round(v*100)+"%");slider("Low",7,-12,12,v->Math.round(v)+" dB");slider("Mid",8,-12,12,v->Math.round(v)+" dB");slider("High",9,-12,12,v->Math.round(v)+" dB");toggle("컴프레서",10);toggle("소음 완화",19);add(button("리버브 공간",()->StudioDialogs.choose(this,"리버브 공간",new String[]{"홀","룸","플레이트","대성당"},Math.round(values[18]),w->set(18,w))));add(button("입력 장치",this::selectInput));
    }
    void updateLatency(TextView label){int[] a=NativeAudio.latencyInfo();label.setText(a[0]>0?String.format(Locale.US,"출력 버퍼 %.1f ms · %d 프레임 · 끊김 %s",1000.0*a[2]/a[3],a[2],a[4]>=0?String.valueOf(a[4]):"측정 불가"):"마이크를 켜면 적용 버퍼를 확인할 수 있습니다.");}
    void applyLatency(int mode,int n,TextView label){if(settingsLocked()){toast("녹음·예약·연습을 마친 뒤 변경하세요.");return;}if(NativeAudio.bufferBursts(n)){AudioRouting.prefs(this).edit().putInt("bursts"+mode,n).apply();updateLatency(label);}else toast("기기에서 버퍼 변경을 허용하지 않았습니다.");}
    void practicePage(){add(title("보컬 연습"));note("한 음씩 천천히 따라 불러보세요.");if(trainingReadout!=null)trainingReadout=null;
        LinearLayout practiceCard=beginCard();trainingReadout=text("목표음 · 내 피치\n연습을 시작하세요",24,LIME);trainingReadout.setMinHeight(dp(100));trainingReadout.setGravity(Gravity.CENTER);add(trainingReadout);practicePlot=new PracticePlot(this);practicePlot.root=practiceRoot;content.addView(practicePlot,new LinearLayout.LayoutParams(-1,dp(128)));endCard(practiceCard);
        practiceButton=button(AudioService.practice!=null&&AudioService.practice.active?"연습 종료 · 결과 저장":"연습 시작",()->{if(AudioService.practice!=null&&AudioService.practice.active){AudioService.practice.stop(false);showTab();}else if(!settingsLocked())ensureMic(5);else toast("현재 녹음 또는 예약을 마친 뒤 시작하세요.");});practiceButton.setTextColor(BG);practiceButton.setBackgroundTintList(ColorStateList.valueOf(LIME));practiceButton.setMinHeight(dp(56));practiceButton.setPadding(dp(16),dp(12),dp(16),dp(12));actionDock.addView(practiceButton,new LinearLayout.LayoutParams(-1,-2));
        add(text("연습 설정",17,TEXT));
        add(button(practiceMode==0?"방식: 듣고 따라 부르기 ▾":"방식: 함께 부르기 ▾",()->StudioDialogs.choose(this,"연습 방식",new String[]{"듣고 따라 부르기","함께 부르기"},practiceMode,w->{if(!settingsLocked()){practiceMode=w;showTab();}})));
        add(button(practicePattern==0?"5음 상하행 ▾":"장음계 1옥타브 ▾",()->StudioDialogs.choose(this,"연습 음계",new String[]{"5음 상하행","장음계 1옥타브"},practicePattern,w->{if(!settingsLocked()){practicePattern=w;showTab();}})));
        add(button("시작음: "+noteName(practiceRoot)+" ▾",()->{String[] notes=new String[28];for(int i=0;i<28;i++)notes[i]=noteName(i+45);StudioDialogs.choose(this,"시작음",notes,practiceRoot-45,w->{if(!settingsLocked()){practiceRoot=45+w;showTab();}});}));
        TextView tempo=text("템포 "+practiceBpm+" BPM",14,TEXT);add(tempo);SeekBar bpm=new SeekBar(this);bpm.setMax(16);bpm.setProgress((practiceBpm-40)/5);bpm.setEnabled(!settingsLocked());bpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int p,boolean u){if(u){practiceBpm=40+p*5;tempo.setText("템포 "+practiceBpm+" BPM");}}});add(bpm);
        Switch capture=new Switch(this);capture.setText("연습 녹음 · WAV + MIDI + 결과");capture.setTextColor(TEXT);capture.setChecked(practiceRecord);capture.setEnabled(!settingsLocked());capture.setOnCheckedChangeListener((b,on)->practiceRecord=on);add(capture);
        add(button("보컬 화음 쌓기 ›",()->chooseTab(9)));add(button("목소리 악기 ›",()->chooseTab(8)));add(button("자유 음정 · MIDI ›",()->chooseTab(2)));add(button("연습 결과 보기",()->showPracticeResult(AudioService.practice==null?null:AudioService.practice.result)));
        if(AudioService.practice!=null&&!AudioService.practice.error.isEmpty())note(AudioService.practice.error);
        if(AudioService.practice!=null&&AudioService.practice.result!=null){org.json.JSONObject result=AudioService.practice.result;note(result.has("accuracy")?String.format(Locale.US,"정확도 %.1f%% · 평균 오차 %.1f¢ · 발성 %.1f%%",result.optDouble("accuracy"),result.optDouble("meanAbsoluteCents"),result.optDouble("coverage")):"유효 발성이 부족해 정확도를 계산하지 않았습니다.");add(button("보관함 ›",()->chooseTab(4)));}
        note("목표음 대비 ±25센트 기준. 스피커·블루투스의 함께 부르기는 화면 가이드로 진행합니다.");
    }
    String noteName(int n){return new String[]{"C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B"}[n%12]+(n/12-1);}
    void showPracticeResult(org.json.JSONObject result){if(result==null){toast("연습을 마치면 결과를 확인할 수 있습니다.");return;}java.util.List<String> lines=new java.util.ArrayList<>();lines.add(result.has("accuracy")?String.format(Locale.US,"정확도 %.1f%% · 평균 오차 %.1f¢",result.optDouble("accuracy"),result.optDouble("meanAbsoluteCents")):"유효 발성이 부족합니다.");lines.add(String.format(Locale.US,"발성 비율 %.1f%%",result.optDouble("coverage")));org.json.JSONArray notes=result.optJSONArray("notes");if(notes!=null)for(int i=0;i<notes.length();i++){org.json.JSONObject n=notes.optJSONObject(i);if(n!=null)lines.add(noteName(n.optInt("targetMidi"))+" · "+(n.has("accuracy")?String.format(Locale.US,"%.1f%% · %.1f¢",n.optDouble("accuracy"),n.optDouble("meanAbsoluteCents")):"발성 부족"));}new AlertDialog.Builder(this).setTitle(result.optBoolean("complete")?"연습 완료":"부분 연습 결과").setMessage(android.text.TextUtils.join("\n",lines)).setPositiveButton("닫기",null).show();}
    void syncTest(){if(settingsLocked())return;ensureMic(6);}
    String outputName(){return new String[]{"이어폰","휴대폰 스피커","블루투스 스피커"}[AudioRouting.selected(this)];}
    void monitoringOptions(){
        LinearLayout panel=column();panel.setPadding(dp(16),dp(8),dp(16),0);
        Button route=button("출력 모드: "+outputName()+"  ›",()->{});panel.addView(route);
        Button monitor=button("",()->{});monitor.setText(values[11]>.5?"내 목소리 듣기 · 켜짐":"내 목소리 듣기 · 꺼짐");monitor.setOnClickListener(v->{if(NativeAudio.stats()[17]>.5){toast("보호 음소거를 해제한 뒤 다시 시작하세요.");return;}if(AudioService.active&&AudioService.canMonitor(this)){set(11,values[11]>.5?0:1);monitor.setText(values[11]>.5?"내 목소리 듣기 · 켜짐":"내 목소리 듣기 · 꺼짐");toast(values[11]>.5?"실시간 출력 켜짐":"실시간 출력 꺼짐");}else toast("마이크와 출력 장치를 먼저 켜세요.");});panel.addView(monitor);
        TextView help=text(AudioRouting.selected(this)==0?"유선 / USB 이어폰을 연결하면 내 목소리를 실시간으로 들을 수 있습니다.":"휴대폰 마이크로 입력합니다. 시스템 스피커 볼륨도 낮게 시작하고 마이크와 스피커를 떨어뜨리세요. 블루투스는 전송 지연이 있습니다.",13,MUTED);help.setPadding(0,dp(12),0,dp(12));panel.addView(help);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("출력 설정").setView(StudioDialogs.scrollable(this,panel)).setPositiveButton("닫기",null).create();
        route.setEnabled(!RecordingManager.active&&!AudioService.waiting()&&!videoPreparing);
        route.setOnClickListener(v->{dialog.dismiss();outputMode();});
        if(AudioRouting.selected(this)>0){
            int gain=AudioService.active?Math.round(NativeAudio.stats()[20]*100):18;
            TextView label=text("스피커 마이크 출력 · "+gain+"%",14,TEXT);panel.addView(label);
            SeekBar level=new SeekBar(this);level.setMax(65);level.setProgress(gain);level.setEnabled(AudioService.active);level.setContentDescription("스피커 마이크 출력, 최대 65퍼센트");level.setProgressTintList(ColorStateList.valueOf(LIME));level.setThumbTintList(ColorStateList.valueOf(LIME));
            level.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int value,boolean user){if(user){NativeAudio.monitorLevel(value/100f);label.setText("스피커 마이크 출력 · "+value+"%");}}});panel.addView(level,new LinearLayout.LayoutParams(-1,dp(48)));
            Button reset=button("보호 음소거 해제 · 18%로 재시작",()->{NativeAudio.resetFeedback();NativeComposer.monitor(true);harmony.monitor(true);set(11,1);dialog.dismiss();toast("18% 출력으로 재시작합니다. 시스템 볼륨도 낮게 유지하세요.");});reset.setEnabled(AudioService.active);panel.addView(reset);
            TextView protection=text("피드백을 감지하면 필터를 적용하고 필요 시 마이크 출력만 음소거합니다. 녹음은 계속됩니다.\n기기 에코 제거: "+(AudioService.active?(AudioService.echoCancellation?"활성":"미지원"):"마이크를 켜면 확인"),12,MUTED);protection.setPadding(0,dp(12),0,dp(8));panel.addView(protection);
        }
        dialog.show();
    }
    void outputMode(){
        if(settingsLocked()){toast("예약 또는 녹음을 마친 뒤 출력 모드를 바꾸세요.");return;}
        StudioDialogs.choose(this,"내 목소리를 어디로 보낼까요?",new String[]{"이어폰 · 지연이 가장 적음","휴대폰 스피커 · 하울링 보호","블루투스 스피커 · 전송 지연 있음"},AudioRouting.selected(this),mode->{
            if(mode==2){List<AudioDeviceInfo> devices=AudioRouting.bluetooth(this);if(devices.isEmpty()){toast("Android 설정에서 블루투스 스피커를 연결한 뒤 다시 선택하세요.");startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));return;}
                String[] names=new String[devices.size()];for(int i=0;i<names.length;i++)names[i]=devices.get(i).getProductName().toString();
                StudioDialogs.choose(this,"연결된 블루투스 출력",names,-1,i->applyOutputMode(2,devices.get(i).getId()));
            }else applyOutputMode(mode,0);
        });
    }
    void applyOutputMode(int mode,int device){
        if(settingsLocked())return;
        set(11,0);AudioRouting.prefs(this).edit().putInt("mode",mode).putInt("bluetoothDevice",device).apply();
        if(AudioService.active)startService(new Intent(this,AudioService.class).setAction(AudioService.ROUTE).putExtra("device",inputDevice));
        showTab();if(mode>0)toast("스피커 출력은 18%로 준비됩니다. 실시간 출력 스위치를 켜고 시스템 볼륨도 낮춰주세요.");
    }
    void recordingOptions(){
        if(settingsLocked()){toast("예약 또는 녹음을 마친 뒤 설정하세요.");return;}
        LinearLayout panel=column();panel.setPadding(dp(16),dp(12),dp(16),0);
        panel.addView(text("녹음 시작",15,TEXT));LinearLayout delays=row();int[] choice={recordDelay};
        int[] options={0,3,5,10};for(int value:options){StudioButton b=StudioTheme.styled(this,value==0?"즉시":value+"초",StudioButton.STANDARD,()->{});b.setTextSize(13);StudioTheme.selected(b,recordDelay==value);b.setOnClickListener(v->{choice[0]=value;for(int i=0;i<delays.getChildCount();i++)StudioTheme.selected((Button)delays.getChildAt(i),delays.getChildAt(i)==b);});weighted(delays,b,48);}panel.addView(delays);
        Switch source=new Switch(this);source.setText("마이크 + 다른 앱 소리");source.setTextColor(TEXT);source.setChecked(captureOtherApps);source.setPadding(0,dp(12),0,dp(8));
        if(CaptureSupport.available())panel.addView(source);
        TextView details=text(CaptureSupport.available()?"Android 공유 권한이 매번 필요합니다. 화면은 저장하지 않으며, 캡처를 허용하는 앱의 오디오만 함께 담습니다.":"마이크와 앱에서 선택한 반주를 녹음합니다. 다른 앱의 소리는 직접 포함되지 않습니다.",12,MUTED);panel.addView(details);
        TextView gain=text("녹음에 담을 다른 앱 소리: "+Math.round(captureGain*100)+"%",12,TEXT);SeekBar slider=new SeekBar(this);slider.setMax(150);slider.setProgress(Math.round(captureGain*100));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int p,boolean user){gain.setText("녹음에 담을 다른 앱 소리: "+p+"%");}});
        if(CaptureSupport.available()){panel.addView(gain);panel.addView(slider);}
        panel.addView(text("카운트다운이 시작되면 다른 앱으로 이동해 재생하세요. 알림에서 예약 취소·녹음 저장이 가능합니다.",12,MUTED));
        new AlertDialog.Builder(this).setTitle("백그라운드 녹음").setView(StudioDialogs.scrollable(this,panel)).setPositiveButton("설정 저장",(d,w)->{
            recordDelay=choice[0];captureOtherApps=CaptureSupport.available()&&source.isChecked();captureGain=slider.getProgress()/100f;
            getPreferences(0).edit().putInt("recordDelay",recordDelay).putBoolean("captureOtherApps",captureOtherApps).putFloat("captureGain",captureGain).apply();
        }).setNegativeButton("취소",null).show();
    }
    void requestRecording(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){recordPermissionPending=true;requestPermissions(Build.VERSION.SDK_INT>=33?new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.POST_NOTIFICATIONS}:new String[]{Manifest.permission.RECORD_AUDIO},12);return;}
        if(captureOtherApps&&CaptureSupport.available()){recordPermissionPending=true;CaptureSupport.request(this,30);}
        else scheduleRecording(null);
    }
    void scheduleRecording(Intent projection){
        recordPermissionPending=false;pendingMonitor=AudioService.canMonitor(this);
        Intent intent=new Intent(this,AudioService.class).setAction(AudioService.RECORD).putExtra("device",inputDevice).putExtra("delay",recordDelay).putExtra("bpm",Math.round(values[15]))
            .putExtra("monitor",pendingMonitor).putExtra("capture",projection!=null&&BuildConfig.PLAYBACK_CAPTURE).putExtra("captureGain",captureGain);
        if(projection!=null)intent.putExtra("projection",projection);
        try{startForegroundService(intent);}catch(Exception e){toast("녹음 예약 실패: "+e.getMessage());}
    }
    void preset(int p){selectedPreset=p;float[][] options={{.2f,1.5f,.34f,300,.38f,0,0,2},{.3f,2.4f,.14f,280,.25f,0,-1,2},{.42f,3.7f,.1f,340,.25f,1,-1,1},{.12f,.9f,.12f,140,.18f,-1,1,3},{.58f,5.5f,.15f,410,.35f,1,-2,0},{0,.3f,0,150,0,0,0,0}};int[] keys={2,3,4,5,6,7,8,9};for(int i=0;i<keys.length;i++)set(keys[i],options[p][i]);set(10,p==5?0:1);set(18,p==4?3:p==3?2:0);selectedPreset=p;getPreferences(0).edit().putInt("preset",p).apply();showTab();toast("프리셋을 적용했습니다.");}
    void backing(){add(title("반주"));String[] sources={"내장 작곡기","내 파일","YouTube · SoundCloud"};add(StudioTheme.styled(this,sources[sourceTab]+" ▾",StudioButton.CHIP,()->StudioDialogs.choose(this,"반주 소스",sources,sourceTab,w->{sourceTab=w;getPreferences(0).edit().putInt("backingSource",w).apply();showTab();})));
        if(sourceTab==1){fileBacking();return;}if(sourceTab==2){youtubeBacking();return;}
        composerScreen.draw(false);
    }
    void fileBacking(){
        StudioButton importButton=StudioTheme.styled(this,"＋ 반주 가져오기",StudioButton.OUTLINE,this::pickBacking);importButton.setTextColor(LIME);add(importButton);TextView format=text("WAV · MP3 · M4A",11,MUTED);format.setGravity(Gravity.CENTER);add(format);
        TextView heading=text("최근 반주",17,TEXT);heading.setTypeface(StudioTheme.bold(this));heading.setPadding(0,dp(10),0,dp(4));add(heading);
        List<JSONObject> items=recent.list();if(items.isEmpty()){note("연습할 반주 파일을 가져오세요. 불러온 파일이 여기에 모입니다.");}
        for(JSONObject j:items){Uri uri=Uri.parse(j.optString("uri"));String filename=j.optString("name"),ext=extension(filename);LinearLayout card=row();card.setPadding(dp(12),dp(10),dp(8),dp(10));card.setBackground(bg(PANEL,13));ImageView icon=new ImageView(this);icon.setPadding(dp(9),dp(9),dp(9),dp(9));icon.setImageDrawable(new StudioGlyph("music",0xffb8c5da));icon.setBackground(bg(RAISED,9));card.addView(icon,new LinearLayout.LayoutParams(dp(42),dp(48)));
            LinearLayout copy=column();copy.setPadding(dp(12),0,dp(4),0);TextView name=text(filename,15,selectedFile.equals(filename)?LIME:TEXT);name.setTypeface(StudioTheme.medium(this));name.setSingleLine();name.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(name);TextView detail=text(ext+" · "+new java.text.SimpleDateFormat("MM.dd",Locale.KOREA).format(new Date(j.optLong("time")))+(selectedFile.equals(filename)?" · 선택됨":""),11,MUTED);detail.setPadding(0,dp(4),0,0);copy.addView(detail);card.addView(copy,new LinearLayout.LayoutParams(0,-2,1));card.setOnClickListener(v->loadBacking(uri));Button play=StudioTheme.icon(this,selectedFile.equals(filename)&&NativeAudio.stats()[16]>.5?"pause":"play",filename+" 재생 / 일시정지",()->{if(selectedFile.equals(filename)){if(NativeAudio.stats()[16]>.5)NativeAudio.playLocal(false);else ensureMic(4);showTab();}else loadBacking(uri,true);});card.addView(play,new LinearLayout.LayoutParams(dp(48),dp(48)));Button more=StudioTheme.icon(this,"more","최근 반주 메뉴",()->StudioDialogs.choose(this,filename,new String[]{"반주 불러오기","최근 목록에서 삭제"},-1,w->{if(w==0)loadBacking(uri);else{recent.remove(uri);showTab();}}));card.addView(more,new LinearLayout.LayoutParams(dp(48),dp(48)));add(card);
        }
        if(!selectedFile.isEmpty()){gap();LinearLayout prev=beginCard();LinearLayout info=row();LinearLayout copy=column();TextView name=text(selectedFile,15,TEXT);name.setSingleLine();name.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(name);playerTime=text("",11,MUTED);playerTime.setTypeface(StudioTheme.mono(this));playerTime.setPadding(0,dp(6),0,0);copy.addView(playerTime);info.addView(copy,new LinearLayout.LayoutParams(0,-2,1));info.addView(StudioTheme.icon(this,NativeAudio.stats()[16]>.5?"pause":"play","선택한 반주 재생 / 일시정지",()->{if(NativeAudio.stats()[16]>.5)NativeAudio.playLocal(false);else ensureMic(4);showTab();}),new LinearLayout.LayoutParams(dp(48),dp(48)));add(info);playerLine=new StudioPlaybackLine(this,fraction->{if(!RecordingManager.active&&!AudioService.waiting()){double[] infoNow=NativeAudio.localPlaybackInfo();NativeAudio.seekLocal(fraction*infoNow[1]);}});content.addView(playerLine,new LinearLayout.LayoutParams(-1,dp(48)));slider("반주 볼륨",1,0,1,v->Math.round(v*100)+"%");endCard(prev);}
    }
    String extension(String name){int dot=name.lastIndexOf('.');return dot>=0&&dot<name.length()-1?name.substring(dot+1).toUpperCase(Locale.ROOT):"AUDIO";}
    void pickBacking(){if(RecordingManager.active||AudioService.waiting()||busy||videoPreparing){toast("녹음과 파일 준비를 마친 뒤 선택하세요.");return;}startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),20);}
    void loadBacking(Uri uri){loadBacking(uri,false);}
    void loadBacking(Uri uri,boolean autoplay){if(RecordingManager.active||AudioService.waiting()||busy||videoPreparing){toast("녹음을 마친 뒤 반주를 불러오세요.");return;}busy=true;String name=recent.name(uri);toast("반주 파일을 준비하고 있습니다…");io.execute(()->{File decodedFile=null;try{MediaTools.Decoded decoded=MediaTools.decode(this,uri);decodedFile=decoded.file;if(!NativeAudio.loadLocalFile(decoded.file.getAbsolutePath(),decoded.rate,decoded.channels))throw new IOException("반주를 불러오지 못했습니다.");recent.remember(uri,name);runOnUiThread(()->{selectedFile=name;toast("반주를 불러왔습니다.");});}catch(Exception e){runOnUiThread(()->{selectedFile="";new AlertDialog.Builder(this).setTitle("파일을 열 수 없어요").setMessage("파일이 이동·삭제되었거나 접근권한이 만료되었을 수 있습니다. 다시 가져와 주세요.").setPositiveButton("확인",null).setNegativeButton("목록에서 삭제",(d,w)->{recent.remove(uri);showTab();}).show();});}finally{if(decodedFile!=null)decodedFile.delete();runOnUiThread(()->{busy=false;if(autoplay&&!selectedFile.isEmpty())ensureMic(4);if(tab==1)showTab();});}});}
    void youtubeBacking(){EditText link=new EditText(this);link.setSingleLine();link.setTextSize(14);link.setTextColor(TEXT);link.setHintTextColor(MUTED);link.setHint("YouTube 주소 또는 검색어");add(link);
        add(button("YouTube에서 열기 ↗",()->{String value=link.getText().toString().trim();if(value.isEmpty())value="노래방 MR";Uri uri=value.startsWith("https://youtu.be/")||value.startsWith("https://www.youtube.com/")?Uri.parse(value):Uri.parse("https://www.youtube.com/results?search_query="+Uri.encode(value));open(uri,"text/html");}));add(button("SoundCloud에서 열기 ↗",()->open(Uri.parse("https://soundcloud.com/"),"text/html")));note(BuildConfig.PLAYBACK_CAPTURE?"개인용 시작 설정에서 허용된 다른 앱 소리를 함께 녹음할 수 있습니다.":"배포용은 외부 앱 소리를 녹음하지 않습니다.");}
    void midi(){add(title("실시간 MIDI"));LinearLayout prev=beginCard();TextView caption=text("현재 음정",14,MUTED);caption.setGravity(Gravity.CENTER);add(caption);pitch=text("—",72,LIME);pitch.setTypeface(Typeface.create(StudioTheme.mono(this),700,false));pitch.setGravity(Gravity.CENTER);add(pitch);pitchDetails=text("음정 감지 대기",16,MUTED);pitchDetails.setTypeface(StudioTheme.mono(this));pitchDetails.setGravity(Gravity.CENTER);add(pitchDetails);pitchMeter=new PitchMeter(this);content.addView(pitchMeter,new LinearLayout.LayoutParams(-1,dp(28)));
        TextView scale=text("−50 ───────── 0 ───────── +50",14,MUTED);scale.setGravity(Gravity.CENTER);add(scale);endCard(prev);
        prev=beginCard();add(text("최근 12초",12,MUTED));visual=new StudioVisual(this,true);content.addView(visual,new LinearLayout.LayoutParams(-1,dp(170)));endCard(prev);toggle("신스 더블링",12);
        add(button("MIDI 저장",()->{File f=store.file("melody_"+System.currentTimeMillis(),".mid");if(NativeAudio.exportMidi(f.getAbsolutePath(),Math.round(values[15]),false))export(f);else toast("MIDI 저장 실패");}));
        Button clear=button("채보 지우기",()->{if(RecordingManager.active||AudioService.waiting()){toast("예약 또는 녹음 중에는 채보를 지울 수 없습니다.");return;}StudioDialogs.confirmDanger(this,"채보를 지울까요?","현재 음정 기록을 지웁니다. 저장한 MIDI 파일은 유지됩니다.","지우기",NativeAudio::clearNotes);});add(clear);note("단선율 보컬 감지 · 잡음에 따라 오차가 생길 수 있어요.");
    }
    void cameraPage(){cameraFailed=false;addModeSelector(true);
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){LinearLayout prev=beginCard();add(text("카메라로 담는 나의 무대",20,TEXT));note("보컬 효과와 반주를 영상에도 그대로");actionDock.setVisibility(View.GONE);add(button("카메라 권한 허용",()->requestPermissions(new String[]{Manifest.permission.CAMERA},11)));endCard(prev);return;}
        FrameLayout stage=new FrameLayout(this);stage.setBackground(bg(PANEL,24));stage.setClipToOutline(true);content.addView(stage,new LinearLayout.LayoutParams(-1,dp(220)));page.flexible(stage,180,520);
        texture=new TextureView(this);stage.addView(texture,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout hud=row();hud.setPadding(dp(12),dp(12),dp(12),0);hud.setBackgroundColor(0x55000000);cameraTimer=text("영상 촬영",16,TEXT);cameraTimer.setTypeface(StudioTheme.mono(this));hud.addView(cameraTimer,new LinearLayout.LayoutParams(0,-2,1));flipButton=button("전·후면 ↻",this::flipCamera);hud.addView(flipButton,new LinearLayout.LayoutParams(dp(112),dp(48)));stage.addView(hud,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        gap();LinearLayout controls=row();pauseButton=button("Ⅱ 일시정지",this::pauseVideo);weighted(controls,pauseButton,56);videoButton=button("● 영상 촬영",this::toggleVideo);videoButton.setTextColor(BG);videoButton.setBackgroundTintList(ColorStateList.valueOf(RED));weighted(controls,videoButton,56);actionDock.addView(controls,new LinearLayout.LayoutParams(-1,-2));
        note("영상 촬영을 시작하면 효과 오디오도 함께 녹음돼요.");
        camera=new CameraController(this,texture,new CameraController.Listener(){public void ready(){if(flipResumeAudio){RecordingManager.pause(false);videoPaused=false;flipResumeAudio=false;}if(pendingAction==2&&AudioService.active)tryPendingAction();}public void error(String t){cameraFailed=true;videoPreparing=false;pendingAction=0;flipResumeAudio=false;if(camera!=null&&camera.recording)stopVideo();toast(t);if(cameraTimer!=null)cameraTimer.setText("카메라 오류 · 재시도 필요");}});
    }
    void flipCamera(){if(camera==null||videoPreparing||busy)return;boolean resume=camera.recording&&!videoPaused;
        if(resume)RecordingManager.pause(true);
        if(camera.flip()){flipResumeAudio=resume;}else{if(resume)RecordingManager.pause(false);toast("카메라가 준비된 뒤 다시 시도해 주세요.");}}
    void pauseVideo(){if(camera==null||!camera.recording||camera.switching)return;boolean next=!videoPaused;
        if(next){RecordingManager.pause(true);if(!camera.pause(true)){RecordingManager.pause(false);return;}}
        else {if(!camera.pause(false))return;RecordingManager.pause(false);}videoPaused=next;}
    void library(){LinearLayout filters=row();weighted(filters,button("녹음",()->{libraryFilter=0;showTab();}),48);weighted(filters,button("내 반주",()->{libraryFilter=1;showTab();}),48);add(filters);if(libraryFilter==1){composerScreen.projects();return;}add(title("보관함"));note("녹음과 연습을 한곳에");if(busy)note("영상에 보컬 오디오를 합치고 있습니다…");List<JSONObject> takes=store.list();if(takes.isEmpty()){LinearLayout prev=beginCard();add(text("첫 번째 목소리를 담아보세요",19,TEXT));note("녹음한 오디오·영상·MIDI가 한 테이크로 모입니다.");StudioButton first=StudioTheme.styled(this,"첫 녹음 시작",StudioButton.OUTLINE,()->chooseTab(0));first.setTextColor(LIME);add(first);endCard(prev);return;}
        for(JSONObject j:takes){String id=j.optString("id");File audio=store.file(id,".wav"),video=store.file(id,".mp4");LinearLayout card=column();card.setPadding(dp(14),dp(13),dp(12),dp(12));card.setBackground(bg(PANEL,14));LinearLayout heading=row();LinearLayout copy=column();TextView name=text(j.optString("name"),17,TEXT);name.setTypeface(StudioTheme.bold(this));name.setSingleLine();name.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(name);TextView meta=text(new java.text.SimpleDateFormat("yyyy.MM.dd",Locale.KOREA).format(new Date(j.optLong("created")))+" · "+formatTime(j.optDouble("seconds")),11,MUTED);meta.setPadding(0,dp(5),0,0);copy.addView(meta);heading.addView(copy,new LinearLayout.LayoutParams(0,-2,1));for(String type:new String[]{"MP4","WAV","MID"}){if(!store.file(id,"."+type.toLowerCase(Locale.ROOT)).exists())continue;TextView badge=text(type,10,MUTED);badge.setPadding(dp(5),dp(4),dp(5),dp(4));badge.setBackground(bg(RAISED,5));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,-2);bp.leftMargin=dp(4);heading.addView(badge,bp);}heading.addView(StudioTheme.icon(this,"more","테이크 메뉴",()->takeMenu(j)),new LinearLayout.LayoutParams(dp(48),dp(48)));card.addView(heading);
            LinearLayout stage=row();LinearLayout.LayoutParams stageParams=new LinearLayout.LayoutParams(-1,dp(78));stageParams.topMargin=dp(10);
            if(video.exists()){ImageView thumbnail=new ImageView(this);thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);thumbnail.setBackground(bg(BG,9));thumbnail.setClipToOutline(true);stage.addView(thumbnail,new LinearLayout.LayoutParams(0,-1,1));io.execute(()->{android.media.MediaMetadataRetriever retriever=new android.media.MediaMetadataRetriever();try{retriever.setDataSource(video.getAbsolutePath());android.graphics.Bitmap frame=retriever.getScaledFrameAtTime(0,android.media.MediaMetadataRetriever.OPTION_CLOSEST_SYNC,320,160);runOnUiThread(()->{if(!isDestroyed())thumbnail.setImageBitmap(frame);});}catch(Exception ignored){}finally{try{retriever.release();}catch(Exception ignored){}}});}
            else if(j.has("practice")&&store.file(id,".mid").exists()){StudioMidiPreview midi=new StudioMidiPreview(this);stage.addView(midi,new LinearLayout.LayoutParams(0,-1,1));io.execute(()->{float[] notes=StudioMidiPreview.read(store.file(id,".mid"));runOnUiThread(()->{if(!isDestroyed()){midi.notes=notes;midi.invalidate();}});});}
            else{StudioTakeWave waveform=new StudioTakeWave(this);stage.addView(waveform,new LinearLayout.LayoutParams(0,-1,1));io.execute(()->{float[] levels=readWaveform(audio);runOnUiThread(()->{if(!isDestroyed()){waveform.levels=levels;waveform.invalidate();}});});}
            StudioButton play=(StudioButton)StudioTheme.icon(this,"play","테이크 재생",()->{if(video.exists())openFile(video);else play(audio);});if(!video.exists())takePlayButtons.put(audio.getAbsolutePath(),play);LinearLayout.LayoutParams playParams=new LinearLayout.LayoutParams(dp(48),dp(48));playParams.leftMargin=dp(6);stage.addView(play,playParams);card.addView(stage,stageParams);
            if(!video.exists()){TextView playback=text("",12,LIME);playback.setPadding(0,dp(8),0,0);playback.setVisibility(View.GONE);takePlaybackStatus.put(audio.getAbsolutePath(),playback);card.addView(playback);}
            if(j.has("practice")){JSONObject pr=j.optJSONObject("practice");TextView score=text(pr!=null&&pr.has("accuracy")?String.format(Locale.US,"정확도 %.1f%% · 연습 결과 ›",pr.optDouble("accuracy")):"보컬 연습 결과 ›",12,LIME);score.setMinHeight(dp(48));score.setGravity(Gravity.CENTER_VERTICAL);score.setOnClickListener(v->showPracticeResult(pr));card.addView(score);}
            if(!j.optString("warning").isEmpty())card.addView(text(j.optString("warning"),11,RED));add(card);
        }
        updateTakePlaybackButtons();
    }
    static float[] readWaveform(File file){float[] levels=new float[64];try(RandomAccessFile wav=new RandomAccessFile(file,"r")){long data=Math.max(0,wav.length()-44);for(int i=0;i<64;i++){long offset=(data*i/64)/4*4;wav.seek(44+offset);double energy=0;int samples=0;for(int n=0;n<64&&wav.getFilePointer()+2<=wav.length();n++){int lo=wav.readUnsignedByte(),hi=wav.readByte();float v=(short)((hi<<8)|lo)/32768f;energy+=v*v;samples++;}levels[i]=samples>0?(float)Math.min(1,Math.sqrt(energy/samples)*3):0;}}catch(Exception ignored){}return levels;}
    void takeMenu(JSONObject j){
        String id=j.optString("id");File wav=store.file(id,".wav"),midi=store.file(id,".mid"),video=store.file(id,".mp4");
        List<String> names=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        if(wav.exists()){names.add("오디오 재생 / 일시정지");actions.add(()->play(wav));names.add("WAV 저장");actions.add(()->export(wav));}
        if(midi.exists()){names.add("MIDI 저장");actions.add(()->export(midi));}
        if(video.exists()){names.add("영상 재생");actions.add(()->openFile(video));names.add("MP4 저장");actions.add(()->export(video));}
        if(video.exists()||wav.exists()){names.add("공유");actions.add(()->share(video.exists()?video:wav));}
        names.add("이름 변경");actions.add(()->{EditText input=new EditText(this);input.setText(j.optString("name"));new AlertDialog.Builder(this).setTitle("테이크 이름").setView(input).setPositiveButton("저장",(a,b)->{try{String name=input.getText().toString().trim();if(name.isEmpty()){toast("이름을 입력해 주세요.");return;}store.rename(j,name);showTab();}catch(Exception e){toast(e.getMessage());}}).setNegativeButton("취소",null).show();});
        names.add("테이크 삭제");actions.add(()->StudioDialogs.confirmDanger(this,"이 테이크를 삭제할까요?","오디오·MIDI·영상이 기기에서 삭제됩니다.","삭제",()->{stopPlayer();store.delete(id);showTab();}));
        StudioDialogs.choose(this,j.optString("name"),names.toArray(new String[0]),-1,w->actions.get(w).run());
    }
    void requestMicToggle(){if(AudioService.active&&(RecordingManager.active||AudioService.waiting())){boolean waiting=AudioService.waiting();new AlertDialog.Builder(this).setTitle("마이크를 끌까요?").setMessage(waiting?"예약을 취소하고 마이크를 끕니다.":"진행 중인 녹음을 저장하고 마이크를 끕니다.").setPositiveButton(waiting?"예약 취소 후 끄기":"저장 후 끄기",(d,w)->toggleMic()).setNegativeButton("계속 사용",null).show();return;}toggleMic();}
    void toggleMic(){if(starting||busy||testActive)return;pendingAction=0;if(AudioService.active){if(camera!=null&&camera.recording)stopVideo();else finishAudio();startService(new Intent(this,AudioService.class).setAction(AudioService.STOP));set(11,0);set(13,0);return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){awaitingMicPermission=true;requestPermissions(Build.VERSION.SDK_INT>=33?new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.POST_NOTIFICATIONS}:new String[]{Manifest.permission.RECORD_AUDIO},10);return;}
        starting=true;pendingMonitor=AudioService.canMonitor(this);startForegroundService(new Intent(this,AudioService.class).putExtra("device",inputDevice).putExtra("monitor",pendingMonitor));handler.postDelayed(()->starting=false,1500);
    }
    boolean requireMic(){if(!AudioService.active||NativeAudio.stats()[10]<.5){toast("스튜디오에서 마이크를 켜주세요.");return false;}return true;}
    void ensureMic(int action){if(busy||videoPreparing)return;if(action==4){composer.pause();startForegroundService(new Intent(this,AudioService.class).setAction(AudioService.PLAYBACK));set(13,0);NativeAudio.playLocal(true);showTab();return;}if(AudioService.active&&NativeAudio.stats()[10]>.5){if(action==5)practiceOwnsMic=false;pendingAction=action;actionDeadline=SystemClock.elapsedRealtime()+10000;tryPendingAction();return;}
        if(action==5)practiceOwnsMic=true;if(!starting)toggleMic();pendingAction=action;actionDeadline=SystemClock.elapsedRealtime()+10000;}
    void tryPendingAction(){if(pendingAction==0||!AudioService.active||NativeAudio.stats()[10]<.5||busy||videoPreparing)return;int action=pendingAction;
        if(action==2&&(camera==null||!camera.ready))return;pendingAction=0;
        if(action==1){stopPlayer();requestRecording();}
        else if(action==2)startVideo();
        else if(action==3){NativeAudio.playLocal(false);set(14,BackingCatalog.SYNTH_STYLES[genre]);set(13,1);showTab();}
        else if(action==5){composer.pause();set(13,0);set(17,0);set(12,0);AudioService.practice.ownedMic=practiceOwnsMic;practiceOwnsMic=false;AudioService.practice.start(practiceRoot,practiceBpm,practiceMode,practicePattern,practiceRecord);showTab();}
        else if(action==7){if(!AudioService.canMonitor(this)){toast("이어폰을 연결하거나 출력 설정에서 스피커를 선택하세요.");monitoringOptions();return;}harmony.live(false);composer.voice(true);showTab();}
        else if(action==6){runSyncTest();}
        else if(action==8||action==9){if(!AudioService.canMonitor(this)){toast("이어폰을 연결하거나 출력 설정에서 스피커를 선택하세요.");monitoringOptions();return;}if(NativeAudio.stats()[17]>.5f){toast("하울링 보호를 출력 설정에서 해제한 뒤 다시 켜세요.");monitoringOptions();return;}set(11,1);harmony.live(true);if(action==9)harmony.arm(harmonyScreen.selected);if(tab==9)harmonyScreen.refresh();else showTab();}
        else if(action==4){if(selectedFile.isEmpty()){toast("반주 파일을 먼저 선택하세요.");return;}set(13,0);NativeAudio.playLocal(true);showTab();}}
    void toggleRecord(){if(testActive||videoPreparing||busy||starting||recordPermissionPending)return;pendingAction=0;
        if(camera!=null&&camera.recording){stopVideo();return;}
        if(AudioService.waiting()){startService(new Intent(this,AudioService.class).setAction(AudioService.CANCEL));return;}
        if(RecordingManager.active){finishAudio();return;}
        stopPlayer();requestRecording();
    }
    void finishAudio(){if(AudioService.active){startService(new Intent(this,AudioService.class).setAction(AudioService.FINISH));return;}
        try{RecordingManager.Result r=RecordingManager.stop();if(r!=null)toast("오디오와 MIDI를 저장했습니다.");}catch(Exception e){toast("보관함 저장 실패: "+e.getMessage());}
    }
    void toggleVideo(){if(videoPreparing||busy)return;if(cameraFailed){showTab();return;}if(camera!=null&&camera.recording){stopVideo();return;}if(RecordingManager.active||AudioService.waiting()){toast("예약 또는 오디오 녹음을 마친 뒤 영상을 시작하세요.");return;}ensureMic(2);}
    void startVideo(){if(camera==null||!requireMic())return;stopPlayer();rawVideo=new File(store.directory,"pending_"+System.currentTimeMillis()+".video.mp4");videoPreparing=true;videoPaused=false;
        final CameraController target=camera;
        if(!camera.start(rawVideo,()->{videoPreparing=false;if(camera!=target||!target.recording)return;RecordingManager.syncMs=AudioRouting.prefs(this).getInt("sync"+AudioRouting.selected(this),0);if(!RecordingManager.start(this,Math.round(values[15]))){target.stop();toast("오디오를 시작하지 못해 영상 녹화를 중지했습니다.");}else videoTakeId=RecordingManager.id;})){videoPreparing=false;toast("카메라 미리보기가 준비된 뒤 다시 눌러주세요.");}}
    void stopVideo(){if(camera==null||!camera.recording)return;pendingAction=0;videoPaused=false;flipResumeAudio=false;boolean videoOk=camera.stop();try{RecordingManager.Result r=RecordingManager.stop();if(r==null&&RecordingManager.lastResult!=null&&RecordingManager.lastResult.id.equals(videoTakeId))r=RecordingManager.lastResult;if(r==null)return;final RecordingManager.Result saved=r;final long voiceDelayUs=Math.max(0,RecordingManager.syncMs)*1000L;File source=rawVideo;if(!videoOk){toast("영상이 너무 짧거나 중단되었습니다. 오디오는 보관함에서 확인하세요.");return;}
            busy=true;long offset=(long)(r.clock*1e6-camera.videoStartNanos/1000.);toast("영상을 저장하고 있습니다…");
            io.execute(()->{try{MediaTools.makeVideo(source,store.file(saved.id,".wav"),store.file(saved.id,".mp4"),offset,voiceDelayUs);org.json.JSONObject meta=store.read(saved.id);meta.put("id",saved.id).put("name","Video Take "+saved.id).put("seconds",saved.seconds).put("video",true).put("warning",saved.warning);store.update(saved.id,meta);runOnUiThread(()->toast("MP4 · WAV · MIDI를 저장했습니다."));}catch(Exception e){store.file(saved.id,".mp4").delete();runOnUiThread(()->toast("영상 결합 실패. WAV와 MIDI는 보관되어 있습니다: "+e.getMessage()));}finally{runOnUiThread(()->{busy=false;if(tab==4)showTab();});}});
        }catch(Exception e){toast("녹화 저장 실패: "+e.getMessage());}finally{if(tab==3&&!isFinishing())showTab();}}
    void selectInput(){if(AudioService.active){toast("마이크를 끈 뒤 입력 장치를 선택하세요.");return;}AudioDeviceInfo[] all=((AudioManager)getSystemService(AUDIO_SERVICE)).getDevices(AudioManager.GET_DEVICES_INPUTS);String[] names=new String[all.length+1];names[0]="시스템 기본";int selected=inputDevice==0?0:-1;for(int i=0;i<all.length;i++){names[i+1]=all[i].getProductName()+" · "+all[i].getId();if(inputDevice==all[i].getId())selected=i+1;}StudioDialogs.choose(this,"마이크 입력",names,selected,w->{inputDevice=w==0?0:all[w-1].getId();toast(names[w]);});}
    void updateTransportVisibility(){boolean practiceRecording=tab==5&&AudioService.practice!=null&&AudioService.practice.active;transport.setVisibility(((RecordingManager.active||AudioService.waiting())&&tab!=0&&tab!=3&&!practiceRecording)?View.VISIBLE:View.GONE);}
    String formatTime(double seconds){int s=(int)Math.max(0,seconds);return String.format(Locale.US,"%02d:%02d",s/60,s%60);}
    final Runnable tick=new Runnable(){public void run(){float[] s=NativeAudio.stats();boolean running=AudioService.active&&s[10]>.5;values[11]=s[21];
        boolean wired=AudioService.hasWiredOutput(MainActivity.this);if(headphones&&!wired&&AudioRouting.selected(MainActivity.this)==0)set(11,0);headphones=wired;
        if(running&&!wasRunning){starting=false;values[11]=s[21];pendingMonitor=false;}
        if(!running&&wasRunning){if(camera!=null&&camera.recording)stopVideo();finishAudio();set(11,0);set(13,0);if(AudioService.active)stopService(new Intent(MainActivity.this,AudioService.class));}
        wasRunning=running;
        if(micButton!=null){micButton.setText(running?"마이크 끄기":starting?"연결 중…":"마이크 켜기");StudioTheme.selected(micButton,running);}
        if(monitorButton!=null){monitorButton.setText(s[17]>.5?"내 목소리 듣기 · 보호 음소거 ›":values[11]>.5?"내 목소리 듣기 · 켜짐":"내 목소리 듣기 · 꺼짐");StudioTheme.selected(monitorButton,values[11]>.5&&s[17]<.5);monitorButton.setEnabled(running&&AudioService.canMonitor(MainActivity.this));}
        if(outputButton!=null)outputButton.setText((AudioRouting.selected(MainActivity.this)==0&&!wired?"이어폰 미연결":outputName())+" ▾");
        if(playerTime!=null){double[] position=NativeAudio.localPlaybackInfo();playerTime.setText(formatTime(position[0])+" / "+formatTime(position[1]));if(playerLine!=null){boolean canSeek=!RecordingManager.active&&!AudioService.waiting();if(playerLine.isEnabled()!=canSeek)playerLine.setContentDescription(canSeek?"반주 재생 위치":"녹음 중에는 반주 위치를 바꿀 수 없습니다");playerLine.setEnabled(canSeek);playerLine.setAlpha(canSeek?1f:.45f);if(!playerLine.dragging)playerLine.progress=position[1]>0?(float)(position[0]/position[1]):0;playerLine.invalidate();}}
        if(player!=null&&playingFile!=null){TextView playback=takePlaybackStatus.get(playingFile.getAbsolutePath());if(playback!=null){try{playback.setText((player.isPlaying()?"재생 중 · ":"일시정지 · ")+formatTime(player.getCurrentPosition()/1000.0)+" / "+formatTime(player.getDuration()/1000.0));}catch(IllegalStateException ignored){}}}
        if(levelText!=null){float db=20*(float)Math.log10(Math.max(.00001f,s[3]));levelText.setText(s[3]>.00001?String.format(Locale.US,"%.0f dB",db):"— dB");levelText.setTextColor(s[3]>.8?RED:LIME);}
        if(tab==5&&trainingReadout!=null&&AudioService.practice!=null){PracticeSession pr=AudioService.practice;if(practiceButton!=null){practiceButton.setText(pr.active?"연습 종료 · 결과 저장":"연습 시작");practiceButton.setBackgroundTintList(ColorStateList.valueOf(pr.active?RED:LIME));}if(pr.active){if(practicePlot!=null)practicePlot.sample(s[15],pr.target,s[0],!Double.isNaN(pr.score.cents));trainingReadout.setText(pr.phase+"\n목표 "+noteName(pr.target)+" · "+(s[0]>0?noteName((int)s[2]):"—")+"\n"+(Double.isNaN(pr.score.cents)?"입력 대기":String.format(Locale.US,"%+.0f¢",pr.score.cents)+(Double.isNaN(pr.score.accuracy())?"":" · "+String.format(Locale.US,"%.1f%%",pr.score.accuracy()))));}else if(pr.index>=0){trainingReadout.setText(pr.phase+"\n"+(Double.isNaN(pr.score.accuracy())?"발성 부족":String.format(Locale.US,"정확도 %.1f%% · 발성 %.1f%%",pr.score.accuracy(),pr.score.coverage())));}}
        if(!AudioService.active&&!AudioService.playbackActive)harmony.poll();harmonyScreen.poll();if(lastHarmonyRevision!=harmony.revision){lastHarmonyRevision=harmony.revision;if(tab==9)harmonyScreen.refresh();}if(!harmony.error.isEmpty()){toast(harmony.error);harmony.error="";}if(!harmony.message.isEmpty()){toast(harmony.message);harmony.message="";}if(composer.ready){if(!AudioService.active&&!AudioService.playbackActive)composer.poll();composerScreen.poll();}if(lastComposerRevision!=composer.revision){lastComposerRevision=composer.revision;if(tab==7||tab==8||(tab==1&&sourceTab==0)||(tab==4&&libraryFilter==1))composerScreen.refresh();}if(!composer.error.isEmpty()){toast(composer.error);composer.error="";}if(!composer.message.isEmpty()){toast(composer.message);composer.message="";}
        updateTransportVisibility();
        if(pendingAction!=0){if(!awaitingMicPermission&&SystemClock.elapsedRealtime()>actionDeadline){pendingAction=0;toast("장치를 시작하지 못했습니다. 권한과 연결을 확인해 주세요.");}else tryPendingAction();}
        if(liveState!=null){liveState.setText(running?"● 마이크 켜짐":"● 마이크 대기");liveState.setTextColor(running?LIME:MUTED);}
        if(status!=null)status.setText(running?"실시간 모니터링 · "+outputName():"마이크를 켜고 목소리를 들려주세요");
        if(!AudioService.error.isEmpty()){toast(AudioService.error);AudioService.error="";}
        boolean waiting=AudioService.waiting();timer.setVisibility(tab!=0||RecordingManager.active||waiting?View.VISIBLE:View.GONE);String elapsed=formatTime(RecordingManager.active?s[8]:0)+String.format(Locale.US,".%d",(int)((RecordingManager.active?s[8]:0)*10)%10);timer.setText(waiting?AudioService.remaining()+"초":elapsed);if(recordCaption!=null){recordCaption.setText(waiting?"예약 녹음":RecordingManager.active?"● 녹음 중":"오디오 녹음");recordCaption.setTextColor(RecordingManager.active||waiting?RED:MUTED);}
        recButton.setText(busy?"저장 / 준비 중…":waiting?"예약 취소":RecordingManager.active?"■ 녹음 종료 · 저장":recordDelay==0?"● 녹음 시작":"● "+recordDelay+"초 후 녹음");recButton.setTextColor(BG);recButton.setBackgroundTintList(ColorStateList.valueOf(RecordingManager.active||waiting?RED:LIME));recButton.setEnabled(!busy&&!videoPreparing&&!recordPermissionPending&&!starting);
        recordOptions.setEnabled(!busy&&!waiting&&!RecordingManager.active&&!recordPermissionPending);
        if(status!=null){status.setVisibility(waiting||s[17]>.5f||AudioService.captureActive?View.VISIBLE:View.GONE);if(waiting)status.setText(AudioService.remaining()+"초 후 녹음 · 다른 앱으로 이동해 재생하세요");
            else if(RecordingManager.active&&AudioService.captureActive)status.setText(AudioService.captureLevel>.001f?"● 마이크 + 다른 앱 소리 입력 중":"● 다른 앱 소리 신호 없음 / 캡처 허용 확인");
            else if(running&&s[19]>0)status.setText("출력: "+AudioService.routeLabel+" · 보호 필터 "+(int)s[18]+"개 · 출력 설정 ›");
            if(running&&s[17]>.5f)status.setText("하울링 보호 음소거 · 출력 설정에서 재시작 ›");}
        if(lastRouteRevision!=AudioService.routeRevision){lastRouteRevision=AudioService.routeRevision;if(tab==0)showTab();}
        if(lastRecordingRevision!=AudioService.revision){lastRecordingRevision=AudioService.revision;if(tab==4)showTab();}
        if(camera!=null&&camera.recording&&!RecordingManager.active&&!videoPreparing)stopVideo();
        if(cameraTimer!=null){boolean active=camera!=null&&camera.recording;cameraTimer.setText(cameraFailed?"카메라 오류 · 재시도":videoPreparing?"카메라 준비 중…":active?(videoPaused?"Ⅱ PAUSED ":"●REC ")+formatTime(s[8]):busy?"영상 저장 중…":"영상 촬영");cameraTimer.setTextColor(active?RED:TEXT);}
        if(videoButton!=null){videoButton.setText(cameraFailed?"카메라 다시 연결":camera!=null&&camera.recording?"■ 중단 · 저장":"● 영상 촬영");videoButton.setEnabled(!busy&&!videoPreparing&&camera!=null&&(!camera.switching));}
        if(pauseButton!=null){pauseButton.setText(videoPaused?"▶ 이어서 촬영":"Ⅱ 일시정지");pauseButton.setEnabled(camera!=null&&camera.recording&&!camera.switching&&!videoPreparing);}
        if(flipButton!=null)flipButton.setEnabled(camera!=null&&camera.ready&&!camera.switching&&!videoPreparing&&!busy);
        if(pitch!=null){int n=(int)s[2];String[] notes={"C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B"};pitch.setText(n>=0?notes[n%12]+(n/12-1):"—");pitchDetails.setText(n>=0?String.format(Locale.US,"%.1f Hz    %+.0f cent",s[0],s[1]):"목소리를 들려주세요");pitchMeter.tracking=n>=0;pitchMeter.cents=s[1];pitchMeter.invalidate();}
        if(visual!=null){visual.stats=s;if(!visual.piano)visual.sampleInput(s[3]);if(visual.piano)visual.notes=NativeAudio.roll();visual.invalidate();}
        handler.postDelayed(this,80);
    }};
    void play(File f){if(RecordingManager.active||AudioService.waiting()){toast("예약 또는 녹음을 마친 뒤 재생하세요.");return;}if(player!=null&&f.equals(playingFile)){try{if(player.isPlaying())player.pause();else player.start();updateTakePlaybackButtons();}catch(IllegalStateException e){stopPlayer();toast("재생 실패");}return;}stopPlayer();set(11,0);set(13,0);NativeAudio.playLocal(false);try{player=new MediaPlayer();player.setDataSource(f.getAbsolutePath());player.setOnCompletionListener(p->{if(player==p)stopPlayer();});player.prepare();playingFile=f;player.start();updateTakePlaybackButtons();}catch(Exception e){stopPlayer();toast("재생 실패");}}
    void stopPlayer(){if(player!=null){player.release();player=null;}playingFile=null;updateTakePlaybackButtons();}
    void updateTakePlaybackButtons(){for(Map.Entry<String,StudioButton> entry:takePlayButtons.entrySet()){boolean current=playingFile!=null&&entry.getKey().equals(playingFile.getAbsolutePath());boolean active=current&&player!=null&&player.isPlaying();entry.getValue().glyph(active?"pause":"play");entry.getValue().setContentDescription(active?"테이크 재생 중 · 일시정지":current?"일시정지됨 · 이어서 재생":"테이크 재생");TextView status=takePlaybackStatus.get(entry.getKey());if(status!=null){status.setVisibility(current?View.VISIBLE:View.GONE);if(current)status.setText(active?"재생 중":"일시정지");}}}
    Uri fileUri(File f){return Uri.parse("content://"+getPackageName()+".files/"+Uri.encode(f.getName()));}
    String mime(File f){return f.getName().endsWith(".mp4")?"video/mp4":f.getName().endsWith(".mid")?"audio/midi":"audio/wav";}
    void openFile(File f){try{startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(fileUri(f),mime(f)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}catch(Exception e){toast("이 파일을 재생할 앱이 없습니다. 파일 저장을 사용하세요.");}}
    void share(File f){Intent i=new Intent(Intent.ACTION_SEND).setType(mime(f)).putExtra(Intent.EXTRA_STREAM,fileUri(f)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"테이크 공유"));}
    void open(Uri u,String type){try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){toast("링크를 열 앱이 없습니다.");}}
    void export(File f){if(!f.exists()){toast("저장할 파일이 없습니다.");return;}exportFile=f;startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).setType(mime(f)).putExtra(Intent.EXTRA_TITLE,f.getName()).addCategory(Intent.CATEGORY_OPENABLE),21);}
    @Override public void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==30){recordPermissionPending=false;if(result==RESULT_OK&&data!=null)scheduleRecording(data);else toast("다른 앱 소리 권한을 허용하지 않아 녹음을 시작하지 않았습니다.");return;}if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        if(request==20){try{if((data.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0)getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(SecurityException ignored){}sourceTab=1;loadBacking(uri);}
        else if(request==21&&exportFile!=null){File f=exportFile;io.execute(()->{try(InputStream in=new FileInputStream(f);OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("저장 위치를 열지 못했습니다.");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);runOnUiThread(()->toast("파일을 저장했습니다."));}catch(Exception e){runOnUiThread(()->toast("파일 저장 실패: "+e.getMessage()));}});}}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==12){recordPermissionPending=false;if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)requestRecording();else toast("마이크 권한이 필요합니다.");return;}if(request==10)awaitingMicPermission=false;if(request==10&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED){int next=pendingAction;toggleMic();pendingAction=next;actionDeadline=SystemClock.elapsedRealtime()+10000;}else if(request==11)showTab();else {pendingAction=0;toast("설정에서 마이크 권한을 허용해주세요.");}}
    void runSyncTest(){try{File f=new File(getCacheDir(),"sync-test.wav");int sync=AudioRouting.prefs(this).getInt("sync"+AudioRouting.selected(this),0);NativeAudio.recordSync(sync);set(13,0);NativeAudio.playLocal(false);set(15,60);set(17,1);if(!NativeAudio.beginRecord(f.getAbsolutePath())){set(17,0);return;}testActive=true;toast("클릭에 맞춰 소리 내세요. 5초 후 테스트를 재생합니다.");handler.postDelayed(()->{if(testActive)finishSyncTest(true);},5000);}catch(Exception e){set(17,0);toast(e.getMessage());}}
    void finishSyncTest(boolean playback){NativeAudio.endRecord();testActive=false;set(17,0);set(11,0);if(playback){play(new File(getCacheDir(),"sync-test.wav"));toast("테스트를 듣고 보정값을 조절하세요.");}}
    boolean testActive=false;
    void toast(String t){Toast.makeText(this,t==null?"오류가 발생했습니다.":t,Toast.LENGTH_LONG).show();}
    @Override public boolean onKeyDown(int k,android.view.KeyEvent e){if(k==android.view.KeyEvent.KEYCODE_ESCAPE){set(11,0);toast("마이크 모니터링 음소거");return true;}return super.onKeyDown(k,e);}
    final Runnable pendingSource=new Runnable(){public void run(){if(activityVisible||pendingAction==0)return;if(awaitingMicPermission){handler.postDelayed(this,100);return;}if(SystemClock.elapsedRealtime()>actionDeadline){pendingAction=0;return;}tryPendingAction();if(pendingAction!=0)handler.postDelayed(this,100);}};
    @Override protected void onPause(){if(testActive)finishSyncTest(false);if(AudioService.practice!=null&&AudioService.practice.active)AudioService.practice.stop(false);activityVisible=false;handler.removeCallbacks(tick);if(pendingAction==3||pendingAction==4)handler.post(pendingSource);videoPreparing=false;if(camera!=null){if(camera.recording)stopVideo();camera.release();camera=null;}super.onPause();}
    @Override protected void onResume(){super.onResume();activityVisible=true;handler.removeCallbacks(pendingSource);handler.removeCallbacks(tick);handler.post(tick);if(tab==3&&content!=null&&camera==null)showTab();}
    @Override protected void onDestroy(){if(drawerPopup!=null)drawerPopup.dismiss();handler.removeCallbacks(tick);handler.removeCallbacks(pendingSource);if(camera!=null)camera.release();stopPlayer();io.shutdown();super.onDestroy();}
}
