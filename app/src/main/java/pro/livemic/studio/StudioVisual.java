package pro.livemic.studio;
import android.content.Context;
import android.graphics.*;
import android.view.View;
public final class StudioVisual extends View {
    public float[] stats=new float[17],notes=new float[0];public boolean piano=false;
    private final Paint p=new Paint(3);
    public StudioVisual(Context c,boolean roll){super(c);piano=roll;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
    private void line(Canvas c,float x,float y,float x2,float y2,int color,float w){p.setColor(color);p.setStrokeWidth(w);c.drawLine(x,y,x2,y2,p);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);float w=getWidth(),h=getHeight(),density=getResources().getDisplayMetrics().density;c.drawColor(0xff151a17);
        if(piano){int min=45,max=81;float key=34*density;double end=Math.max(12,stats[15]),start=end-12;
            for(int i=min;i<=max;i++){float y=h-(i-min)*h/(max-min);line(c,key,y,w,y,0xff28302a,1);if(i%12==0){p.setColor(0xff8d998f);p.setTextSize(10*density);c.drawText("C"+(i/12-1),4*density,y-2*density,p);}}
            for(int i=0;i<=12;i++){float x=key+i*(w-key)/12;line(c,x,0,x,h,0xff2e352f,1);}
            p.setColor(0xffd0f56b);for(int i=0;i+2<notes.length;i+=3){float x=(float)((notes[i]-start)/12*(w-key)+key),nw=(float)(notes[i+1]/12*(w-key));float y=h-(notes[i+2]-min+1)*h/(max-min);if(x+nw>=key&&x<w)c.drawRoundRect(Math.max(key,x),y,Math.min(w,x+Math.max(3,nw)),y+h/(max-min)-1,3,3,p);}
            line(c,w-2,0,w-2,h,0xffd0f56b,2);
        }else {for(int i=0;i<5;i++)line(c,0,h*i/4,w,h*i/4,0xff263029,1);
            float level=stats[3];Path path=new Path();for(int i=0;i<160;i++){float x=i*w/159;float envelope=(float)Math.sin(Math.PI*i/159);float y=h/2+(float)(Math.sin(i*.44)*Math.sin(i*.13+stats[15]*4))*Math.min(.85f,level*8)*h*.43f*envelope;if(i==0)path.moveTo(x,y);else path.lineTo(x,y);}
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2*density);p.setColor(0xffd0f56b);c.drawPath(path,p);p.setStyle(Paint.Style.FILL);
            p.setColor(0xff8d998f);p.setTextSize(10*density);c.drawText(stats[10]>.5?"LIVE INPUT · 반응형 레벨 표시":"마이크를 켜면 입력 레벨을 표시합니다",12*density,h-12*density,p);
        }
    }
}
