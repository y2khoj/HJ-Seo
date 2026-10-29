package pro.livemic.studio;
import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.*;
import android.os.*;

public class AudioService extends Service {
    public static boolean active=false;
    public static String error="";
    private AudioManager manager;
    private final AudioDeviceCallback devices=new AudioDeviceCallback(){
        @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed){if(!hasWiredOutput(AudioService.this))NativeAudio.set(11,0);}
    };
    private final BroadcastReceiver noisy=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){NativeAudio.set(11,0);}};
    public static boolean hasWiredOutput(Context c){
        AudioManager m=(AudioManager)c.getSystemService(AUDIO_SERVICE);
        for(AudioDeviceInfo d:m.getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
            int t=d.getType();if(t==AudioDeviceInfo.TYPE_WIRED_HEADPHONES||t==AudioDeviceInfo.TYPE_WIRED_HEADSET||t==AudioDeviceInfo.TYPE_USB_HEADSET||t==AudioDeviceInfo.TYPE_USB_DEVICE||t==AudioDeviceInfo.TYPE_USB_ACCESSORY)return true;
        }return false;
    }
    @Override public void onCreate(){super.onCreate();manager=(AudioManager)getSystemService(AUDIO_SERVICE);manager.registerAudioDeviceCallback(devices,new Handler(getMainLooper()));
        if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        NotificationChannel channel=new NotificationChannel("live_audio","마이크 스튜디오",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&"STOP".equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
        Intent open=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,AudioService.class).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(this,"live_audio").setSmallIcon(R.drawable.ic_launcher).setContentTitle("LiveMic Pro · 마이크 사용 중").setContentText("효과 처리 및 녹음 · 탭하여 스튜디오 열기").setOngoing(true).setContentIntent(pi).addAction(new Notification.Action.Builder(null,"마이크 끄기",stop).build()).build();
        try {
            if(Build.VERSION.SDK_INT>=29)startForeground(41,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);else startForeground(41,n);
            if(!active){int device=intent==null?0:intent.getIntExtra("device",0);active=NativeAudio.start(device);if(!active){error="오디오 장치를 열지 못했습니다. 다른 녹음 앱을 닫고 다시 시도하세요.";stopSelf();}}
        }catch(Exception e){error="마이크를 시작할 수 없습니다: "+e.getMessage();stopSelf();}
        return START_NOT_STICKY;
    }
    @Override public void onDestroy(){try{RecordingManager.stop();}catch(Exception ignored){}NativeAudio.stop();active=false;manager.unregisterAudioDeviceCallback(devices);unregisterReceiver(noisy);stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
