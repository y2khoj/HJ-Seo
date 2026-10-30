package pro.livemic.studio;
import android.content.Context;
import android.graphics.*;
import android.view.View;
public final class StudioVisual extends View {
    public float[] stats=new float[17],notes=new float[0];public boolean piano=false;
    private final Paint p=new Paint(3);
    public StudioVisual(Context c){this(c,false);}
    public StudioVisual(Context c,boolean roll){super(c);p.setTypeface(StudioTheme.mono(c));piano=roll;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    private void line(Canvas c,float x,float y,float x2,float y2,int color,float w){p.setColor(color);p.setStrokeWidth(w);c.drawLine(x,y,x2,y2,p);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),density=getResources().getDisplayMetrics().density;c.drawColor(StudioTheme.SURFACE);
        if(piano){int min=45,max=81;float key=34*density;double end=Math.max(12,stats[15]),start=end-12;
            for(int i=min;i<=max;i++){float y=h-(i-min)*h/(max-min);line(c,key,y,w,y,0xff28302a,1);if(i%12==0){p.setColor(0xff8d998f);p.setTextSize(10*density);c.drawText("C"+(i/12-1),4*density,y-2*density,p);}}
            for(int i=0;i<=12;i++){float x=key+i*(w-key)/12;line(c,x,0,x,h,0xff2e352f,1);}
            p.setColor(StudioTheme.ACCENT);for(int i=0;i+2<notes.length;i+=3){float x=(float)((notes[i]-start)/12*(w-key)+key),nw=(float)(notes[i+1]/12*(w-key));float y=h-(notes[i+2]-min+1)*h/(max-min);if(x+nw>=key&&x<w)c.drawRoundRect(Math.max(key,x),y,Math.min(w,x+Math.max(3,nw)),y+h/(max-min)-1,3,3,p);}
            line(c,w-2,0,w-2,h,StudioTheme.ACCENT,2);
        }else {
            float level=Math.min(1,Math.max(0,(float)((20*Math.log10(Math.max(.00001f,stats[3]))+60)/60)));
            float gap=4*density,bar=(w-31*gap)/32;
            for(int i=0;i<32;i++){p.setColor(i/32f<level?(i>=29?StudioTheme.DANGER:StudioTheme.ACCENT):StudioTheme.BORDER);
                float bh=h*(.25f+.75f*i/31f);c.drawRoundRect(i*(bar+gap),(h-bh)/2,i*(bar+gap)+bar,(h+bh)/2,2*density,2*density,p);}
        }
    }
}
