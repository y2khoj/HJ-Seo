package pro.livemic.studio;
import android.content.Context;
import android.graphics.*;
import android.view.View;
public final class StudioVisual extends View {
    public float[] stats=new float[17],notes=new float[0];public boolean piano=false;
    private final Paint p=new Paint(3);private final float[] envelope=new float[64];private int cursor=0;
    public void sampleInput(float rms){envelope[cursor]=rms;cursor=(cursor+1)%64;}
    public StudioVisual(Context c){this(c,false);}
    public StudioVisual(Context c,boolean roll){super(c);p.setTypeface(StudioTheme.mono(c));piano=roll;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    private void line(Canvas c,float x,float y,float x2,float y2,int color,float w){p.setColor(color);p.setStrokeWidth(w);c.drawLine(x,y,x2,y2,p);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),density=getResources().getDisplayMetrics().density;if(piano)c.drawColor(0xff12161c);
        if(piano){int min=45,max=81;float key=34*density;double end=Math.max(12,stats[15]),start=end-12;
            for(int i=min;i<=max;i++){float y=h-(i-min)*h/(max-min);line(c,key,y,w,y,0xff28302a,1);if(i%12==0){p.setColor(0xff8d998f);p.setTextSize(10*density);c.drawText("C"+(i/12-1),4*density,y-2*density,p);}}
            for(int i=0;i<=12;i++){float x=key+i*(w-key)/12;line(c,x,0,x,h,0xff2e352f,1);}
            p.setColor(StudioTheme.ACCENT);for(int i=0;i+2<notes.length;i+=3){float x=(float)((notes[i]-start)/12*(w-key)+key),nw=(float)(notes[i+1]/12*(w-key));float y=h-(notes[i+2]-min+1)*h/(max-min);if(x+nw>=key&&x<w)c.drawRoundRect(Math.max(key,x),y,Math.min(w,x+Math.max(3,nw)),y+h/(max-min)-1,3,3,p);}
            line(c,w-2,0,w-2,h,StudioTheme.ACCENT,2);
        }else {
            float step=w/64f,bar=Math.max(density,step-2*density);boolean live=stats.length>10&&stats[10]>.5f;
            for(int i=0;i<64;i++){float raw=envelope[(cursor+i)%64];float level=raw<.0001f?0:(float)Math.sqrt(Math.max(0,Math.min(1,(20*Math.log10(raw)+55)/55)));float bh=Math.max(2*density,(h-16*density)*level);p.setColor(live&&level>0?StudioTheme.ACCENT:0xff4b535e);p.setAlpha(live?245:130);c.drawRoundRect(i*step,(h-bh)/2,i*step+bar,(h+bh)/2,density,density,p);}p.setAlpha(255);

        }
    }
}
