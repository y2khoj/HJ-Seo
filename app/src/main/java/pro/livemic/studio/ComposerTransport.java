package pro.livemic.studio;
import java.util.*;

/** Expands harmony and seeded articulation on the worker. No project parsing in AAudio. */
final class ComposerTransport {
    static final int STRIDE=9;
    static final class Plan {double[] events;double beats,bar,bpm;int key,scale;}
    private final List<double[]> events=new ArrayList<>();
    private Random random;private double bar;private ComposerProject project;
    private void event(double beat,int type,int track,int note,double velocity,int program,int bank,double pan,double volume){events.add(new double[]{beat,type,track,note,velocity,program,bank,pan,volume});}
    private void note(double beat,double length,int ch,int pitch,double velocity,boolean human){double offset=human?random.nextDouble()*.018:0;event(beat+offset,1,ch,Math.max(0,Math.min(127,pitch)),Math.max(.05,Math.min(1,velocity+(human?(random.nextDouble()-.5)*.09:0))),0,0,0,0);event(beat+offset+Math.max(.03,length),0,ch,pitch,0,0,0,0,0);}
    Plan compile(ComposerProject p,int selected){project=p;p.validate();events.clear();random=new Random(p.seed);bar=MusicTheory.barBeats(p.meter);double beat=0;
        if(selected>=0)beat=part(p.parts.get(selected),selected,0);
        else for(ComposerProject.Order o:p.order){int index=p.parts.indexOf(p.part(o.part));for(int r=0;r<o.repeats;r++)beat=part(p.parts.get(index),index,beat);}
        events.sort((a,b)->{int c=Double.compare(a[0],b[0]);return c!=0?c:Integer.compare(rank((int)a[1]),rank((int)b[1]));});
        Plan result=new Plan();result.events=new double[events.size()*STRIDE];for(int i=0;i<events.size();i++)System.arraycopy(events.get(i),0,result.events,i*STRIDE,STRIDE);result.beats=beat;result.bar=bar;result.bpm=MusicTheory.quarterBpm(p.bpm,p.meter);result.key=p.key;result.scale=p.scale;return result;
    }
    private static int rank(int type){return type==3?0:type==2?1:type==0?2:type==4?3:4;}
    private double part(ComposerProject.Part p,int index,double start){
        event(start,3,0,index,p.bars,p.rhythm,0,0,0);
        for(int ch=0;ch<p.tracks.size();ch++){TrackConfig t=p.tracks.get(ch);event(start,2,ch,0,0,t.program,t.bank,t.pan,t.muted?0:t.volume);
            if(t.voice){for(VoicePhrase ph:p.phrases)if(ph.trackId.equals(t.id))phrase(ph,t,ch,start,p.bars*bar);continue;}
            for(int b=0;b<p.bars;b++){List<ComposerProject.Chord> chords=p.chords.get(b);for(int c=0;c<chords.size();c++){double pos=start+b*bar+(c==1?MusicTheory.split(project.meter):0),length=chords.size()==1?bar:c==0?MusicTheory.split(project.meter):bar-MusicTheory.split(project.meter);play(t,ch,chords.get(c),pos,length,p.rhythm,b==p.bars-1);}}
        }return start+p.bars*bar;
    }
    private void phrase(VoicePhrase phrase,TrackConfig t,int channel,double start,double duration){
        for(VoicePhrase.Note n:phrase.notes){
            if(n.beat>=duration)continue;
            int root=n.midi;
            if(n.automatic&&n.degree>=0){int target=MusicTheory.pc(project.key+MusicTheory.SCALE[project.scale][n.degree]);root+=Math.floorMod(target-MusicTheory.pc(root)+6,12)-6;}
            int q=n.automatic&&n.degree>=0?MusicTheory.quality(project.scale,n.degree,false):n.quality;
            int[] tones;
            if(!phrase.chordMode||t.family()==4)tones=new int[]{root};
            else if(t.family()==3)tones=MusicTheory.guitar(root,Math.max(0,Math.min(10,q)),0);
            else{int[] intervals=MusicTheory.INTERVALS[Math.max(0,Math.min(10,q))];tones=new int[intervals.length];for(int i=0;i<tones.length;i++)tones[i]=root+intervals[i];}
            event(start+n.beat,4,channel,(int)n.bend,n.expression,0,0,0,0);
            for(double[] point:n.curve)if(n.beat+point[0]<duration)event(start+n.beat+point[0],4,channel,(int)point[1],point[2],0,0,0,0);
            for(int i=0;i<tones.length;i++){double offset=t.family()==3?i*n.offset:0;if(offset>=n.length)continue;note(start+n.beat+offset,Math.min(n.length-offset,duration-n.beat-offset),channel,tones[i],n.velocity/127.,false);}
        }
    }
    private static double[] subdivisions(double length,double step){double[] values=new double[(int)Math.ceil(length/step)];for(int i=0;i<values.length;i++)values[i]=i*step;return values;}
    private void play(TrackConfig t,int ch,ComposerProject.Chord chord,double pos,double length,int rhythm,boolean fill){int root=48+chord.root,bass=36+(chord.bass>=0?chord.bass:chord.root);int[] tones=MusicTheory.INTERVALS[chord.quality];int family=t.family();
        if(t.drums()){
            double barStart=Math.floor((pos+.00001)/bar)*bar;
            for(ComposerRhythm.Hit hit:ComposerRhythm.drums(rhythm,fill)){
                double at=barStart+hit.beat;
                if(at>=pos-.001&&at<pos+length-.001)note(at,.09,ch,hit.note,hit.gain,true);
            }return;
        }
        if(family==4){double step=t.pattern==3?.5:project.meter==2?1.5:1;for(double at=0;at<length-.01;at+=step){int i=(int)Math.round(at/step),pitch=bass;if(t.pattern==1&&i%2==1)pitch+=7;else if(t.pattern==2&&i%2==1)pitch+=12;else if(t.pattern==3)pitch+=tones[i%tones.length];note(pos+at,Math.min(step*.83,length-at),ch,MusicTheory.fit(pitch,28,55),.78,true);}return;}
        if(family==3){
            int[] voicing=MusicTheory.guitar(root,chord.quality,t.pattern);double barStart=Math.floor((pos+.00001)/bar)*bar;
            double[] attacks=t.pattern==1||t.pattern==2?subdivisions(bar,.5):ComposerRhythm.strums(rhythm);
            for(int i=0;i<attacks.length;i++){
                double at=barStart+attacks[i];if(at<pos-.001||at>=pos+length-.001)continue;
                if(t.pattern==1){if(voicing.length>0)note(at,Math.min(.8,pos+length-at),ch,voicing[i%voicing.length],.65,true);}
                else{boolean up=i%2==1;for(int string=0;string<voicing.length;string++){int n=up?voicing.length-string-1:string;note(at+string*.012,Math.min(t.pattern==2?.12:.8,pos+length-at),ch,voicing[n],up?.5:.65,true);}}
            }return;
        }
        if(family==7||family==8||family==9||family==10){double step=t.pattern==1?length:.5;for(double at=0;at<length-.01;at+=step)note(pos+at,Math.min(step*.9,length-at),ch,60+chord.root+tones[((int)(at*2))%tones.length],.6,true);return;}
        double step=t.pattern==2?length:t.pattern==1?.5:1;for(double at=0;at<length-.01;at+=step){if(t.pattern==1)note(pos+at,Math.min(.7,length-at),ch,root+12+tones[(int)(at*2)%tones.length],.55,true);else for(int tone:tones)note(pos+at,Math.min(step*.9,length-at),ch,root+12+tone,.52,true);}
    }
}
