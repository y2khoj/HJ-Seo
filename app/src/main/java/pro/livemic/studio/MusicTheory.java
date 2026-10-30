package pro.livemic.studio;

import java.util.*;

/** Pitch classes and quarter-note timing, independent of Android and the audio thread. */
final class MusicTheory {
    static final String[] ROOTS={"C","C♯","D","E♭","E","F","F♯","G","A♭","A","B♭","B"};
    static final String[] SCALES={"메이저","자연 마이너","화성 마이너","도리안","믹솔리디안"};
    static final int[][] SCALE={{0,2,4,5,7,9,11},{0,2,3,5,7,8,10},{0,2,3,5,7,8,11},{0,2,3,5,7,9,10},{0,2,4,5,7,9,10}};
    static final String[] QUALITIES={"","m","dim","aug","sus2","sus4","7","maj7","m7","m7♭5","add9"};
    static final int[][] INTERVALS={{0,4,7},{0,3,7},{0,3,6},{0,4,8},{0,2,7},{0,5,7},{0,4,7,10},{0,4,7,11},{0,3,7,10},{0,3,6,10},{0,4,7,14}};
    static final String[] PRESETS={"팝 I–V–vi–IV","발라드 I–vi–IV–V","캐논 I–V–vi–iii–IV–I–IV–V","재즈 ii7–V7–Imaj7","마이너 i–VI–III–VII","마이너 종지 i–VI–iv–V7"};
    static final int[][] PROGRESSIONS={{0,4,5,3},{0,5,3,4},{0,4,5,2,3,0,3,4},{1,4,0,0},{0,5,2,6},{0,5,3,4}};
    static final String[] RHYTHMS={"팝 8비트","팝 16비트","셔플","R&B","록","보사노바","왈츠","6/8 발라드"};
    static int pc(int n){return Math.floorMod(n,12);}
    static String note(int n){return ROOTS[pc(n)]+(Math.floorDiv(n,12)-1);}
    static int quality(int scale,int degree,boolean seventh){
        int[] s=SCALE[scale];int d=Math.floorMod(degree,7),a=(s[(d+2)%7]+((d+2)/7)*12-s[d]),b=(s[(d+4)%7]+((d+4)/7)*12-s[d]);
        if(!seventh)return b==6?2:b==8?3:a==3?1:0;
        int c=s[(d+6)%7]+((d+6)/7)*12-s[d];return b==6?9:a==3?8:c==11?7:6;
    }
    static int nearest(int midi,int key,int scale){int result=midi,dist=99;for(int n=midi-6;n<=midi+6;n++)for(int interval:SCALE[scale])if(pc(n-key)==interval&&Math.abs(n-midi)<dist){result=n;dist=Math.abs(n-midi);}return result;}
    static double barBeats(int meter){return meter==0?4:3;}
    static double quarterBpm(int bpm,int meter){return meter==2?bpm*1.5:bpm;}
    static double split(int meter){return meter==0?2:meter==1?2:1.5;}
    static boolean rhythmFits(int rhythm,int meter){return meter==0?rhythm<6:meter==1?rhythm==6:rhythm==7;}
    static int fit(int midi,int lo,int hi){while(midi<lo)midi+=12;while(midi>hi)midi-=12;return midi;}
    /** One fretted pitch per real guitar string, <=4 frets from the chosen hand position. */
    static int[] guitar(int root,int quality,int pattern){
        int[] strings={40,45,50,55,59,64};Set<Integer> tones=new HashSet<>();
        if(pattern==3){tones.add(pc(root));tones.add(pc(root+7));}else for(int i:INTERVALS[quality])tones.add(pc(root+i));
        int[] best=new int[0];int score=-999;
        for(int hand=0;hand<=8;hand++){int[] notes=new int[6];int count=0;for(int open:strings){int chosen=-1;for(int fret=0;fret<=hand+4;fret++){if(fret>0&&fret<hand)continue;if(tones.contains(pc(open+fret))){chosen=open+fret;break;}}if(chosen>=0)notes[count++]=chosen;}int value=count*20-hand;if(count>0&&pc(notes[0])==pc(root))value+=10;if(value>score){score=value;best=Arrays.copyOf(notes,count);}}
        return best;
    }
}
