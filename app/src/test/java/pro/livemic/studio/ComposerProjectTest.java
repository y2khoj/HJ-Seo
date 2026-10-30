package pro.livemic.studio;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ComposerProjectTest {
 @Test public void allKeysTransposeManualSlashAndPhraseAndScalePreservesPerformance()throws Exception{
  for(int key=0;key<12;key++){ComposerProject p=new ComposerProject();ComposerProject.Part a=p.parts.get(0);ComposerProject.Chord manual=a.chords.get(1).get(0);manual.automatic=false;manual.root=7;manual.bass=11;
   VoicePhrase phrase=new VoicePhrase();VoicePhrase.Note n=new VoicePhrase.Note();n.midi=60;n.length=1;n.velocity=100;n.curve.add(new double[]{.2,12,.8});phrase.notes.add(n);a.phrases.add(phrase);
   p.transpose(key);int[] roots={0,7,9,5};for(int i=0;i<4;i++)assertEquals((roots[i]+key)%12,a.chords.get(i).get(0).root);assertEquals((11+key)%12,manual.bass);assertEquals(60+key,n.midi);
   p.scale(2);assertEquals((7+key)%12,manual.root);assertEquals(60+key,n.midi);assertEquals(1,n.curve.size());
  }
 }
 @Test public void diatonicHarmoniesAndBeatSplits(){
  int[][] expected={{0,1,1,0,0,1,2},{1,2,0,1,1,0,0},{1,2,3,1,0,0,2},{1,1,0,0,1,2,0},{0,1,2,0,1,1,0}};
  for(int scale=0;scale<5;scale++)for(int degree=0;degree<7;degree++)assertEquals(expected[scale][degree],MusicTheory.quality(scale,degree,false));
  assertEquals(2,MusicTheory.split(0),0);assertEquals(2,MusicTheory.split(1),0);assertEquals(1.5,MusicTheory.split(2),0);assertEquals(135,MusicTheory.quarterBpm(90,2),0);
 }
 @Test public void guitarVoicingsStayOnSixStringsAndBassWithinRange(){for(int root=0;root<12;root++)for(int quality=0;quality<11;quality++){int[] notes=MusicTheory.guitar(48+root,quality,0);assertTrue(notes.length>=3&&notes.length<=6);for(int n:notes){assertTrue(n>=40&&n<=76);boolean chord=false;for(int interval:MusicTheory.INTERVALS[quality])chord|=MusicTheory.pc(n-root)==MusicTheory.pc(interval);assertTrue(chord);}}for(int n=0;n<128;n++){int fit=MusicTheory.fit(n,28,55);assertTrue(fit>=28&&fit<=55);}}
 @Test public void projectLimitsAndVersionedRoundTrip()throws Exception{ComposerProject p=new ComposerProject();p.duplicate(p.parts.get(0));p.order.add(new ComposerProject.Order(p.parts.get(1).id,2));ComposerProject restored=ComposerProject.read(p.json());assertEquals(24,restored.totalBars());assertEquals(p.parts.get(0).tracks.get(0).id,restored.parts.get(1).tracks.get(0).id);
  p.order.get(0).repeats=40;try{p.validate();fail();}catch(IllegalArgumentException expected){}JSONObject future=restored.json().put("version",999);try{ComposerProject.read(future);fail();}catch(JSONException expected){}
 }
 @Test public void quantizationRetainsStringDelayAndSavedExpression()throws Exception{VoicePhrase p=new VoicePhrase();VoicePhrase.Note n=new VoicePhrase.Note();n.beat=.21;n.length=.73;n.offset=.018;n.midi=60;n.velocity=100;n.curve.add(new double[]{.1,23,.7});p.notes.add(n);p.quantize(8);assertEquals(0,n.beat,0);assertEquals(1,n.length,0);assertEquals(.018,n.offset,0);VoicePhrase restored=VoicePhrase.read(p.json());assertEquals(23,restored.notes.get(0).curve.get(0)[1],0);}
 @Test public void arrangementSeedAndPhraseTimingSurviveCompilation()throws Exception{ComposerProject p=new ComposerProject();TrackConfig t=new TrackConfig("목소리",24,false);t.voice=true;p.addTrack(t);VoicePhrase v=new VoicePhrase();v.trackId=t.id;VoicePhrase.Note n=new VoicePhrase.Note();n.midi=64;n.beat=.333;n.length=.6;n.velocity=80;v.notes.add(n);p.parts.get(0).phrases.add(v);ComposerTransport.Plan a=new ComposerTransport().compile(p,-1),b=new ComposerTransport().compile(p,-1);assertArrayEquals(a.events,b.events,0);boolean exact=false;for(int i=0;i<a.events.length;i+=9)if(a.events[i+1]==1&&a.events[i+2]==4){assertEquals(.333,a.events[i],0);exact=true;}assertTrue(exact);}
 @Test public void twentyStepHistoryAndAtomicRecovery()throws Exception{ComposerStore store=new ComposerStore(RuntimeEnvironment.getApplication());ComposerProject p=new ComposerProject();for(int i=0;i<23;i++){store.checkpoint(p);p.bpm=90+i;}assertEquals(20,store.undo.size());int original=p.bpm;p=store.history(p,false);assertEquals(original-1,p.bpm);p=store.history(p,true);assertEquals(original,p.bpm);store.write(p,true);ComposerProject restored=store.draft();assertEquals(p.json().toString(),restored.json().toString());store.write(p,false);assertTrue(store.list().stream().anyMatch(x->x.id.equals(restored.id)));store.delete(p.id);assertFalse(store.list().stream().anyMatch(x->x.id.equals(restored.id)));}
}
