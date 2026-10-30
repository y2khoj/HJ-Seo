package pro.livemic.studio;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native view primitives using the approved LiveMic design tokens. */
final class StudioTheme {
    static final int BG=0xff101411, SURFACE=0xff1c231e, RAISED=0xff27312a,
            ACCENT=0xffd0f56b, TEXT=0xfff0f3ed, MUTED=0xff9ba99c, DANGER=0xffff7d7d, BORDER=0xff334036;
    static int dp(Context c,float n){return Math.round(c.getResources().getDisplayMetrics().density*n);}
    static GradientDrawable background(Context c,int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;
    }
    static TextView text(Context c,String value,int size,int color){
        TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setFontFeatureSettings("tnum");t.setLineSpacing(dp(c,3),1);return t;
    }
    static Button button(Context c,String value,Runnable action){
        Button b=new Button(c);b.setText(value);b.setTextSize(14);b.setTextColor(TEXT);b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setMinWidth(0);b.setMinimumWidth(0);
        b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));b.setGravity(Gravity.CENTER);
        b.setPadding(dp(c,8),dp(c,6),dp(c,8),dp(c,6));b.setBackground(background(c,RAISED,14));
        b.setBackgroundTintList(ColorStateList.valueOf(RAISED));b.setOnClickListener(v->action.run());return b;
    }
    static void selected(Button b,boolean selected){
        b.setBackgroundTintList(ColorStateList.valueOf(selected?ACCENT:RAISED));
        b.setTextColor(selected?BG:TEXT);b.setSelected(selected);
    }
    static LinearLayout card(Context c){
        LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);
        int pad=dp(c,16);l.setPadding(pad,pad,pad,pad);l.setBackground(background(c,SURFACE,14));return l;
    }
}
