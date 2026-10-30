package pro.livemic.studio;

import android.content.*;
import android.os.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Service-owned project and transactional performance capture. No UI timer triggers audio. */
final class ComposerSession {
    private static ComposerSession instance;
    static synchronized ComposerSession get(Context c){if(instance==null)instance=new ComposerSession(c.getApplicationContext());return instance;}
    final Context context;final ComposerStore store;final ExecutorService worker=Executors.newSingleThreadExecutor();final Handler main=new Handler(Looper.getMainLooper());
    ComposerProject project=new ComposerProject();int selectedPart=0,revision=0;volatile boolean ready=false;boolean voiceOn=false,capturing=false;String error="",message="";
    private VoicePhrase pendingPhrase;private VoicePhrase.Note pendingNote;private int recordingPart=0;private String recordingBefore;private double[] state={0,0,0,0,32,-1,-1,0,0,0};
    private ComposerSession(Context c){context=c;store=new ComposerStore(c);worker.execute(()->{try{
        ComposerProject draft=store.draft();if(draft==null){draft=new ComposerProject();android.content.SharedPreferences old=c.getSharedPreferences("MainActivity",0);if(old.contains("p15")){draft.bpm=Math.max(40,Math.min(240,Math.round(old.getFloat("p15",90))));draft.transpose(MusicTheory.pc(Math.round(old.getFloat("p16",0))));}}
        File sf=new File(c.getFilesDir(),"generaluser-gs-1.44.sf2");if(sf.length()!=31279314L){android.util.AtomicFile atomic=new android.util.AtomicFile(sf);FileOutputStream out=atomic.startWrite();try(InputStream in=c.getAssets().open("soundfonts/generaluser-gs-1.44.sf2")){byte[] b=new byte[65536];int n;while((n=in.read(b))>0)out.write(b,0,n);atomic.finishWrite(out);}catch(Exception e){atomic.failWrite(out);throw e;}}
        if(!NativeComposer.loadSoundFont(sf.getAbsolutePath()))throw new IOException("음원 초기화 실패");ComposerProject restored=draft;main.post(()->{project=restored;ready=true;revision++;publish(-1);});
    }catch(Exception e){main.post(()->{error="작곡기 준비 실패: "+e.getMessage();revision++;});}});}
    double[] state(){return state;}
    boolean playing(){return state[1]>.5;}
    ComposerProject.Part part(){selectedPart=Math.max(0,Math.min(selectedPart,project.parts.size()-1));return project.parts.get(selectedPart);}
    void edit(Consumer<ComposerProject> change,boolean structure){if(capturing){error="프레이즈 기록을 마친 뒤 편집하세요.";return;}if(structure&&playing()){error="일시정지 후 파트 구성을 편집하세요.";return;}
        try{String before=project.json().toString();try{change.accept(project);project.validate();}catch(Exception e){project=ComposerProject.read(new JSONObject(before));throw e;}store.checkpoint(ComposerProject.read(new JSONObject(before)));revision++;persist();publish(-1);if(voiceOn)applyVoice();}catch(Exception e){error=e.getMessage();}}
    void persist(){try{ComposerProject snapshot=ComposerProject.read(project.json());worker.execute(()->{try{store.write(snapshot,true);}catch(Exception e){main.post(()->error="초안 저장 실패: "+e.getMessage());}});}catch(JSONException e){error=e.getMessage();}}
    void publish(int selected){if(!ready)return;try{ComposerProject snapshot=ComposerProject.read(project.json());worker.execute(()->{try{ComposerTransport.Plan plan=new ComposerTransport().compile(snapshot,selected);if(!NativeComposer.publish(plan.events,plan.beats,plan.bar,plan.bpm,plan.key,plan.scale))throw new IOException("재생 계획 큐가 가득 찼습니다.");}catch(Exception e){main.post(()->error="반주 준비 실패: "+e.getMessage());}});}catch(Exception e){error=e.getMessage();}}
    void play(){if(!ready){error="음원을 준비하고 있습니다.";return;}publish(-1);worker.execute(()->main.post(()->{AudioService.composerRequested=true;context.startForegroundService(new Intent(context,AudioService.class).setAction(AudioService.PLAYBACK));NativeAudio.playLocal(false);NativeComposer.transport(1);}));}
    void pause(){NativeComposer.transport(2);}
    void stop(){if(capturing)cancelPhrase();NativeComposer.transport(0);AudioService.composerRequested=false;if(AudioService.playbackActive)context.startService(new Intent(context,AudioService.class).setAction(AudioService.COMPOSER_STOP));}
    void voice(boolean on){voiceOn=on;applyVoice();revision++;}
    void applyVoice(){VoiceInstrumentSettings s=project.voice;NativeComposer.voice(voiceOn,s.program,s.mode,s.octave,s.autoRange,s.snap,s.response,s.quality,s.rhythm,s.mixVoice,s.confidence,s.gate);}
    void history(boolean redo){if(capturing){error="기록을 마친 뒤 실행 취소하세요.";return;}try{project=store.history(project,redo);revision++;persist();publish(-1);if(voiceOn)applyVoice();}catch(Exception e){error=e.getMessage();}}
    void save(){try{ComposerProject copy=ComposerProject.read(project.json());worker.execute(()->{try{store.write(copy,false);main.post(()->{message="내 반주에 저장했습니다.";revision++;});}catch(Exception e){main.post(()->error="저장 실패: "+e.getMessage());}});}catch(Exception e){error=e.getMessage();}}
    void open(ComposerProject p){if(capturing){error="기록을 마친 뒤 프로젝트를 여세요.";return;}stop();voice(false);project=p;selectedPart=0;store.undo.clear();store.redo.clear();persist();publish(-1);revision++;}
    void duplicate(){edit(p->{p.id=UUID.randomUUID().toString();p.name+=" 복제";},true);save();}
    void recordPhrase(){if(!voiceOn||!AudioService.active){error="목소리 악기를 먼저 켜세요.";return;}if(capturing){cancelPhrase();return;}try{
        boolean exists=false;for(TrackConfig t:part().tracks)if(t.voice)exists=true;if(!exists&&part().tracks.size()>=8){error="프레이즈를 기록할 트랙 공간이 없습니다. 악기 트랙을 줄여주세요.";return;}
        boolean wasPlaying=playing();recordingPart=wasPlaying?(int)state[2]:selectedPart;recordingBefore=project.json().toString();pendingPhrase=new VoicePhrase();pendingPhrase.chordMode=project.voice.mode==1;pendingNote=null;capturing=true;NativeComposer.drain();
        if(!wasPlaying){publish(selectedPart);worker.execute(()->main.post(()->{AudioService.composerRequested=true;context.startForegroundService(new Intent(context,AudioService.class).setAction(AudioService.PLAYBACK));NativeComposer.transport(3);NativeComposer.record(true);}));}
        else NativeComposer.record(true);revision++;
    }catch(Exception e){error=e.getMessage();}}
    void cancelPhrase(){NativeComposer.record(false);NativeComposer.drain();pendingPhrase=null;pendingNote=null;capturing=false;revision++;message="재기록을 취소했습니다. 이전 프레이즈를 유지합니다.";}
    void poll(){if(!ready)return;state=NativeComposer.state();if(!capturing)return;double[] data=NativeComposer.drain();for(int i=0;i+7<data.length;i+=8){double time=data[i];int kind=(int)data[i+1];if(kind==1){closeNote(time);pendingNote=new VoicePhrase.Note();pendingNote.beat=time;pendingNote.midi=(int)data[i+2];pendingNote.velocity=(int)data[i+3];pendingNote.bend=(float)data[i+4];pendingNote.expression=(float)data[i+5];pendingNote.degree=(int)data[i+6];pendingNote.quality=(int)data[i+7];pendingNote.automatic=pendingPhrase.chordMode&&project.voice.quality<0;pendingNote.offset=pendingPhrase.chordMode&&project.voice.program/8==3?.012*MusicTheory.quarterBpm(project.bpm,project.meter)/60:0;}
            else if(kind==0)closeNote(time);else if(kind==2&&pendingNote!=null)pendingNote.curve.add(new double[]{time-pendingNote.beat,data[i+4],data[i+5]});}
        if(state[8]==3){closeNote(Math.max(0,state[4]-state[3]));finishPhrase();}}
    private void closeNote(double time){if(pendingNote!=null){pendingNote.length=Math.max(.01,time-pendingNote.beat);pendingPhrase.notes.add(pendingNote);pendingNote=null;}}
    private void finishPhrase(){NativeComposer.record(false);capturing=false;try{if(state[9]>.5)throw new IOException("기록 버퍼가 가득 차 이전 프레이즈를 유지합니다.");if(pendingPhrase.notes.isEmpty()){message="기록한 음이 없습니다. 이전 프레이즈를 유지합니다.";return;}store.checkpoint(ComposerProject.read(new JSONObject(recordingBefore)));ComposerProject.Part part=project.parts.get(recordingPart);TrackConfig voice=null;for(TrackConfig t:part.tracks)if(t.voice)voice=t;if(voice==null){voice=new TrackConfig("목소리 "+InstrumentCatalog.GM[project.voice.program],project.voice.program,false);voice.voice=true;project.addTrack(voice);voice=part.tracks.get(part.tracks.size()-1);}voice.program=project.voice.program;voice.muted=false;pendingPhrase.trackId=voice.id;pendingPhrase.quantize(project.voice.quantize);String id=voice.id;part.phrases.removeIf(p->p.trackId.equals(id));part.phrases.add(pendingPhrase);persist();publish(-1);message="프레이즈를 기록했습니다. 실행 취소로 이전 연주를 복구할 수 있습니다.";}catch(Exception e){error=e.getMessage();}finally{pendingPhrase=null;revision++;}}
}
