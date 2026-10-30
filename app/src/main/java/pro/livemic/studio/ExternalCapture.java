package pro.livemic.studio;

/** Optional, personal-build-only producer. It never plays its captured samples back. */
public interface ExternalCapture extends AutoCloseable {
    float level();
    boolean receivedAudioDuringRecording();
    @Override void close();
}
