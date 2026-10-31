package pro.livemic.studio;
/** Exports the real scheduler's event stream for the native offline renderer. */
public class ArrangementExport {
 public static void main(String[] args) {
  int rhythm=Integer.parseInt(args[0]); ComposerProject p=new ComposerProject();
  p.meter=rhythm<6?0:rhythm==6?1:2; p.parts.get(0).rhythm=rhythm; p.bpm=90;
  ComposerTransport.Plan plan=new ComposerTransport().compile(p,-1);
  System.out.printf(java.util.Locale.ROOT,"%.9f %.9f %.9f%n",plan.beats,plan.bar,plan.bpm);
  for(int i=0;i<plan.events.length;i+=9)System.out.printf(java.util.Locale.ROOT,"%.9f %.0f %.0f %.0f %.9f %.0f %.0f %.9f %.9f%n",plan.events[i],plan.events[i+1],plan.events[i+2],plan.events[i+3],plan.events[i+4],plan.events[i+5],plan.events[i+6],plan.events[i+7],plan.events[i+8]);
 }
}
