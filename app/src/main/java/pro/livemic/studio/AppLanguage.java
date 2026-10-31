package pro.livemic.studio;

import android.content.Context;

/** App UI language only; never changes the device locale or saved user content. */
final class AppLanguage {
    private static final String PREFS="app_language", KEY="english";
    private AppLanguage() {}
    static boolean english(Context context){return context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(KEY,false);}
    static void set(Context context,boolean english){context.getApplicationContext().getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(KEY,english).apply();}
    static String text(Context context,String korean,String english){return english(context)?english:korean;}
}
