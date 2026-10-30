package pro.livemic.studio;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.function.IntConsumer;

/** Shared, scrollable option sheet for app-owned choices. System pickers remain native. */
final class StudioDialogs {
    private StudioDialogs() {}

    static void choose(Activity activity, String title, String[] items, int selected, IntConsumer onChoose) {
        Dialog dialog=new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        int pad=StudioTheme.dp(activity,20);
        LinearLayout sheet=new LinearLayout(activity);
        sheet.setOrientation(LinearLayout.VERTICAL);
        sheet.setPadding(pad,StudioTheme.dp(activity,12),pad,StudioTheme.dp(activity,18));
        sheet.setBackground(StudioTheme.background(activity,StudioTheme.SURFACE,24));

        TextView handle=StudioTheme.text(activity,"━━━━",16,StudioTheme.MUTED);
        handle.setGravity(Gravity.CENTER);
        sheet.addView(handle,new LinearLayout.LayoutParams(-1,StudioTheme.dp(activity,20)));
        TextView heading=StudioTheme.text(activity,title,20,StudioTheme.TEXT);
        heading.setTypeface(StudioTheme.bold(activity));
        heading.setPadding(0,StudioTheme.dp(activity,12),0,StudioTheme.dp(activity,14));
        sheet.addView(heading);

        ScrollView scroll=new ScrollView(activity);
        scroll.setFillViewport(false);
        LinearLayout choices=new LinearLayout(activity);
        choices.setOrientation(LinearLayout.VERTICAL);
        for(int i=0;i<items.length;i++){
            final int index=i;
            StudioButton option=StudioTheme.styled(activity,(i==selected?"✓  ":"")+items[i],StudioButton.OPTION,()->{
                dialog.dismiss();
                if(onChoose!=null)onChoose.accept(index);
            });
            option.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
            option.setSingleLine(false);
            option.setContentDescription(items[i]+(i==selected?", 선택됨":""));
            if(i==selected){option.setSelected(true);option.setTextColor(StudioTheme.ACCENT);}
            else if(items[i].contains("삭제")||items[i].contains("지우기"))option.setTextColor(StudioTheme.DANGER);
            LinearLayout.LayoutParams itemParams=new LinearLayout.LayoutParams(-1,ViewGroup.LayoutParams.WRAP_CONTENT);
            itemParams.bottomMargin=StudioTheme.dp(activity,8);
            choices.addView(option,itemParams);
        }
        scroll.addView(choices);
        int maxList=Math.max(StudioTheme.dp(activity,96),activity.getResources().getDisplayMetrics().heightPixels-StudioTheme.dp(activity,230));
        int listHeight=Math.min(maxList,StudioTheme.dp(activity,items.length*64));
        sheet.addView(scroll,new LinearLayout.LayoutParams(-1,listHeight));
        StudioButton close=StudioTheme.styled(activity,"닫기",StudioButton.QUIET,dialog::dismiss);
        sheet.addView(close,new LinearLayout.LayoutParams(-1,StudioTheme.dp(activity,48)));
        dialog.setContentView(sheet);
        dialog.setCanceledOnTouchOutside(true);
        Window window=dialog.getWindow();
        if(window!=null){
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setGravity(Gravity.BOTTOM);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams params=window.getAttributes();
            params.dimAmount=.68f;
            window.setAttributes(params);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
        if(window!=null)window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    static void confirmDanger(Activity activity,String title,String message,String action,Runnable confirmed){
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(title).setMessage(message)
            .setNegativeButton("취소",null).setPositiveButton(action,(d,w)->confirmed.run()).create();
        dialog.setOnShowListener(ignored->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(StudioTheme.DANGER));
        dialog.show();
    }
}
