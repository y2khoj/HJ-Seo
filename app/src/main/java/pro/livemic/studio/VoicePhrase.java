package pro.livemic.studio;
import org.json.*;
import java.util.*;

/** Performance time is in quarter notes. String offsets and expression are retained. */
final class VoicePhrase {
    String trackId="";int quantize=0;boolean chordMode=false;
    final List<Note> notes=new ArrayList<>();
    static final class Note {
        double beat,length,offset;int midi,velocity,degree=-1,quality=0;boolean automatic=false;float bend=0,expression=1;
        final List<double[]> curve=new ArrayList<>();
        JSONObject json()throws JSONException{JSONArray c=new JSONArray();for(double[] point:curve)c.put(new JSONArray(point));return new JSONObject().put("beat",beat).put("length",length).put("offset",offset).put("midi",midi).put("velocity",velocity).put("degree",degree).put("quality",quality).put("automatic",automatic).put("bend",bend).put("expression",expression).put("curve",c);}
    }
    void transpose(int delta){for(Note n:notes)n.midi=Math.max(0,Math.min(127,n.midi+delta));}
    void quantize(int grid){quantize=grid;if(grid==0)return;double step=4.0/grid;for(Note n:notes){double end=Math.rint((n.beat+n.length)/step)*step;n.beat=Math.max(0,Math.rint(n.beat/step)*step);n.length=Math.max(step,end-n.beat);}}
    JSONObject json()throws JSONException{JSONArray a=new JSONArray();for(Note n:notes)a.put(n.json());return new JSONObject().put("trackId",trackId).put("quantize",quantize).put("chordMode",chordMode).put("notes",a);}
    static VoicePhrase read(JSONObject j)throws JSONException{VoicePhrase p=new VoicePhrase();p.trackId=j.getString("trackId");p.quantize=j.optInt("quantize");p.chordMode=j.optBoolean("chordMode");JSONArray a=j.getJSONArray("notes");if(a.length()>20000)throw new JSONException("프레이즈 음표 한도");for(int i=0;i<a.length();i++){JSONObject v=a.getJSONObject(i);Note n=new Note();n.beat=Math.max(0,v.getDouble("beat"));n.length=Math.max(.01,v.getDouble("length"));n.offset=v.optDouble("offset");n.midi=Math.max(0,Math.min(127,v.getInt("midi")));n.velocity=Math.max(1,Math.min(127,v.getInt("velocity")));n.degree=v.optInt("degree",-1);n.quality=v.optInt("quality");n.automatic=v.optBoolean("automatic");n.bend=(float)v.optDouble("bend");n.expression=(float)v.optDouble("expression",1);JSONArray curve=v.optJSONArray("curve");if(curve!=null){if(curve.length()>12000)throw new JSONException("표현 포인트 한도");for(int x=0;x<curve.length();x++){JSONArray point=curve.getJSONArray(x);n.curve.add(new double[]{point.getDouble(0),point.getDouble(1),point.getDouble(2)});}}p.notes.add(n);}return p;}
}
