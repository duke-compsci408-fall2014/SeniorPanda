package com.bmh.ms101;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;

import com.bmh.ms101.ex.DFNotAddedException;
import com.bmh.ms101.ex.UserMedsNotAddedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Object that represents information about a user (No personal info). Because multiple instances of
 * this object can exist at one time during the lifecycle of the app, there should never be any
 * non-static data stored in it, with the exception of the context that the User object was created
 * with.
 */
public class User {

    // Data types
    public static final int MED = 0;
    public static final int SYMP = 1;
    public static final int STRESS = 2;

    public static final int SYMP_DATA_TYPE = 11;

    // Key strings to get values from the pref file
    private static final String PREF_MEDS = "key_meds";
    private static final String PREF_PIN = "key_pin";

    private static final String MEDS_PREF_FILE_NAME = "todays_meds";
    private static final String MEDS_DATE = "key_date";
    private static final String MEDS_PREF_KEY_PREFIX = "key_";

    public static final String DF_PREF_EMAIL = "email";
    public static final String DF_PREF_PWD = "password";
    public static final String DF_PREF_SESSION_ID = "session_id";
    public static final String DF_PREF_SESSION_ID_EXPIRES = "session_id_expires";
    // (Duration is in seconds) Sessions may not be valid after 24 minutes.
    public static final int DF_SESSION_DURATION = 1440;

    private static final String SECRET_KEY = "ms101BAMBOOmobileHealth@ms101.me";

    // Corresponds to the proper indexes of the meds in the strings.xml file. Sorted by ID #.
    public static String[] MED_NAMES = null;
    private static ArrayList<String> mDevList = new ArrayList<>();
    private final Context mCtx;
    private PrefsUtil mPrefsUtil;

    /**
     * Create new User object using the given context
     * @param context Context used to create this backend
     */
    public User(Context context) {
        mCtx = context;
        mPrefsUtil = new PrefsUtil(mCtx);
        mDevList.add("9076224588@ms101.me");
        mDevList.add("8284541964@ms101.me");
        mDevList.add("test1@pd101.me");
        // Populate an array with the list of meds at indices corresponding to their ID numbers
        if (MED_NAMES == null) {
            String[] meds = mCtx.getResources().getStringArray(R.array.medications);
            MED_NAMES = new String[meds.length + 1];
            MED_NAMES[0] = ""; // No med has an ID of 0
            for (String med : meds) {
                String[] idAndMed = med.split(":");
                MED_NAMES[Integer.parseInt(idAndMed[0])] = idAndMed[1];
            }
        }
    }

    /**
     * Checks if there is a user account and that meds are set up for this user. Can be told to
     * ignore one of the two checks. Called from main activity
     * @throws UserMedsNotAddedException If the user has no meds set up
     */
    public void ensureSetupComplete(boolean ignoreDf) throws UserMedsNotAddedException, DFNotAddedException {
        if (!ignoreDf) ensureDFAdded();
        ensureMedsAdded();
    }

    /**
     * Checks the pref file to see if there is
     * @throws com.bmh.ms101.ex.DFNotAddedException
     */
    void ensureDFAdded() throws DFNotAddedException {
        if (getDFSessionId().length() == 0) throw new DFNotAddedException();
    }

    /**
     * Checks the pref file to make sure this user has some meds set up
     * @throws UserMedsNotAddedException If the user has no meds set up
     */
    void ensureMedsAdded() throws UserMedsNotAddedException {
        if (mPrefsUtil.getPrefStringSet(PREF_MEDS, null) == null) throw new UserMedsNotAddedException();
    }

    /**
     * Attempts to get the user's account name from the pref file
     * @return User's account name or null
     */
    public String getAccountName() {
        return mPrefsUtil.getDFString(DF_PREF_EMAIL, null);
    }

    /**
     * @return The user's account ID (The account name sans "@ms101.me")
     */
    public String getAccountID() {
        return getAccountName().replace("@ms101.me", "");
    }

    /**
     * Attempts to get the String Set of meds from the pref file
     * @return Either the found set or a new empty set
     */
    public Set<String> getMeds() {
        return mPrefsUtil.getPrefStringSet(PREF_MEDS, new HashSet<String>());
    }

