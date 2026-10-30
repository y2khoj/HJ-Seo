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

/** Functional native checkpoint UI. Screen composition can be replaced from the Figma specification. */
public class MainActivity extends Activity {
    static final int BG=0xff101411,PANEL=0xff1c231e,TEXT=0xfff0f3ed,MUTED=0xff9ba99c,LIME=0xffd0f56b,RED=0xffff7d7d;
    final Handler handler=new Handler(Looper.getMainLooper());final ExecutorService io=Executors.newSingleThreadExecutor();
    final float[] values={1,.55f,.27f,2.2f,.14f,280,.25f,0,0,1,1,0,0,0,0,78,0,0,0,1};
    LinearLayout root,content,nav;Button micButton,recButton,recordOptions;TextView status,pitch,timer;StudioVisual visual;
    int tab=0,inputDevice=0,recordDelay=0,lastRecordingRevision=-1,lastRouteRevision=-1;boolean captureOtherApps=false,recordPermissionPending=false;float captureGain=.7f;String videoTakeId=null;boolean starting=false,busy=false,wasRunning=false,headphones=false,pendingMonitor=false,videoPreparing=false;
    CameraController camera;TextureView texture;File rawVideo,exportFile;TakeStore store;MediaPlayer player;
    @Override public void onCreate(Bundle state){super.onCreate(state);store=new TakeStore(this);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        recordDelay=getPreferences(0).getInt("recordDelay",0);captureOtherApps=BuildConfig.PLAYBACK_CAPTURE&&getPreferences(0).getBoolean("captureOtherApps",false);captureGain=getPreferences(0).getFloat("captureGain",.7f);
        for(int i=0;i<values.length;i++)if(i!=11&&i!=13&&i!=12)values[i]=getPreferences(0).getFloat("p"+i,values[i]);
        if(!AudioService.active)for(int i=0;i<values.length;i++)NativeAudio.set(i,values[i]);build();handler.post(tick);
    }
    int dp(float n){return(int)(getResources().getDisplayMetrics().density*n+.5f);}
    GradientDrawable bg(int c,int r){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(r));return d;}
    TextView text(String t,int size,int color){TextView v=new TextView(this);v.setText(t);v.setTextSize(size);v.setTextColor(color);v.setLineSpacing(dp(3),1);return v;}
    TextView title(String t){TextView v=text(t,22,TEXT);v.setTypeface(null,Typeface.BOLD);v.setPadding(0,dp(12),0,dp(10));return v;}
    LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    Button button(String t,Runnable r){Button b=new Button(this);b.setText(t);b.setTextSize(13);b.setTextColor(TEXT);b.setAllCaps(false);b.setMinHeight(dp(46));b.setPadding(dp(10),dp(5),dp(10),dp(5));b.setBackgroundTintList(ColorStateList.valueOf(PANEL));b.setOnClickListener(v->r.run());return b;}
    void add(View v){content.addView(v,new LinearLayout.LayoutParams(-1,-2));}
    void gap(){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,dp(12)));}
    void note(String t){TextView v=text(t,12,MUTED);v.setPadding(0,dp(8),0,dp(10));add(v);}
    void build(){root=column();root.setBackgroundColor(BG);root.setPadding(dp(16),0,dp(16),0);
        root.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());root.setPadding(dp(16)+i.left,i.top,dp(16)+i.right,i.bottom);}return insets;});setContentView(root);
        LinearLayout header=row();TextView brand=text(BuildConfig.PLAYBACK_CAPTURE?"LiveMic 개인용":"LiveMic PRO",20,LIME);brand.setTypeface(null,Typeface.BOLD);header.addView(brand,new LinearLayout.LayoutParams(0,dp(64),1));brand.setGravity(Gravity.CENTER_VERTICAL);
        Button mute=button("음소거",()->{set(11,0);toast("마이크 모니터링을 껐습니다.");});header.addView(mute);
        micButton=button("마이크 켜기",this::toggleMic);header.addView(micButton);root.addView(header);
        status=text("마이크 OFF · 유선 / USB 이어폰 권장",11,MUTED);root.addView(status);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);content=column();content.setPadding(0,dp(8),0,dp(12));scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout transport=row();recButton=button("●  오디오 녹음",this::toggleRecord);recButton.setTextColor(RED);transport.addView(recButton,new LinearLayout.LayoutParams(0,dp(54),1));recordOptions=button("시작 설정",this::recordingOptions);transport.addView(recordOptions);timer=text("00:00",17,TEXT);timer.setTypeface(Typeface.MONOSPACE);timer.setPadding(dp(12),0,dp(8),0);transport.addView(timer);root.addView(transport);
        nav=row();String[] labels={"스튜디오","반주","MIDI","카메라","보관함"};for(int i=0;i<labels.length;i++){final int k=i;Button b=button(labels[i],()->chooseTab(k));b.setTextSize(11);nav.addView(b,new LinearLayout.LayoutParams(0,dp(54),1));}root.addView(nav);showTab();
    }
    void chooseTab(int k){if(videoPreparing){toast("카메라를 준비하고 있습니다.");return;}if(camera!=null&&camera.recording){new AlertDialog.Builder(this).setTitle("녹화를 마칠까요?").setMessage("영상을 저장한 뒤 화면을 이동합니다.").setPositiveButton("저장 후 이동",(d,w)->{stopVideo();tab=k;showTab();}).setNegativeButton("계속 녹화",null).show();return;}tab=k;showTab();}
    void showTab(){setVolumeControlStream(AudioRouting.selected(this)==1?AudioManager.STREAM_VOICE_CALL:AudioManager.STREAM_MUSIC);if(camera!=null){camera.release();camera=null;}content.removeAllViews();visual=null;pitch=null;texture=null;
        for(int i=0;i<nav.getChildCount();i++)((Button)nav.getChildAt(i)).setTextColor(i==tab?LIME:MUTED);
        if(tab==0)studio();else if(tab==1)backing();else if(tab==2)midi();else if(tab==3)cameraPage();else library();
    }
    void set(int k,float v){values[k]=v;NativeAudio.set(k,v);if(k!=11&&k!=12&&k!=13)getPreferences(0).edit().putFloat("p"+k,v).apply();}
    interface Formatter{String value(float v);}
    void slider(String label,int k,float min,float max,Formatter f){LinearLayout box=column();TextView caption=text(label+"   "+f.value(values[k]),13,TEXT);box.addView(caption);SeekBar s=new SeekBar(this);s.setMax(1000);s.setProgress(Math.round((values[k]-min)/(max-min)*1000));s.setProgressTintList(ColorStateList.valueOf(LIME));s.setThumbTintList(ColorStateList.valueOf(LIME));s.setContentDescription(label);s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int progress,boolean user){if(user){float v=min+(max-min)*progress/1000;set(k,v);caption.setText(label+"   "+f.value(v));}}});box.addView(s,new LinearLayout.LayoutParams(-1,dp(40)));add(box);}
    void toggle(String label,int k){Switch s=new Switch(this);s.setText(label);s.setTextColor(TEXT);s.setTextSize(13);s.setPadding(0,dp(10),0,dp(10));s.setChecked(values[k]>.5);s.setOnCheckedChangeListener((b,on)->{if(k==11&&on&&!AudioService.canMonitor(this)){s.setChecked(false);toast("이어폰을 연결하거나 출력 모드에서 스피커를 선택하세요.");return;}set(k,on?1:0);});add(s);}
    void studio(){add(title("내 목소리, 내 사운드."));note(BuildConfig.PLAYBACK_CAPTURE?"개인용 · 다른 앱 소리 캡처 선택 가능":"마이크 전용 배포 버전 · 백그라운드 예약 녹음");visual=new StudioVisual(this,false);content.addView(visual,new LinearLayout.LayoutParams(-1,dp(140)));gap();
        add(button("출력 모드: "+new String[]{"이어폰","휴대폰 스피커","블루투스 스피커"}[AudioRouting.selected(this)],this::outputMode));
        toggle("내 목소리 실시간 출력",11);
        if(AudioRouting.selected(this)>0){
            note("휴대폰 내장 마이크를 사용하며 낮은 출력으로 시작합니다. 마이크와 스피커를 떨어뜨리세요. 블루투스는 전송 지연이 있습니다. 하울링 완전 제거를 보장하지 않습니다.");
            TextView label=text("스피커 마이크 출력 · 18%에서 시작",13,TEXT);add(label);SeekBar level=new SeekBar(this);level.setMax(65);level.setProgress(AudioService.active?Math.round(NativeAudio.stats()[20]*100):18);
            level.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int p,boolean user){if(user){NativeAudio.monitorLevel(p/100f);label.setText("스피커 마이크 출력 · "+p+"%");}}});add(level);
            add(button("보호 음소거 해제 · 낮은 출력으로 재시작",()->{NativeAudio.resetFeedback();set(11,1);showTab();toast("18% 출력으로 재시작합니다. 시스템 스피커 볼륨도 낮게 유지하세요.");}));
        }else note("이어폰 분리 시 마이크 출력이 자동으로 꺼집니다.");
        add(text("보컬 프리셋",14,LIME));String[] presets={"노래방","발라드","콘서트 홀","팝 보컬","대성당","생목소리"};
        for(int r=0;r<2;r++){LinearLayout line=row();for(int j=0;j<3;j++){int p=r*3+j;line.addView(button(presets[p],()->preset(p)),new LinearLayout.LayoutParams(0,dp(46),1));}add(line);}gap();
        slider("마이크 게인",0,0,2,v->Math.round(v*100)+"%");slider("반주 볼륨",1,0,1,v->Math.round(v*100)+"%");
        add(title("공간 & 에코"));slider("리버브 믹스",2,0,1,v->Math.round(v*100)+"%");slider("잔향 시간",3,.3f,6,v->String.format(Locale.US,"%.1f s",v));
        add(button("공간 선택: 룸 / 홀 / 플레이트 / 대성당",()->new AlertDialog.Builder(this).setTitle("리버브 공간").setItems(new String[]{"홀","룸","플레이트","대성당"},(d,w)->set(18,w)).show()));
        slider("에코 믹스",4,0,.8f,v->Math.round(v*100)+"%");slider("에코 간격",5,70,700,v->Math.round(v)+" ms");slider("에코 반복",6,0,.7f,v->Math.round(v*100)+"%");
        add(title("톤 & 다이내믹"));slider("저음 · 180 Hz",7,-12,12,v->String.format(Locale.US,"%+.1f dB",v));slider("중음 · 1.4 kHz",8,-12,12,v->String.format(Locale.US,"%+.1f dB",v));slider("고음 · 6.5 kHz",9,-12,12,v->String.format(Locale.US,"%+.1f dB",v));toggle("컴프레서",10);toggle("룸 노이즈 완화",19);
        add(button("마이크 입력 장치 선택",this::selectInput));note("표시 지연은 출력 버퍼 길이입니다. 마이크 입력·변환을 포함한 실제 왕복 지연과 다릅니다.");
    }
    void outputMode(){
        if(RecordingManager.active||AudioService.waiting()||videoPreparing){toast("예약 또는 녹음을 마친 뒤 출력 모드를 바꾸세요.");return;}
        new AlertDialog.Builder(this).setTitle("내 목소리를 어디로 보낼까요?").setItems(new String[]{"이어폰 · 지연이 가장 적음","휴대폰 스피커 · 하울링 보호","블루투스 스피커 · 전송 지연 있음"},(d,mode)->{
            if(mode==2){List<AudioDeviceInfo> devices=AudioRouting.bluetooth(this);if(devices.isEmpty()){toast("Android 설정에서 블루투스 스피커를 연결한 뒤 다시 선택하세요.");startActivity(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));return;}
                String[] names=new String[devices.size()];for(int i=0;i<names.length;i++)names[i]=devices.get(i).getProductName().toString();
                new AlertDialog.Builder(this).setTitle("연결된 블루투스 출력").setItems(names,(a,i)->applyOutputMode(2,devices.get(i).getId())).show();
            }else applyOutputMode(mode,0);
        }).show();
    }
    void applyOutputMode(int mode,int device){
        if(RecordingManager.active||AudioService.waiting())return;
        set(11,0);AudioRouting.prefs(this).edit().putInt("mode",mode).putInt("bluetoothDevice",device).apply();
        if(AudioService.active)startService(new Intent(this,AudioService.class).setAction(AudioService.ROUTE).putExtra("device",inputDevice));
        showTab();if(mode>0)toast("스피커 출력은 18%로 준비됩니다. 실시간 출력 스위치를 켜고 시스템 볼륨도 낮춰주세요.");
    }
    void preset(int p){float[][] options={{.2f,1.5f,.34f,300,.38f,0,0,2},{.3f,2.4f,.14f,280,.25f,0,-1,2},{.42f,3.7f,.1f,340,.25f,1,-1,1},{.12f,.9f,.12f,140,.18f,-1,1,3},{.58f,5.5f,.15f,410,.35f,1,-2,0},{0,.3f,0,150,0,0,0,0}};int[] keys={2,3,4,5,6,7,8,9};for(int i=0;i<keys.length;i++)set(keys[i],options[p][i]);set(10,p==5?0:1);set(18,p==4?3:p==3?2:0);showTab();toast("프리셋을 적용했습니다.");}
    void backing(){add(title("함께 연주하는 밴드"));note("내장 반주와 로컬 반주는 보컬과 함께 녹음됩니다.");String[] styles={"감성 발라드","시티팝 / R&B","어쿠스틱","댄스"};
        for(int i=0;i<4;i++){final int k=i;add(button((int)values[14]==i?"✓  "+styles[i]:styles[i],()->{set(14,k);set(15,new int[]{78,96,100,124}[k]);showTab();}));}
        add(button(values[13]>.5?"■ 밴드 정지":"▶ 밴드 재생",()->{if(!requireMic())return;NativeAudio.playLocal(false);set(13,values[13]>.5?0:1);showTab();}));
        slider("템포",15,40,200,v->Math.round(v)+" BPM");slider("키 조절",16,-6,6,v->{int n=Math.round(v);NativeAudio.set(16,n);return String.format(Locale.US,"%+d 반음",n);});toggle("메트로놈",17);
        add(title("내 반주 파일"));add(button("오디오 파일 불러오기",()->{if(RecordingManager.active||AudioService.waiting()){toast("예약 또는 녹음을 마친 뒤 파일을 선택하세요.");return;}Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,20);}));
        add(button("▶ 파일 재생 / 일시정지",()->{if(!requireMic())return;boolean play=NativeAudio.stats()[16]<.5;set(13,0);NativeAudio.playLocal(play);toast(play?"선택한 반주를 재생합니다.":"반주 일시정지");}));note("MP3 · M4A · WAV 등 기기 지원 형식, 최대 8분. 가져온 파일은 기기 안에서 처리합니다.");
        add(title("YouTube 반주"));EditText link=new EditText(this);link.setSingleLine();link.setTextColor(TEXT);link.setHintTextColor(MUTED);link.setHint("YouTube 주소 또는 검색어");add(link);
        add(button("YouTube에서 열기",()->{String value=link.getText().toString().trim();if(value.isEmpty())value="노래방 MR";Uri uri=value.startsWith("https://youtu.be/")||value.startsWith("https://www.youtube.com/")?Uri.parse(value):Uri.parse("https://www.youtube.com/results?search_query="+Uri.encode(value));open(uri,"text/html");}));
        add(button("SoundCloud에서 열기",()->open(Uri.parse("https://soundcloud.com/"),"text/html")));
        note(BuildConfig.PLAYBACK_CAPTURE?"시작 설정에서 마이크 + 다른 앱 소리를 선택하면 Android가 캡처를 허용하는 소리만 함께 저장됩니다. 차단된 앱은 무음일 수 있습니다.":"배포용은 내 목소리만 녹음합니다. YouTube / SoundCloud 반주는 파일에 포함되지 않습니다.");
    }
    void midi(){add(title("목소리를 멜로디로"));pitch=text("—",46,LIME);pitch.setTypeface(Typeface.MONOSPACE,Typeface.BOLD);add(pitch);note("75–1,100 Hz 단선율 채보 · 440 Hz 기준");visual=new StudioVisual(this,true);content.addView(visual,new LinearLayout.LayoutParams(-1,dp(260)));gap();toggle("감지된 음정으로 신스 연주",12);
        add(button("MIDI 파일 저장",()->{String id="melody_"+System.currentTimeMillis();File f=store.file(id,".mid");if(NativeAudio.exportMidi(f.getAbsolutePath(),Math.round(values[15]),false))export(f);else toast("MIDI 저장 실패");}));
        add(button("채보 지우기",()->new AlertDialog.Builder(this).setTitle("채보를 지울까요?").setMessage("녹음 중이면 MIDI 결과에도 영향을 줍니다.").setPositiveButton("지우기",(d,w)->{if(RecordingManager.active||AudioService.waiting()){toast("예약 또는 녹음 중에는 채보를 지울 수 없습니다.");return;}NativeAudio.clearNotes();}).setNegativeButton("취소",null).show()));
        note("화음·반주가 마이크로 들어오면 오인식할 수 있습니다. DAW에서 노트와 타이밍을 다듬어 사용하세요. SMF Format 0 · 480 PPQ.");
    }
    void cameraPage(){add(title("노래하는 순간까지"));note("카메라 + 효과 보컬 + 반주 → MP4 영상");
        if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){add(button("카메라 권한 허용",()->requestPermissions(new String[]{Manifest.permission.CAMERA},11)));return;}
        texture=new TextureView(this);content.addView(texture,new LinearLayout.LayoutParams(-1,dp(360)));gap();
        camera=new CameraController(this,texture,new CameraController.Listener(){public void ready(){}public void error(String t){videoPreparing=false;toast(t);}});
        add(button("전면 / 후면 전환",()->{if(camera!=null&&!camera.recording&&!videoPreparing)camera.flip();}));add(button("● 영상 녹화 / ■ 저장",this::toggleVideo));
        note("촬영 중에도 유선·USB 이어폰으로 모니터링할 수 있습니다. 화면을 벗어나면 녹화를 마칩니다. 저장 시 효과가 적용된 WAV를 AAC로 변환해 영상과 결합합니다.");
    }
    void library(){add(title("내 테이크"));if(busy)note("영상의 보컬 오디오를 합치는 중입니다…");List<JSONObject> takes=store.list();if(takes.isEmpty()){note("아직 저장한 테이크가 없습니다. 마이크를 켜고 아래 녹음 버튼을 눌러보세요.");return;}
        for(JSONObject j:takes){String id=j.optString("id");LinearLayout card=column();card.setPadding(dp(14),dp(12),dp(14),dp(12));card.setBackground(bg(PANEL,14));TextView name=text(j.optString("name"),17,TEXT);card.addView(name);card.addView(text(formatTime(j.optDouble("seconds"))+"  ·  WAV + MIDI"+(store.file(id,".mp4").exists()?" + MP4":""),11,MUTED));
            if(!j.optString("warning").isEmpty())card.addView(text(j.optString("warning"),11,RED));
            card.addView(button("▶ 재생 / 공유 / 파일 저장",()->takeMenu(j)));add(card);gap();}
    }
    void takeMenu(JSONObject j){String id=j.optString("id");new AlertDialog.Builder(this).setTitle(j.optString("name")).setItems(new String[]{"오디오 재생 / 정지","WAV 저장","MIDI 저장","MP4 재생","MP4 저장","공유","이름 변경","삭제"},(d,w)->{File wav=store.file(id,".wav"),video=store.file(id,".mp4");if(w==0){play(wav);}else if(w==1)export(wav);else if(w==2)export(store.file(id,".mid"));else if(w==3){if(video.exists())openFile(video);else toast("영상이 없는 테이크입니다.");}else if(w==4)export(video);else if(w==5)share(video.exists()?video:wav);else if(w==6){EditText input=new EditText(this);input.setText(j.optString("name"));new AlertDialog.Builder(this).setTitle("테이크 이름").setView(input).setPositiveButton("저장",(a,b)->{try{store.rename(j,input.getText().toString().trim());showTab();}catch(Exception e){toast(e.getMessage());}}).setNegativeButton("취소",null).show();}else new AlertDialog.Builder(this).setTitle("이 테이크를 삭제할까요?").setMessage("오디오·MIDI·영상이 기기에서 삭제됩니다.").setPositiveButton("삭제",(a,b)->{stopPlayer();store.delete(id);showTab();}).setNegativeButton("취소",null).show();}).show();}
    void toggleMic(){if(starting||busy)return;if(AudioService.active){if(camera!=null&&camera.recording)stopVideo();else finishAudio();stopService(new Intent(this,AudioService.class));set(11,0);set(13,0);return;}
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(Build.VERSION.SDK_INT>=33?new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.POST_NOTIFICATIONS}:new String[]{Manifest.permission.RECORD_AUDIO},10);return;}
        starting=true;pendingMonitor=AudioService.canMonitor(this);startForegroundService(new Intent(this,AudioService.class).putExtra("device",inputDevice).putExtra("monitor",pendingMonitor));handler.postDelayed(()->starting=false,1500);
    }
    boolean requireMic(){if(!AudioService.active||NativeAudio.stats()[10]<.5){toast("상단에서 마이크를 먼저 켜주세요.");return false;}return true;}
    void recordingOptions(){
        if(RecordingManager.active||AudioService.waiting()){toast("예약 또는 녹음을 마친 뒤 설정하세요.");return;}
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
    void toggleRecord(){if(videoPreparing||busy||starting||recordPermissionPending)return;
        if(camera!=null&&camera.recording){stopVideo();return;}
        if(AudioService.waiting()){startService(new Intent(this,AudioService.class).setAction(AudioService.CANCEL));return;}
        if(RecordingManager.active){finishAudio();return;}
        stopPlayer();requestRecording();
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
    void finishAudio(){if(AudioService.active){startService(new Intent(this,AudioService.class).setAction(AudioService.FINISH));return;}
        try{RecordingManager.Result r=RecordingManager.stop();if(r!=null)toast("오디오와 MIDI를 저장했습니다.");}catch(Exception e){toast("보관함 저장 실패: "+e.getMessage());}
    }
    void toggleVideo(){if(videoPreparing||busy)return;if(camera!=null&&camera.recording){stopVideo();return;}if(!requireMic())return;if(RecordingManager.active||AudioService.waiting()){toast("예약 또는 오디오 녹음을 마친 뒤 영상을 시작하세요.");return;}if(camera==null)return;
        stopPlayer();rawVideo=new File(store.directory,"pending_"+System.currentTimeMillis()+".video.mp4");videoPreparing=true;
        if(!camera.start(rawVideo,()->{videoPreparing=false;if(!RecordingManager.start(this,Math.round(values[15]))){camera.stop();toast("오디오를 시작하지 못해 영상 녹화를 중지했습니다.");}else videoTakeId=RecordingManager.id;})){videoPreparing=false;toast("카메라 미리보기가 준비된 뒤 다시 눌러주세요.");}
    }
    void stopVideo(){if(camera==null||!camera.recording)return;boolean videoOk=camera.stop();try{RecordingManager.Result r=RecordingManager.stop();if(r==null&&RecordingManager.lastResult!=null&&RecordingManager.lastResult.id.equals(videoTakeId))r=RecordingManager.lastResult;if(r==null)return;final RecordingManager.Result saved=r;File source=rawVideo;if(!videoOk){toast("영상이 너무 짧거나 중단되었습니다. 오디오는 보관함에서 확인하세요.");return;}
            busy=true;long offset=(long)(r.clock*1e6-camera.videoStartNanos/1000.);toast("영상을 저장하고 있습니다…");
            io.execute(()->{try{MediaTools.makeVideo(source,store.file(saved.id,".wav"),store.file(saved.id,".mp4"),offset);store.save(saved.id,"Video Take "+saved.id,saved.seconds,true,saved.warning);runOnUiThread(()->toast("MP4 · WAV · MIDI를 저장했습니다."));}catch(Exception e){store.file(saved.id,".mp4").delete();runOnUiThread(()->toast("영상 결합 실패. WAV와 MIDI는 보관되어 있습니다: "+e.getMessage()));}finally{runOnUiThread(()->{busy=false;if(tab==4)showTab();});}});
        }catch(Exception e){toast("녹화 저장 실패: "+e.getMessage());}}
    void selectInput(){if(AudioService.active){toast("마이크를 끈 뒤 입력 장치를 선택하세요.");return;}AudioDeviceInfo[] all=((AudioManager)getSystemService(AUDIO_SERVICE)).getDevices(AudioManager.GET_DEVICES_INPUTS);String[] names=new String[all.length+1];names[0]="시스템 기본";for(int i=0;i<all.length;i++)names[i+1]=all[i].getProductName()+" · "+all[i].getId();new AlertDialog.Builder(this).setTitle("마이크 입력").setItems(names,(d,w)->{inputDevice=w==0?0:all[w-1].getId();toast(names[w]);}).show();}
    String formatTime(double seconds){int s=(int)Math.max(0,seconds);return String.format(Locale.US,"%02d:%02d",s/60,s%60);}
    final Runnable tick=new Runnable(){public void run(){float[] s=NativeAudio.stats();boolean running=s[10]>.5;
        boolean wired=AudioService.hasWiredOutput(MainActivity.this);if(headphones&&!wired&&AudioRouting.selected(MainActivity.this)==0)set(11,0);headphones=wired;
        if(running&&!wasRunning){starting=false;values[11]=s[21];pendingMonitor=false;}
        if(!running&&wasRunning){if(camera!=null&&camera.recording)stopVideo();finishAudio();set(11,0);set(13,0);if(AudioService.active)stopService(new Intent(MainActivity.this,AudioService.class));}
        wasRunning=running;micButton.setText(running?"마이크 끄기":starting?"연결 중…":"마이크 켜기");micButton.setTextColor(running?LIME:TEXT);
        status.setText(running?String.format(Locale.US,"● LIVE  ·  %.0f Hz  ·  출력 버퍼 %.1f ms  ·  %s",s[6],s[7],wired?"이어폰 연결됨":"모니터링 꺼짐"):"마이크 OFF · 유선 / USB 이어폰 권장");
        if(!AudioService.error.isEmpty()){toast(AudioService.error);AudioService.error="";}
        boolean waiting=AudioService.waiting();timer.setText(waiting?AudioService.remaining()+"초":formatTime(RecordingManager.active?s[8]:0));
        recButton.setText(waiting?"예약 취소":RecordingManager.active?"■ 녹음 저장":recordDelay==0?"● 오디오 녹음":"● "+recordDelay+"초 후 녹음");recButton.setEnabled(!busy&&!videoPreparing&&!recordPermissionPending&&!starting);
        recordOptions.setEnabled(!busy&&!waiting&&!RecordingManager.active&&!recordPermissionPending);
        if(waiting)status.setText(AudioService.remaining()+"초 후 녹음 · 다른 앱으로 이동해 재생하세요");
        else if(RecordingManager.active&&AudioService.captureActive)status.setText(AudioService.captureLevel>.001f?"● 녹음 중 · 마이크 + 다른 앱 소리 입력 중":"● 녹음 중 · 다른 앱 소리 신호 없음 / 캡처 허용 확인");
        if(running&&s[19]>0)status.setText("출력: "+AudioService.routeLabel+" · 보호 필터 "+(int)s[18]+"개 · AEC "+(AudioService.echoCancellation?"ON":"미지원"));
        if(running&&s[17]>.5f)status.setText("하울링 보호 음소거 · 스튜디오에서 낮은 출력으로 재시작");
        if(lastRouteRevision!=AudioService.routeRevision){lastRouteRevision=AudioService.routeRevision;if(tab==0)showTab();}
        if(lastRecordingRevision!=AudioService.revision){lastRecordingRevision=AudioService.revision;if(tab==4)showTab();}
        if(camera!=null&&camera.recording&&!RecordingManager.active&&!videoPreparing)stopVideo();
        if(pitch!=null){int n=(int)s[2];String[] notes={"C","C♯","D","D♯","E","F","F♯","G","G♯","A","A♯","B"};pitch.setText(n>=0?notes[n%12]+(n/12-1)+String.format(Locale.US,"   %+.0f¢\n%.1f Hz",s[1],s[0]):"—\n목소리를 들려주세요");}
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
        if(request==20){busy=true;toast("반주 파일을 준비하고 있습니다…");io.execute(()->{try{MediaTools.Decoded decoded=MediaTools.decode(this,uri);boolean ok=NativeAudio.loadLocalFile(decoded.file.getAbsolutePath(),decoded.rate,decoded.channels);decoded.file.delete();runOnUiThread(()->toast(ok?"반주를 불러왔습니다.":"반주를 불러오지 못했습니다."));}catch(Exception e){runOnUiThread(()->toast(e.getMessage()));}finally{runOnUiThread(()->busy=false);}});}
        else if(request==21&&exportFile!=null){File f=exportFile;io.execute(()->{try(InputStream in=new FileInputStream(f);OutputStream out=getContentResolver().openOutputStream(uri)){if(out==null)throw new IOException("저장 위치를 열지 못했습니다.");byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);runOnUiThread(()->toast("파일을 저장했습니다."));}catch(Exception e){runOnUiThread(()->toast("파일 저장 실패: "+e.getMessage()));}});}}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants){super.onRequestPermissionsResult(request,permissions,grants);if(request==12){recordPermissionPending=false;if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)requestRecording();else toast("마이크 권한이 필요합니다.");return;}if(request==10&&checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)toggleMic();else if(request==11)showTab();else toast("설정에서 마이크 권한을 허용해주세요.");}
    void toast(String t){Toast.makeText(this,t==null?"오류가 발생했습니다.":t,Toast.LENGTH_LONG).show();}
    @Override public boolean onKeyDown(int k,android.view.KeyEvent e){if(k==android.view.KeyEvent.KEYCODE_ESCAPE){set(11,0);toast("마이크 모니터링 음소거");return true;}return super.onKeyDown(k,e);}
    @Override protected void onPause(){handler.removeCallbacks(tick);videoPreparing=false;if(camera!=null){if(camera.recording)stopVideo();camera.release();camera=null;}super.onPause();}
    @Override protected void onResume(){super.onResume();handler.removeCallbacks(tick);handler.post(tick);if(tab==3&&content!=null&&camera==null)showTab();}
    @Override protected void onDestroy(){handler.removeCallbacks(tick);if(camera!=null)camera.release();stopPlayer();io.shutdown();super.onDestroy();}
}
