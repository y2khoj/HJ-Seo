package pro.livemic.studio;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** A twelve-second target/voice trace; gaps stay gaps when no voice is detected. */
final class PracticePlot extends View {
 final Paint p=new Paint(3);final float[] times=new float[600],targets=new float[600],voices=new float[600];int count=0,cursor=0;float latest=0;int root=48;
 PracticePlot(Context c){super(c);setContentDescription("목표음과 실제 피치, 최근 12초");}
 void sample(float time,int target,float frequency,boolean valid){if(time<=latest)return;latest=time;times[cursor]=time;targets[cursor]=target;voices[cursor]=valid&&frequency>0?(float)(69+12*Math.log(frequency/440)/Math.log(2)):Float.NaN;cursor=(cursor+1)%600;count=Math.min(600,count+1);invalidate();}
 float y(float n){return getHeight()-(n-root+2)*getHeight()/16f;}
 protected void onDraw(Canvas c){c.drawColor(StudioTheme.SURFACE);float d=getResources().getDisplayMetrics().density;p.setStrokeWidth(d);p.setColor(StudioTheme.BORDER);for(int i=0;i<8;i++)c.drawLine(0,i*getHeight()/8f,getWidth(),i*getHeight()/8f,p);c.save();c.clipRect(0,0,getWidth(),getHeight());float px=0,pt=0,pv=Float.NaN;boolean first=true;for(int i=0;i<count;i++){int k=(cursor-count+i+600)%600;float x=(times[k]-latest+12)*getWidth()/12;if(x<0)continue;if(!first){p.setColor(StudioTheme.MUTED);p.setStrokeWidth(2*d);c.drawLine(px,y(pt),x,y(pt),p);c.drawLine(x,y(pt),x,y(targets[k]),p);if(!Float.isNaN(pv)&&!Float.isNaN(voices[k])){p.setColor(StudioTheme.ACCENT);p.setStrokeWidth(3*d);c.drawLine(px,y(pv),x,y(voices[k]),p);}}first=false;px=x;pt=targets[k];pv=voices[k];}c.restore();}
}
