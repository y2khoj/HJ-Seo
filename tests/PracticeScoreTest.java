import pro.livemic.studio.PracticeScore;
public class PracticeScoreTest {
 public static void main(String[] args){PracticeScore s=new PracticeScore();for(int i=0;i<20;i++)s.add(440,1,.1,69,.04);assert s.accuracy()==100;assert s.coverage()==100;
  s=new PracticeScore();for(int i=0;i<20;i++)s.add(880,1,.1,69,.04);assert s.accuracy()==0;assert Math.abs(s.cents-1200)<.001;
  s=new PracticeScore();for(int i=0;i<20;i++)s.add(0,0,0,69,.04);assert Double.isNaN(s.accuracy());assert s.coverage()==0;
  s=new PracticeScore();for(int i=0;i<20;i++)s.add(440*Math.pow(2,25./1200),1,.1,69,.04);assert s.accuracy()==100;
  s=new PracticeScore();for(int i=0;i<20;i++)s.add(440*Math.pow(2,26./1200),1,.1,69,.04);assert s.accuracy()==0;
  s=new PracticeScore();for(int i=0;i<20;i++)s.add(440,.5,.1,69,.04);assert Double.isNaN(s.accuracy());s=new PracticeScore();s.add(440,1,.1,69,2);assert s.coverage()==0;assert Double.isNaN(s.accuracy());System.out.println("PASS: target-relative accuracy, octave errors, boundary, silence and confidence gate");}
}
