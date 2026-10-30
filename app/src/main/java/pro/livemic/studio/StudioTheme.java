package pro.livemic.studio;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Native primitives and the typefaces named in the saved Figma handoff. */
final class StudioTheme {
    static final int BG=0xff0b0c0f, SURFACE=0xff1c1f26, RAISED=0xff292d36,
            ACCENT=0xffe7f500, TEXT=0xfff4f6f8, MUTED=0xffb8c0cc, DANGER=0xffff4d60, BORDER=0xff7a8492;
    private static Typeface body,medium,bold,mono;
    static Typeface body(Context c){if(body==null)body=c.getResources().getFont(R.font.studio_sans);return body;}
    static Typeface medium(Context c){if(medium==null)medium=Typeface.create(body(c),500,false);return medium;}
    static Typeface bold(Context c){if(bold==null)bold=Typeface.create(body(c),700,false);return bold;}
    static Typeface mono(Context c){if(mono==null)mono=c.getResources().getFont(R.font.studio_numbers);return mono;}
    static int dp(Context c,float n){return Math.round(c.getResources().getDisplayMetrics().density*n);}
    static GradientDrawable background(Context c,int color,int radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));if(color==RAISED||color==SURFACE){d.setColors(new int[]{0xff272b33,0xff1b1f26});d.setOrientation(GradientDrawable.Orientation.TL_BR);d.setStroke(dp(c,1),0xff373e49);}return d;
    }
    static TextView text(Context c,String value,int size,int color){
        TextView t=new TextView(c);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setTypeface(body(c));t.setIncludeFontPadding(false);
        t.setFontFeatureSettings("tnum");t.setLineSpacing(dp(c,3),1);return t;
    }
    static Button button(Context c,String value,Runnable action){
        StudioButton b=new StudioButton(c);b.setText(value);b.setOnClickListener(v->action.run());return b;
    }
    static StudioButton styled(Context c,String value,int role,Runnable action){StudioButton b=(StudioButton)button(c,value,action);b.role(role);return b;}
    static Button icon(Context c,String glyph,String description,Runnable action){StudioButton b=styled(c,"",glyph.equals("play")||glyph.equals("pause")?StudioButton.CIRCLE:StudioButton.QUIET,action);b.glyph(glyph);b.setContentDescription(description);b.setPadding(dp(c,12),0,dp(c,12),0);return b;}
    static void selected(Button b,boolean selected){
        b.setBackgroundTintList(ColorStateList.valueOf(selected?ACCENT:RAISED));
        b.setTextColor(selected?BG:TEXT);b.setSelected(selected);
    }
    static LinearLayout card(Context c){
        LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);
        int pad=dp(c,14);l.setPadding(pad,pad,pad,pad);l.setBackground(background(c,SURFACE,14));return l;
    }
}
