package pro.livemic.studio;
import android.app.*;
import android.content.*;
import android.media.*;
import android.os.*;

/** Owns audio and scheduled recording even while the Activity is backgrounded or recreated. */
public class AudioService extends Service {
    public static final String RECORD="pro.livemic.RECORD",CANCEL="pro.livemic.CANCEL",FINISH="pro.livemic.FINISH",STOP="STOP",ROUTE="pro.livemic.ROUTE";
    public static volatile boolean active=false,captureActive=false;
    public static volatile String error="",message="";
    public static float captureLevel=0;public static String routeLabel="이어폰";public static boolean echoCancellation=false;public static int routeRevision=0;
    public static int revision=0;
    private static final RecordingCountdown countdown=new RecordingCountdown();
    public static boolean waiting(){return countdown.armed();}
    public static int remaining(){return countdown.remaining(SystemClock.elapsedRealtime());}
    private final Handler main=new Handler(Looper.getMainLooper());
    private AudioManager manager;private PowerManager.WakeLock wakeLock;private ExternalCapture capture;
    private int pendingTempo=78,lastRemaining=-1;private long lastNotification=0;
    private boolean destroying=false;private AudioRouting routing;
    private final AudioDeviceCallback devices=new AudioDeviceCallback(){
        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed){
            if(AudioRouting.selected(AudioService.this)==0&&!hasWiredOutput(AudioService.this))NativeAudio.set(11,0);
            if(routing!=null&&routing.mode==2)for(AudioDeviceInfo d:removed)if(d.getId()==routing.output){NativeAudio.set(11,0);message=error="블루투스 스피커 연결이 해제되어 마이크를 껐습니다.";stopSelf();break;}
        }
    };
    private final BroadcastReceiver noisy=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){NativeAudio.set(11,0);}};
    public static boolean hasWiredOutput(Context c){
        AudioManager m=(AudioManager)c.getSystemService(AUDIO_SERVICE);
        for(AudioDeviceInfo d:m.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
            int t=d.getType();if(t==AudioDeviceInfo.TYPE_WIRED_HEADPHONES||t==AudioDeviceInfo.TYPE_WIRED_HEADSET||t==AudioDeviceInfo.TYPE_USB_HEADSET||t==AudioDeviceInfo.TYPE_USB_DEVICE||t==AudioDeviceInfo.TYPE_USB_ACCESSORY)return true;
        }return false;
    }
    public static boolean canMonitor(Context c){return AudioRouting.selected(c)>0||hasWiredOutput(c);}
    @Override public void onCreate(){super.onCreate();manager=(AudioManager)getSystemService(AUDIO_SERVICE);manager.registerAudioDeviceCallback(devices,main);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        NotificationChannel channel=new NotificationChannel("live_audio","마이크 스튜디오",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(channel);
        wakeLock=((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"LiveMic:Recording");wakeLock.setReferenceCounted(false);
        main.post(pulse);
    }
    private PendingIntent action(String action,int request){return PendingIntent.getService(this,request,new Intent(this,AudioService.class).setAction(action),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}
    private Notification notification(){
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        String title=getString(R.string.app_name)+" · 마이크 사용 중",detail="효과 처리 중 · 탭하여 스튜디오 열기";
        if(waiting()){title=remaining()+"초 후 녹음 시작";detail="다른 앱으로 이동해 재생하세요 · "+(captureActive?"마이크 + 허용된 앱 소리":"마이크 녹음");}
        else if(RecordingManager.active){float seconds=NativeAudio.stats()[8];title=String.format(java.util.Locale.US,(RecordingManager.paused?"일시정지":"녹음 중")+" · %02d:%02d",(int)seconds/60,(int)seconds%60);detail=captureActive?(captureLevel>.001f?"내 목소리 + 다른 앱 소리 입력 중":"다른 앱 소리 신호 없음 · 재생/캡처 허용 확인"):"내 목소리를 녹음하고 있습니다.";}
        else if(!message.isEmpty())detail=message;else detail="출력: "+routeLabel+" · 탭하여 스튜디오 열기";
        if(active&&NativeAudio.stats()[17]>.5f)detail="하울링 보호로 마이크 출력만 음소거했습니다.";
        Notification.Builder builder=new Notification.Builder(this,"live_audio").setSmallIcon(R.drawable.ic_mic_status).setContentTitle(title).setContentText(detail).setStyle(new Notification.BigTextStyle().bigText(detail)).setOngoing(true).setContentIntent(open).setOnlyAlertOnce(true);
        if(waiting())builder.addAction(new Notification.Action.Builder(null,"예약 취소",action(CANCEL,2)).build());
        else if(RecordingManager.active)builder.addAction(new Notification.Action.Builder(null,"녹음 저장",action(FINISH,3)).build());
        return builder.addAction(new Notification.Action.Builder(null,"마이크 끄기",action(STOP,1)).build()).build();
    }
    private void foreground(boolean external){
        if(Build.VERSION.SDK_INT>=30||(Build.VERSION.SDK_INT>=29&&external))startForeground(41,notification(),CaptureSupport.serviceTypes(external));else startForeground(41,notification());
        lastNotification=SystemClock.elapsedRealtime();
    }
    private void notifyState(){getSystemService(NotificationManager.class).notify(41,notification());lastNotification=SystemClock.elapsedRealtime();}
    private void keepAwake(){if(!wakeLock.isHeld())wakeLock.acquire(32*60*1000L);}
    private void releaseAwake(){if(wakeLock.isHeld())wakeLock.release();}
    @Override public int onStartCommand(Intent intent,int flags,int id){
        String action=intent==null?"":intent.getAction();
        if(STOP.equals(action)){NativeAudio.set(11,0);countdown.cancel();stopSelf();return START_NOT_STICKY;}
        if(CANCEL.equals(action)){if(waiting()){cancelSchedule();if(active)foreground(false);}else if(active)notifyState();else stopSelf();return START_NOT_STICKY;}
        if(FINISH.equals(action)){android.util.Log.i("LiveMicRecord","Save requested");finishRecording("");if(active)foreground(false);else stopSelf();return START_NOT_STICKY;}
        if(ROUTE.equals(action)){
            if(waiting()||RecordingManager.active){error="예약 또는 녹음을 마친 뒤 출력 모드를 바꾸세요.";return START_NOT_STICKY;}
            if(active){NativeAudio.set(11,0);if(routing!=null){routing.close();routing=null;}NativeAudio.stop();active=false;}
        }
        boolean requested=RECORD.equals(action),external=requested&&intent.getBooleanExtra("capture",false);
        try {
            if(requested&&(waiting()||RecordingManager.active)){message="이미 예약 또는 녹음 중입니다.";return START_NOT_STICKY;}
            if(external&&(!BuildConfig.PLAYBACK_CAPTURE||!CaptureSupport.available()))throw new IllegalStateException("이 버전은 마이크 녹음 전용입니다.");
            foreground(external);
            if(!active){int device=intent==null?0:intent.getIntExtra("device",0);routing=new AudioRouting(this,device);routing.prepare();
                active=NativeAudio.start(routing.input,routing.output,routing.mode);
                if(!active)throw new IllegalStateException("선택한 오디오 경로를 열지 못했습니다. 장치 연결과 다른 녹음 앱을 확인하세요.");
                routing.attachEffects();NativeAudio.set(11,intent!=null&&intent.getBooleanExtra("monitor",false)&&canMonitor(this)?1:0);routeLabel=routing.label;echoCancellation=routing.echoEnabled();routeRevision++;

            }
            if(requested){
                message="";pendingTempo=Math.max(40,Math.min(240,intent.getIntExtra("bpm",78)));
                if(external){Intent consent=Build.VERSION.SDK_INT>=33?intent.getParcelableExtra("projection",Intent.class):intent.getParcelableExtra("projection");
                    NativeAudio.enablePlaybackCapture(true,intent.getFloatExtra("captureGain",.7f));
                    capture=CaptureSupport.start(this,consent,(int)NativeAudio.stats()[6],()->captureStopped());captureActive=true;
                }
                countdown.arm(intent.getIntExtra("delay",0),SystemClock.elapsedRealtime());keepAwake();lastRemaining=-1;revision++;checkStart();notifyState();
            }
        }catch(Exception e){error="녹음 준비 실패: "+e.getMessage();message=error;cancelSchedule();if(!active){if(routing!=null){routing.close();routing=null;}stopSelf();}else foreground(false);}
        return START_NOT_STICKY;
    }
    private void checkStart(){
        if(countdown.consumeIfDue(SystemClock.elapsedRealtime())){
            if(!RecordingManager.start(this,pendingTempo)){message=error="녹음을 시작하지 못했습니다. 저장 공간과 마이크 상태를 확인하세요.";closeCapture();releaseAwake();}
            else message="녹음을 시작했습니다.";
            revision++;notifyState();
        }
    }
    private void cancelSchedule(){boolean had=waiting();countdown.cancel();closeCapture();if(!RecordingManager.active)releaseAwake();if(had){message="예약 녹음을 취소했습니다.";revision++;}}
    private void closeCapture(){if(capture!=null){capture.close();capture=null;}captureActive=false;captureLevel=0;NativeAudio.enablePlaybackCapture(false,0);}
    private void captureStopped(){if(destroying)return;String reason="다른 앱 소리 캡처가 중단되어 녹음을 마쳤습니다.";
        if(waiting()){cancelSchedule();message=error="다른 앱 소리 캡처 권한이 해제되어 예약을 취소했습니다.";}else finishRecording(reason);
        if(active)foreground(false);
    }
    private void finishRecording(String warning){
        countdown.cancel();
        if(capture!=null&&!capture.receivedAudioDuringRecording())warning+=(warning.isEmpty()?"":"\n")+"다른 앱 소리가 감지되지 않았습니다. 재생되지 않았거나 해당 앱이 캡처를 허용하지 않았을 수 있습니다.";
        RecordingManager.extraWarning=warning;
        try{RecordingManager.Result result=RecordingManager.stop();if(result!=null){message=result.warning.isEmpty()?"오디오와 MIDI를 저장했습니다.":result.warning;revision++;}}
        catch(Exception e){android.util.Log.e("LiveMicRecord","Save failed",e);message=error="보관함 저장 실패: "+e.getMessage();}
        finally{closeCapture();releaseAwake();if(active&&!destroying)foreground(false);}
    }
    private final Runnable pulse=new Runnable(){@Override public void run(){if(destroying)return;
        if(active){float[] stats=NativeAudio.stats();
            if(stats[10]<.5f){message=error="오디오 장치 연결이 중단되어 녹음을 마쳤습니다.";finishRecording(message);stopSelf();return;}
            checkStart();stats=NativeAudio.stats();if(capture!=null)captureLevel=capture.level();
            if(RecordingManager.active){keepAwake();if(stats[8]>=1800)finishRecording("30분 녹음 한도에 도달해 저장했습니다.");}
            else if(!waiting()){if(capture!=null)closeCapture();releaseAwake();}
            int next=remaining();long now=SystemClock.elapsedRealtime();if(next!=lastRemaining||now-lastNotification>=1000){lastRemaining=next;notifyState();}
        }
        main.postDelayed(this,waiting()?50:500);
    }};
    @Override public void onDestroy(){destroying=true;main.removeCallbacks(pulse);countdown.cancel();finishRecording("");if(routing!=null){routing.close();routing=null;}NativeAudio.stop();active=false;echoCancellation=false;manager.unregisterAudioDeviceCallback(devices);unregisterReceiver(noisy);releaseAwake();stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
