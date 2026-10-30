package pro.livemic.studio;

/** Independent live vocal harmony and four-layer loop studio JNI. */
final class NativeHarmony {
    static { System.loadLibrary("livemic"); }
    static native boolean prepare(int bpm, int meter, int bars);
    static native void configure(boolean enabled, int key, int scale, int preset, float wet, float dry, float spread);
    static native void monitor(boolean enabled);
    static native void transport(int command);
    static native boolean arm(int slot);
    static native void cancel();
    static native void layer(int slot, float volume, float pan, boolean muted);
    static native void clear(int slot);
    static native boolean saveLayer(int slot, String path);
    static native boolean loadLayer(int slot, String path);
    static native double[] state();
}
