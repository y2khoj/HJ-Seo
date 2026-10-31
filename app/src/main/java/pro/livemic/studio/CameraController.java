package pro.livemic.studio;
import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.hardware.camera2.*;
import android.media.MediaRecorder;
import android.os.*;
import android.util.*;
import android.view.*;
import java.io.*;
import java.util.*;

public final class CameraController {
    public interface Listener { void ready(); void error(String text); }
    private final Activity activity;private final TextureView texture;private final Listener listener;
    private final HandlerThread thread=new HandlerThread("LiveMic-Camera");private final Handler handler;
    private CameraDevice camera;private CameraCaptureSession session;private MediaRecorder recorder;private Surface preview;
    private String cameraId;private boolean front=true,closed=false,resumeAfterSwitch=false;private int generation=0,orientation=90;private Size size=new Size(1280,720),recordingSize;
    public volatile boolean recording=false,paused=false,switching=false,ready=false;public long videoStartNanos=0;
    public CameraController(Activity a,TextureView t,Listener l){activity=a;texture=t;listener=l;thread.start();handler=new Handler(thread.getLooper());
        t.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){public void onSurfaceTextureAvailable(SurfaceTexture s,int w,int h){open();}public void onSurfaceTextureSizeChanged(SurfaceTexture s,int w,int h){transform();}public boolean onSurfaceTextureDestroyed(SurfaceTexture s){closeCamera();return true;}public void onSurfaceTextureUpdated(SurfaceTexture s){}});
        if(t.isAvailable())open();
    }
    private void report(String e){ready=false;switching=false;int token=generation;activity.runOnUiThread(()->{if(!closed&&token==generation)listener.error(e);});}
    public boolean flip(){if(switching||!ready)return false;
        try{if(recording&&!paused){recorder.pause();paused=true;resumeAfterSwitch=true;}else resumeAfterSwitch=false;
            switching=true;ready=false;front=!front;closeCamera();open();return true;
        }catch(Exception e){report(AppLanguage.text(activity,"카메라 전환 실패: ","Could not switch camera: ")+e.getMessage());return false;}}
    public boolean pause(boolean value){if(!recording||recorder==null||switching)return false;try{if(value)recorder.pause();else recorder.resume();paused=value;return true;}catch(Exception e){report(AppLanguage.text(activity,"일시정지 상태 변경 실패: ","Could not change pause state: ")+e.getMessage());return false;}}
    public void open(){if(closed||camera!=null||!texture.isAvailable()||activity.checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
        try{CameraManager manager=(CameraManager)activity.getSystemService(Context.CAMERA_SERVICE);cameraId=null;String fallback=null;
            for(String id:manager.getCameraIdList()){CameraCharacteristics c=manager.getCameraCharacteristics(id);fallback=id;Integer facing=c.get(CameraCharacteristics.LENS_FACING);if(facing!=null&&facing==(front?CameraCharacteristics.LENS_FACING_FRONT:CameraCharacteristics.LENS_FACING_BACK)){cameraId=id;break;}}
            if(cameraId==null)cameraId=fallback;if(cameraId==null)throw new IOException(AppLanguage.text(activity,"사용 가능한 카메라가 없습니다.","No camera is available."));
            CameraCharacteristics c=manager.getCameraCharacteristics(cameraId);int sensor=c.get(CameraCharacteristics.SENSOR_ORIENTATION);int rotation=activity.getWindowManager().getDefaultDisplay().getRotation()*90;
            front=c.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_FRONT;
            orientation=front?(sensor+rotation)%360:(sensor-rotation+360)%360;
            Size[] options=c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(MediaRecorder.class);size=options[options.length-1];
            long best=Long.MAX_VALUE;for(Size s:options){if(s.getWidth()<=1280&&s.getHeight()<=720){long score=Math.abs((long)s.getWidth()*s.getHeight()-1280L*720);if(score<best){best=score;size=s;}}}
            if(recording){boolean supported=false;for(Size candidate:options)if(candidate.equals(recordingSize))supported=true;
                if(!supported)throw new IOException(AppLanguage.text(activity,"전환할 렌즈가 녹화 해상도를 지원하지 않습니다. 촬영을 마친 뒤 전환하세요.","The selected lens does not support the recording resolution. Finish recording before switching."));size=recordingSize;}
            transform();int token=++generation;
            manager.openCamera(cameraId,new CameraDevice.StateCallback(){@Override public void onOpened(CameraDevice d){if(closed||token!=generation){d.close();return;}camera=d;createSession(recording,null);}
                @Override public void onDisconnected(CameraDevice d){d.close();if(closed||token!=generation)return;camera=null;report(AppLanguage.text(activity,"카메라 연결이 끊겼습니다.","Camera disconnected."));}
                @Override public void onError(CameraDevice d,int e){d.close();if(closed||token!=generation)return;camera=null;report(AppLanguage.text(activity,"카메라를 열지 못했습니다 (","Could not open camera (")+e+AppLanguage.text(activity,"). 다른 카메라 앱을 닫고 다시 시도하세요.","). Close other camera apps and try again."));}},handler);
        }catch(Exception e){report(e.getMessage());}
    }
    private void transform(){activity.runOnUiThread(()->{if(!texture.isAvailable())return;float w=texture.getWidth(),h=texture.getHeight();
        Matrix m=new Matrix();RectF view=new RectF(0,0,w,h);boolean rotate=orientation==90||orientation==270;RectF buffer=new RectF(0,0,rotate?size.getHeight():size.getWidth(),rotate?size.getWidth():size.getHeight());buffer.offset(view.centerX()-buffer.centerX(),view.centerY()-buffer.centerY());
        m.setRectToRect(view,buffer,Matrix.ScaleToFit.FILL);float scale=Math.max(w/buffer.width(),h/buffer.height());m.postScale(scale,scale,w/2,h/2);
        int rotation=activity.getWindowManager().getDefaultDisplay().getRotation();if(rotation==Surface.ROTATION_90||rotation==Surface.ROTATION_270)m.postRotate(90*(rotation-2),w/2,h/2);
        texture.setTransform(m);texture.setScaleX(front?-1:1);
    });}
    private void createSession(boolean video,Runnable started){
        if(camera==null||closed)return;
        final int token=generation;
        try{if(session!=null){session.close();session=null;}SurfaceTexture st=texture.getSurfaceTexture();if(st==null)throw new IOException(AppLanguage.text(activity,"카메라 미리보기를 준비하지 못했습니다.","Could not prepare camera preview."));st.setDefaultBufferSize(size.getWidth(),size.getHeight());if(preview!=null)preview.release();preview=new Surface(st);
            List<Surface> surfaces=new ArrayList<>();surfaces.add(preview);if(video)surfaces.add(recorder.getSurface());
            CaptureRequest.Builder request=camera.createCaptureRequest(video?CameraDevice.TEMPLATE_RECORD:CameraDevice.TEMPLATE_PREVIEW);for(Surface s:surfaces)request.addTarget(s);
            request.set(CaptureRequest.CONTROL_MODE,CaptureRequest.CONTROL_MODE_AUTO);request.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
            camera.createCaptureSession(surfaces,new CameraCaptureSession.StateCallback(){@Override public void onConfigured(CameraCaptureSession s){if(closed||camera==null||token!=generation){s.close();return;}session=s;
                    try{s.setRepeatingRequest(request.build(),null,handler);if(video&&!recording){videoStartNanos=System.nanoTime();recorder.start();recording=true;paused=false;ready=true;activity.runOnUiThread(()->{if(!closed&&recording&&started!=null)started.run();});}
                    else {if(video&&resumeAfterSwitch){recorder.resume();paused=false;resumeAfterSwitch=false;}switching=false;ready=true;activity.runOnUiThread(()->{if(!closed)listener.ready();});}}catch(Exception e){report(AppLanguage.text(activity,"카메라 시작 실패: ","Could not start camera: ")+e.getMessage());}}
                @Override public void onConfigureFailed(CameraCaptureSession s){if(closed||token!=generation)return;report(AppLanguage.text(activity,"이 기기에서 카메라 설정을 지원하지 않습니다.","This camera configuration is not supported on this device."));}},handler);
        }catch(Exception e){report(AppLanguage.text(activity,"카메라 설정 실패: ","Camera setup failed: ")+e.getMessage());}
    }
    public boolean start(File output,Runnable started){if(camera==null||recording||!ready||switching)return false;ready=false;
        try{recordingSize=size;recorder=new MediaRecorder();recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);recorder.setOutputFile(output.getAbsolutePath());recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);recorder.setVideoSize(size.getWidth(),size.getHeight());recorder.setVideoFrameRate(30);recorder.setVideoEncodingBitRate(4_000_000);recorder.setOrientationHint(orientation);recorder.prepare();createSession(true,started);return true;
        }catch(Exception e){report(AppLanguage.text(activity,"녹화 준비 실패: ","Could not prepare video recording: ")+e.getMessage());if(recorder!=null){recorder.release();recorder=null;}return false;}
    }
    public boolean stop(){boolean ok=true;switching=false;resumeAfterSwitch=false;if(recording&&recorder!=null){try{if(session!=null){session.stopRepeating();session.abortCaptures();}recorder.stop();}catch(Exception e){ok=false;}recording=false;paused=false;}
        if(recorder!=null){recorder.reset();recorder.release();recorder=null;}if(!closed)createSession(false,null);return ok;
    }
    private void closeCamera(){ready=false;generation++;if(session!=null){session.close();session=null;}if(camera!=null){camera.close();camera=null;}if(preview!=null){preview.release();preview=null;}}
    public void release(){closed=true;stop();closeCamera();thread.quitSafely();}
}
