package pro.livemic.studio;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Locale;

/** The live console presents real input and transport state; audio remains owned by the service. */
final class LiveScreen {
    final MainActivity a;
    TextView mic,route,clock,backing,pitch,level;
    ProgressBar meter;
    final int[] ids={2,3,4,5,6,0};final StudioKnob[] knobs=new StudioKnob[6];
    LiveScreen(MainActivity activity){a=activity;}
    String tr(String ko,String en){return a.tr(ko,en);}
    TextView label(String text,int size,int color){return a.text(text,size,color);}
    void draw(){
        LinearLayout heading=a.row();TextView title=a.text(tr("라이브 콘솔","LIVE CONSOLE"),20,MainActivity.TEXT);title.setTypeface(StudioTheme.bold(a));heading.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        StudioButton presets=StudioTheme.styled(a,a.presetName()+" ▾",StudioButton.CHIP,a::presetPicker);presets.setTextSize(12);presets.setMaxLines(1);presets.setEllipsize(android.text.TextUtils.TruncateAt.END);a.presetChip=presets;heading.addView(presets,new LinearLayout.LayoutParams(-2,a.dp(48)));a.add(heading);
        LinearLayout screen=a.column();screen.setPadding(a.dp(16),a.dp(12),a.dp(16),a.dp(12));screen.setBackground(StudioTheme.background(a,0xff182819,16));
        LinearLayout top=a.row();mic=label("",12,MainActivity.LIME);mic.setTypeface(StudioTheme.bold(a));top.addView(mic,new LinearLayout.LayoutParams(0,-2,1));clock=label("",13,MainActivity.TEXT);clock.setTypeface(StudioTheme.mono(a));clock.setGravity(Gravity.END);top.addView(clock);screen.addView(top);
        route=label("",12,0xffb9cbb7);route.setPadding(0,a.dp(5),0,a.dp(5));screen.addView(route);
        backing=label("",12,0xffb9cbb7);screen.addView(backing);
        LinearLayout input=a.row();input.setPadding(0,a.dp(8),0,0);pitch=label("",19,MainActivity.LIME);pitch.setTypeface(StudioTheme.mono(a));input.addView(pitch,new LinearLayout.LayoutParams(0,-2,1));level=label("",12,MainActivity.TEXT);level.setTypeface(StudioTheme.mono(a));input.addView(level);screen.addView(input);
        meter=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal);meter.setMax(1000);meter.setContentDescription(tr("입력 레벨","Input level"));meter.setProgressTintList(android.content.res.ColorStateList.valueOf(MainActivity.LIME));meter.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff3c4c3b));LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,a.dp(6));mp.topMargin=a.dp(6);screen.addView(meter,mp);a.add(screen);
        LinearLayout controls=a.row();a.micButton=StudioTheme.styled(a,tr("마이크 켜기","Turn mic on"),StudioButton.STANDARD,a::requestMicToggle);a.weighted(controls,a.micButton,48);a.outputButton=StudioTheme.styled(a,tr("출력 ▾","Output ▾"),StudioButton.STANDARD,a::monitoringOptions);a.weighted(controls,a.outputButton,48);a.add(controls);
        a.monitorButton=StudioTheme.styled(a,tr("내 목소리 듣기 · 꺼짐","Voice monitoring · off"),StudioButton.STANDARD,()->{if(NativeAudio.stats()[17]>.5){a.monitoringOptions();return;}if(AudioService.active&&AudioService.canMonitor(a))a.set(11,a.values[11]>.5?0:1);else a.toast(tr("마이크와 출력 장치를 먼저 켜주세요.","Turn on the microphone and output first."));});a.add(a.monitorButton);
        LinearLayout effects=a.column();effects.setPadding(a.dp(10),a.dp(12),a.dp(10),a.dp(12));effects.setBackground(StudioTheme.background(a,MainActivity.PANEL,18));
        LinearLayout section=a.row();TextView effectTitle=label(tr("보컬 효과","VOCAL EFFECTS"),15,MainActivity.TEXT);effectTitle.setTypeface(StudioTheme.bold(a));section.addView(effectTitle,new LinearLayout.LayoutParams(0,-2,1));Button reset=StudioTheme.styled(a,tr("초기화","Reset"),StudioButton.QUIET,this::reset);reset.setTextSize(12);section.addView(reset,new LinearLayout.LayoutParams(a.dp(72),a.dp(48)));effects.addView(section);
        TextView hint=label(tr("노브를 위아래로 움직여 조절하세요","Drag knobs up or down to adjust"),11,MainActivity.MUTED);hint.setPadding(a.dp(4),0,a.dp(4),a.dp(4));effects.addView(hint);
        addKnobs(effects);a.add(effects);

        LinearLayout links=a.row();a.weighted(links,a.button(tr("화음 ›","Harmony ›"),()->a.chooseTab(9)),48);a.weighted(links,a.button(tr("반주 ›","Backing ›"),()->a.chooseTab(1)),48);a.add(links);
        a.status=a.text("",12,MainActivity.MUTED);a.status.setOnClickListener(v->a.monitoringOptions());a.add(a.status);
        a.content=a.actionDock;a.addModeSelector(false);LinearLayout captionRow=a.row();a.recordCaption=a.text(tr("오디오 녹음","Audio recording"),12,MainActivity.MUTED);captionRow.addView(a.recordCaption,new LinearLayout.LayoutParams(0,-2,1));a.timer=a.text("",13,MainActivity.RED);a.timer.setTypeface(StudioTheme.mono(a));captionRow.addView(a.timer);a.actionDock.addView(captionRow,new LinearLayout.LayoutParams(-1,a.dp(24)));
        LinearLayout recordRow=a.row();a.recButton=StudioTheme.styled(a,tr("● 녹음 시작","● Start recording"),StudioButton.STANDARD,a::toggleRecord);a.recButton.setTextColor(MainActivity.BG);a.recButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(MainActivity.LIME));a.recButton.setMinHeight(a.dp(56));recordRow.addView(a.recButton,new LinearLayout.LayoutParams(0,-2,1));a.recordOptions=a.button("⋯",a::recordingOptions);a.recordOptions.setTextSize(22);a.recordOptions.setContentDescription(tr("녹음 설정 · 시작 시간 및 다른 앱 소리","Recording settings · start time and other app audio"));LinearLayout.LayoutParams options=new LinearLayout.LayoutParams(a.dp(52),a.dp(56));options.leftMargin=a.dp(8);recordRow.addView(a.recordOptions,options);a.actionDock.addView(recordRow);a.content=a.page;
    }
    void reset(){int[] ids={0,2,3,4,5,6};float[] defaults={1,.27f,2.2f,.14f,280,.25f};for(int i=0;i<ids.length;i++)a.set(ids[i],defaults[i]);syncKnobs();}
    void addKnobs(LinearLayout effects){
        String[] labels={tr("리버브","Reverb"),tr("잔향 길이","Decay"),tr("에코 양","Echo"),tr("에코 간격","Delay"),tr("에코 반복","Repeat"),tr("목소리 크기","Voice level")};
        float[] mins={0,.3f,0,70,0,0},maxs={1,6,.8f,700,.7f,2};
        MainActivity.Formatter[] formats={v->Math.round(v*100)+"%",v->String.format(Locale.US,"%.1f s",v),v->Math.round(v*100)+"%",v->Math.round(v)+" ms",v->Math.round(v*100)+"%",v->Math.round(v*100)+"%"};
        int widthDp=(int)(a.getResources().getDisplayMetrics().widthPixels/a.getResources().getDisplayMetrics().density);int columns=widthDp>=390?3:2;
        for(int i=0;i<knobs.length;){LinearLayout row=a.row();for(int c=0;c<columns&&i<knobs.length;c++,i++){final int index=i;StudioKnob knob=new StudioKnob(a,labels[i],mins[i],maxs[i],a.values[ids[i]],formats[i],v->a.set(ids[index],v));knobs[i]=knob;row.addView(knob,new LinearLayout.LayoutParams(0,-2,1));}effects.addView(row,new LinearLayout.LayoutParams(-1,-2));}
    }
    void syncKnobs(){for(int i=0;i<knobs.length;i++)if(knobs[i]!=null)knobs[i].sync(a.values[ids[i]]);}
    void poll(float[] s,boolean running){if(mic==null)return;mic.setText(running?tr("● 마이크 켜짐","● MIC ON"):tr("● 마이크 대기","● MIC READY"));route.setText(tr("출력: ","Output: ")+(AudioRouting.selected(a)==0&&!a.headphones?tr("이어폰 미연결","Headphones disconnected"):a.outputName())+" · "+(s[17]>.5?tr("보호 음소거","safety mute"):running&&s[21]>.5?tr("내 목소리 듣기 켜짐","monitor on"):tr("내 목소리 듣기 꺼짐","monitor off")));clock.setText(RecordingManager.active?(RecordingManager.paused?"Ⅱ ":"● ")+a.formatTime(s[8]):AudioService.waiting()?tr("예약 ","START IN ")+AudioService.remaining()+tr("초","s"):"00:00");double[] cs=a.composer.state();if(a.composer.playing())backing.setText(tr("작곡기 · ","Composer · ")+tr("마디 ","bar ")+(1+(int)(Math.max(0,cs[0])/MusicTheory.barBeats(a.composer.project.meter))));else if(s[16]>.5){double[] local=NativeAudio.localPlaybackInfo();backing.setText(tr("반주 · ","Backing · ")+a.formatTime(local[0])+" / "+a.formatTime(local[1]));}else backing.setText(tr("반주 대기","Backing idle"));pitch.setText(tr("입력 음 ","INPUT ")+(s[0]>0?a.noteName((int)s[2]):"—"));float rms=Math.max(0,s[3]);level.setText(rms>.00001?String.format(Locale.US,"%.0f dB",20*Math.log10(rms)):"— dB");meter.setProgress((int)(1000*Math.max(0,Math.min(1,(20*Math.log10(Math.max(.00001,rms))+60)/60))));syncKnobs();}
}
