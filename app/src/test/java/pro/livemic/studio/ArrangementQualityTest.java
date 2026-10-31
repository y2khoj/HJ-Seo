package pro.livemic.studio;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ArrangementQualityTest {
    private static ComposerProject project(int rhythm) {
        ComposerProject p=new ComposerProject();
        p.meter=rhythm<6?0:rhythm==6?1:2;
        p.parts.get(0).rhythm=rhythm;
        return p;
    }
    private static ComposerTransport.Plan plan(ComposerProject p) {return new ComposerTransport().compile(p,-1);}
    @Test public void seededPlansAreDeterministicAndNotesCloseWithinTheirHarmony() {
        for(int rhythm=0;rhythm<8;rhythm++) {
            ComposerProject p=project(rhythm); ComposerProject.Part part=p.parts.get(0);
            ComposerProject.Chord second=new ComposerProject.Chord();second.degree=4;second.resolve(p);
            part.chords.get(0).add(second);
            ComposerTransport.Plan a=plan(p), b=plan(p);
            assertArrayEquals(a.events,b.events,0);
            assertEquals(part.bars*MusicTheory.barBeats(p.meter),a.beats,1e-9);
            double[][] active=new double[8][128];for(double[] row:active)Arrays.fill(row,-1);
            for(int i=0;i<a.events.length;i+=ComposerTransport.STRIDE) {
                double time=a.events[i];int type=(int)a.events[i+1],ch=(int)a.events[i+2],pitch=(int)a.events[i+3];
                assertTrue("event outside plan",time>=-1e-9&&time<=a.beats+1e-9);
                if(type==1) {assertEquals("overlapping note "+ch+":"+pitch,-1,active[ch][pitch],0);active[ch][pitch]=time;}
                if(type==0) {
                    assertTrue("unmatched note off",active[ch][pitch]>=0);
                    double start=active[ch][pitch];
                    assertFalse("note crosses split harmony",start<MusicTheory.split(p.meter)-1e-9&&time>MusicTheory.split(p.meter)+1e-9);
                    assertFalse("note crosses bar",Math.floor((start+1e-9)/a.bar)<Math.floor((time-1e-9)/a.bar));
                    active[ch][pitch]=-1;
                }
            }
            for(double[] row:active)for(double v:row)assertEquals("unclosed note",-1,v,0);
        }
    }
    @Test public void rhythmsStayInTheirMeterAndFillReplacesLastBeat() {
        for(int rhythm=0;rhythm<8;rhythm++) {
            double bar=rhythm<6?4:3;
            for(ComposerRhythm.Hit h:ComposerRhythm.drums(rhythm,false,0,.55))assertTrue(h.beat>=0&&h.beat<bar);
            List<ComposerRhythm.Hit> fill=ComposerRhythm.drums(rhythm,true,0,.55);
            int late=0;for(ComposerRhythm.Hit h:fill) {assertTrue(h.beat>=0&&h.beat<bar);if(h.beat>=bar-1)late++;}
            assertTrue("fill should be sparse",late>0&&late<=4);
        }
    }
    @Test public void recordedPhraseIsNotHumanized() {
        for(float humanize:new float[]{0,1}) {
            ComposerProject p=project(0);TrackConfig t=new TrackConfig("Voice",24,false);t.voice=true;p.addTrack(t);
            VoicePhrase phrase=new VoicePhrase();phrase.trackId=t.id;VoicePhrase.Note n=new VoicePhrase.Note();n.midi=64;n.beat=.333;n.length=.6;n.velocity=80;phrase.notes.add(n);p.parts.get(0).phrases.add(phrase);p.parts.get(0).humanize=humanize;
            ComposerTransport.Plan a=plan(p);boolean on=false,off=false;
            for(int i=0;i<a.events.length;i+=9)if((int)a.events[i+2]==4&&(int)a.events[i+3]==64){if(a.events[i+1]==1){assertEquals(.333,a.events[i],1e-9);on=true;}if(a.events[i+1]==0){assertEquals(.933,a.events[i],1e-9);off=true;}}
            assertTrue(on&&off);
        }
    }
    @Test public void oldProjectsGetDefaultsAndNewValuesRoundTrip() throws Exception {
        ComposerProject p=new ComposerProject();JSONObject json=p.json();JSONObject part=json.getJSONArray("parts").getJSONObject(0);
        part.remove("energy");part.remove("humanize");part.remove("fill");ComposerProject.Part restored=ComposerProject.read(json).parts.get(0);
        assertEquals(.55,restored.energy,1e-6);assertEquals(.65,restored.humanize,1e-6);assertTrue(restored.fill);
        part.put("energy",.2);part.put("humanize",.8);part.put("fill",false);restored=ComposerProject.read(json).parts.get(0);
        assertEquals(.2,restored.energy,1e-6);assertEquals(.8,restored.humanize,1e-6);assertFalse(restored.fill);
        part.put("energy",-2);part.put("humanize",3);restored=ComposerProject.read(json).parts.get(0);
        assertEquals(0,restored.energy,0);assertEquals(1,restored.humanize,0);
    }
    @Test public void splitChordSweepBalancesAllGeneratedNotes() throws Exception {
        for(int bpm:new int[]{40,90,240})for(int rhythm=0;rhythm<8;rhythm++)for(int root=0;root<12;root++)for(int quality=0;quality<11;quality++) {
            ComposerProject p=project(rhythm);p.bpm=bpm;ComposerProject.Part part=p.parts.get(0);p.resize(part,4);part.tracks.get(0).pattern=3;
            for(List<ComposerProject.Chord> bar:part.chords){bar.clear();for(int n=0;n<2;n++){ComposerProject.Chord c=new ComposerProject.Chord();c.automatic=false;c.root=(root+n*5)%12;c.quality=quality;bar.add(c);}}
            p.order.get(0).repeats=2;ComposerProject.Part copy=p.duplicate(part);p.order.add(new ComposerProject.Order(copy.id,1));
            ComposerTransport.Plan plan=plan(p);boolean[][] active=new boolean[8][128];
            for(int i=0;i<plan.events.length;i+=9){double time=plan.events[i];int type=(int)plan.events[i+1],ch=(int)plan.events[i+2],note=(int)plan.events[i+3];assertTrue(time>=0&&time<=plan.beats+1e-8);if(type==1){assertFalse("overlap bpm="+bpm+" rhythm="+rhythm+" root="+root+" quality="+quality,active[ch][note]);active[ch][note]=true;}else if(type==0){assertTrue("unmatched off",active[ch][note]);active[ch][note]=false;}}
            for(boolean[] row:active)for(boolean playing:row)assertFalse("unclosed note",playing);
        }
    }
    @Test public void compoundMeterPatternsUseQuarterWalkingAndEighthArpeggios() {
        ComposerProject p=project(7);ComposerProject.Part part=p.parts.get(0);part.humanize=0;part.fill=false;part.tracks.get(0).pattern=1;part.tracks.get(1).pattern=3;
        ComposerTransport.Plan plan=plan(p);List<Double> guitar=new ArrayList<>(),bass=new ArrayList<>();
        for(int i=0;i<plan.events.length;i+=9)if(plan.events[i+1]==1&&plan.events[i]<3){if(plan.events[i+2]==0)guitar.add(plan.events[i]);if(plan.events[i+2]==1)bass.add(plan.events[i]);}
        assertEquals(Arrays.asList(0.,.5,1.,1.5,2.,2.5),guitar);assertEquals(Arrays.asList(0.,1.,2.),bass);
    }
    @Test public void ensembleArpeggioIsArticulated() {
        for(int program:new int[]{40,48,88}) {ComposerProject p=project(0);ComposerProject.Part part=p.parts.get(0);part.humanize=0;part.tracks.get(0).program=program;part.tracks.get(0).pattern=1;ComposerTransport.Plan plan=plan(p);List<Double> starts=new ArrayList<>();for(int i=0;i<plan.events.length;i+=9)if(plan.events[i+1]==1&&plan.events[i+2]==0&&plan.events[i]<2)starts.add(plan.events[i]);assertEquals(Arrays.asList(0.,.5,1.,1.5),starts);}
    }
    @Test public void HumanizationIsBoundedInMillisecondsAtAllTempos() {
        for(int bpm:new int[]{40,90,240}) {ComposerProject p=project(0);p.bpm=bpm;ComposerProject.Part part=p.parts.get(0);part.humanize=1;part.fill=false;part.tracks.get(0).pattern=1;ComposerTransport.Plan plan=plan(p);
            for(int i=0;i<plan.events.length;i+=9)if(plan.events[i+1]==1&&plan.events[i+2]==0){double beat=plan.events[i],nearest=Math.rint(beat*2)/2;assertTrue("groove exceeds 8 ms at "+bpm,Math.abs(beat-nearest)*60000/plan.bpm<=8.001);}
        }
    }

    @Test public void walkingBassAnchorsShortSplitHarmonyBeforeApproachingNext() {
        ComposerProject p=project(6);ComposerProject.Part part=p.parts.get(0);part.humanize=0;part.tracks.get(1).pattern=3;part.energy=1;
        ComposerProject.Chord c=new ComposerProject.Chord();c.automatic=false;c.root=0;c.quality=0;c.bass=4;part.chords.get(3).add(c);
        ComposerTransport.Plan plan=plan(p);boolean found=false;for(int i=0;i<plan.events.length;i+=9)if(plan.events[i+1]==1&&plan.events[i+2]==1&&Math.abs(plan.events[i]-11)<1e-9){assertEquals(4,Math.floorMod((int)plan.events[i+3],12));found=true;}assertTrue(found);
    }

    @Test public void guitarPatternsRemainBalancedAtTempoExtremes() {
        for(int bpm:new int[]{40,90,240})for(int rhythm=0;rhythm<8;rhythm++)for(int pattern=0;pattern<4;pattern++) {
            ComposerProject p=project(rhythm);p.bpm=bpm;ComposerProject.Part part=p.parts.get(0);part.tracks.get(0).pattern=pattern;ComposerProject.Chord c=new ComposerProject.Chord();c.degree=4;c.resolve(p);part.chords.get(0).add(c);p.order.get(0).repeats=2;
            ComposerTransport.Plan plan=plan(p);boolean[] active=new boolean[128];for(int i=0;i<plan.events.length;i+=9)if(plan.events[i+2]==0){int note=(int)plan.events[i+3];if(plan.events[i+1]==1){assertFalse(active[note]);active[note]=true;}else if(plan.events[i+1]==0){assertTrue(active[note]);active[note]=false;}}for(boolean a:active)assertFalse(a);
        }
    }

}
