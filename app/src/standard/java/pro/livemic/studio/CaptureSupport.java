package pro.livemic.studio;
import android.app.Activity;
import android.content.*;
import android.content.pm.ServiceInfo;

/** Public build has no projection implementation or mediaProjection permission. */
public final class CaptureSupport {
    public static boolean available(){return false;}
    public static void request(Activity activity,int code){throw new UnsupportedOperationException("마이크 전용 버전입니다.");}
    public static int serviceTypes(boolean capture){return ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;}
    public static ExternalCapture start(Context context,Intent consent,int rate,Runnable stopped){throw new UnsupportedOperationException("배포용은 마이크만 녹음합니다.");}
}
