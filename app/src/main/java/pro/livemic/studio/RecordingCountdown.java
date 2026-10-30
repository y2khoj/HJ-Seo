package pro.livemic.studio;

/** Uses elapsed realtime supplied by the service; independent of any Activity lifecycle. */
public final class RecordingCountdown {
    private long deadline=-1;
    public synchronized void arm(int seconds,long now){
        if(seconds!=0&&seconds!=3&&seconds!=5&&seconds!=10)throw new IllegalArgumentException("Unsupported delay");
        if(deadline>=0)throw new IllegalStateException("Already armed");
        deadline=now+seconds*1000L;
    }
    public synchronized boolean armed(){return deadline>=0;}
    public synchronized int remaining(long now){return deadline<0?0:(int)Math.max(0,(deadline-now+999)/1000);}
    public synchronized boolean consumeIfDue(long now){if(deadline<0||now<deadline)return false;deadline=-1;return true;}
    public synchronized void cancel(){deadline=-1;}
}
