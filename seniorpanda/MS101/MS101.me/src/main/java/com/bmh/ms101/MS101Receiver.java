package com.bmh.ms101;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;

import com.bmh.ms101.jobs.DreamFactoryLoginJob;
import com.bmh.ms101.models.SubscribeDataModel;
import com.path.android.jobqueue.JobManager;

import java.util.Calendar;
import java.util.List;

/**
 * Receives and handles alarm events when they fire. Also receives broadcasts from the system both
 * when the device is booted and when the application package is replaced (app was updated) so that
 * the alarm can be set again.
 */
public class MS101Receiver extends BroadcastReceiver {

    // Custom actions for intents that this class is setup to receive when broadcast
    public static final String MD_PILL_CLICK = "com.bmh.ms101.intent.action.MD_PILL_CLICK";
    public static final String MD_TEC_MISS = "com.bmh.ms101.intent.action.MD_TEC_MISS";
    // Keys for extras for intents received by this class
    public static final String MD_TEC_IS_MORNING_DOSE = "isMorningDose";

    public enum Notif {
        SYMP, STRESS, MEDS
    }
    // Request codes so we can differentiate between the alarms
    private static final int CANCEL_NOTIFS_ALARM = -999;
    private static final int REFRESH_SESSION_ALARM = -998;
    private static final int MEDS_ALARM = 0;
    private static final int SYMP_STRESS_ALARM = 1;
    private static final int TEST_ALARM = 2;
    public static final int TEC_ALARM_1 = 3;
    private static final int CANCEL_TEC_ALARM_1 = -3;
    public static final int TEC_ALARM_2 = 4;
    private static final int CANCEL_TEC_ALARM_2 = -4;

    // Hours that alarms with fire
    private static final int DEFAULT_MIN = 0;
    //// Normal alarms
    private static final int CANCEL_NOTIFS_HOUR = 0; // Midnight
    private static final int REFRESH_SESSION_HOUR = 9; // 9 AM
    private static final int MEDS_NOTIF_HOUR = 18; // 6 PM
    private static final int SYMP_STRESS_NOTIF_HOUR = 12; // 12 Noon
    private static final int MEDS_MORNING_DOSE_NOTIF_HOUR = 7; // 8AM
    private static final int MEDS_AFTERNOON_DOSE_NOTIF_HOUR = 14; // 2PM
    private static final int MEDS_EVENING_DOSE_NOTIF_HOUR = 21; // 9PM
    //// Tecfidera Widget alarms
    private static final int TEC_1_START_HOUR = 0;
    private static final int TEC_1_START_MIN = 5; // 12:05 AM
    private static final int TEC_1_END_HOUR = 11;
    private static final int TEC_1_END_MIN = DEFAULT_MIN; // 11 AM
    private static final int TEC_2_START_HOUR = 14;
    private static final int TEC_2_START_MIN = DEFAULT_MIN; // 2 PM
    private static final int TEC_2_END_HOUR = 19;
    private static final int TEC_2_END_MIN = DEFAULT_MIN; // 7 PM

    // Pattern that the phone will vibrate in when the alarms go off (in milliseconds)
    private static final long[] VIBRATE_PATTERN = new long[] {0, 100, 100, 100, 100, 100, 100,
            100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100, 100};
    // The notification the symp/envir alarm triggers will have a different action based on the day
    private static final Notif[] SYMP_STRESS_SCHEDULE = {Notif.SYMP, Notif.SYMP, Notif.SYMP,
            Notif.SYMP, Notif.SYMP, Notif.SYMP, Notif.SYMP};
    public static final String EXTRA_NOTIF_TYPE = "notif_type";

