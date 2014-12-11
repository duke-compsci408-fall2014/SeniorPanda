package com.bmh.ms101;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

import com.bmh.ms101.PhotoFlipping.SlideShowActivity;
import com.bmh.ms101.events.DFLoginResponseEvent;
import com.bmh.ms101.ex.DFNotAddedException;
import com.bmh.ms101.ex.UserMedsNotAddedException;
import com.bmh.ms101.jobs.DreamFactoryLoginJob;
import com.bmh.ms101.jobs.DreamFactorySendJob;
import com.path.android.jobqueue.JobManager;

import java.util.Calendar;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * App's Main activity, shown from the launcher.
 */
public class MainActivity extends Activity {

    private final EventBus eventBus = EventBus.getDefault();

    public static final String IS_INITIAL_SETUP = "is_initial_setup";
    public static final String IS_UNLOCKED = "is_unlocked";
    public static final String IS_FROM_MAIN = "is_from_main";

    // Use in OnActivityResult
    private static final int REQUEST_NONE = -1;
    public static final int REQUEST_LOGIN = 0;
    public static final int REQUEST_MEDS = 1;
    public static final int REQUEST_CREATE_PIN = 2;
    public static final int REQUEST_UNLOCK = 3;

    private Backend mBackend;
    private User mUser;
    private JobManager mJobManager;
    private boolean mIsUnlocked = false;
    private boolean mIsForcingRelogin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBackend = new Backend(this);
        mUser = mBackend.getUser(); // Get User object
        mJobManager = MS101.getInstance().getJobManager();
        if (savedInstanceState != null)
            mIsUnlocked = savedInstanceState.getBoolean(IS_UNLOCKED, false);
        eventBus.register(this, 2);
        tryInitMainScreen(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!eventBus.isRegistered(this)) eventBus.register(this, 2);
    }

    @Override
    protected void onStop() {
        super.onStop();
        eventBus.unregister(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_UNLOCKED, mIsUnlocked);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Hide dev action if user isn't a dev
        if (!mUser.isDev()) menu.setGroupVisible(R.id.dev_actions, false);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_manage_meds:
           //   startActivity(new Intent(this, SetupMedsActivity.class));
                startActivity(new Intent(this, SetupMedicationActivity.class));

                return true;
            case R.id.action_force_df_relogin:
                mIsForcingRelogin = true;
                mJobManager.addJobInBackground(new DreamFactoryLoginJob(true));
                return true;
            case R.id.action_logout:
                mBackend.logoutFromAll();
                mIsUnlocked = false;
                startNewTask(LoginActivity.class, REQUEST_LOGIN, true);
                return true;
            case R.id.action_test_screen:
                startActivity(new Intent(this, TestActivity.class));
                return true;
            case R.id.action_test_notif:
                MS101Receiver.testNotif(this);
                return true;
            case R.id.action_test_jobs:
                JobManager jobManager = MS101.getInstance().getJobManager();
                Set<String> currMedIds = mUser.getMedsIds(); // ID #s of meds the user has
                int[] medIds = new int[currMedIds.size()], medDoses = new int[currMedIds.size()];
                // Only show the meds that this user takes
                int i = 0;
                for (String id : currMedIds) {
                    medIds[i] = Integer.parseInt(id);
                    int doses = mUser.getDosesFromToday(Integer.parseInt(id));
                    medDoses[i] = doses == -2 ? -1 : doses;
                    i++;
                }
                String data = mBackend.encodeMedsRow(medIds, medDoses);
                long timesent = Calendar.getInstance().getTimeInMillis();
                jobManager.addJobInBackground(new DreamFactorySendJob(User.MED, data, timesent));
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_LOGIN && resultCode == RESULT_OK) {
            // If we got here after logging in, then we'll be ignoring the login check this time
            tryInitMainScreen(false);
        } else if ((requestCode == MainActivity.REQUEST_UNLOCK || requestCode == MainActivity.REQUEST_CREATE_PIN) &&
                resultCode == RESULT_OK) {
            // Only if the result was from the PIN screen
            mIsUnlocked = true;
            tryInitMainScreen(false);
        } else {
            tryInitMainScreen(false);
            Util.toast(this, "Authentication finished. Now select medication");
//            finish();
        }
    }

    /**
     * Try to set up the main screen. Will show the login screen or the set up meds screen if
     * needed
     */
    private void tryInitMainScreen(boolean ignoreDF) {
        try {
            // Ensure the user object was/is fully set up. Can throw exceptions
            if (mBackend.needsGoogleDisconnect()) mBackend.disconnectGoogle();
            mUser.ensureSetupComplete(ignoreDF);
            if (!mIsUnlocked) mUser.requestUnlockOrCreatePin();
            setContentView(R.layout.activity_main);
            setupButtons();
        } catch (DFNotAddedException e) {
            // Start login flow
            startNewTask(LoginActivity.class, REQUEST_LOGIN, true);
        } catch (UserMedsNotAddedException e) {
            // Start meds setup flow
            startNewTask(SetupMedicationActivity.class, REQUEST_MEDS, true);
        }
    }

    /**
     * Make the buttons pretty colors and sets up their onClick() methods
     */
    private void setupButtons() {
        ImageButton buttonMeds = (ImageButton) findViewById(R.id.buttonMeds);
        ImageButton buttonSymptoms = (ImageButton) findViewById(R.id.buttonSymptoms);
        ImageButton buttonProgress = (ImageButton) findViewById(R.id.buttonLog);
        ImageButton buttonPhotoFlipping = (ImageButton) findViewById(R.id.buttonPhotoFlipping);
        Util.makeGreen(buttonMeds, this);
        Util.makeRed(buttonSymptoms, this);
        Util.makeBlue(buttonProgress, this);
        Util.makeYellow(buttonPhotoFlipping, this);
        buttonMeds.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Util.trackUiEvent("click_main_button_meds", MainActivity.this);
                Intent medsIntent = new Intent(MainActivity.this, MedicationActivity.class);
                medsIntent.putExtra(IS_FROM_MAIN, true);
                medsIntent.putExtra(IS_UNLOCKED, true);
                startActivity(medsIntent);
            }
        });
        buttonSymptoms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Util.trackUiEvent("click_main_button_symptoms", MainActivity.this);
                Intent sympIntent = new Intent(MainActivity.this, SymptomsActivity.class);
                sympIntent.putExtra(IS_UNLOCKED, true);
                startActivity(sympIntent);
            }
        });
        buttonProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Util.trackUiEvent("click_main_button_log", MainActivity.this);
                startActivity(new Intent(MainActivity.this, LogActivity.class));
            }
        });
        buttonPhotoFlipping.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Util.trackUiEvent("click_main_button_slide_show", MainActivity.this);
                startActivity(new Intent(MainActivity.this, SlideShowActivity.class));
            }
        });
    }

    /**
     * Called when we get a response from DreamFactory after trying to log in.
     *
     * @param event DFLoginResponseEvent
     */
    public void onEventMainThread(DFLoginResponseEvent event) {
        if (event.response instanceof String) {
            // If the response is a string, handle it directly
            switch ((String) event.response) {
                case "Retry":
                    mJobManager.addJobInBackground(new DreamFactoryLoginJob());
                    break;
                case "JSON Exception":
                    Util.toast(this, R.string.toast_json_error);
                case "Server Problems":
                case "Cancelled":
                    mBackend.destroyDF();
                case "Success":
                    if (mIsForcingRelogin) {
                        mIsForcingRelogin = false;
                        Util.toast(this, R.string.toast_df_force_refresh_success);
                    }
                case "Already Logged In":
                    tryInitMainScreen(true);
                    break;
            }
        } else {
            // Handle it if it's an exception of some kind
            Util.handleDFLoginError(this, mUser, (Exception) event.response);
        }
    }

    /**
     * Convenience method for starting activities
     *
     * @param act         Activity to be started
     * @param requestCode Request code to start it with
     */
    private void startNewTask(Class<? extends Activity> act, int requestCode, boolean isSetup) {
        Intent intent = new Intent(this, act);
        if (isSetup) intent.putExtra(IS_INITIAL_SETUP, true);
        startActivityForResult(intent, requestCode);
    }
}
