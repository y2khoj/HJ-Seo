package pro.livemic.studio;
/** Target-relative pitch scoring. Silence never contributes a fabricated score. */
public final class PracticeScore {
    public double voiced=0,inTune=0,absolute=0,total=0,cents=Double.NaN;
    public void add(double hz,double confidence,double rms,int target,double seconds){
        seconds=Math.max(0,seconds);total+=seconds;cents=Double.NaN;if(seconds>.1)return;seconds=Math.min(.05,seconds);
        if(hz<75||hz>1100||confidence<.85||rms<.007)return;
        cents=1200*Math.log(hz/(440*Math.pow(2,(target-69)/12.0)))/Math.log(2);
        voiced+=seconds;absolute+=Math.abs(cents)*seconds;if(Math.abs(cents)<=25.00001)inTune+=seconds;
    }
    public double accuracy(){return voiced>=.3?100*inTune/voiced:Double.NaN;}
    public double coverage(){return total>0?100*voiced/total:0;}
}