    /**
     * Get the ID numbers of the meds that are set up for this user
     * @return String set of med ID numbers
     */
    public Set<String> getMedsIds() {
        Set<String> ids = new HashSet<>();
        for (String med : getMeds()) {
            ids.add(med.split(":")[0]);
        }
        return ids;
    }

    /**
     * Puts the String Set of meds into the pref file and sets an alarm for every day at 6 PM
     * @param selectedMeds String Set of meds selected for the user
     */
    public void recordAddedMeds(Set<String> selectedMeds) {
        mPrefsUtil.getPrefs().edit().putStringSet(PREF_MEDS, selectedMeds).commit();
        checkForSpecialMeds();
        MS101Receiver.createAlarms(mCtx);
    }

    /**
     * Checks for medications such as Tecfidera, which has a special widget and notifications that
     * need to be enabled.
     */
    private void checkForSpecialMeds() {
        // Logic for Tecfidera
        if (getMedsIds().contains(PrefsUtil.MK_TECFIDERA)) {
            // If the user is set up for Tecfidera
            if (!hasMedDoseWidget(PrefsUtil.MK_TECFIDERA)) {
                // and doesn't have a widget for it yet
                new AlertDialog.Builder(mCtx).setMessage(String.format(mCtx.getResources().getString(R.string.md_setup_prompt), MED_NAMES[3]))
                        .setNeutralButton(R.string.okay, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                        .show();
            } else {
                // Or if they do have the widget, be sure it's set up
                if (!getMedDoseWidgetConfigured(getMedDoseWidgetID(PrefsUtil.MK_TECFIDERA))) {
                    // If the widget isn't set up, then re-"set it up"
                    int widgetId = getMedDoseWidgetID(PrefsUtil.MK_TECFIDERA);
                    recordMedDoseWidgetUnconfigured(widgetId, false);
                    triggerWidgetRefresh(mCtx, widgetId);
                }
            }
        } else if (!getMedsIds().contains(PrefsUtil.MK_TECFIDERA) && hasMedDoseWidget(PrefsUtil.MK_TECFIDERA)) {
            // User isn't set up for tecfidera, but does have a widget for tecfidera, so set the widget to be unconfigured
            int widgetId = getMedDoseWidgetID(PrefsUtil.MK_TECFIDERA);
            recordMedDoseWidgetUnconfigured(widgetId, true);
            triggerWidgetRefresh(mCtx, widgetId);
        }
        // Add logic for any other special meds here
    }

    /**
     * @return An array of any meds the user is set up for that are defined as special for some mReason
     */
    public String[] getSpecialMeds() {
        ArrayList<String> specialMeds = new ArrayList<>(Arrays.asList(mCtx.getResources().getStringArray(R.array.special_meds)));
        Set<String> currMeds = getMeds();
        Iterator<String> specialIterator = specialMeds.iterator();
        while (specialIterator.hasNext()) {
            String specialMed = specialIterator.next();
            if (!currMeds.contains(specialMed)) specialIterator.remove();
        }
        return specialMeds.toArray(new String[specialMeds.size()]);
    }

    /**
     * @param medKey Med's ID as a String
     * @return The ID of a widget that is tracking it, or -1 if there isn't one.
     */
    public int getMedDoseWidgetID(String medKey) {
        return mPrefsUtil.getWidgetPrefs().getInt(medKey, -1);
    }

    /**
     * @param widgetId Widget ID to check
     * @return The String Med ID that this widget tracks, or null if it isn't tracking currently
     */
    public String getMedKey(int widgetId) {
        return mPrefsUtil.getWidgetPrefs().getString(String.valueOf(widgetId), "null");
    }

    /**
     * @param widgetId Widget ID to check
     * @return True if this widget is configured for a med
     */
    public boolean getMedDoseWidgetConfigured(int widgetId) {
        return !getMedKey(widgetId).contains("null");
    }

    /**
     * Stores med key and widget id and associates them with each other. Removes current associations
     * if need be.
     * @param medKey Med's ID as a String
     * @param widgetId Widget's ID
     */
    public void recordMedDoseWidgetId(String medKey, int widgetId) {
        String currMedKey = mPrefsUtil.getWidgetPrefs().getString(String.valueOf(widgetId), "null");
        SharedPreferences.Editor widgetPrefs = mPrefsUtil.getWidgetPrefs().edit();
        if (!currMedKey.equals("null")) widgetPrefs.remove(currMedKey.replace("null", ""));
        widgetPrefs.putInt(medKey, widgetId).putString(String.valueOf(widgetId), medKey)
                .commit();
    }

    /**
     * Set a particular widget to an unconfigured state or not
     * @param widgetId Widget's ID
     * @param setUnconfigured True to set unconfigured, false to set configured
     */
    public void recordMedDoseWidgetUnconfigured(int widgetId, boolean setUnconfigured) {
        String medKey = mPrefsUtil.getWidgetPrefs().getString(String.valueOf(widgetId), null).replace("null", "");
        mPrefsUtil.getWidgetPrefs().edit()
                .putString(String.valueOf(widgetId), (setUnconfigured ? "null" : "") + medKey)
                .commit();
    }

    /**
     * Removes widget/medkey combo from the prefs
     * @param medKey Med's ID as a String
     * @param widgetId Widget's ID
     */
    public void removeMedDoseWidgetId(String medKey, int widgetId) {
        mPrefsUtil.getWidgetPrefs().edit()
                .remove(medKey)
                .remove(String.valueOf(widgetId))
                .commit();
    }

    /**
     * Checks to see if there is a valid Widget ID stored for a medication, which would indicate that
     * we have a med dose widget for that medication on the home screen currently.
     * @param medKey Med's ID as a String
     * @return True if there is a widget ID stored for this ID
     */
    private boolean hasMedDoseWidget(String medKey) {
        return getMedDoseWidgetID(medKey) != -1;
    }

    /**
     * Refreshes the widget with the specified widget ID
     * @param context Context to use
     * @param widgetId Widget's ID
     */
    public static void triggerWidgetRefresh(Context context, int widgetId) {
        context.sendBroadcast(new Intent(context, WidgetProvider.class)
                .setAction("android.appwidget.action.APPWIDGET_UPDATE")
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] {widgetId}));
    }

    /**
     * Check to see if we have stored dosage data for this med from today
     * @param medID The ID of the med to check
     * @return -2 if no data, -1 if N/A, or 0-4
     */
    public int getDosesFromToday(int medID) {
        SharedPreferences prefs = mCtx.getSharedPreferences(MEDS_PREF_FILE_NAME, Context.MODE_PRIVATE);
        if (isDosageDataExpired(prefs)) return -2;
        String medKey = MEDS_PREF_KEY_PREFIX + medID;
        return prefs.getInt(medKey, -2);
    }

    /**
     * Stores the does of a med that the user has already taken today
     * @param medIDs The IDs of the meds to store
     * @param doses The number of doses to store for each med
     */
    public void recordDosesFromToday(int[] medIDs, int[] doses) {
        SharedPreferences.Editor prefs = mCtx.getSharedPreferences(MEDS_PREF_FILE_NAME,
                Context.MODE_PRIVATE).edit();
        prefs.putLong(MEDS_DATE, Calendar.getInstance().getTimeInMillis());
        for (int i = 0; i < medIDs.length; i++) {
            prefs.putInt(MEDS_PREF_KEY_PREFIX + medIDs[i], doses[i]);
        }
        prefs.commit();
    }

    /**
     * Check to make sure the dosage data we have stored isn't from yesterday
     * @param prefs The prefs file that stores today's med dosage data
     * @return True if the data isn't from today
     */
    private boolean isDosageDataExpired(SharedPreferences prefs) {
        long storedDate = prefs.getLong(MEDS_DATE, -1);
        if (storedDate == -1) return false;
        Calendar c = Calendar.getInstance();
        int currDayOfYear = c.get(Calendar.DAY_OF_YEAR);
        c.setTimeInMillis(storedDate);
        int storedDayOfYear = c.get(Calendar.DAY_OF_YEAR);
        return !(currDayOfYear == storedDayOfYear);
    }

    /**
     * @return Current session ID
     */
    public String getDFSessionId() {
        return mPrefsUtil.getDFString(DF_PREF_SESSION_ID, "");
    }

    /**
     * @return True if we haven't passed the session's expiration time
     */
    public boolean isDFSessionIdValid() {
        long now = Calendar.getInstance().getTimeInMillis();
        long expireTime = mPrefsUtil.getDFLong(DF_PREF_SESSION_ID_EXPIRES, -1);
        return expireTime > now;
    }

    /**
     * Records the session ID for us to use, as well as the time it will expire.
     * @param sessionId Session ID for future use
     */
    public void recordDFSessionId(String sessionId) {
        // To get expire time, add the number of milliseconds in duration to current time in milliseconds
        long expireTime = Calendar.getInstance().getTimeInMillis() + (DF_SESSION_DURATION * 1000);
        mPrefsUtil.putDFLong(DF_PREF_SESSION_ID_EXPIRES, expireTime);
        mPrefsUtil.putDFString(DF_PREF_SESSION_ID, sessionId);
    }

    /**
     * @return Email used to login to DF or empty string if none exists
     */
    public String getDFEmail() {
        return mPrefsUtil.getDFString(DF_PREF_EMAIL, "");
    }

    /**
     * Records the email used to login to DF
     * @param email Email used to login to DF
     */
    public void recordDFEmail(String email) {
        mPrefsUtil.putDFString(DF_PREF_EMAIL, email);
    }

    /**
     * @return Password used to login to DF or empty string if none exists
     */
    public String getDFPass() {
        String pass = mPrefsUtil.getDFString(DF_PREF_PWD, "");
        CryptHelper cryptHelper = new CryptHelper();
        try {
            pass = pass.equals("") ? pass : cryptHelper.decrypt(pass, getSecretKey().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pass;
    }

    /**
     * Encrypts and records password used to login to DF
     * @param pass Password used to login to DF
     */
    public void recordDFPass(String pass) {
        CryptHelper cryptHelper = new CryptHelper();
        String ePass = "";
        try {
            ePass = cryptHelper.encrypt(pass, getSecretKey().trim());
        } catch (Exception e) {
            e.printStackTrace();
        }
        mPrefsUtil.putDFString(DF_PREF_PWD, ePass);
    }

    /**
     * Called to request that the app show the pin screen so the user can either unlock the app or
     * create a pin.
     */
    public void requestUnlockOrCreatePin() {
        Intent pinIntent = new Intent(mCtx, LockActivity.class);
        // Set request code based on if we have an existing pin or not
        int requestCode = pinExists() ? MainActivity.REQUEST_UNLOCK : MainActivity.REQUEST_CREATE_PIN;
        // If we don't have an existing pin, we'll need to tell LockActivity to create one
        if (requestCode == MainActivity.REQUEST_CREATE_PIN)
            pinIntent.putExtra(MainActivity.IS_INITIAL_SETUP, true);
        ((Activity) mCtx).startActivityForResult(pinIntent, requestCode);
    }

    /**
     * @return True if user has already set up a pin
     */
    private boolean pinExists() {
        return mPrefsUtil.getPrefs().contains(PREF_PIN);
    }

    /**
     * Records user's pin in pref file
     * @param pin Encrypted pin
     */
    public void recordPin(String pin) {
        mPrefsUtil.getPrefs().edit().putString(PREF_PIN, pin).commit();
    }

    /**
     * Checks the pin the user provided against the stored pin to determine if it is valid
     * @param pin Encrypted pin that user provided
     * @return True if pins match, false otherwise
     */
    public boolean verifyPin(String pin) {
        String storedPin = mPrefsUtil.getPrefString(PREF_PIN, null);
        return storedPin != null && pin.equals(storedPin.trim());
    }

    /**
     * @return Secret key
     */
    public String getSecretKey() {
        return SECRET_KEY;
    }

    /**
     * Gets the PrefsUtil for this user.
     * @return The PrefsUtil for this user
     */
    public PrefsUtil getPrefsUtil() {
        return mPrefsUtil;
    }

    /**
     * @return True if the user is a dev
     */
    public boolean isDev() {
        return mDevList.contains(getAccountName());
    }
}
