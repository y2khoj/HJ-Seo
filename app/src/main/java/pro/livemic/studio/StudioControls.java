package pro.livemic.studio;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;

/** Shared, native-drawn studio controls. All interaction stays connected to the audio engine. */
final class StudioGlyph extends Drawable {
    final String kind; final Paint p=new Paint(3); int color;
    StudioGlyph(String k,int c){kind=k;color=c;}
    public void draw(Canvas canvas){canvas.save();canvas.translate(getBounds().left,getBounds().top);canvas.scale(getBounds().width()/24f,getBounds().height()/24f);draw(canvas,kind,color,p);canvas.restore();}
    static void draw(Canvas c,String k,int color,Paint p){p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);Path a=new Path();
        switch(k){
        case "menu":for(int y=6;y<=18;y+=6)c.drawLine(3,y,21,y,p);break;
        case "close":c.drawLine(5,5,19,19,p);c.drawLine(19,5,5,19,p);break;
        case "mic":case "mute":c.drawRoundRect(9,2,15,14,3,3,p);a.moveTo(6,10);a.lineTo(6,12);a.cubicTo(6,20,18,20,18,12);a.lineTo(18,10);c.drawPath(a,p);c.drawLine(12,18,12,22,p);c.drawLine(9,22,15,22,p);if(k.equals("mute"))c.drawLine(2,2,22,22,p);break;
        case "music":a.moveTo(9,17);a.lineTo(9,5);a.lineTo(20,3);a.lineTo(20,15);c.drawPath(a,p);c.drawLine(9,8,20,6,p);p.setStyle(Paint.Style.FILL);c.drawOval(3,16,9,21,p);c.drawOval(14,14,20,19,p);break;
        case "folder":a.moveTo(2,7);a.lineTo(2,5);a.quadTo(2,3,4,3);a.lineTo(10,3);a.lineTo(12,6);a.lineTo(20,6);a.quadTo(22,6,22,8);a.lineTo(22,19);a.quadTo(22,21,20,21);a.lineTo(4,21);a.quadTo(2,21,2,19);a.close();c.drawPath(a,p);break;
        case "camera":c.drawRoundRect(2,6,18,20,3,3,p);a.moveTo(18,10);a.lineTo(23,7);a.lineTo(23,19);a.lineTo(18,16);c.drawPath(a,p);break;
        case "play":p.setStyle(Paint.Style.FILL);a.moveTo(8,5);a.lineTo(20,12);a.lineTo(8,19);a.close();c.drawPath(a,p);break;
        case "pause":p.setStyle(Paint.Style.FILL);c.drawRoundRect(6,5,10,19,1,1,p);c.drawRoundRect(14,5,18,19,1,1,p);break;
        case "more":p.setStyle(Paint.Style.FILL);for(int y=5;y<=19;y+=7)c.drawCircle(12,y,1.6f,p);break;
        case "down":a.moveTo(6,9);a.lineTo(12,15);a.lineTo(18,9);c.drawPath(a,p);break;
        case "arrow":a.moveTo(9,5);a.lineTo(16,12);a.lineTo(9,19);c.drawPath(a,p);break;
        case "headphones":a.moveTo(3,15);a.lineTo(3,11);a.cubicTo(3,0,21,0,21,11);a.lineTo(21,15);c.drawPath(a,p);c.drawRoundRect(2,11,6,21,2,2,p);c.drawRoundRect(18,11,22,21,2,2,p);break;
        case "settings":c.drawCircle(12,12,6,p);c.drawCircle(12,12,2,p);for(int i=0;i<8;i++){double r=i*Math.PI/4;c.drawLine(12+(float)Math.cos(r)*7,12+(float)Math.sin(r)*7,12+(float)Math.cos(r)*10,12+(float)Math.sin(r)*10,p);}break;
        case "record":c.drawCircle(12,12,9,p);p.setStyle(Paint.Style.FILL);c.drawCircle(12,12,5,p);break;
        case "piano":c.drawRoundRect(2,3,22,21,2,2,p);for(int i=7;i<22;i+=5)c.drawLine(i,3,i,21,p);p.setStyle(Paint.Style.FILL);for(int i=5;i<20;i+=5)c.drawRect(i,3,i+3,13,p);break;
        default:c.drawCircle(12,12,9,p);p.setStyle(Paint.Style.FILL);p.setTextSize(15);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);c.drawText("?",12,17,p);p.setTextAlign(Paint.Align.LEFT);
        }p.setStyle(Paint.Style.FILL);
    }
    public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}

