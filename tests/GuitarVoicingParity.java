package pro.livemic.studio;
import java.io.*;
import java.util.*;
public class GuitarVoicingParity {
 public static void main(String[] args)throws Exception {int cases=0;try(Scanner in=new Scanner(new File(args[0]))){while(in.hasNextLine()){String[] fields=in.nextLine().split(" ");int root=Integer.parseInt(fields[0]),quality=Integer.parseInt(fields[1]);int[] actual=MusicTheory.guitar(root,quality,0);if(actual.length!=fields.length-2)throw new AssertionError("length root="+root+" quality="+quality);for(int i=0;i<actual.length;i++)if(actual[i]!=Integer.parseInt(fields[i+2]))throw new AssertionError("tone root="+root+" quality="+quality);cases++;}}if(cases!=132)throw new AssertionError("incomplete input");System.out.println("PASS: Java/native guitar voicings for "+cases+" root/quality combinations");}
}
