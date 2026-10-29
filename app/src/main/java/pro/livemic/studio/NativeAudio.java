package pro.livemic.studio;
public final class NativeAudio {
    static { System.loadLibrary("livemic"); }
    public static native boolean start(int inputDevice);
    public static native void stop();
    public static native void set(int key,float value);
    public static native float[] stats();
    public static native float[] roll();
    public static native boolean beginRecord(String path);
    public static native void pauseRecord(boolean pause);
    public static native void endRecord();
    public static native boolean exportMidi(String path,int bpm,boolean recordingOnly);
    public static native void clearNotes();
    public static native double recordClock();
    public static native void loadLocal(float[] pcm,int sampleRate);
    public static native boolean loadLocalFile(String path,int sampleRate,int channels);
    public static native void playLocal(boolean play);
}