class StudioButton extends Button {
    static final int STANDARD=0,QUIET=1,CHIP=2,OUTLINE=3,RECORD=4,CIRCLE=5;
    int role=STANDARD; final Paint p=new Paint(3); final RectF box=new RectF();
    StudioButton(Context c){super(c);setBackgroundColor(Color.TRANSPARENT);setStateListAnimator(null);setElevation(0);setMinWidth(0);setMinimumWidth(0);setMinHeight(StudioTheme.dp(c,48));setMinimumHeight(StudioTheme.dp(c,48));setAllCaps(false);setIncludeFontPadding(false);setTypeface(StudioTheme.medium(c));setTextSize(14);setLetterSpacing(0);setGravity(Gravity.CENTER);setPadding(StudioTheme.dp(c,12),0,StudioTheme.dp(c,12),0);setTextColor(StudioTheme.TEXT);}
    StudioButton role(int v){role=v;invalidate();return this;}
    StudioButton glyph(String k){StudioGlyph d=new StudioGlyph(k,role==RECORD?StudioTheme.DANGER:StudioTheme.MUTED);int size=StudioTheme.dp(getContext(),18);d.setBounds(0,0,size,size);setCompoundDrawables(d,null,null,null);setCompoundDrawablePadding(StudioTheme.dp(getContext(),7));return this;}
    StudioButton trailing(String k){StudioGlyph d=new StudioGlyph(k,StudioTheme.MUTED);int size=StudioTheme.dp(getContext(),16);d.setBounds(0,0,size,size);Drawable[] old=getCompoundDrawables();setCompoundDrawables(old[0],old[1],d,old[3]);return this;}
    protected void onDraw(Canvas c){Drawable glyph=getCompoundDrawables()[0];if(glyph instanceof StudioGlyph)((StudioGlyph)glyph).color=getCurrentTextColor();float d=getResources().getDisplayMetrics().density;box.set(.7f*d,.7f*d,getWidth()-.7f*d,getHeight()-.7f*d);float radius=role==CHIP||role==OUTLINE||role==CIRCLE?getHeight()/2f:12*d;if(role==CIRCLE){float diameter=Math.min(getWidth(),getHeight())-2*d;box.set((getWidth()-diameter)/2,(getHeight()-diameter)/2,(getWidth()+diameter)/2,(getHeight()+diameter)/2);}
        boolean selected=isSelected();ColorStateList tint=getBackgroundTintList();int custom=tint==null?0:tint.getDefaultColor();boolean colored=!selected&&(custom==StudioTheme.ACCENT||custom==StudioTheme.DANGER);int fill=selected?StudioTheme.ACCENT:colored?custom:role==RECORD?0xff30191f:StudioTheme.SURFACE;
        if(role!=QUIET){p.setShader(selected||colored||role==RECORD?null:new LinearGradient(0,0,getWidth(),getHeight(),0xff282c34,0xff191c22,Shader.TileMode.CLAMP));p.setColor(fill);p.setAlpha(isEnabled()?255:125);c.drawRoundRect(box,radius,radius,p);p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(.8f*d);p.setColor(role==RECORD?0xffb34052:role==OUTLINE?StudioTheme.ACCENT:selected?StudioTheme.ACCENT:0xff454b57);c.drawRoundRect(box,radius,radius,p);p.setStyle(Paint.Style.FILL);}
        if(isPressed()){p.setColor(0x18ffffff);c.drawRoundRect(box,radius,radius,p);}c.saveLayerAlpha(0,0,getWidth(),getHeight(),isEnabled()?255:110);super.onDraw(c);c.restore();
    }
}

