package pro.livemic.studio;
import android.content.*;
import org.json.*;
import java.io.*;
import java.util.*;
public final class TakeStore {
    public final File directory;
    public TakeStore(Context c){this(c,true);}
    public TakeStore(Context c,boolean recoverFiles){directory=new File(c.getFilesDir(),"takes");directory.mkdirs();if(recoverFiles)recover();}
    private void recover(){File[] files=directory.listFiles((d,n)->n.endsWith(".wav"));if(files==null)return;
        for(File f:files){String id=f.getName().replace(".wav","");if(new File(directory,id+".json").exists()||f.length()<=44||(RecordingManager.active&&id.equals(RecordingManager.id)))continue;
            try{int bytesPerSecond;try(RandomAccessFile r=new RandomAccessFile(f,"rw")){r.seek(28);bytesPerSecond=Integer.reverseBytes(r.readInt());if(bytesPerSecond<=0)continue;r.seek(4);int riff=Integer.reverseBytes((int)f.length()-8);r.writeInt(riff);r.seek(40);r.writeInt(Integer.reverseBytes((int)f.length()-44));}
                save(id,"복구된 테이크",(f.length()-44)/(double)bytesPerSecond,false,"중단된 녹음을 복구했습니다.");}catch(Exception ignored){}}
    }
    public void save(String id,String name,double seconds,boolean video,String warning)throws Exception{
        JSONObject j=new JSONObject().put("id",id).put("name",name).put("seconds",seconds).put("created",System.currentTimeMillis()).put("video",video).put("warning",warning);
        write(id,j);
    }
    private void write(String id,JSONObject j)throws Exception{File tmp=new File(directory,id+".tmp");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(j.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}if(!tmp.renameTo(new File(directory,id+".json")))throw new IOException("보관함 저장 실패");}
    public List<JSONObject> list(){List<JSONObject> list=new ArrayList<>();File[] fs=directory.listFiles((d,n)->n.endsWith(".json"));if(fs!=null)for(File f:fs)try{list.add(new JSONObject(new String(java.nio.file.Files.readAllBytes(f.toPath()),java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception ignored){}
        list.sort((a,b)->Long.compare(b.optLong("created"),a.optLong("created")));return list;}
    public void rename(JSONObject j,String name)throws Exception{j.put("name",name);write(j.getString("id"),j);}
    public void delete(String id){for(String ext:new String[]{".wav",".mid",".mp4",".json",".video.mp4",".aac.m4a"})new File(directory,id+ext).delete();}
    public File file(String id,String ext){return new File(directory,id+ext);}
}
