package com.bmh.ms101.push;

/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.bmh.ms101.Backend;
import com.bmh.ms101.MS101;
import com.bmh.ms101.R;
import com.bmh.ms101.User;
import com.bmh.ms101.Util;
import com.bmh.ms101.events.GetDeviceDFEvent;
import com.bmh.ms101.events.GetMedsDFEvent;
import com.bmh.ms101.events.SendDeviceRegDFEvent;
import com.bmh.ms101.events.SendSubscribeDFEvent;
import com.bmh.ms101.ex.DFCredentialsInvalidException;
import com.bmh.ms101.jobs.DreamFactoryGetJob;
import com.bmh.ms101.jobs.DreamFactoryLoginJob;
import com.bmh.ms101.jobs.DreamFactorySendJob;
import com.bmh.ms101.models.DeviceDataModel;
import com.bmh.ms101.models.MedicationDataModel;
import com.bmh.ms101.models.SubscribeDataModel;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.path.android.jobqueue.JobManager;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.droidparts.net.http.HTTPException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Main UI for the push.
 */
public class PushActivity extends Activity {

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;


    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    //  String SENDER_ID = "Your-Sender-ID";
    String SENDER_ID = "344666265827";


    /**
     * Tag used on log messages.
     */
    static final String TAG = "GCM Demo";

    TextView mDisplay;
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    Context context;

    String regid;

    private User mUser;
    private JobManager mJobManager;
    private long lastReportedTime;
    private ArrayList<String> jobsRunning = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getApplicationContext();
        mJobManager = MS101.getInstance().getJobManager();
        mUser = new User(this);
        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);


            if (regid.isEmpty()) {
                registerInBackground();
            } else {
                Log.i(TAG, "Found registration id : " + regid);
                loadDeviceData();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check device for Play Services APK.
        checkPlayServices();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = mUser.getPrefsUtil().getPrefs();
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        Log.i(TAG, "Reg id " + regId);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
        // send registrationId to backend server
        this.sendRegistrationIdToBackend(regid);
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    public String getRegistrationId(Context context) {
        //   final SharedPreferences prefs = getGcmPreferences(context);
        final SharedPreferences prefs = mUser.getPrefsUtil().getPrefs();
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                Log.i(TAG, "In doInBackground.");
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    Log.i(TAG, "In doInBackground calling gcm.register . + " + SENDER_ID);
                    regid = gcm.register(SENDER_ID);
                    Log.i(TAG, "In doInBackground calling gcm.register . + " + regid);
                    msg = "Device registered, registration ID=" + regid;


                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                Log.i(TAG, "In onPostExecute msg :  " + msg);
              //  mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(PushActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend(String regId) {
        // Your implementation here.
        JSONObject regIdJson = this.prepareRegIdData(regId);
        try {
            Log.i(TAG, "In sendRegistrationIdToBackend sending :  ");
            lastReportedTime = Calendar.getInstance().getTimeInMillis();
            jobsRunning.add("REGID_SEND_DF");
            // Start the jobs
            mJobManager.addJobInBackground(new DreamFactorySendJob(User.DEVICE_DATA_TYPE, regIdJson.toString(), lastReportedTime));
        } catch (Exception e) {

        }
        Log.i(TAG, "In sendRegistrationIdToBackend :  ");
        return;
    }

    private JSONObject prepareRegIdData(String regId) {
        Log.i(TAG, "In prepareRegIdData regid :  " + regid);
        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        DeviceDataModel deviceData = new DeviceDataModel();
        deviceData.setUserId(mUser.getUserId());
        deviceData.setRegId(regId);
        JSONObject deviceJson = DeviceDataModel.toJson(deviceData);
        jsonArray.put(deviceJson);
        try {
            json.put("record", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Called when we finish trying to send registration data to Dreamfactory.
     *
     * @param event SendDeviceRegDFEvent
     */
    public void onEventMainThread(SendDeviceRegDFEvent event) {
        if (event.wasSuccess) {
            List<SubscribeDataModel> subscriptions =  (ArrayList<SubscribeDataModel>)event.response;
            mUser.setSubscriptions(subscriptions);
            mUser.setSubscriptionAlarms();
            jobsRunning.remove("REGID_SEND_DF");
            if (jobsRunning.isEmpty()) finishedSending();
            startActivity(new Intent(this, PushActivity.class));
        } else {
            if (event.response instanceof DFCredentialsInvalidException) {
                mJobManager.addJobInBackground(new DreamFactoryLoginJob());
            } else {
                Util.handleSendJobFailure(this, (Exception) event.response);
            }
        }
    }

    private void finishedSending() {
        setResult(RESULT_OK);
        finish();
    }

    /**
     * Called to load subscribe data.
     */
    private void loadDeviceData() {
        System.out.println("Debug## :: loadDeviceData() ");
        mJobManager.addJobInBackground(new DreamFactoryGetJob(User.DEVICE_DATA_TYPE));
    }

    /**
     * Called when we get a response from DreamFactory after requesting some of the User's data.
     * @param event GetDataDFEvent
     */
    public void onEventMainThread(GetDeviceDFEvent event) {
        Log.i(TAG, "In onEventMainThread ");
        if (event.wasSuccess) {
            Log.i(TAG, "In onEventMainThread GetDeviceDFEvent success event");
        } else {
            if (event.response instanceof DFCredentialsInvalidException) {
            } else {
                Util.handleGetJobFailure(this, (Exception) event.response);
                Log.i(TAG, "In onEventMainThread GetDeviceDFEvent failure event");
            }
        }
    }
}