final class StudioNavButton extends StudioButton {
    final String icon; final Paint paint=new Paint(3);
    StudioNavButton(Context c,String label,String icon,Runnable action){super(c);this.icon=icon;setText(label);setOnClickListener(v->action.run());setContentDescription(label);}
    protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,sp=getResources().getDisplayMetrics().scaledDensity;int color=isSelected()?StudioTheme.ACCENT:0xffa5adbb;c.save();c.translate(getWidth()/2f-11*d,8*d);c.scale(22*d/24,22*d/24);StudioGlyph.draw(c,icon,color,paint);c.restore();paint.setTypeface(StudioTheme.medium(getContext()));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(12*sp);paint.setColor(color);c.drawText(getText().toString(),getWidth()/2f,49*d,paint);paint.setTextAlign(Paint.Align.LEFT);}
}

final class StudioDialButton extends StudioButton {
    final Paint paint=new Paint(3); final RectF arc=new RectF();
    StudioDialButton(Context c,Runnable action){super(c);setOnClickListener(v->action.run());}
    protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,sp=getResources().getDisplayMetrics().scaledDensity;float x=getWidth()/2f,y=52*d,r=37*d;boolean on=isSelected();c.save();c.scale(getHeight()/(126*d),getHeight()/(126*d),x,0);
        c.save();if(isPressed())c.scale(.98f,.98f,x,y);
        for(int i=0;i<44;i++){double a=Math.toRadians(130+i*280./43);paint.setColor(on&&i<33?StudioTheme.ACCENT:0xff66707d);paint.setAlpha(on&&i<33?235:170);paint.setStrokeWidth((i%4==0?1.5f:1)*d);c.drawLine(x+(float)Math.cos(a)*45*d,y+(float)Math.sin(a)*45*d,x+(float)Math.cos(a)*50*d,y+(float)Math.sin(a)*50*d,paint);}paint.setAlpha(255);
        paint.setColor(0xff080a0d);c.drawCircle(x,y+3*d,r+5*d,paint);paint.setShader(new LinearGradient(x-r,y-r,x+r,y+r,new int[]{0xff68717d,0xff222831,0xff080b0f},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r+3*d,paint);paint.setShader(new RadialGradient(x-12*d,y-19*d,r*1.8f,new int[]{0xff444b56,0xff292f39,0xff151a21},null,Shader.TileMode.CLAMP));c.drawCircle(x,y,r,paint);paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.75f*d);paint.setColor(0xff68717e);c.drawCircle(x,y,r-1*d,paint);paint.setColor(0xff12171d);c.drawCircle(x,y,r-4*d,paint);paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(2*d);paint.setStrokeCap(Paint.Cap.ROUND);paint.setColor(on?StudioTheme.ACCENT:0xffe2e7f0);c.drawLine(x,y-r+8*d,x,y-r+18*d,paint);
        c.save();c.translate(x-11*d,y-7*d);c.scale(22*d/24,22*d/24);StudioGlyph.draw(c,"mic",on?StudioTheme.ACCENT:0xffbac3d1,paint);c.restore();c.restore();paint.setColor(StudioTheme.MUTED);paint.setTypeface(StudioTheme.medium(getContext()));paint.setTextSize(12*sp);paint.setTextAlign(Paint.Align.CENTER);c.drawText(getText().toString(),x,118*d,paint);paint.setTextAlign(Paint.Align.LEFT);c.restore();
    }
}

