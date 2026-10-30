package pro.livemic.studio;

import android.content.*;
import android.media.*;
import android.media.audiofx.*;
import android.os.Build;
import java.util.*;

/** Explicit media output route, with phone microphone input for both speaker modes. */
public final class AudioRouting implements AutoCloseable {
    public final int mode,input,output;public final String label;
    private final AudioManager manager;private final int previousMode;
    private boolean changedMode=false,communicationDevice=false;
    private AcousticEchoCanceler echo;private NoiseSuppressor noise;private AutomaticGainControl automaticGain;
    public static SharedPreferences prefs(Context c){return c.getSharedPreferences("routing",0);}
    public static int selected(Context c){return prefs(c).getInt("mode",0);}
    public static List<AudioDeviceInfo> bluetooth(Context c){List<AudioDeviceInfo> list=new ArrayList<>();for(AudioDeviceInfo d:((AudioManager)c.getSystemService(Context.AUDIO_SERVICE)).getDevices(AudioManager.GET_DEVICES_OUTPUTS)){
        int type=d.getType();if(type==AudioDeviceInfo.TYPE_BLUETOOTH_A2DP||type==AudioDeviceInfo.TYPE_BLE_SPEAKER)list.add(d);
    }return list;}
    public AudioRouting(Context c,int requestedInput)throws Exception {
        manager=(AudioManager)c.getSystemService(Context.AUDIO_SERVICE);previousMode=manager.getMode();mode=selected(c);
        int in=requestedInput,out=0;String name="이어폰";
        if(mode>0){
            in=0;for(AudioDeviceInfo d:manager.getDevices(AudioManager.GET_DEVICES_INPUTS))if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_MIC){in=d.getId();break;}
            if(in==0)throw new IllegalStateException("휴대폰 내장 마이크를 찾지 못했습니다.");
            if(mode==1){for(AudioDeviceInfo d:manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER){out=d.getId();break;}name="휴대폰 스피커";}
            else {int chosen=prefs(c).getInt("bluetoothDevice",0);for(AudioDeviceInfo d:bluetooth(c))if(chosen==0||chosen==d.getId()){out=d.getId();name=d.getProductName().toString();break;}}
            if(out==0)throw new IllegalStateException("출력 장치를 찾지 못했습니다. 연결 후 출력 모드에서 다시 선택하세요.");
        }
        input=in;output=out;label=name;
    }
    public void prepare()throws Exception {
        if(mode>0&&(previousMode==AudioManager.MODE_IN_CALL||previousMode==AudioManager.MODE_IN_COMMUNICATION))throw new IllegalStateException("통화 중에는 스피커 모드를 시작할 수 없습니다.");
        if(mode==1){
            if(previousMode==AudioManager.MODE_IN_CALL||previousMode==AudioManager.MODE_IN_COMMUNICATION)throw new IllegalStateException("통화 중에는 휴대폰 스피커 모드를 시작할 수 없습니다.");
            manager.setMode(AudioManager.MODE_IN_COMMUNICATION);changedMode=true;
            if(Build.VERSION.SDK_INT>=31){AudioDeviceInfo speaker=null;for(AudioDeviceInfo d:manager.getAvailableCommunicationDevices())if(d.getType()==AudioDeviceInfo.TYPE_BUILTIN_SPEAKER){speaker=d;break;}
                if(speaker==null||!manager.setCommunicationDevice(speaker))throw new IllegalStateException("휴대폰 스피커 경로를 열지 못했습니다.");communicationDevice=true;
            }else manager.setSpeakerphoneOn(true);
        }
    }
    public void attachEffects(){if(mode==0)return;int session=NativeAudio.inputSession();if(session<=0)return;
        try{if(AcousticEchoCanceler.isAvailable()){echo=AcousticEchoCanceler.create(session);if(echo!=null)echo.setEnabled(true);}}catch(Exception ignored){}
        try{if(NoiseSuppressor.isAvailable()){noise=NoiseSuppressor.create(session);if(noise!=null)noise.setEnabled(true);}}catch(Exception ignored){}
        try{if(AutomaticGainControl.isAvailable()){automaticGain=AutomaticGainControl.create(session);if(automaticGain!=null)automaticGain.setEnabled(false);}}catch(Exception ignored){}
        NativeAudio.monitorLevel(.18f);
    }
    public boolean echoEnabled(){return echo!=null&&echo.getEnabled();}
    @Override public void close(){if(echo!=null){echo.release();echo=null;}if(noise!=null){noise.release();noise=null;}if(automaticGain!=null){automaticGain.release();automaticGain=null;}
        if(communicationDevice&&Build.VERSION.SDK_INT>=31)manager.clearCommunicationDevice();
        if(changedMode){if(Build.VERSION.SDK_INT<31)manager.setSpeakerphoneOn(false);if(manager.getMode()==AudioManager.MODE_IN_COMMUNICATION)manager.setMode(previousMode);changedMode=false;}
    }
}
