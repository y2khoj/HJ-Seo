import pro.livemic.studio.RecordingCountdown;

public class RecordingCountdownTest {
    static void check(boolean result,String message){if(!result)throw new AssertionError(message);}
    public static void main(String[] args){
        RecordingCountdown timer=new RecordingCountdown();
        for(int seconds:new int[]{0,3,5,10}){
            timer.arm(seconds,12345);check(timer.armed(),"reservation missing");
            check(timer.remaining(12345)==seconds,"wrong countdown");
            if(seconds>0){check(!timer.consumeIfDue(12345+seconds*1000-1),"started early");check(timer.remaining(12345+seconds*1000-1)==1,"premature zero");}
            check(timer.consumeIfDue(12345+seconds*1000),"did not start at deadline");
            check(!timer.consumeIfDue(999999),"duplicate start");
        }
        timer.arm(5,1000);timer.cancel();check(!timer.consumeIfDue(7000),"cancelled reservation started");
        timer.arm(10,8000);check(!timer.consumeIfDue(9000),"old deadline started a new reservation");check(timer.consumeIfDue(18000),"rearm failed");
        boolean rejected=false;try{timer.arm(-1,0);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"invalid delay accepted");
        timer.arm(3,0);rejected=false;try{timer.arm(5,0);}catch(IllegalStateException e){rejected=true;}check(rejected,"replaced live reservation");
        System.out.println("PASS: immediate/3/5/10-second deadlines, cancellation, rearm, duplicate and invalid requests");
    }
}
