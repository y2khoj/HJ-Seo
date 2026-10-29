package pro.livemic.studio;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import java.io.*;
public class ShareProvider extends ContentProvider {
    @Override public boolean onCreate(){return true;}
    private File file(Uri uri)throws FileNotFoundException{String name=uri.getLastPathSegment();File root=new File(getContext().getFilesDir(),"takes");File f=new File(root,name==null?"":name);
        try{if(!f.getCanonicalFile().getParentFile().equals(root.getCanonicalFile())||!f.isFile())throw new FileNotFoundException();}catch(IOException e){throw new FileNotFoundException();}return f;}
    @Override public String getType(Uri u){String p=u.toString();return p.endsWith(".mp4")?"video/mp4":p.endsWith(".mid")?"audio/midi":"audio/wav";}
    @Override public ParcelFileDescriptor openFile(Uri u,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException();return ParcelFileDescriptor.open(file(u),ParcelFileDescriptor.MODE_READ_ONLY);}
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String order){try{File f=file(u);String[] cols=p==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:p;MatrixCursor c=new MatrixCursor(cols);Object[] row=new Object[cols.length];for(int i=0;i<cols.length;i++)row[i]=cols[i].equals(OpenableColumns.DISPLAY_NAME)?f.getName():cols[i].equals(OpenableColumns.SIZE)?f.length():null;c.addRow(row);return c;}catch(Exception e){return null;}}
    @Override public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
}