    private Backend mBackend;
    private User mUser;
    private JobManager mJobManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        mBackend = new Backend(context);
        mUser = mBackend.getUser();
        mJobManager = MS101.getInstance().getJobManager();
        int widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        String action = intent.getAction();
        if (action == null) action = "null";
        switch (action) {
            case Intent.ACTION_BOOT_COMPLETED:
            case Intent.ACTION_MY_PACKAGE_REPLACED:
                // Device was rebooted or app updated, set alarms again
                Util.trackBgEvent("will_reset_alarm_after_boot", context);
                createAlarms(context);
                Util.trackBgEvent("done_reset_alarm_after_boot", context);
                break;
            case MD_PILL_CLICK:
                // What to do if the pill was clicked
                MedicationActivity.handleMDWidgetPill(context, widgetId);
                break;
            case MD_TEC_MISS:
                // What to do if the user missed a dose of Tecfidera
                MedicationActivity.handleMDTecMiss(context, widgetId,
                        intent.getBooleanExtra(MD_TEC_IS_MORNING_DOSE, false));
                break;
            default:
                doReminder(context, intent);
                break;
        }
    }

    /**
     * Called when the system fires one of our alarm events
     * @param context Context in which the receiver is running
     * @param intent Intent from the broadcast for our use
     */
    private void doReminder(Context context, Intent intent) {
        NotificationManager notifManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        switch (intent.getIntExtra("requestCode", -1)) {
            case CANCEL_NOTIFS_ALARM:
                // Simple alarm that just cancels all our pending notifications at midnight
                notifManager.cancelAll();
                // TODO add logic that counts how many times there are leftover alarms if we do the slacker notif later
                break;
            case TEST_ALARM: //TODO Currently using the test alarm for testing refresh session alarm
            case REFRESH_SESSION_ALARM:
                // Try to login to dreamfactory, just to be sure our credentials are valid.
                mJobManager.addJobInBackground(new DreamFactoryLoginJob(true));
                break;
            case TEC_ALARM_1:
                builder.setContentTitle(context.getResources().getString(R.string.tec_notif_title))
                        .setContentText(context.getResources().getString(R.string.tec_notif_text_1))
                        .setSmallIcon(R.drawable.ic_stat_ms101)
                        .setOngoing(true);
                notifManager.notify(TEC_ALARM_1, builder.build());
                break;
            case CANCEL_TEC_ALARM_1:
                notifManager.cancel(TEC_ALARM_1);
                // If user hasn't reported at least 1 dose of tecfidera, ask them why (use an intent, we don't want to do it here)
                if (mUser.getDosesFromToday(3) < 1) {
                    context.sendBroadcast(new Intent(MD_TEC_MISS)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, intent.getIntExtra(
                                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))
                            .putExtra(MD_TEC_IS_MORNING_DOSE, true));
                }
                break;
            case TEC_ALARM_2:
                builder.setContentTitle(context.getResources().getString(R.string.tec_notif_title))
                        .setContentText(context.getResources().getString(R.string.tec_notif_text_2))
                        .setSmallIcon(R.drawable.ic_stat_ms101)
                        .setOngoing(true);
                notifManager.notify(TEC_ALARM_2, builder.build());
                break;
            case CANCEL_TEC_ALARM_2:
                notifManager.cancel(TEC_ALARM_2);
                // If user hasn't reported both doses of Tecfidera, ask them why (use an intent, we don't want to do it here)
                if (mUser.getDosesFromToday(3) < 2) {
                    context.sendBroadcast(new Intent(MD_TEC_MISS)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, intent.getIntExtra(
                                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))
                            .putExtra(MD_TEC_IS_MORNING_DOSE, false));
                }
                break;
            default:
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                // Create the wakelock needed to turn screen on when
                @SuppressWarnings("deprecation")
                PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                        | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, "");
                // Acquire it then do work
                wl.acquire();
                Util.trackBgEvent("alarm_went_off", context);
                // Figure out what type of notification we want to send, then send it. Also start activity
                switch (intent.getIntExtra("requestCode", -1)) {
                    case MEDS_ALARM:
                        context.startActivity(sendNotification(context, Notif.MEDS));
                        break;
                    case SYMP_STRESS_ALARM:
                        Notif type = SYMP_STRESS_SCHEDULE[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1];
                        context.startActivity(sendNotification(context, type));
                        break;
                }
                // Vibrate with specific pattern
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(VIBRATE_PATTERN, -1);
                // Release wakelock when done
                wl.release();
                break;
        }
    }

    /**
     * Sends the notification specified. Also returns the intent that is used to make the
     * notification's pending intent so that the activity can be opened automatically. This is for
     * ease of use purposes, and is why the notification is ongoing until the activity that is
     * opened cancels it when the user is finished reporting data
     * @param context Context in which the receiver is running
     * @param type What notification type
     * @return Intent used to start an activity based on notification type
     */

    private Intent sendNotification(Context context, Notif type) {
        Class<?> targetActivity = null;
        String notifTitle = null;
        String notifText = null;
        switch (type) {
            case MEDS:
                targetActivity = MedicationActivity.class;
                notifTitle = context.getResources().getString(R.string.meds_notif_title);
                notifText = context.getResources().getString(R.string.notif_subtext);
                break;
            case STRESS:
                targetActivity = SymptomsActivity.class;
                notifTitle = context.getResources().getString(R.string.stress_notif_title);
                notifText = context.getResources().getString(R.string.notif_subtext);
                break;
            case SYMP:
                targetActivity = SymptomsActivity.class;
                notifTitle = context.getResources().getString(R.string.symp_notif_title);
                notifText = context.getResources().getString(R.string.notif_subtext);
                break;
        }

        // Intent used in notification's PendingIntent, and returned.
        Intent intent = new Intent(context, targetActivity);
        intent.putExtra(EXTRA_NOTIF_TYPE, type.ordinal());
        if (targetActivity == MedicationActivity.class) intent.putExtra("from_main", false);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        // Now build and send notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(notifTitle).setContentText(notifText)
                .setSmallIcon(R.drawable.ic_stat_ms101).setOngoing(true);

        PendingIntent notifyIntent = PendingIntent.getActivity(context, type.ordinal(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(notifyIntent);

        NotificationManager notifManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(type.ordinal(), builder.build());
        Util.trackBgEvent("alarm_notif_pushed_" + type, context);
        return intent;
    }

    /**
     * Sets specified alarms
     * @param context Context in which the receiver is running
     */
    public static void createAlarms(Context context) {
        // Cancel all our alarms first
        cancelAlarms(context);
        Class cls = MS101Receiver.class;
        // Create pending intents for the alarms
        PendingIntent cancelNotifsAlarm = createAlarmPendingIntent(context, cls, CANCEL_NOTIFS_ALARM);
        PendingIntent refreshSessionAlarm = createAlarmPendingIntent(context, cls, REFRESH_SESSION_ALARM);
        PendingIntent medsPIntent = createAlarmPendingIntent(context, cls, MEDS_ALARM);
        PendingIntent sympEnvirPIntent = createAlarmPendingIntent(context, cls, SYMP_STRESS_ALARM);
        // Set the alarms
        setAlarm(context, cancelNotifsAlarm, CANCEL_NOTIFS_HOUR);
        setAlarm(context, refreshSessionAlarm, REFRESH_SESSION_HOUR);
        setAlarm(context, medsPIntent, MEDS_NOTIF_HOUR);
        setAlarm(context, sympEnvirPIntent, SYMP_STRESS_NOTIF_HOUR);
    }

    /**
     * Sets specified alarms
     * @param context Context in which the receiver is running
     */
    public static void createPDAlarms(Context context) {
        // Cancel all our alarms first
        /*cancelAlarms(context);
        Class cls = MS101Receiver.class;
        // Create pending intents for the alarms
        PendingIntent cancelNotifsAlarm = createAlarmPendingIntent(context, cls, CANCEL_NOTIFS_ALARM);
        PendingIntent refreshSessionAlarm = createAlarmPendingIntent(context, cls, REFRESH_SESSION_ALARM);
        PendingIntent medsPIntent = createAlarmPendingIntent(context, cls, MEDS_ALARM);
        PendingIntent sympEnvirPIntent = createAlarmPendingIntent(context, cls, SYMP_STRESS_ALARM);
        // Set the alarms
        setAlarm(context, cancelNotifsAlarm, CANCEL_NOTIFS_HOUR);
        setAlarm(context, refreshSessionAlarm, REFRESH_SESSION_HOUR);
        setAlarm(context, medsPIntent, MEDS_NOTIF_HOUR);
        setAlarm(context, sympEnvirPIntent, SYMP_STRESS_NOTIF_HOUR);*/
        testRepeatNotif(context);
    }

    /**
     * Sets specified alarms
     * @param context Context in which the receiver is running
     */
    public static void createPDSubscriptionAlarms(Context context, boolean oncePerDay,
                                                  boolean twicePerDay, boolean thricePerDay) {
        Class cls = MS101Receiver.class;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar c = Calendar.getInstance();
        if (oncePerDay) {
            PendingIntent medsMorningPIntent = createAlarmPendingIntent(context, cls, MEDS_ALARM);
            c.add(Calendar.HOUR_OF_DAY, MEDS_MORNING_DOSE_NOTIF_HOUR);
            am.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), (24* 60 * 60 * 60 *1000), medsMorningPIntent);
            System.out.println("Set once a day alarm");
        } else if (twicePerDay) {
            PendingIntent medsMorningPIntent = createAlarmPendingIntent(context, cls, MEDS_ALARM);
            c.add(Calendar.HOUR_OF_DAY, MEDS_MORNING_DOSE_NOTIF_HOUR);
            am.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), (24* 60 * 60 * 60 *1000), medsMorningPIntent);

            PendingIntent medsEveningPIntent = createAlarmPendingIntent(context, cls, MEDS_ALARM);
            c.add(Calendar.HOUR_OF_DAY, MEDS_EVENING_DOSE_NOTIF_HOUR);
            am.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), (24* 60 * 60 * 60 *1000), medsEveningPIntent);
            System.out.println("Set twice a day alarms");
        } else if (thricePerDay) {
            PendingIntent medsMorningPIntent = createAlarmPendingIntent(context, cls, MEDS_ALARM);
            c.add(Calendar.HOUR_OF_DAY, MEDS_MORNING_DOSE_NOTIF_HOUR);
            am.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), (24* 60 * 60 * 60 *1000), medsMorningPIntent);

            PendingIntent medsAfternoonPIntent = createAlarmPendingIntent(context, cls, MEDS_ALARM);
            c.add(Calendar.HOUR_OF_DAY, MEDS_AFTERNOON_DOSE_NOTIF_HOUR);
            am.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), (24* 60 * 60 * 60 *1000), medsMorningPIntent);

            PendingIntent medsEveningPIntent = createAlarmPendingIntent(context, cls, MEDS_ALARM);
            c.add(Calendar.HOUR_OF_DAY, MEDS_EVENING_DOSE_NOTIF_HOUR);
            am.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), (24* 60 * 60 * 60 *1000), medsEveningPIntent);
            System.out.println("Set thrice a day alarms");
        }
    }

    /**
     * Convenience method for creating pending intents to use for setting alarms.
     * @param context Context to use
     * @param cls Class to use
     * @param requestCode Request code to store
     * @return Pending Intent to be used for setting an alarm
     */
    private static PendingIntent createAlarmPendingIntent(Context context, Class<?> cls, int requestCode) {
        Intent intent = new Intent(context, cls);
        intent.putExtra("requestCode", requestCode);
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Set alarm using the default minute value, zero, so alarms ring on the hour.
     * @param context Context to use
     * @param pendingIntent The pending intent to supply to the Alarm Manager for this alarm
     * @param hourOfAlarm Hour the alarm should go off (0-23)
     */
    private static void setAlarm(Context context, PendingIntent pendingIntent, int hourOfAlarm) {
        setAlarm(context, pendingIntent, hourOfAlarm, DEFAULT_MIN);
    }

    /**
     * Convenience method for setting an alarm that goes off every 24 hours at the top of the
     * specified hour.
     * @param context Context to use
     * @param pendingIntent The pending intent to supply to the Alarm Manager for this alarm
     * @param hourOfAlarm Hour the alarm should go off (0-23)
     * @param minOfAlarm Minute within the hour that alarm should go off (0-59)
     */
    private static void setAlarm(Context context, PendingIntent pendingIntent, int hourOfAlarm, int minOfAlarm) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Interval of time, in milliseconds, that should elapse before the alarm goes off again
        long interval = 1000 * 3600 * 24;
        // Set alarm reminder time using calendar
        Calendar c = Calendar.getInstance();
        if (c.get(Calendar.HOUR_OF_DAY) > hourOfAlarm ||
                (c.get(Calendar.HOUR_OF_DAY) == hourOfAlarm && c.get(Calendar.MINUTE) >= minOfAlarm)) {
            // We need the alarm to be set for tomorrow if the time has passed already
            c.add(Calendar.DAY_OF_WEEK, 1);
        }
        c.set(Calendar.HOUR_OF_DAY, hourOfAlarm);
        c.set(Calendar.MINUTE, minOfAlarm);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        // Set the alarm
        am.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), interval, pendingIntent);
    }


    /**
     * Sets a test alarm for a notif.
     * @param context Context to use
     */
    public static void testNotif(Context context) {
        Class cls = MS101Receiver.class;
        PendingIntent testPIntent = createAlarmPendingIntent(context, cls, TEST_ALARM);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar c = Calendar.getInstance();
        // This alarm will only run once, 20 seconds after the current time
        c.add(Calendar.SECOND, 30);
        am.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), testPIntent);
    }

    public static void testRepeatNotif(Context context) {
        Class cls = MS101Receiver.class;
        //   PendingIntent testPIntent = createAlarmPendingIntent(context, cls, TEST_ALARM);
        PendingIntent testPIntent = createAlarmPendingIntent(context, cls, MEDS_ALARM);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar c = Calendar.getInstance();
        // This alarm will only run once, 20 seconds after the current time
        c.add(Calendar.SECOND, 30);
        //    am.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), (60 *1000)/2, testPIntent);
        am.setRepeating(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), (60 * 60 *1000), testPIntent);
    }

    /**
     * Checks to see if an alarm with our PendingIntent already exists. (PendingIntents with the
     * flag FLAG_NO_CREATE will return null if the intent DOES NOT already exist, contrary to what
     * the documentation says in some places)
     * Note that if the cancel notifs alarm is set, so are the other ones. They get set at the same time.
     * @param context Context in which the receiver is running
     * @return True if alarms are set (PendingIntent exists), false otherwise
     */
    public static boolean areAlarmsSet(Context context) {
        return PendingIntent.getBroadcast(context, CANCEL_NOTIFS_ALARM, new Intent(context,
                MS101Receiver.class), PendingIntent.FLAG_NO_CREATE) != null;
    }

    /**
     * Cancels our alarms
     * @param context Context in which the receiver is running
     */
    public static void cancelAlarms(Context context) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Class cls = MS101Receiver.class;
        // Cancel the alarms using the pending intents
        am.cancel(createAlarmPendingIntent(context, cls, CANCEL_NOTIFS_ALARM));
        am.cancel(createAlarmPendingIntent(context, cls, REFRESH_SESSION_ALARM));
        am.cancel(createAlarmPendingIntent(context, cls, MEDS_ALARM));
        am.cancel(createAlarmPendingIntent(context, cls, SYMP_STRESS_ALARM));
    }

    /**
     * Checks to see if special alarms are needed for a particular med ID
     * @param context Context to use
     * @param medKey ID of Med, in string form
     * @return True if we should set special alarms for this medication at this time
     */
    public static boolean needsSpecialAlarms(Context context, String medKey) {
        SharedPreferences widgetPrefs = context.getSharedPreferences(PrefsUtil.WIDGET_PREF_FILE_NAME, Context.MODE_PRIVATE);
        boolean needs = false;
        // Ensure that there is a widget ID stored using this med's ID as the key
        int widgetId = widgetPrefs.getInt(medKey, -1);
        // Now check to make sure that this widget is actually set up right now by getting the value
        // stored using the widget's ID as the key. If the widget is set up, the value will just be
        // the Med ID as a string, but if it's present but not set up, the value will be "nullX",
        // where X is the med ID that it had been tracking. (there is a mReason for storing it like this)
        String pairedMedKey = widgetId != -1 ? widgetPrefs.getString(String.valueOf(widgetId), "null") : "null";
        // Only set the alarms if the widget is present AND set up
        if (!pairedMedKey.contains("null")) needs = true;
        return needs;
    }

    /**
     * Convenience method for creating pending intents to use for setting special alarms
     * @param context Context to use
     * @param cls Class to use
     * @param requestCode Request code to store
     * @param widgetId WidgetId
     * @return Pending Intent to be used for setting an alarm
     */
    private static PendingIntent createSpecialAlarmPendingIntent(Context context, Class<?> cls, int requestCode, int widgetId) {
        Intent intent = new Intent(context, cls);
        intent.putExtra("requestCode", requestCode);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Creates the alarms that work with the MD widget for Tecfidera
     * @param context Context to use
     */
    public static void createSpecialAlarms(Context context, String medKey) {
        cancelSpecialAlarms(context, medKey);
        switch (medKey) {
            // Create Tecfidera alarms
            case PrefsUtil.MK_TECFIDERA:
                if (needsSpecialAlarms(context, medKey)) {
                    Class cls = MS101Receiver.class;
                    int widgetId = new User(context).getMedDoseWidgetID(medKey);
                    // Create our pending intents
                    PendingIntent tec1PI = createSpecialAlarmPendingIntent(context, cls, TEC_ALARM_1, widgetId);
                    PendingIntent cancelTec1PI = createSpecialAlarmPendingIntent(context, cls, CANCEL_TEC_ALARM_1, widgetId);
                    PendingIntent tec2PI = createSpecialAlarmPendingIntent(context, cls, TEC_ALARM_2, widgetId);
                    PendingIntent cancelTec2PI = createSpecialAlarmPendingIntent(context, cls, CANCEL_TEC_ALARM_2, widgetId);
                    // Set Alarms
                    setAlarm(context, tec1PI, TEC_1_START_HOUR, TEC_1_START_MIN);
                    setAlarm(context, cancelTec1PI, TEC_1_END_HOUR, TEC_1_END_MIN);
                    setAlarm(context, tec2PI, TEC_2_START_HOUR, TEC_2_START_MIN);
                    setAlarm(context, cancelTec2PI, TEC_2_END_HOUR, TEC_2_END_MIN);
                    // Check to see if we need to start one of the notifications if we're within a time range
                    Calendar now = Calendar.getInstance();
                    // Check the first time range
                    Calendar tecStart = Calendar.getInstance();
                    tecStart.set(Calendar.HOUR_OF_DAY, TEC_1_START_HOUR);
                    tecStart.set(Calendar.MINUTE, TEC_1_START_MIN);
                    Calendar tecEnd = Calendar.getInstance();
                    tecEnd.set(Calendar.HOUR_OF_DAY, TEC_1_END_HOUR);
                    tecEnd.set(Calendar.MINUTE, TEC_1_END_MIN);
                    if (now.after(tecStart) && now.before(tecEnd)) {
                        // We are in the first time range, create that notif
                        NotificationManager notifManager = (NotificationManager) context
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                        notifManager.notify(TEC_ALARM_1, new NotificationCompat.Builder(context)
                                .setContentTitle(context.getResources().getString(R.string.tec_notif_title))
                                .setContentText(context.getResources().getString(R.string.tec_notif_text_1))
                                .setSmallIcon(R.drawable.ic_stat_ms101)
                                .setOngoing(true)
                                .build());
                    } else {
                        // We aren't in the first time range, check the second
                        tecStart.set(Calendar.HOUR_OF_DAY, TEC_2_START_HOUR);
                        tecStart.set(Calendar.MINUTE, TEC_2_START_MIN);
                        tecEnd.set(Calendar.HOUR_OF_DAY, TEC_2_END_HOUR);
                        tecEnd.set(Calendar.MINUTE, TEC_2_END_MIN);
                        if (now.after(tecStart) && now.before(tecEnd)) {
                            // We are in the second range, create that notif
                            NotificationManager notifManager = (NotificationManager) context
                                    .getSystemService(Context.NOTIFICATION_SERVICE);
                            notifManager.notify(TEC_ALARM_2, new NotificationCompat.Builder(context)
                                    .setContentTitle(context.getResources().getString(R.string.tec_notif_title))
                                    .setContentText(context.getResources().getString(R.string.tec_notif_text_2))
                                    .setSmallIcon(R.drawable.ic_stat_ms101)
                                    .setOngoing(true)
                                    .build());
                        }
                    }
                }
                break;
        }
    }

    /**
     * Cancels the alarms that work with the MD widget for Tecfidera
     * @param context Context to use
     */
    public static void cancelSpecialAlarms(Context context, String medKey) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Class cls = MS101Receiver.class;
        switch (medKey) {
            // Cancel Tecfidera alarms
            case PrefsUtil.MK_TECFIDERA:
                // Cancel the alarms using the pending intents
                am.cancel(createAlarmPendingIntent(context, cls, TEC_ALARM_1));
                am.cancel(createAlarmPendingIntent(context, cls, CANCEL_TEC_ALARM_1));
                am.cancel(createAlarmPendingIntent(context, cls, TEC_ALARM_2));
                am.cancel(createAlarmPendingIntent(context, cls, CANCEL_TEC_ALARM_2));
                // Now deal with any notifications we may have
                nm.cancel(TEC_ALARM_1);
                nm.cancel(TEC_ALARM_2);
                break;
        }

    }
}
