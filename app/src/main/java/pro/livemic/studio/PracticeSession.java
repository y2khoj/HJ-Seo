package pro.livemic.studio;
import android.content.Context;
import android.os.*;
import org.json.*;
/** Service-owned practice; timing uses the audio frame clock, never UI rendering. */
public final class PracticeSession {
    final Context context;final Handler handler=new Handler(Looper.getMainLooper());
    public boolean active=false,record=false,ownedMic=false;public int root=48,bpm=60,mode=0,pattern=0,target=48,index=-1;public String phase="준비",error="";
    public PracticeScore score=new PracticeScore();public JSONObject result=null;
    int[] notes;double start,last,intervalStart;int previous=-99;JSONArray perNote=new JSONArray();PracticeScore noteScore=new PracticeScore();
    public PracticeSession(Context c){context=c.getApplicationContext();}
    public void start(int root,int bpm,int mode,int pattern,boolean record){
        if(active||RecordingManager.active||AudioService.waiting())return;
        this.root=Math.max(45,Math.min(72,root));this.bpm=Math.max(40,Math.min(120,bpm));this.mode=mode;this.pattern=pattern;this.record=record;
        notes=pattern==0?new int[]{0,2,4,5,7,5,4,2,0}:new int[]{0,2,4,5,7,9,11,12,11,9,7,5,4,2,0};
        NativeAudio.set(13,0);NativeAudio.set(17,0);NativeAudio.playLocal(false);NativeAudio.set(12,0);
        score=new PracticeScore();noteScore=new PracticeScore();perNote=new JSONArray();result=null;previous=-99;index=-1;error="";start=last=NativeAudio.pitchFrame()[3];active=true;handler.post(pulse);
    }
    void closeNote(){if(previous<0)return;try{JSONObject n=new JSONObject().put("targetMidi",root+notes[previous]).put("voicedSeconds",noteScore.voiced).put("coverage",noteScore.coverage());if(!Double.isNaN(noteScore.accuracy()))n.put("accuracy",noteScore.accuracy()).put("meanAbsoluteCents",noteScore.absolute/noteScore.voiced);perNote.put(n);}catch(Exception ignored){}noteScore=new PracticeScore();}
    final Runnable pulse=new Runnable(){public void run(){if(!active)return;if(!AudioService.active||NativeAudio.stats()[10]<.5f){stop(false);return;}
        float[] p=NativeAudio.pitchFrame();double now=p[3],dt=Math.max(0,now-last);last=now;double beat=60.0/bpm,t=now-start-4*beat;
        if(t<0){phase="시작까지 "+(int)Math.ceil(-t/beat);NativeAudio.guideTone(0);handler.postDelayed(this,40);return;}
        if(record&&!RecordingManager.active&&index==-1){RecordingManager.syncMs=0;if(!RecordingManager.start(context,bpm)){error="연습 녹음을 시작하지 못했습니다.";stop(false);return;}}
        double duration=mode==0?3*beat+.5:2*beat;int next=(int)(t/duration);
        if(next>=notes.length){stop(true);return;}
        index=next;target=root+notes[index];if(index!=previous){closeNote();previous=index;intervalStart=now;}
        double within=t-index*duration;boolean guiding=mode==0?within<beat:within<Math.min(.35,beat);
        boolean audible=mode==0||AudioService.hasWiredOutput(context);
        NativeAudio.guideTone(guiding&&audible?(float)(440*Math.pow(2,(target-69)/12.0)):0);
        boolean singing=mode==0?within>=beat+.5:within>=.2;
        phase=mode==0?(within<beat?"목표음을 들어보세요":singing?"따라 불러보세요":"준비하세요"):(audible?"함께 불러보세요":"화면 가이드에 맞춰 불러보세요");
        if(singing){score.add(p[0],p[1],p[2],target,dt);noteScore.add(p[0],p[1],p[2],target,dt);}else score.cents=Double.NaN;
        handler.postDelayed(this,40);
    }};
    public void stop(boolean complete){if(!active)return;active=false;handler.removeCallbacks(pulse);NativeAudio.guideTone(0);closeNote();phase=complete?"연습 완료":"연습 종료";
        try{result=new JSONObject().put("complete",complete).put("mode",mode).put("pattern",pattern).put("rootMidi",root).put("bpm",bpm).put("coverage",score.coverage()).put("notes",perNote);
            if(!Double.isNaN(score.accuracy()))result.put("accuracy",score.accuracy()).put("meanAbsoluteCents",score.absolute/score.voiced);
            if(record&&RecordingManager.active){RecordingManager.practiceResult=result;RecordingManager.stop();AudioService.revision++;}
        }catch(Exception e){error=e.getMessage();}
        if(ownedMic){ownedMic=false;context.stopService(new android.content.Intent(context,AudioService.class));}
    }
}
