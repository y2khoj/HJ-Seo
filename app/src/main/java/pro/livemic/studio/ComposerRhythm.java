package pro.livemic.studio;
import java.util.*;

/** All positions are quarter notes; compound meter has six eighth-note subdivisions. */
final class ComposerRhythm {
    static final class Hit {final double beat,gain;final int note;Hit(double b,int n,double g){beat=b;note=n;gain=g;}}
    static List<Hit> drums(int rhythm,boolean fill){
        List<Hit> hits=new ArrayList<>();double length=rhythm==6||rhythm==7?3:4;
        if(rhythm==2){for(int i=0;i<4;i++){hits.add(new Hit(i,42,.48));hits.add(new Hit(i+2./3,42,.3));}}
        else {double step=rhythm==1||rhythm==3?.25:rhythm==6?1:.5;for(double beat=0;beat<length;beat+=step)hits.add(new Hit(beat,(rhythm==4&&beat%2==1.5)?46:rhythm==6?44:42,(Math.round(beat/step)%2==0?.47:.28)));}
        double[] kicks=rhythm==1?new double[]{0,1.75,2.5}:rhythm==3?new double[]{0,1.75,2.5}:rhythm==4?new double[]{0,.5,2,2.5}:rhythm==5?new double[]{0,1.5,2,3.5}:rhythm==6?new double[]{0}:rhythm==7?new double[]{0}:new double[]{0,2};
        for(double beat:kicks)hits.add(new Hit(beat,36,.82));
        double[] snares=rhythm==5?new double[]{0,1.5,2.5}:rhythm==6?new double[]{2}:rhythm==7?new double[]{1.5}:new double[]{1,3};for(double beat:snares)hits.add(new Hit(beat,rhythm==5?37:38,rhythm==6?.48:.74));if(rhythm==3)hits.add(new Hit(2.75,38,.3));
        if(fill)for(int i=0;i<4;i++)hits.add(new Hit(length-1+i*.25,new int[]{45,47,48,50}[i],.55+i*.06));return hits;
    }
    static double[] strums(int rhythm){switch(rhythm){case 1:return new double[]{0,.75,1.25,1.75,2,2.75,3.25,3.75};case 2:return new double[]{0,2./3,1,1+2./3,2,2+2./3,3,3+2./3};case 3:return new double[]{0,.75,1.5,2.5,3.25};case 4:return new double[]{0,.5,1,1.5,2,2.5,3,3.5};case 5:return new double[]{0,.75,1.5,2.5,3.25};case 6:return new double[]{0,1,2};case 7:return new double[]{0,1.5,2.5};default:return new double[]{0,1,1.5,2.5,3,3.5};}}
}
