package com.bmh.ms101;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Set;

/**
 * Utility class for preferences
 */
public class PrefsUtil {

    private static final String DF_PREF_FILE_NAME = "_dreamf_pref";
    public static final String WIDGET_PREF_FILE_NAME = "widgets_pref";

    // Med Keys
    public static final String MK_TECFIDERA = "3";

    private Context mCtx;

    public PrefsUtil(Context ctx) {
        mCtx = ctx;
    }

    /**
     * Gets the pref file
     * @return SharedPreferences object representing pref file
     */
    public SharedPreferences getPrefs() {
        return mCtx.getSharedPreferences(mCtx.getPackageName(), Context.MODE_PRIVATE);
    }

    /**
     * Attempts to get a String Set from the pref file
     * @param key Key in pref file for String Set
     * @param def Default value to return if the String Set isn't found
     * @return Either the String Set or the default value
     */
    Set<String> getPrefStringSet(String key, Set<String> def) {
        return getPrefs().getStringSet(key, def);
    }

    /**
     * Attempts to get a String from the pref file
     * @param key Key in pref file for String
     * @param def Default value to return if the String isn't found
     * @return Either the String or the default value
     */
    String getPrefString(String key, String def) {
        return getPrefs().getString(key, def);
    }

    /**
     * Attempts to get a boolean from the pref file
     * @param key Key in pref file for boolean
     * @param def Default value to return if the boolean isn't found
     * @return Either the boolean or the default value
     */
    boolean getPrefBoolean(String key, boolean def) {
        return getPrefs().getBoolean(key, def);
    }

    /**
     * Puts a boolean into the pref file
     * @param key Key in pref file for boolean
     * @param bool Boolean to put
     */
    void putPrefBoolean(String key, boolean bool) {
        SharedPreferences settings = getPrefs();
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, bool);
        editor.commit();
    }

    /**
     * Attempts to get an int from the pref file
     * @param key Key in pref file for int
     * @param def Default value to return if the int isn't found
     * @return Either the int or the default value
     */
    public int getPrefInt(String key, int def) {
        return getPrefs().getInt(key, def);
    }

    /**
     * @return Dreamfactory Prefs file
     */
    public SharedPreferences getDFPrefs() {
        return mCtx.getSharedPreferences(DF_PREF_FILE_NAME, Context.MODE_PRIVATE);
    }

    public String getDFString(String key) {
        SharedPreferences settings = getDFPrefs();
        return settings.getString(key, "");
    }

    public String getDFString(String key, String defaultString) {
        SharedPreferences settings = getDFPrefs();
        return settings.getString(key, defaultString);
    }

    public long getDFLong(String key, long defaultLong) {
        SharedPreferences settings = getDFPrefs();
        return settings.getLong(key, defaultLong);
    }

    public synchronized void putDFString(String key, String value) {
        SharedPreferences settings = getDFPrefs();
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public synchronized void putDFLong(String key, long value) {
        SharedPreferences settings = getDFPrefs();
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    /**
     * @return Widget Prefs file
     */
    public SharedPreferences getWidgetPrefs() {
        return mCtx.getSharedPreferences(WIDGET_PREF_FILE_NAME, Context.MODE_PRIVATE);
    }
}
