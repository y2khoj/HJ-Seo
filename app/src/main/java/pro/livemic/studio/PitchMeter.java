package pro.livemic.studio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Cent meter driven by detected pitch; no fabricated pitch while waiting. */
final class PitchMeter extends View {
    float cents;boolean tracking;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    PitchMeter(Context c){super(c);setContentDescription("음정 편차, 마이너스 50에서 플러스 50 센트");}
    @Override protected void onDraw(Canvas c){
        float d=getResources().getDisplayMetrics().density,w=getWidth(),y=getHeight()/2f;
        paint.setStrokeWidth(2*d);paint.setColor(StudioTheme.BORDER);c.drawLine(8*d,y,w-8*d,y,paint);
        for(int i=0;i<=4;i++){float x=8*d+(w-16*d)*i/4;paint.setColor(i==2?StudioTheme.ACCENT:StudioTheme.MUTED);c.drawLine(x,y-4*d,x,y+4*d,paint);}
        if(tracking){paint.setColor(Math.abs(cents)<=10?StudioTheme.ACCENT:StudioTheme.DANGER);float x=8*d+(w-16*d)*(Math.max(-50,Math.min(50,cents))+50)/100;c.drawCircle(x,y,5*d,paint);}
    }
}
