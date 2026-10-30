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
    static final int BG=StudioTheme.BG,PANEL=StudioTheme.SURFACE,TEXT=StudioTheme.TEXT,MUTED=StudioTheme.MUTED,LIME=StudioTheme.ACCENT,RED=StudioTheme.DANGER;
    final Handler handler=new Handler(Looper.getMainLooper());final ExecutorService io=Executors.newSingleThreadExecutor();
    final float[] values={1,.55f,.27f,2.2f,.14f,280,.25f,0,0,1,1,0,0,0,0,78,0,0,0,1};
    LinearLayout root,content,nav;Button practiceButton,micButton,monitorButton,recButton,videoButton,pauseButton,flipButton,recordOptions;TextView status,pitch,timer,cameraTimer,levelText;StudioVisual visual;
    int sourceTab=0,genre=0,selectedPreset=-1,pendingAction=0; boolean effectsExpanded=false,videoPaused=false,flipResumeAudio=false,cameraFailed=false,awaitingMicPermission=false;
    TextView pitchDetails;PitchMeter pitchMeter;
    RecentBackings recent;String selectedFile="";long actionDeadline=0;
    int recordDelay=0,lastRecordingRevision=-1,lastRouteRevision=-1;boolean captureOtherApps=false,recordPermissionPending=false;float captureGain=.7f;String videoTakeId=null;boolean activityVisible=false;
    LinearLayout transport;TextView trainingReadout;PracticePlot practicePlot;boolean practiceOwnsMic=false;boolean practiceRecord=false;int practiceRoot=48,practiceBpm=60,practiceMode=0,practicePattern=0;
    int tab=0,inputDevice=0;boolean starting=false,busy=false,wasRunning=false,headphones=false,pendingMonitor=false,videoPreparing=false;
    CameraController camera;TextureView texture;File rawVideo,exportFile;TakeStore store;MediaPlayer player;
    @Override public void onCreate(Bundle state){super.onCreate(state);store=new TakeStore(this);recent=new RecentBackings(this);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        selectedPreset=getPreferences(0).getInt("preset",-1);recordDelay=getPreferences(0).getInt("recordDelay",0);captureOtherApps=BuildConfig.PLAYBACK_CAPTURE&&getPreferences(0).getBoolean("captureOtherApps",false);captureGain=getPreferences(0).getFloat("captureGain",.7f);
        for(int i=0;i<values.length;i++)if(i!=11&&i!=13&&i!=12)values[i]=getPreferences(0).getFloat("p"+i,values[i]);
        if(!AudioService.active)for(int i=0;i<values.length;i++)NativeAudio.set(i,values[i]);build();handler.post(tick);
    }
    int dp(float n){return(int)(getResources().getDisplayMetrics().density*n+.5f);}
    GradientDrawable bg(int c,int r){return StudioTheme.background(this,c,r);}
    TextView text(String t,int size,int color){return StudioTheme.text(this,t,size,color);}
    TextView title(String t){TextView v=text(t,24,TEXT);v.setTypeface(StudioTheme.bold(this));v.setPadding(0,dp(8),0,dp(16));return v;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    Button button(String t,Runnable r){return StudioTheme.button(this,t,r);}
    void add(View v){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);content.addView(v,lp);}
    void gap(){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,dp(4)));}
    void note(String t){TextView v=text(t,12,MUTED);v.setPadding(0,dp(4),0,dp(4));add(v);}
    LinearLayout beginCard(){LinearLayout previous=content;LinearLayout card=StudioTheme.card(this);add(card);content=card;return previous;}
    void endCard(LinearLayout previous){content=previous;}
    void weighted(LinearLayout line,View view,int height){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(height),1);if(line.getChildCount()>0)lp.leftMargin=dp(8);line.addView(view,lp);}
    void build(){root=column();root.setBackgroundColor(BG);root.setPadding(dp(20),0,dp(20),0);
        root.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());root.setPadding(dp(20)+i.left,i.top,dp(20)+i.right,i.bottom);}else root.setPadding(dp(20)+insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),dp(20)+insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});setContentView(root);root.requestApplyInsets();
        LinearLayout header=row();header.addView(button("☰",this::drawer));header.setMinimumHeight(dp(60));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.live_vault_mark);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setContentDescription("라이브볼트 로고");header.addView(logo,new LinearLayout.LayoutParams(dp(40),dp(40)));
        LinearLayout identity=column();identity.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams identityParams=new LinearLayout.LayoutParams(0,dp(60),1);identityParams.leftMargin=dp(8);header.addView(identity,identityParams);
        TextView brand=text("Live Vault",20,TEXT);brand.setTypeface(StudioTheme.bold(this));brand.setSingleLine();brand.setAutoSizeTextTypeUniformWithConfiguration(16,20,1,android.util.TypedValue.COMPLEX_UNIT_SP);identity.addView(brand,new LinearLayout.LayoutParams(-1,-2));identity.addView(text(BuildConfig.PLAYBACK_CAPTURE?"Personal":"Vocal Studio",11,MUTED));
        header.addView(button("음소거",()->{set(11,0);toast("마이크 실시간 출력만 음소거했습니다.");}));root.addView(header);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);content=column();content.setPadding(0,0,0,dp(8));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        transport=row();transport.setPadding(dp(12),dp(10),dp(12),dp(10));transport.setBackground(bg(PANEL,14));recButton=button("●  오디오 녹음",this::toggleRecord);transport.addView(recButton,new LinearLayout.LayoutParams(0,dp(52),1));recordOptions=button("⋯",this::recordingOptions);recordOptions.setTextSize(22);recordOptions.setContentDescription("녹음 시작 설정 · 즉시, 3초, 5초, 10초");transport.addView(recordOptions,new LinearLayout.LayoutParams(dp(48),dp(52)));LinearLayout clock=column();clock.setPadding(dp(12),0,0,0);timer=text("00:00.0",16,TEXT);timer.setTypeface(StudioTheme.mono(this));clock.addView(timer);clock.addView(text("WAV + MIDI",10,MUTED));transport.addView(clock);root.addView(transport,new LinearLayout.LayoutParams(-1,dp(76)));
        nav=row();nav.setPadding(0,dp(10),0,dp(8));String[] labels={"라이브","반주","보관함"};for(int i=0;i<labels.length;i++){final int k=new int[]{0,1,4}[i];Button b=button(labels[i],()->chooseTab(k));b.setTextSize(13);b.setPadding(0,0,0,0);weighted(nav,b,52);}root.addView(nav);showTab();
    }
    void chooseTab(int k){if(testActive)finishSyncTest(false);if(AudioService.practice!=null&&AudioService.practice.active)AudioService.practice.stop(false);pendingAction=0;if(videoPreparing){toast("카메라를 준비하고 있습니다.");return;}if(camera!=null&&camera.recording){new AlertDialog.Builder(this).setTitle("녹화를 마칠까요?").setMessage("영상을 저장한 뒤 화면을 이동합니다.").setPositiveButton("저장 후 이동",(d,w)->{stopVideo();tab=k;showTab();}).setNegativeButton("계속 녹화",null).show();return;}tab=k;showTab();}
    void showTab(){setVolumeControlStream(AudioRouting.selected(this)==1?AudioManager.STREAM_VOICE_CALL:AudioManager.STREAM_MUSIC);if(camera!=null&&camera.recording)return;if(camera!=null){camera.release();camera=null;}content.removeAllViews();visual=null;pitch=null;pitchDetails=null;pitchMeter=null;texture=null;micButton=null;monitorButton=null;status=null;cameraTimer=null;videoButton=null;pauseButton=null;flipButton=null;levelText=null;
        for(int i=0;i<nav.getChildCount();i++){Button b=(Button)nav.getChildAt(i);b.setTextColor(new int[]{0,1,4}[i]==(tab==3||tab==5||tab==6?0:tab)?LIME:MUTED);b.setBackgroundTintList(ColorStateList.valueOf(new int[]{0,1,4}[i]==(tab==3||tab==5||tab==6?0:tab)?StudioTheme.RAISED:BG));}
        if(tab==0)studio();else if(tab==1)backing();else if(tab==2)midi();else if(tab==3)cameraPage();else if(tab==5)practicePage();else if(tab==6)audioSettings();else library();
    }
    void set(int k,float v){if((k>=2&&k<=10)||k==18){selectedPreset=-1;getPreferences(0).edit().putInt("preset",-1).apply();}values[k]=v;NativeAudio.set(k,v);if(k!=11&&k!=12&&k!=13)getPreferences(0).edit().putFloat("p"+k,v).apply();}
    interface Formatter{String value(float v);}
    void slider(String label,int k,float min,float max,Formatter f){LinearLayout box=column();TextView caption=text(label+"   "+f.value(values[k]),14,TEXT);box.addView(caption);SeekBar s=new SeekBar(this);s.setMax(1000);s.setProgress(Math.round((values[k]-min)/(max-min)*1000));s.setProgressTintList(ColorStateList.valueOf(LIME));s.setThumbTintList(ColorStateList.valueOf(LIME));s.setContentDescription(label);s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int progress,boolean user){if(user){float v=min+(max-min)*progress/1000;set(k,v);caption.setText(label+"   "+f.value(v));}}});box.addView(s,new LinearLayout.LayoutParams(-1,dp(48)));add(box);}
    void toggle(String label,int k){Switch s=new Switch(this);s.setText(label);s.setTextColor(TEXT);s.setTextSize(14);s.setPadding(0,dp(10),0,dp(10));s.setChecked(values[k]>.5);s.setOnCheckedChangeListener((b,on)->{if(k==11&&on&&!AudioService.canMonitor(this)){s.setChecked(false);toast("이어폰을 연결하거나 출력 모드에서 스피커를 선택하세요.");return;}set(k,on?1:0);});add(s);}
    void studio(){
        LinearLayout chips=row();weighted(chips,button(outputName()+" ▾",this::monitoringOptions),48);weighted(chips,button((selectedPreset>=0?new String[]{"노래방","발라드","콘서트 홀","팝 보컬","대성당","생목소리"}[selectedPreset]:"사용자 설정")+" ▾",()->new AlertDialog.Builder(this).setTitle("보컬 프리셋").setItems(new String[]{"노래방","발라드","콘서트 홀","팝 보컬","대성당","생목소리"},(d,w)->preset(w)).show()),48);add(chips);
        LinearLayout prev=beginCard();add(text("내 목소리",18,TEXT));visual=new StudioVisual(this,false);content.addView(visual,new LinearLayout.LayoutParams(-1,dp(84)));status=text("입력 대기",12,MUTED);status.setMinHeight(dp(48));status.setGravity(Gravity.CENTER_VERTICAL);status.setOnClickListener(v->monitoringOptions());content.addView(status);endCard(prev);
        slider("목소리 크기",0,0,2,v->Math.round(v*100)+"%");slider("리버브",2,0,1,v->Math.round(v*100)+"%");
        LinearLayout controls=row();controls.setGravity(Gravity.CENTER);micButton=button("마이크 시작",this::toggleMic);micButton.setBackground(bg(LIME,48));android.graphics.drawable.Drawable mic=getDrawable(R.drawable.ic_mic_status);mic.setBounds(0,0,dp(24),dp(24));micButton.setCompoundDrawables(null,mic,null,null);controls.addView(micButton,new LinearLayout.LayoutParams(dp(104),dp(104)));add(controls);
        LinearLayout media=row();weighted(media,button("● 오디오",()->{}),48);weighted(media,button("영상 촬영 ›",()->chooseTab(3)),48);add(media);
        add(button(selectedFile.isEmpty()?"♫ 반주 선택 ›":"♫ "+selectedFile,()->chooseTab(1)));
    }
    void drawer(){
        LinearLayout panel=column();panel.setPadding(dp(16),dp(16),dp(16),dp(16));panel.setBackground(bg(PANEL,14));
        PopupWindow popup=new PopupWindow(panel,Math.min(dp(320),getResources().getDisplayMetrics().widthPixels-dp(32)),-2,true);popup.setBackgroundDrawable(bg(PANEL,14));popup.setOutsideTouchable(true);
        panel.addView(text("Live Vault",22,LIME));
        String[] names={"오디오 설정","녹음 설정","보컬 연습","도움말 · 앱 정보"};String[] subtitles={"출력 · 레이턴시 · 상세 효과","예약 녹음 · 저장 옵션","스케일 · 음정 · MIDI","사용 방법 · 버전"};
        for(int i=0;i<4;i++){final int k=i;Button item=button(names[i]+"\n"+subtitles[i],()->{popup.dismiss();if(k==0)chooseTab(6);else if(k==1)recordingOptions();else if(k==2)chooseTab(5);else new AlertDialog.Builder(this).setTitle("Live Vault 2.0").setMessage("유선/USB 이어폰을 권장합니다. 스피커는 낮은 출력에서 시작하세요. 출력 버퍼는 실제 왕복 지연 측정값이 아닙니다.\n\n폰트: Noto Sans KR · Roboto Mono (SIL OFL). 라이선스는 APK assets/licenses에 포함됩니다.").setPositiveButton("닫기",null).show();});panel.addView(item,new LinearLayout.LayoutParams(-1,dp(76)));}
        popup.showAtLocation(root,Gravity.TOP|Gravity.START,dp(16),root.getPaddingTop()+dp(60));
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
        add(text("상세 효과",18,TEXT));slider("반주 볼륨",1,0,1,v->Math.round(v*100)+"%");slider("리버브 잔향",3,.3f,6,v->String.format(Locale.US,"%.1f s",v));slider("에코 양",4,0,.8f,v->Math.round(v*100)+"%");slider("에코 간격",5,70,700,v->Math.round(v)+" ms");slider("에코 반복",6,0,.7f,v->Math.round(v*100)+"%");slider("Low",7,-12,12,v->Math.round(v)+" dB");slider("Mid",8,-12,12,v->Math.round(v)+" dB");slider("High",9,-12,12,v->Math.round(v)+" dB");toggle("컴프레서",10);toggle("소음 완화",19);add(button("리버브 공간",()->new AlertDialog.Builder(this).setItems(new String[]{"홀","룸","플레이트","대성당"},(d,w)->set(18,w)).show()));add(button("입력 장치",this::selectInput));
    }
    void updateLatency(TextView label){int[] a=NativeAudio.latencyInfo();label.setText(a[0]>0?String.format(Locale.US,"출력 버퍼 %.1f ms · %d 프레임 · 끊김 %s",1000.0*a[2]/a[3],a[2],a[4]>=0?String.valueOf(a[4]):"측정 불가"):"마이크를 켜면 적용 버퍼를 확인할 수 있습니다.");}
    void applyLatency(int mode,int n,TextView label){if(settingsLocked()){toast("녹음·예약·연습을 마친 뒤 변경하세요.");return;}if(NativeAudio.bufferBursts(n)){AudioRouting.prefs(this).edit().putInt("bursts"+mode,n).apply();updateLatency(label);}else toast("기기에서 버퍼 변경을 허용하지 않았습니다.");}
    void practicePage(){add(title("보컬 연습"));if(trainingReadout!=null)trainingReadout=null;
        add(button("자유 음정 · MIDI ›",()->chooseTab(2)));
        add(button(practiceMode==0?"방식: 듣고 따라 부르기 ▾":"방식: 함께 부르기 ▾",()->new AlertDialog.Builder(this).setItems(new String[]{"듣고 따라 부르기","함께 부르기"},(d,w)->{if(!settingsLocked()){practiceMode=w;showTab();}}).show()));
        add(button(practicePattern==0?"5음 상하행 ▾":"장음계 1옥타브 ▾",()->new AlertDialog.Builder(this).setItems(new String[]{"5음 상하행","장음계 1옥타브"},(d,w)->{if(!settingsLocked()){practicePattern=w;showTab();}}).show()));
        add(button("시작음: "+noteName(practiceRoot)+" ▾",()->{String[] notes=new String[28];for(int i=0;i<28;i++)notes[i]=noteName(i+45);new AlertDialog.Builder(this).setItems(notes,(d,w)->{if(!settingsLocked()){practiceRoot=45+w;showTab();}}).show();}));
        TextView tempo=text("템포 "+practiceBpm+" BPM",14,TEXT);add(tempo);SeekBar bpm=new SeekBar(this);bpm.setMax(16);bpm.setProgress((practiceBpm-40)/5);bpm.setEnabled(!settingsLocked());bpm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int p,boolean u){if(u){practiceBpm=40+p*5;tempo.setText("템포 "+practiceBpm+" BPM");}}});add(bpm);
        Switch capture=new Switch(this);capture.setText("연습 녹음 · WAV + MIDI + 결과");capture.setTextColor(TEXT);capture.setChecked(practiceRecord);capture.setEnabled(!settingsLocked());capture.setOnCheckedChangeListener((b,on)->practiceRecord=on);add(capture);
        trainingReadout=text("목표음 · 내 피치\n연습을 시작하세요",24,LIME);trainingReadout.setMinHeight(dp(120));trainingReadout.setGravity(Gravity.CENTER);add(trainingReadout);practicePlot=new PracticePlot(this);practicePlot.root=practiceRoot;content.addView(practicePlot,new LinearLayout.LayoutParams(-1,dp(160)));
        practiceButton=button(AudioService.practice!=null&&AudioService.practice.active?"연습 종료 · 결과 저장":"연습 시작",()->{if(AudioService.practice!=null&&AudioService.practice.active){AudioService.practice.stop(false);showTab();}else if(!settingsLocked())ensureMic(5);else toast("현재 녹음 또는 예약을 마친 뒤 시작하세요.");});add(practiceButton);add(button("연습 결과 보기",()->showPracticeResult(AudioService.practice==null?null:AudioService.practice.result)));
        if(AudioService.practice!=null&&!AudioService.practice.error.isEmpty())note(AudioService.practice.error);
        if(AudioService.practice!=null&&AudioService.practice.result!=null){org.json.JSONObject result=AudioService.practice.result;note(result.has("accuracy")?String.format(Locale.US,"정확도 %.1f%% · 평균 오차 %.1f¢ · 발성 %.1f%%",result.optDouble("accuracy"),result.optDouble("meanAbsoluteCents"),result.optDouble("coverage")):"유효 발성이 부족해 정확도를 계산하지 않았습니다.");add(button("보관함 ›",()->chooseTab(4)));}
        note("목표음 대비 ±25센트 기준. 스피커·블루투스의 함께 부르기는 화면 가이드로 진행합니다.");
    }
    String noteName(int n){return new String[]{"C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B"}[n%12]+(n/12-1);}
    void showPracticeResult(org.json.JSONObject result){if(result==null){toast("연습을 마치면 결과를 확인할 수 있습니다.");return;}java.util.List<String> lines=new java.util.ArrayList<>();lines.add(result.has("accuracy")?String.format(Locale.US,"정확도 %.1f%% · 평균 오차 %.1f¢",result.optDouble("accuracy"),result.optDouble("meanAbsoluteCents")):"유효 발성이 부족합니다.");lines.add(String.format(Locale.US,"발성 비율 %.1f%%",result.optDouble("coverage")));org.json.JSONArray notes=result.optJSONArray("notes");if(notes!=null)for(int i=0;i<notes.length();i++){org.json.JSONObject n=notes.optJSONObject(i);if(n!=null)lines.add(noteName(n.optInt("targetMidi"))+" · "+(n.has("accuracy")?String.format(Locale.US,"%.1f%% · %.1f¢",n.optDouble("accuracy"),n.optDouble("meanAbsoluteCents")):"발성 부족"));}new AlertDialog.Builder(this).setTitle(result.optBoolean("complete")?"연습 완료":"부분 연습 결과").setItems(lines.toArray(new String[0]),null).setPositiveButton("닫기",null).show();}
    void syncTest(){if(settingsLocked())return;ensureMic(6);}
    String outputName(){return new String[]{"이어폰","휴대폰 스피커","블루투스 스피커"}[AudioRouting.selected(this)];}
    void monitoringOptions(){
        LinearLayout panel=column();panel.setPadding(dp(20),dp(8),dp(20),0);
        Button route=button("출력 모드: "+outputName()+"  ›",()->{});panel.addView(route);panel.addView(button("내 목소리 출력 켜기 / 끄기",()->{if(AudioService.active&&AudioService.canMonitor(this)){set(11,values[11]>.5?0:1);toast(values[11]>.5?"실시간 출력 켜짐":"실시간 출력 꺼짐");}else toast("마이크와 출력 장치를 먼저 켜세요.");}));
        TextView help=text(AudioRouting.selected(this)==0?"유선 / USB 이어폰을 연결하면 내 목소리를 실시간으로 들을 수 있습니다.":"휴대폰 마이크로 입력합니다. 시스템 스피커 볼륨도 낮게 시작하고 마이크와 스피커를 떨어뜨리세요. 블루투스는 전송 지연이 있습니다.",13,MUTED);help.setPadding(0,dp(12),0,dp(12));panel.addView(help);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("출력 설정").setView(panel).setPositiveButton("닫기",null).create();
        route.setEnabled(!RecordingManager.active&&!AudioService.waiting()&&!videoPreparing);
        route.setOnClickListener(v->{dialog.dismiss();outputMode();});
        if(AudioRouting.selected(this)>0){
            int gain=AudioService.active?Math.round(NativeAudio.stats()[20]*100):18;
            TextView label=text("스피커 마이크 출력 · "+gain+"%",14,TEXT);panel.addView(label);
            SeekBar level=new SeekBar(this);level.setMax(65);level.setProgress(gain);level.setEnabled(AudioService.active);level.setContentDescription("스피커 마이크 출력, 최대 65퍼센트");level.setProgressTintList(ColorStateList.valueOf(LIME));level.setThumbTintList(ColorStateList.valueOf(LIME));
            level.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int value,boolean user){if(user){NativeAudio.monitorLevel(value/100f);label.setText("스피커 마이크 출력 · "+value+"%");}}});panel.addView(level,new LinearLayout.LayoutParams(-1,dp(48)));
            Button reset=button("보호 음소거 해제 · 18%로 재시작",()->{NativeAudio.resetFeedback();set(11,1);dialog.dismiss();toast("18% 출력으로 재시작합니다. 시스템 볼륨도 낮게 유지하세요.");});reset.setEnabled(AudioService.active);panel.addView(reset);
            TextView protection=text("피드백을 감지하면 필터를 적용하고 필요 시 마이크 출력만 음소거합니다. 녹음은 계속됩니다.\n기기 에코 제거: "+(AudioService.active?(AudioService.echoCancellation?"활성":"미지원"):"마이크를 켜면 확인"),12,MUTED);protection.setPadding(0,dp(12),0,dp(8));panel.addView(protection);
        }
        dialog.show();
    }
    void outputMode(){
        if(settingsLocked()){toast("예약 또는 녹음을 마친 뒤 출력 모드를 바꾸세요.");return;}
        new AlertDialog.Builder(this).setTitle("내 목소리를 어디로 보낼까요?").setItems(new String[]{"이어폰 · 지연이 가장 적음","휴대폰 스피커 · 하울링 보호","블루투스 스피커 · 전송 지연 있음"},(d,mode)->{
            if(mode==2){List<AudioDeviceInfo> devices=AudioRouting.bluetooth(this);if(devices.isEmpty()){toast("Android 설정에서 블루투스 스피커를 연결한 뒤 다시 선택하세요.");startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));return;}
                String[] names=new String[devices.size()];for(int i=0;i<names.length;i++)names[i]=devices.get(i).getProductName().toString();
                new AlertDialog.Builder(this).setTitle("연결된 블루투스 출력").setItems(names,(a,i)->applyOutputMode(2,devices.get(i).getId())).show();
            }else applyOutputMode(mode,0);
        }).show();
    }
    void applyOutputMode(int mode,int device){
        if(settingsLocked())return;
        set(11,0);AudioRouting.prefs(this).edit().putInt("mode",mode).putInt("bluetoothDevice",device).apply();
        if(AudioService.active)startService(new Intent(this,AudioService.class).setAction(AudioService.ROUTE).putExtra("device",inputDevice));
        showTab();if(mode>0)toast("스피커 출력은 18%로 준비됩니다. 실시간 출력 스위치를 켜고 시스템 볼륨도 낮춰주세요.");
    }
    void recordingOptions(){
        if(settingsLocked()){toast("예약 또는 녹음을 마친 뒤 설정하세요.");return;}
        LinearLayout panel=column();panel.setPadding(dp(20),dp(12),dp(20),0);
        panel.addView(text("녹음 시작",15,TEXT));RadioGroup delays=new RadioGroup(this);delays.setOrientation(LinearLayout.HORIZONTAL);
        int[] options={0,3,5,10};for(int value:options){RadioButton b=new RadioButton(this);b.setId(100+value);b.setText(value==0?"즉시":value+"초");b.setTextColor(TEXT);b.setTextSize(13);delays.addView(b);if(recordDelay==value)delays.check(b.getId());}panel.addView(delays);
        Switch source=new Switch(this);source.setText("마이크 + 다른 앱 소리");source.setTextColor(TEXT);source.setChecked(captureOtherApps);source.setPadding(0,dp(12),0,dp(8));
        if(CaptureSupport.available())panel.addView(source);
        TextView details=text(CaptureSupport.available()?"Android 공유 권한이 매번 필요합니다. 화면은 저장하지 않으며, 캡처를 허용하는 앱의 오디오만 함께 담습니다.":"이 버전은 마이크만 녹음합니다. 이어폰으로 듣는 외부 반주는 저장되지 않습니다.",12,MUTED);panel.addView(details);
        TextView gain=text("녹음에 담을 다른 앱 소리: "+Math.round(captureGain*100)+"%",12,TEXT);SeekBar slider=new SeekBar(this);slider.setMax(150);slider.setProgress(Math.round(captureGain*100));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int p,boolean user){gain.setText("녹음에 담을 다른 앱 소리: "+p+"%");}});
        if(CaptureSupport.available()){panel.addView(gain);panel.addView(slider);}
        panel.addView(text("카운트다운이 시작되면 다른 앱으로 이동해 재생하세요. 알림에서 예약 취소·녹음 저장이 가능합니다.",12,MUTED));
        new AlertDialog.Builder(this).setTitle("백그라운드 녹음").setView(panel).setPositiveButton("설정 저장",(d,w)->{
            recordDelay=Math.max(0,delays.getCheckedRadioButtonId()-100);captureOtherApps=CaptureSupport.available()&&source.isChecked();captureGain=slider.getProgress()/100f;
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
    void backing(){add(title("반주"));String[] sources={"내장 연습 반주","내 파일","YouTube · SoundCloud"};add(button(sources[sourceTab]+" ▾",()->new AlertDialog.Builder(this).setItems(sources,(d,w)->{sourceTab=w;showTab();}).show()));
        if(sourceTab==1){fileBacking();return;}if(sourceTab==2){youtubeBacking();return;}
        add(button("스타일: "+BackingCatalog.GENRES[genre]+" ▾",()->new AlertDialog.Builder(this).setItems(BackingCatalog.GENRES,(d,w)->{genre=w;showTab();}).show()));
        LinearLayout prev=beginCard();add(text("연습용 합성 반주",16,TEXT));add(button(values[13]>.5?"■  합성 반주 정지":"▶  합성 반주 재생",()->{if(values[13]>.5){set(13,0);showTab();return;}ensureMic(3);}));
        slider("템포",15,40,200,v->Math.round(v)+" BPM");slider("전조",16,-6,6,v->{int n=Math.round(v);NativeAudio.set(16,n);return String.format(Locale.US,"%+d 반음",n);});toggle("메트로놈",17);endCard(prev);
    }
    void fileBacking(){LinearLayout heading=row();heading.addView(text("최근 불러온 반주",16,TEXT),new LinearLayout.LayoutParams(0,-2,1));heading.addView(text("WAV · MP3 · M4A 등",10,MUTED));add(heading);
        Button importButton=button("+  반주 파일 가져오기",this::pickBacking);StudioTheme.selected(importButton,true);add(importButton);
        if(!selectedFile.isEmpty()){LinearLayout prev=beginCard();add(text(selectedFile,16,LIME));add(button(NativeAudio.stats()[16]>.5?"Ⅱ  파일 일시정지":"▶  파일 재생",()->{if(NativeAudio.stats()[16]>.5){NativeAudio.playLocal(false);showTab();}else ensureMic(4);}));endCard(prev);}
        List<JSONObject> items=recent.list();if(items.isEmpty())note("아직 불러온 파일이 없어요. 반주 파일을 선택해 주세요.");
        for(JSONObject j:items){Uri uri=Uri.parse(j.optString("uri"));String name=j.optString("name");LinearLayout prev=beginCard();Button b=button(name,()->loadBacking(uri));b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setBackgroundTintList(ColorStateList.valueOf(PANEL));b.setOnLongClickListener(v->{new AlertDialog.Builder(this).setTitle("최근 목록에서 삭제할까요?").setMessage("원본 파일은 삭제하지 않습니다.").setPositiveButton("삭제",(d,w)->{recent.remove(uri);showTab();}).setNegativeButton("취소",null).show();return true;});add(b);note(new java.text.SimpleDateFormat("MM.dd HH:mm",Locale.KOREA).format(new Date(j.optLong("time")))+" · 눌러서 불러오기");endCard(prev);}
        note("기기가 지원하는 모노·스테레오 오디오 · 최대 8분");
    }
    void pickBacking(){if(RecordingManager.active||AudioService.waiting()||busy||videoPreparing){toast("녹음과 파일 준비를 마친 뒤 선택하세요.");return;}startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION),20);}
    void loadBacking(Uri uri){if(RecordingManager.active||AudioService.waiting()||busy||videoPreparing){toast("녹음을 마친 뒤 반주를 불러오세요.");return;}busy=true;String name=recent.name(uri);toast("반주 파일을 준비하고 있습니다…");io.execute(()->{File decodedFile=null;try{MediaTools.Decoded decoded=MediaTools.decode(this,uri);decodedFile=decoded.file;if(!NativeAudio.loadLocalFile(decoded.file.getAbsolutePath(),decoded.rate,decoded.channels))throw new IOException("반주를 불러오지 못했습니다.");recent.remember(uri,name);runOnUiThread(()->{selectedFile=name;toast("반주를 불러왔습니다.");});}catch(Exception e){runOnUiThread(()->{selectedFile="";new AlertDialog.Builder(this).setTitle("파일을 열 수 없어요").setMessage("파일이 이동·삭제되었거나 접근권한이 만료되었을 수 있습니다. 다시 가져와 주세요.").setPositiveButton("확인",null).setNegativeButton("목록에서 삭제",(d,w)->{recent.remove(uri);showTab();}).show();});}finally{if(decodedFile!=null)decodedFile.delete();runOnUiThread(()->{busy=false;if(tab==1)showTab();});}});}
    void youtubeBacking(){EditText link=new EditText(this);link.setSingleLine();link.setTextSize(14);link.setTextColor(TEXT);link.setHintTextColor(MUTED);link.setHint("YouTube 주소 또는 검색어");add(link);
        add(button("YouTube에서 열기 ↗",()->{String value=link.getText().toString().trim();if(value.isEmpty())value="노래방 MR";Uri uri=value.startsWith("https://youtu.be/")||value.startsWith("https://www.youtube.com/")?Uri.parse(value):Uri.parse("https://www.youtube.com/results?search_query="+Uri.encode(value));open(uri,"text/html");}));add(button("SoundCloud에서 열기 ↗",()->open(Uri.parse("https://soundcloud.com/"),"text/html")));note(BuildConfig.PLAYBACK_CAPTURE?"개인용 시작 설정에서 허용된 다른 앱 소리를 함께 녹음할 수 있습니다.":"배포용은 외부 앱 소리를 녹음하지 않습니다.");}
    void midi(){add(title("실시간 MIDI"));LinearLayout prev=beginCard();TextView caption=text("현재 음정",14,MUTED);caption.setGravity(Gravity.CENTER);add(caption);pitch=text("—",72,LIME);pitch.setTypeface(Typeface.create(StudioTheme.mono(this),700,false));pitch.setGravity(Gravity.CENTER);add(pitch);pitchDetails=text("음정 감지 대기",16,MUTED);pitchDetails.setTypeface(StudioTheme.mono(this));pitchDetails.setGravity(Gravity.CENTER);add(pitchDetails);pitchMeter=new PitchMeter(this);content.addView(pitchMeter,new LinearLayout.LayoutParams(-1,dp(28)));
        TextView scale=text("−50 ───────── 0 ───────── +50",14,MUTED);scale.setGravity(Gravity.CENTER);add(scale);endCard(prev);
        prev=beginCard();add(text("최근 12초",12,MUTED));visual=new StudioVisual(this,true);content.addView(visual,new LinearLayout.LayoutParams(-1,dp(170)));endCard(prev);toggle("신스 더블링",12);
        add(button("MIDI 저장",()->{File f=store.file("melody_"+System.currentTimeMillis(),".mid");if(NativeAudio.exportMidi(f.getAbsolutePath(),Math.round(values[15]),false))export(f);else toast("MIDI 저장 실패");}));
        Button clear=button("채보 지우기",()->{if(RecordingManager.active||AudioService.waiting()){toast("예약 또는 녹음 중에는 채보를 지울 수 없습니다.");return;}new AlertDialog.Builder(this).setTitle("채보를 지울까요?").setPositiveButton("지우기",(d,w)->NativeAudio.clearNotes()).setNegativeButton("취소",null).show();});add(clear);note("단선율 보컬 감지 · 잡음에 따라 오차가 생길 수 있어요.");
    }
    void cameraPage(){cameraFailed=false;add(title("카메라"));
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){LinearLayout prev=beginCard();add(text("카메라로 담는 나의 무대",20,TEXT));note("보컬 효과와 반주를 영상에도 그대로");add(button("카메라 권한 허용",()->requestPermissions(new String[]{Manifest.permission.CAMERA},11)));endCard(prev);return;}
        FrameLayout stage=new FrameLayout(this);stage.setBackground(bg(PANEL,24));stage.setClipToOutline(true);int previewHeight=Math.max(260,Math.min(480,(int)(getResources().getDisplayMetrics().heightPixels/getResources().getDisplayMetrics().density)-390));content.addView(stage,new LinearLayout.LayoutParams(-1,dp(previewHeight)));
        texture=new TextureView(this);stage.addView(texture,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout hud=row();hud.setPadding(dp(12),dp(12),dp(12),0);hud.setBackgroundColor(0x55000000);cameraTimer=text("영상 촬영",16,TEXT);cameraTimer.setTypeface(StudioTheme.mono(this));hud.addView(cameraTimer,new LinearLayout.LayoutParams(0,-2,1));flipButton=button("전·후면 ↻",this::flipCamera);hud.addView(flipButton,new LinearLayout.LayoutParams(dp(112),dp(48)));stage.addView(hud,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        gap();LinearLayout controls=row();pauseButton=button("Ⅱ 일시정지",this::pauseVideo);weighted(controls,pauseButton,56);videoButton=button("● 영상 촬영",this::toggleVideo);videoButton.setTextColor(BG);videoButton.setBackgroundTintList(ColorStateList.valueOf(RED));weighted(controls,videoButton,56);add(controls);
        note("영상 촬영을 시작하면 효과 오디오도 함께 녹음돼요.");
        camera=new CameraController(this,texture,new CameraController.Listener(){public void ready(){if(flipResumeAudio){RecordingManager.pause(false);videoPaused=false;flipResumeAudio=false;}if(pendingAction==2&&AudioService.active)tryPendingAction();}public void error(String t){cameraFailed=true;videoPreparing=false;pendingAction=0;flipResumeAudio=false;if(camera!=null&&camera.recording)stopVideo();toast(t);if(cameraTimer!=null)cameraTimer.setText("카메라 오류 · 재시도 필요");}});
    }
    void flipCamera(){if(camera==null||videoPreparing||busy)return;boolean resume=camera.recording&&!videoPaused;
        if(resume)RecordingManager.pause(true);
        if(camera.flip()){flipResumeAudio=resume;}else{if(resume)RecordingManager.pause(false);toast("카메라가 준비된 뒤 다시 시도해 주세요.");}}
    void pauseVideo(){if(camera==null||!camera.recording||camera.switching)return;boolean next=!videoPaused;
        if(next){RecordingManager.pause(true);if(!camera.pause(true)){RecordingManager.pause(false);return;}}
        else {if(!camera.pause(false))return;RecordingManager.pause(false);}videoPaused=next;}
    void library(){add(title("보관함"));if(busy)note("영상의 보컬 오디오를 합치는 중입니다…");List<JSONObject> takes=store.list();if(takes.isEmpty()){LinearLayout prev=beginCard();add(text("아직 녹음한 테이크가 없어요",20,TEXT));note("나만의 목소리를 담아 보세요.");add(button("첫 녹음 시작 →",()->chooseTab(0)));endCard(prev);return;}
        for(JSONObject j:takes){String id=j.optString("id");LinearLayout card=column();card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(bg(PANEL,14));TextView name=text(j.optString("name"),17,TEXT);card.addView(name);card.addView(text(formatTime(j.optDouble("seconds"))+"  ·  WAV + MIDI"+(store.file(id,".mp4").exists()?" + MP4":""),11,MUTED));
            if(j.has("practice")){org.json.JSONObject pr=j.optJSONObject("practice");TextView resultLabel=text(pr!=null&&pr.has("accuracy")?String.format(Locale.US,"연습 정확도 %.1f%% · 결과 보기 ›",pr.optDouble("accuracy")):"보컬 연습 · 결과 보기 ›",14,LIME);resultLabel.setMinHeight(dp(48));resultLabel.setGravity(Gravity.CENTER_VERTICAL);resultLabel.setOnClickListener(v->showPracticeResult(pr));card.addView(resultLabel);}
            if(!j.optString("warning").isEmpty())card.addView(text(j.optString("warning"),11,RED));
            LinearLayout actions=row();weighted(actions,button("▶ 재생",()->play(store.file(id,".wav"))),48);actions.addView(button("⋯",()->takeMenu(j)),new LinearLayout.LayoutParams(dp(48),dp(48)));card.addView(actions);add(card);gap();}
    }
    void takeMenu(JSONObject j){String id=j.optString("id");new AlertDialog.Builder(this).setTitle(j.optString("name")).setItems(new String[]{"오디오 재생 / 정지","WAV 저장","MIDI 저장","MP4 재생","MP4 저장","공유","이름 변경","삭제"},(d,w)->{File wav=store.file(id,".wav"),video=store.file(id,".mp4");if(w==0){play(wav);}else if(w==1)export(wav);else if(w==2)export(store.file(id,".mid"));else if(w==3){if(video.exists())openFile(video);else toast("영상이 없는 테이크입니다.");}else if(w==4)export(video);else if(w==5)share(video.exists()?video:wav);else if(w==6){EditText input=new EditText(this);input.setText(j.optString("name"));new AlertDialog.Builder(this).setTitle("테이크 이름").setView(input).setPositiveButton("저장",(a,b)->{try{store.rename(j,input.getText().toString().trim());showTab();}catch(Exception e){toast(e.getMessage());}}).setNegativeButton("취소",null).show();}else new AlertDialog.Builder(this).setTitle("이 테이크를 삭제할까요?").setMessage("오디오·MIDI·영상이 기기에서 삭제됩니다.").setPositiveButton("삭제",(a,b)->{stopPlayer();store.delete(id);showTab();}).setNegativeButton("취소",null).show();}).show();}
    void toggleMic(){if(starting||busy||testActive)return;pendingAction=0;if(AudioService.active){if(camera!=null&&camera.recording)stopVideo();else finishAudio();stopService(new Intent(this,AudioService.class));set(11,0);set(13,0);return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){awaitingMicPermission=true;requestPermissions(Build.VERSION.SDK_INT>=33?new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.POST_NOTIFICATIONS}:new String[]{Manifest.permission.RECORD_AUDIO},10);return;}
        starting=true;pendingMonitor=AudioService.canMonitor(this);startForegroundService(new Intent(this,AudioService.class).putExtra("device",inputDevice).putExtra("monitor",pendingMonitor));handler.postDelayed(()->starting=false,1500);
    }
    boolean requireMic(){if(!AudioService.active||NativeAudio.stats()[10]<.5){toast("스튜디오에서 마이크를 켜주세요.");return false;}return true;}
    void ensureMic(int action){if(busy||videoPreparing)return;if(AudioService.active&&NativeAudio.stats()[10]>.5){if(action==5)practiceOwnsMic=false;pendingAction=action;actionDeadline=SystemClock.elapsedRealtime()+10000;tryPendingAction();return;}
        if(action==5)practiceOwnsMic=true;if(!starting)toggleMic();pendingAction=action;actionDeadline=SystemClock.elapsedRealtime()+10000;}
    void tryPendingAction(){if(pendingAction==0||!AudioService.active||NativeAudio.stats()[10]<.5||busy||videoPreparing)return;int action=pendingAction;
        if(action==2&&(camera==null||!camera.ready))return;pendingAction=0;
        if(action==1){stopPlayer();requestRecording();}
        else if(action==2)startVideo();
        else if(action==3){NativeAudio.playLocal(false);set(14,BackingCatalog.SYNTH_STYLES[genre]);set(15,BackingCatalog.TEMPOS[genre]);set(13,1);showTab();}
        else if(action==5){set(13,0);set(17,0);set(12,0);AudioService.practice.ownedMic=practiceOwnsMic;practiceOwnsMic=false;AudioService.practice.start(practiceRoot,practiceBpm,practiceMode,practicePattern,practiceRecord);showTab();}
        else if(action==6){runSyncTest();}
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
    void selectInput(){if(AudioService.active){toast("마이크를 끈 뒤 입력 장치를 선택하세요.");return;}AudioDeviceInfo[] all=((AudioManager)getSystemService(AUDIO_SERVICE)).getDevices(AudioManager.GET_DEVICES_INPUTS);String[] names=new String[all.length+1];names[0]="시스템 기본";for(int i=0;i<all.length;i++)names[i+1]=all[i].getProductName()+" · "+all[i].getId();new AlertDialog.Builder(this).setTitle("마이크 입력").setItems(names,(d,w)->{inputDevice=w==0?0:all[w-1].getId();toast(names[w]);}).show();}
    String formatTime(double seconds){int s=(int)Math.max(0,seconds);return String.format(Locale.US,"%02d:%02d",s/60,s%60);}
    final Runnable tick=new Runnable(){public void run(){float[] s=NativeAudio.stats();boolean running=s[10]>.5;values[11]=s[21];
        boolean wired=AudioService.hasWiredOutput(MainActivity.this);if(headphones&&!wired&&AudioRouting.selected(MainActivity.this)==0)set(11,0);headphones=wired;
        if(running&&!wasRunning){starting=false;values[11]=s[21];pendingMonitor=false;}
        if(!running&&wasRunning){if(camera!=null&&camera.recording)stopVideo();finishAudio();set(11,0);set(13,0);if(AudioService.active)stopService(new Intent(MainActivity.this,AudioService.class));}
        wasRunning=running;
        if(micButton!=null){micButton.setText(running?"마이크 끄기":starting?"연결 중…":"마이크 켜기");StudioTheme.selected(micButton,running);}
        if(monitorButton!=null){monitorButton.setText(s[17]>.5?"보호 음소거":values[11]>.5?"모니터링 켜짐":"모니터링 꺼짐");StudioTheme.selected(monitorButton,values[11]>.5&&s[17]<.5);monitorButton.setEnabled(running&&AudioService.canMonitor(MainActivity.this));}
        if(levelText!=null){float db=20*(float)Math.log10(Math.max(.00001f,s[3]));levelText.setText(s[3]>.00001?String.format(Locale.US,"%.0f dB",db):"— dB");levelText.setTextColor(s[3]>.8?RED:LIME);}
        if(tab==5&&trainingReadout!=null&&AudioService.practice!=null){PracticeSession pr=AudioService.practice;if(practiceButton!=null)practiceButton.setText(pr.active?"연습 종료 · 결과 저장":"연습 시작");if(pr.active){if(practicePlot!=null)practicePlot.sample(s[15],pr.target,s[0],!Double.isNaN(pr.score.cents));trainingReadout.setText(pr.phase+"\n목표 "+noteName(pr.target)+" · "+(s[0]>0?noteName((int)s[2]):"—")+"\n"+(Double.isNaN(pr.score.cents)?"입력 대기":String.format(Locale.US,"%+.0f¢",pr.score.cents)+(Double.isNaN(pr.score.accuracy())?"":" · "+String.format(Locale.US,"%.1f%%",pr.score.accuracy()))));}else if(pr.index>=0){trainingReadout.setText(pr.phase+"\n"+(Double.isNaN(pr.score.accuracy())?"발성 부족":String.format(Locale.US,"정확도 %.1f%% · 발성 %.1f%%",pr.score.accuracy(),pr.score.coverage())));}}
        transport.setVisibility((tab==0||((RecordingManager.active||AudioService.waiting())&&tab!=3&&tab!=5))?View.VISIBLE:View.GONE);
        if(pendingAction!=0){if(!awaitingMicPermission&&SystemClock.elapsedRealtime()>actionDeadline){pendingAction=0;toast("장치를 시작하지 못했습니다. 권한과 연결을 확인해 주세요.");}else tryPendingAction();}
        if(status!=null)status.setText(running?String.format(Locale.US,"● LIVE  ·  %.0f Hz  ·  출력 버퍼 %.1f ms  ·  %s",s[6],s[7],"출력 설정 ›"):"출력: "+outputName()+" · 출력 설정 ›");
        if(!AudioService.error.isEmpty()){toast(AudioService.error);AudioService.error="";}
        boolean waiting=AudioService.waiting();timer.setText(waiting?AudioService.remaining()+"초":formatTime(RecordingManager.active?s[8]:0)+String.format(Locale.US,".%d",(int)((RecordingManager.active?s[8]:0)*10)%10));
        recButton.setText(busy?"저장 / 준비 중…":waiting?"예약 취소":RecordingManager.active?"■ 녹음 저장":recordDelay==0?"● 오디오 녹음":"● "+recordDelay+"초 후 녹음");recButton.setTextColor(RecordingManager.active?RED:TEXT);recButton.setEnabled(!busy&&!videoPreparing&&!recordPermissionPending&&!starting);
        recordOptions.setEnabled(!busy&&!waiting&&!RecordingManager.active&&!recordPermissionPending);
        if(status!=null){if(waiting)status.setText(AudioService.remaining()+"초 후 녹음 · 다른 앱으로 이동해 재생하세요");
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
        if(visual!=null){visual.stats=s;if(visual.piano)visual.notes=NativeAudio.roll();visual.invalidate();}
        handler.postDelayed(this,80);
    }};
    void play(File f){if(player!=null){stopPlayer();return;}if(RecordingManager.active||AudioService.waiting()){toast("예약 또는 녹음을 마친 뒤 재생하세요.");return;}set(11,0);set(13,0);NativeAudio.playLocal(false);try{player=new MediaPlayer();player.setDataSource(f.getAbsolutePath());player.setOnCompletionListener(p->stopPlayer());player.prepare();player.start();toast("테이크 재생 중 · 다시 누르면 정지");}catch(Exception e){stopPlayer();toast("재생 실패");}}
    void stopPlayer(){if(player!=null){player.release();player=null;}}
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
    @Override protected void onDestroy(){handler.removeCallbacks(tick);handler.removeCallbacks(pendingSource);if(camera!=null)camera.release();stopPlayer();io.shutdown();super.onDestroy();}
}
