package pro.livemic.studio;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Bounded MRU history. Keep document URIs, never broad storage permissions. */
final class RecentBackings {
    private final Context context;
    RecentBackings(Context context){this.context=context.getApplicationContext();}
    List<JSONObject> list(){
        List<JSONObject> out=new ArrayList<>();
        try{JSONArray a=new JSONArray(context.getSharedPreferences("backing-history",0).getString("items","[]"));
            for(int i=0;i<a.length()&&out.size()<10;i++){JSONObject item=a.optJSONObject(i);if(item!=null&&!item.optString("uri").isEmpty())out.add(item);}
        }catch(Exception ignored){}return out;
    }
    String name(Uri uri){
        try(Cursor c=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){
            if(c!=null&&c.moveToFirst()&&!c.isNull(0))return c.getString(0);
        }catch(Exception ignored){}return uri.getLastPathSegment()==null?"선택한 반주":uri.getLastPathSegment();
    }
    void remember(Uri uri,String name){
        List<JSONObject> items=list();items.removeIf(j->uri.toString().equals(j.optString("uri")));
        try{JSONObject item=new JSONObject();item.put("uri",uri.toString());item.put("name",name);item.put("time",System.currentTimeMillis());items.add(0,item);}catch(Exception ignored){}
        save(items);
    }
    void remove(Uri uri){List<JSONObject> items=list();items.removeIf(j->uri.toString().equals(j.optString("uri")));save(items);}
    private void save(List<JSONObject> items){JSONArray a=new JSONArray();for(int i=0;i<Math.min(10,items.size());i++)a.put(items.get(i));context.getSharedPreferences("backing-history",0).edit().putString("items",a.toString()).apply();}
}
