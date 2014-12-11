package com.bmh.ms101.push;

/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.bmh.ms101.MedicationActivity;
import com.bmh.ms101.R;
import com.bmh.ms101.SymptomsActivity;
import com.bmh.ms101.Util;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.bmh.ms101.MS101Receiver.Notif;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    public static final String EXTRA_NOTIF_TYPE = "notif_type";
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }
    public static final String TAG = "GCM Demo";

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.i(TAG, "In  onHandleIntent");
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);
        Log.i(TAG, "In  onHandleIntent + messageType : " + messageType);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            Log.i(TAG, "In  onHandleIntent + extras is not empty : ");
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                // This loop represents the service doing some work.
                for (int i = 0; i < 5; i++) {
                    Log.i(TAG, "Working... " + (i + 1)
                            + "/5 @ " + SystemClock.elapsedRealtime());
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                    }
                }
                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
             //   sendNotification("Received: " + extras.toString());
                Log.i(TAG, "Received: " + extras.toString());
                Log.i(TAG, "got default reminder MEDS");
                this.startActivity(sendNotification(this, Notif.MEDS_AFTERNOON));
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, PushActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_stat_ms101)
                        .setContentTitle("GCM Notification")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }

    private Intent sendNotification(Context context, Notif type) {
        Class<?> targetActivity = null;
        String notifTitle = null;
        String notifText = null;
        boolean medsMorning = false;
        boolean medsAfternoon = false;
        boolean medsEvening = false;
        // Intent used in notification's PendingIntent, and returned.
        switch (type) {
            case MEDS:
                targetActivity = MedicationActivity.class;
                notifTitle = context.getResources().getString(R.string.meds_notif_title);
                notifText = context.getResources().getString(R.string.notif_subtext);
                break;
            case MEDS_MORNING:
                targetActivity = MedicationActivity.class;
                notifTitle = context.getResources().getString(R.string.meds_notif_morning_title);
                notifText = context.getResources().getString(R.string.notif_subtext);
                medsMorning = true;
                medsAfternoon = false;
                medsEvening = false;
                break;
            case MEDS_AFTERNOON:
                targetActivity = MedicationActivity.class;
                notifTitle = context.getResources().getString(R.string.meds_notif_afternoon_title);
                notifText = context.getResources().getString(R.string.notif_subtext);
                medsMorning = false;
                medsAfternoon = true;
                medsEvening = false;
                break;
            case MEDS_EVENING:
                targetActivity = MedicationActivity.class;
                notifTitle = context.getResources().getString(R.string.meds_notif_evening_title);
                notifText = context.getResources().getString(R.string.notif_subtext);
                medsMorning = false;
                medsAfternoon = false;
                medsEvening = true;
                break;
        }

        // Intent used in notification's PendingIntent, and returned.
        Intent intent = new Intent(context, targetActivity);
        intent.putExtra(EXTRA_NOTIF_TYPE, type.ordinal());
        if (targetActivity == MedicationActivity.class) {
            intent.putExtra("from_main", false);
            intent.putExtra("meds_morning", medsMorning);
            intent.putExtra("meds_afternoon", medsAfternoon);
            intent.putExtra("meds_evening", medsEvening);
        }
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
}
