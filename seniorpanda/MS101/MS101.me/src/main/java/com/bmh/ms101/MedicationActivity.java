package com.bmh.ms101;

import android.app.Activity;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bmh.ms101.events.DFLoginResponseEvent;
import com.bmh.ms101.events.SendMedsDFEvent;
import com.bmh.ms101.ex.DFCredentialsInvalidException;
import com.bmh.ms101.jobs.DreamFactoryLoginJob;
import com.bmh.ms101.jobs.DreamFactorySendJob;
import com.path.android.jobqueue.JobManager;

import org.droidparts.net.http.HTTPException;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * Allow users to report their medication dosage adherence. Used both from the main app and from the
 * notification that fires every day at 6 PM, with small UI changes depending on which is was called
 * from.
 */
public class MedicationActivity extends Activity {

    private final EventBus eventBus = EventBus.getDefault();

    private Button mConfirmBtn;
    private LinearLayout mMedsList;
    private static Backend mBackend;
    private static User mUser;
    private JobManager mJobManager;

    private ArrayList<String> jobsRunning = new ArrayList<>();
    private boolean mIsFromMain;
    private boolean mIsUnlocked = false;
    private String lastReportedMedData;
    private long lastReportedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBackend = new Backend(this);
        mUser = mBackend.getUser();
        mJobManager = MS101.getInstance().getJobManager();
        if (getIntent().getBooleanExtra(MainActivity.IS_UNLOCKED, false) || savedInstanceState != null &&
                savedInstanceState.getBoolean(MainActivity.IS_UNLOCKED, false)) {
            mIsUnlocked = true;
        } else {
            mUser.requestUnlockOrCreatePin();
        }
        mIsFromMain = getIntent().getBooleanExtra(MainActivity.IS_FROM_MAIN, true);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getActionBar().setDisplayHomeAsUpEnabled(mIsFromMain);
        setContentView(R.layout.activity_medication);
        setProgressBarIndeterminateVisibility(false);
        setupUI();
    }

    @Override
    protected void onStart() {
        super.onStart();
        eventBus.register(this, 3);
        if (mUser.getDFSessionId().equals("")) mJobManager.addJobInBackground(new DreamFactoryLoginJob());
    }

    @Override
    protected void onStop() {
        super.onStop();
        eventBus.unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();
        getMenuInflater().inflate(R.menu.medication, menu);
        return mIsFromMain; // Only show our Help button if we called this activity from the main app
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(MainActivity.IS_UNLOCKED, mIsUnlocked);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == MainActivity.REQUEST_UNLOCK || requestCode == MainActivity.REQUEST_CREATE_PIN) &&
                resultCode == RESULT_OK) {
            mIsUnlocked = true;
        } else {
            if (jobsRunning.isEmpty()) finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_show_meds_help:
                Util.buildInfoDialog(this, R.string.help_meds_title, R.string.help_meds_content,
                        R.string.okay);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets up the UI, with slight differences depending on if the activity was called from the main
     * activity or a notification.
     */
    private void setupUI() {
        RelativeLayout mContent = (RelativeLayout) findViewById(R.id.meds_content);
        TextView tvHeader = (TextView) mContent.findViewById(R.id.meds_header);
        if (mIsFromMain) {
            setTitle(R.string.title_activity_medication_confirm);
            tvHeader.setText(R.string.report_meds_header);
        } else {
            setTitle(R.string.title_activity_medication_alarm);
            tvHeader.setText(R.string.reminder_meds_header);
        }
        mConfirmBtn = (Button) findViewById(R.id.meds_confirm);
        Util.makeGreen(mConfirmBtn, this);
        mConfirmBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMedData();
            }
        });
        setupListOfMeds();
    }

    /**
     * Fills in a list of with an meds_item for each med that the user takes. Each one of
     * those items has the med's name and a radio group so the user can indicate how many they've
     * taken. If the user has reported things already today, fill in that data to the radio buttons.
     */
    private void setupListOfMeds() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mMedsList = (LinearLayout) findViewById(R.id.meds_list); // List of med_item
        Set<String> currMedIds = mUser.getMedsIds(); // ID #s of meds the user has
        // Only show the meds that this user takes
        for (String id : currMedIds) {
            LinearLayout medItem = (LinearLayout) inflater.inflate(R.layout.meds_item, null);
            medItem.setTag(id); // Set the item's tag to be the med's ID #
            TextView label = (TextView) medItem.findViewById(R.id.med_label);
            label.setText(User.MED_NAMES[Integer.parseInt(id)]); // Set the name
            fillInMedDoses(Integer.parseInt(id), medItem);
            // Add the item to the list
            mMedsList.addView(medItem,
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        }
    }

    /**
     * Check to see if there is stored dosage data for this med from today. If there is, checks the
     * appropriate radio button.
     * @param medID ID of med to check for dosage data
     * @param medItem LinearLayout of the med item
     */
    private void fillInMedDoses(int medID, LinearLayout medItem) {
        int medDoses = mUser.getDosesFromToday(medID);
        if (medDoses >= 0) {
            RadioGroup rgMedDoses = (RadioGroup) medItem.findViewById(R.id.med_doses);
            ((RadioButton) rgMedDoses.getChildAt(medDoses + 1)).setChecked(true);
        }
    }

    /**
     * Uses the radio buttons in the layout to encode a medication dosage data string. Also stores
     * latest the reported doses so that we can keep track of it over the course of the day.
     * @return Encoded medication dosage data string
     */
    private String prepareMedData() {
        int[] mMedIDs = new int[mMedsList.getChildCount()], mDoses = new int[mMedsList.getChildCount()];
        // Get two int arrays with med IDs and the doses of each med
        for (int i = 0; i < mMedsList.getChildCount(); i++) {
            mMedIDs[i] = Integer.valueOf((String) mMedsList.getChildAt(i).getTag());
            LinearLayout medItem = (LinearLayout) mMedsList.getChildAt(i);
            RadioGroup rg = (RadioGroup) medItem.findViewById(R.id.med_doses);
            RadioButton rb = (RadioButton) rg.findViewById(rg.getCheckedRadioButtonId());
            mDoses[i] = Integer.valueOf((String) rb.getTag());
        }
        // Store the med doses for today in the meds pref file
        mUser.recordDosesFromToday(mMedIDs, mDoses);
        // Return the encoded Med data
        return mBackend.encodeMedsRow(mMedIDs, mDoses);
    }

    /**
     * Starts Jobs that send data to backends.
     */
    private void sendMedData() {
        // Change UI
        setProgressBarIndeterminateVisibility(true);
        mConfirmBtn.setText(getString(R.string.sending));
        mConfirmBtn.setEnabled(false);
        mMedsList.setEnabled(false);
        // Get data
        lastReportedMedData = prepareMedData();
        lastReportedTime = Calendar.getInstance().getTimeInMillis();
        // Store the fact that we're running jobs
        jobsRunning.add("DF");
        // Start the jobs
        mJobManager.addJobInBackground(new DreamFactorySendJob(User.MED, lastReportedMedData, lastReportedTime));
    }

    /**
     * Called when data has been sent successfully to all servers.
     */
    private void finishedSending() {
        // Change UI
        setProgressBarIndeterminateVisibility(false);
        mConfirmBtn.setText(getString(R.string.send));
        mConfirmBtn.setEnabled(true);
        mMedsList.setEnabled(true);
        // Toast
        Util.toast(MedicationActivity.this, getString(R.string.toast_thanks));
        // Dismiss notification if needed then finish
        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.cancel(MS101Receiver.Notif.MEDS.ordinal());
        finish();
    }

    /**
     * Called when we get a response from DreamFactory after trying to log in.
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
                    jobsRunning.remove("DF");
                    mBackend.destroyDF();
                    if (jobsRunning.isEmpty()) finishedSending();
                    break;
                case "Success":
                case "Already Logged In":
                    if (jobsRunning.contains("DF")) {
                        mJobManager.addJobInBackground(new DreamFactorySendJob(User.MED, lastReportedMedData, lastReportedTime));
                    }
                    break;
            }
        } else {
            // Handle it if it's an exception of some kind
            Util.handleDFLoginError(this, mUser, (Exception) event.response);
        }
    }

    /**
     * Called when we finish trying to send medication data to Dreamfactory.
     * @param event SendMedsDFEvent
     */
    public void onEventMainThread(SendMedsDFEvent event) {
        if (event.wasSuccess) {
            jobsRunning.remove("DF");
            if (jobsRunning.isEmpty()) finishedSending();
        } else {
            if (event.response instanceof DFCredentialsInvalidException) {
                mJobManager.addJobInBackground(new DreamFactoryLoginJob());
            } else {
                Util.handleSendJobFailure(this, (Exception) event.response);
            }
        }
    }

    /* ************************* For handling when the user clicks the pill on the widget ************************* */
    //TODO move this stuff to TecfideraWidgetHandler class

    /**
     * What to do what the pill on an MD widget is clicked. Is a generic method that works for any
     * med that a widget is tracking.
     *
     * TODO make this use jobs!!
     *
     * @param context Context to use
     * @param widgetId Widget that was clicked, used to find out what med to work with.
     */
    public static void handleMDWidgetPill(final Context context, int widgetId) {
        mBackend = new Backend(context);
        mUser = mBackend.getUser();
        int medKey = Integer.parseInt(mUser.getMedKey(widgetId));
        String[] currMedIds = mUser.getMedsIds().toArray(new String[mUser.getMedsIds().size()]);
        ArrayList<Integer> medIds = new ArrayList<>();
        ArrayList<Integer> medDoses = new ArrayList<>();
        for (int i = 0; i < currMedIds.length; i++) {
            int id = Integer.valueOf(currMedIds[i]);
            // Add med id to that arraylist
            medIds.add(i, id);
            int dosesForToday = mUser.getDosesFromToday(id);
            if (dosesForToday == -2) dosesForToday++; // If this is new for today (-2), set to -1
            if (medKey == id) {
                // Increment if this is the med the widget is tracking, may take us from -1 to 0...
                dosesForToday++;
                // ...in which case, increment again to get to 1
                if (dosesForToday == 0) dosesForToday++;
            }
            // Add doses to that arraylist
            medDoses.add(i, dosesForToday);
        }
        // Convert to actual arrays of ints
        int[] medIdsArr = new int[medIds.size()];
        int[] medDosesArr = new int[medDoses.size()];
        for (int i = 0; i < medIds.size(); i++) {
            medIdsArr[i] = medIds.get(i);
            medDosesArr[i] = medDoses.get(i);
        }
        // Save doses
        mUser.recordDosesFromToday(medIdsArr, medDosesArr);
        String medsRow = mBackend.encodeMedsRow(medIdsArr, medDosesArr);
        new AsyncTask<String, Void, Object>() {
            @Override
            protected void onPreExecute() {
                Util.toast(context, R.string.sending);
            }
            @Override
            protected Object doInBackground(String... params) {
                try {
                    mBackend.sendToDF(User.MED, params[0]); // Send to DF backend
                } catch (HTTPException e) {
                    e.printStackTrace();
                    return e;
                } catch (JSONException e) {
                    e.printStackTrace();
                    return e;
                } catch (DFCredentialsInvalidException e) {
                    e.printStackTrace();
                    return e;
                }
                return "Success";
            }
            @Override
            protected void onPostExecute(Object result) {
                if (result instanceof String) {
                    context.startActivity(new Intent(context, PopUpActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    NotificationManager notifManager = (NotificationManager) context
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    notifManager.cancel(MS101Receiver.TEC_ALARM_1);
                    notifManager.cancel(MS101Receiver.TEC_ALARM_2);
                }
            }
        }.execute(medsRow);
    }

    /**
     * What to do if the user misses a dose of Tecfidera. Is specific to Tecfidera.
     * @param context Context to use
     * @param widgetId Widget that is tied to Tecfidera
     * @param isMorningDose True if the morning dose was missed, false if the afternoon dose was missed
     */
    public static void handleMDTecMiss(Context context, int widgetId, boolean isMorningDose) {
        Intent intent = new Intent(context, PopUpActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.putExtra(MS101Receiver.MD_TEC_IS_MORNING_DOSE, isMorningDose);
        intent.putExtra(PopUpActivity.POPUP_LAYOUT, PopUpActivity.TEC_DOSE_MISS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