final class StudioFader extends View {
    interface Changed{void changed(float value);}
    final Paint p=new Paint(3);final String label;final MainActivity.Formatter formatter;final float min,max;float value;final Changed listener;
    StudioFader(Context c,String label,float min,float max,float value,MainActivity.Formatter formatter,Changed listener){super(c);this.label=label;this.min=min;this.max=max;this.value=value;this.formatter=formatter;this.listener=listener;setFocusable(true);setContentDescription(label);}
    protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,sp=getResources().getDisplayMetrics().scaledDensity,w=getWidth(),left=3*d,right=w-8*d,y=40*d,f=Math.max(0,Math.min(1,(value-min)/(max-min)));p.setTypeface(StudioTheme.medium(getContext()));p.setTextSize(14*sp);p.setColor(StudioTheme.TEXT);c.drawText(label,0,17*d,p);p.setTypeface(StudioTheme.mono(getContext()));p.setTextSize(13*sp);p.setTextAlign(Paint.Align.RIGHT);c.drawText(formatter.value(value),w-2*d,17*d,p);p.setTextAlign(Paint.Align.LEFT);
        for(int i=0;i<=52;i++){float x=left+(right-left)*i/52;float tick=(i%5==0?6:3)*d;p.setStrokeWidth(d);p.setColor(i/52f<=f?0xffa9b32a:0xff4d535e);c.drawLine(x,y-tick,x,y+tick,p);}float x=left+(right-left)*f;p.setColor(0x66000000);c.drawCircle(x,y+2*d,8*d,p);p.setColor(0xffedf0f6);c.drawCircle(x,y,6.5f*d,p);p.setColor(0xffffffff);c.drawCircle(x-1.5f*d,y-1.5f*d,2*d,p);
    }
    void change(float v){value=Math.max(min,Math.min(max,v));listener.changed(value);setContentDescription(label+" "+formatter.value(value));invalidate();}
    public boolean onTouchEvent(MotionEvent e){if(!isEnabled())return false;switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:getParent().requestDisallowInterceptTouchEvent(true);setPressed(true);case MotionEvent.ACTION_MOVE:change(min+Math.max(0,Math.min(1,(e.getX()-3*getResources().getDisplayMetrics().density)/(getWidth()-11*getResources().getDisplayMetrics().density)))*(max-min));return true;case MotionEvent.ACTION_UP:setPressed(false);getParent().requestDisallowInterceptTouchEvent(false);performClick();return true;case MotionEvent.ACTION_CANCEL:setPressed(false);getParent().requestDisallowInterceptTouchEvent(false);return true;}return false;}
    public boolean performClick(){super.performClick();return true;}
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo i){super.onInitializeAccessibilityNodeInfo(i);i.setClassName(SeekBar.class.getName());i.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,min,max,value));i.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);i.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);i.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);}
    public boolean performAccessibilityAction(int action,Bundle b){if(!isEnabled())return false;if(action==AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId()&&b!=null){change(b.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE));return true;}if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD||action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){change(value+(max-min)*.05f*(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1));return true;}return super.performAccessibilityAction(action,b);}
}

final class StudioTakeWave extends View {
    float[] levels; final Paint p=new Paint(3);
    StudioTakeWave(Context c){super(c);setContentDescription("녹음 파형");}
    protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight();p.setColor(0xff10151a);c.drawRoundRect(0,0,w,h,9*d,9*d,p);p.setColor(0x124a5261);p.setStrokeWidth(.7f*d);for(int i=1;i<12;i++)c.drawLine(w*i/12,0,w*i/12,h,p);for(int i=1;i<4;i++)c.drawLine(0,h*i/4,w,h*i/4,p);if(levels==null)return;float gap=2*d,bw=(w-20*d)/levels.length;for(int i=0;i<levels.length;i++){float v=Math.max(.025f,Math.min(1,levels[i]));p.setColor(StudioTheme.ACCENT);float bh=(h-16*d)*v;c.drawRoundRect(10*d+i*bw,(h-bh)/2,10*d+i*bw+Math.max(d,bw-gap),(h+bh)/2,1*d,1*d,p);}}
}

final class StudioPlaybackLine extends View {
    float progress;final Paint p=new Paint(3);
    StudioPlaybackLine(Context c){super(c);setContentDescription("반주 재생 위치");}
    protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,y=getHeight()/2f;p.setColor(0xff414854);c.drawRoundRect(0,y-1.5f*d,getWidth(),y+1.5f*d,2*d,2*d,p);p.setColor(StudioTheme.ACCENT);c.drawRoundRect(0,y-1.5f*d,Math.max(0,Math.min(1,progress))*getWidth(),y+1.5f*d,2*d,2*d,p);}
}

