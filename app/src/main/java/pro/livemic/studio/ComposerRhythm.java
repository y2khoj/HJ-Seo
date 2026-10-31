package pro.livemic.studio;
import java.util.*;

/** Rhythms use quarter-note positions, including three-quarter compound bars. */
final class ComposerRhythm {
    static final class Hit {final double beat,gain;final int note;Hit(double b,int n,double g){beat=b;note=n;gain=g;}}
    static List<Hit> drums(int rhythm,boolean fill){return drums(rhythm,fill,0,.55);}
    static List<Hit> drums(int rhythm,boolean fill,int barIndex,double energy){
        List<Hit> hits=new ArrayList<>();double length=rhythm>=6?3:4;
        double step=rhythm==1||rhythm==3?.25:rhythm==6?1:.5;
        if(rhythm==2){for(int i=0;i<4;i++){if(!fill||i<length-1){hits.add(new Hit(i,42,.42));hits.add(new Hit(i+2./3,42,.25));}}}
        else for(double beat=0;beat<length;beat+=step){
            int i=(int)Math.round(beat/step);
            if(fill&&beat>=length-1)continue;
            if(step==.25&&i%2==1&&energy<.45)continue;
            hits.add(new Hit(beat,rhythm==6?44:rhythm==4&&i%4==3?46:42,i%2==0?.43:.24));
        }
        double[] kicks=kicks(rhythm,barIndex);
        for(double beat:kicks)if(!fill||beat<length-1)hits.add(new Hit(beat,36,.78));
        double[] snares=rhythm==5?new double[]{1.5,2.5}:rhythm==6?new double[]{2}:rhythm==7?new double[]{1.5}:new double[]{1,3};
        for(double beat:snares)if(!fill||beat<length-1)hits.add(new Hit(beat,rhythm==5?37:38,rhythm==6?.44:.7));
        if(!fill&&(rhythm==3||rhythm==1)&&energy>.35)hits.add(new Hit(2.75,38,.19));
        if(fill){hits.add(new Hit(length-.75,38,.33));hits.add(new Hit(length-.5,47,.48));hits.add(new Hit(length-.25,45,.58));}
        return hits;
    }
    static double[] kicks(int rhythm,int barIndex){
        switch(rhythm){case 1:case 3:return barIndex%4==3?new double[]{0,1.75,2.5,3.5}:new double[]{0,1.75,2.5};case 4:return new double[]{0,.5,2,2.5};case 5:return new double[]{0,1.5,2,3.5};case 6:case 7:return new double[]{0};default:return barIndex%4==3?new double[]{0,2,3.5}:new double[]{0,2};}
    }
    static double[] strums(int rhythm){switch(rhythm){case 1:return new double[]{0,.75,1.25,1.75,2,2.75,3.25,3.75};case 2:return new double[]{0,2./3,1,1+2./3,2,2+2./3,3,3+2./3};case 3:return new double[]{0,.75,1.5,2.5,3.25};case 4:return new double[]{0,.5,1,1.5,2,2.5,3,3.5};case 5:return new double[]{0,.75,1.5,2.5,3.25};case 6:return new double[]{0,1,2};case 7:return new double[]{0,1.5,2.5};default:return new double[]{0,1,1.5,2.5,3,3.5};}}
}