/** Compact preview of notes actually present in this app's saved Standard MIDI File. */
final class StudioMidiPreview extends View {
    float[] notes;final Paint p=new Paint(3);
    StudioMidiPreview(Context c){super(c);setContentDescription("저장된 MIDI 피아노 롤");}
    protected void onDraw(Canvas c){float d=getResources().getDisplayMetrics().density,w=getWidth(),h=getHeight();p.setColor(0xff10151a);c.drawRoundRect(0,0,w,h,9*d,9*d,p);p.setColor(0xff293139);p.setStrokeWidth(.6f*d);for(int i=1;i<16;i++)c.drawLine(i*w/16,0,i*w/16,h,p);for(int i=1;i<8;i++)c.drawLine(0,i*h/8,w,i*h/8,p);if(notes==null||notes.length==0)return;float end=1,lo=127,hi=0;for(int i=0;i<notes.length;i+=3){end=Math.max(end,notes[i]+notes[i+1]);lo=Math.min(lo,notes[i+2]);hi=Math.max(hi,notes[i+2]);}lo-=2;hi+=2;p.setColor(StudioTheme.ACCENT);for(int i=0;i<notes.length;i+=3){float left=6*d+notes[i]/end*(w-12*d),right=6*d+(notes[i]+notes[i+1])/end*(w-12*d),y=h-6*d-(notes[i+2]-lo)/(hi-lo)*(h-12*d);c.drawRoundRect(left,y-2*d,Math.max(left+3*d,right-1*d),y+2*d,d,d,p);}}
    static float[] read(java.io.File file){java.util.List<Float> out=new java.util.ArrayList<>();try{if(file.length()>2*1024*1024)return new float[0];byte[] bytes=java.nio.file.Files.readAllBytes(file.toPath());java.io.DataInputStream in=new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes));if(in.readInt()!=0x4d546864)return new float[0];int header=in.readInt();if(header<6||header>1000)return new float[0];in.readUnsignedShort();int tracks=in.readUnsignedShort();in.readUnsignedShort();in.skipBytes(header-6);for(int track=0;track<tracks&&in.available()>8;track++){if(in.readInt()!=0x4d54726b)break;int len=in.readInt();if(len<0||len>in.available())break;byte[] data=new byte[len];in.readFully(data);java.io.DataInputStream t=new java.io.DataInputStream(new java.io.ByteArrayInputStream(data));int tick=0,running=0;int[] starts=new int[2048];java.util.Arrays.fill(starts,-1);while(t.available()>0&&out.size()<6000){tick+=vlq(t);int status=t.readUnsignedByte(),first=-1;if(status<128){first=status;status=running;}else if(status<240)running=status;if(status==255){t.readUnsignedByte();int n=vlq(t);if(t.skipBytes(n)!=n)break;continue;}if(status==240||status==247){int n=vlq(t);if(t.skipBytes(n)!=n)break;continue;}if(status<128||status>=240)break;int key=first>=0?first:t.readUnsignedByte(),kind=status&240,velocity=kind==192||kind==208?0:t.readUnsignedByte();int slot=(status&15)*128+key;if(key>=128)break;if(kind==144&&velocity>0)starts[slot]=tick;else if((kind==128||kind==144)&&starts[slot]>=0){out.add((float)starts[slot]);out.add((float)Math.max(1,tick-starts[slot]));out.add((float)key);starts[slot]=-1;}}}}catch(Exception ignored){}float[] result=new float[out.size()];for(int i=0;i<result.length;i++)result[i]=out.get(i);return result;}
    static int vlq(java.io.DataInputStream in)throws java.io.IOException{int value=0;for(int i=0;i<4;i++){int b=in.readUnsignedByte();value=(value<<7)|(b&127);if((b&128)==0)return value;}throw new java.io.IOException("MIDI variable length overflow");}
}
