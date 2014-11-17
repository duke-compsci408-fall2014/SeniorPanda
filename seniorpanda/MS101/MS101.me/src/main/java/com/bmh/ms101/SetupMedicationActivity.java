package com.bmh.ms101;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bmh.ms101.events.DFLoginResponseEvent;
import com.bmh.ms101.events.GetMedsDFEvent;
import com.bmh.ms101.events.SendMedsDFEvent;
import com.bmh.ms101.events.SendSubscribeDFEvent;
import com.bmh.ms101.ex.DFCredentialsInvalidException;
import com.bmh.ms101.jobs.DreamFactoryGetJob;
import com.bmh.ms101.jobs.DreamFactoryLoginJob;
import com.bmh.ms101.jobs.DreamFactorySendJob;
import com.bmh.ms101.models.BaseRecordModel;
import com.bmh.ms101.models.MedRecordModel;
import com.bmh.ms101.models.MedicationDataModel;
import com.bmh.ms101.models.StressFactorRecordModel;
import com.bmh.ms101.models.SubscribeDataModel;
import com.bmh.ms101.models.SymptomRecordModel;
import com.bmh.ms101.models.TakenDataModel;
import com.path.android.jobqueue.JobManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Shows all of the available meds that this app can track so we can set up the app to track the
 * meds the user takes
 */
public class SetupMedicationActivity extends Activity {
    private final EventBus eventBus = EventBus.getDefault();

    private Backend mBackend;
    private User mUser;
    private JobManager mJobManager;

    private ListView mMedsListView;
    private String[] mMeds;
    private String[] mMedsNames;
    private String[] mMedsIds;
    private int[] mMedicationIds;
    private boolean mIsInitialSetup;

    private Set<String> mCurrentMedIds;

    private List<MedicationDataModel> mMedsDataList;
    private List<Integer> mCurrentSubscribedMedIds;
    private long lastReportedTime;
    private ArrayList<String> jobsRunning = new ArrayList<>();
    Map<Integer, SubscribeDataModel> oldSubscriptionsMap = new HashMap<Integer, SubscribeDataModel>();
    List<SubscribeDataModel> updatedSubscriptions = new ArrayList<SubscribeDataModel>();

    Button next;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meds);
        mJobManager = MS101.getInstance().getJobManager();
        // Get the user and their current meds
        mUser = new User(this);
        mCurrentSubscribedMedIds = new ArrayList<Integer>();
        mCurrentMedIds = mUser.getMedsIds();
        // Show the Up button in the action bar if not in tutorial.
        mIsInitialSetup = getIntent().getBooleanExtra(MainActivity.IS_INITIAL_SETUP, false);
        setupActionBar();
        //  setupList();
        loadMedData();
        setupButton();
    }

    @Override
    protected void onStart() {
        super.onStart();
        eventBus.register(this, 3);
    }

    @Override
    protected void onStop() {
        super.onStop();
        eventBus.unregister(this);
    }

    /**
     * Called when we get a response from DreamFactory after requesting some of the User's data.
     * @param event GetDataDFEvent
     */
    public void onEventMainThread(GetMedsDFEvent event) {
        System.out.println("Debug## :: In onEventMainThread ");
        if (event.wasSuccess) {
            ArrayList<MedicationDataModel> medicationDataModel = (ArrayList<MedicationDataModel>) event.response;
            System.out.println("Debug## :: In onEventMainThread list.size() " + medicationDataModel.size());
            //   populateMedsDF(medicationDataModel);

            setupList(medicationDataModel);
            //afterLoadLogData(true);
        } else {
            if (event.response instanceof DFCredentialsInvalidException) {
                mJobManager.addJobInBackground(new DreamFactoryGetJob(User.MEDICATION_DATA_TYPE));
            } else {
                Util.handleGetJobFailure(this, (Exception) event.response);
                //    afterLoadLogData(false);
            }
        }
    }

    /**
     * Called when we finish trying to send medication data to Dreamfactory.
     *
     * @param event SendMedsDFEvent
     */
    public void onEventMainThread(SendSubscribeDFEvent event) {
        if (event.wasSuccess) {
            //       List<SubscribeDataModel> subscriptions =  (ArrayList<SubscribeDataModel>)event.response;
            //        mUser.setSubscriptions(subscriptions);
            //        mUser.setSubscriptionAlarms();
            jobsRunning.remove("SUBSCRIBE_SEND_DF");
            if (jobsRunning.isEmpty()) finishedSending();
        } else {
            if (event.response instanceof DFCredentialsInvalidException) {
                mJobManager.addJobInBackground(new DreamFactoryLoginJob());
            } else {
                Util.handleSendJobFailure(this, (Exception) event.response);
            }
        }
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
                    mBackend.destroyDF();
                    //  afterLoadLogData(false);
                    break;
                case "Success":
                case "Already Logged In":
                    // TODO - check this
                    mJobManager.addJobInBackground(new DreamFactoryGetJob(User.MEDICATION_DATA_TYPE));
                    break;
            }
        } else {
            // Handle it if it's an exception of some kind
            Util.handleDFLoginError(this, mUser, (Exception) event.response);
        }
    }

    /**
     * Called to load log data.
     */
    private void loadMedData() {
        //    prepareLoadLogData();
        mJobManager.addJobInBackground(new DreamFactoryGetJob(User.MEDICATION_DATA_TYPE));
    }

    /**
     * Changes UI based on if we successfully loaded the Log data or not.
     * @param wasSuccess True on success
     */
/*    private void afterLoadLogData(boolean wasSuccess) {
        mLoading.setVisibility(View.GONE);
        if (wasSuccess) {
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            Util.toast(LogActivity.this, R.string.toast_get_df_data_error);
            mReloadBtn.setVisibility(View.VISIBLE);
        }
    }*/

   /* *//**
     * Takes objects that extend BaseRecordModel, filters out unwanted ones, then uses the remaining
     * ones to create a list of LogItems, then tells the LogAdapter to refresh.
     * Used with the DF backend.
     *
     * @param medicationDataList ArrayList of objects that extend BaseDataModel
     *//*
    private void populateMedsDF(ArrayList<MedicationDataModel> medicationDataList) {
        mLogItems.clear();
        // Turn the user data items into LogItems and add them to the list
        for (BaseRecordModel record : userRecords) {
            // Wrapped in a try/catch in case any data is incorrectly formatted
            try {
                long time = record.getLongTime();
                String title = "";
                String text = "";
                if (record instanceof MedRecordModel) {
                    String medRow = ((MedRecordModel) record).getMedication();
                    // We only care about the new med reporting format, ignore the old way
                    if (medRow.contains("took") || medRow.contains("miss") || medRow.contains("skipped"))
                        continue;
                    title = getString(R.string.log_meds_label);
                    int[][] medRowData = mBackend.decodeMedsRow(medRow);
                    // Construct the text of the log item
                    StringBuilder logItemText = new StringBuilder();
                    for (int j = 0; j < medRowData[0].length; j++) {
                        // Replace any "-1" dosage with "NS" and any "4" dosage with "4+"
                        logItemText.append(User.MED_NAMES[medRowData[0][j]]).append(": ")
                                .append(String.valueOf(medRowData[1][j])
                                        .replace("-1", getString(R.string.dosage_NS))
                                        .replace("4", getString(R.string.dosage_4_plus)))
                                .append("\n");
                    }
                    // Trim the trailing "\n"
                    text = logItemText.delete(logItemText.length() - 1, logItemText.length()).toString();
                } else if (record instanceof SymptomRecordModel) {
                    title = getString(R.string.log_symp_label);
                    String sympRow = ((SymptomRecordModel) record).getSymptoms();
                    String[] sympLabels = getResources().getStringArray(R.array.symptoms);
                    String[] sympRowData = sympRow.split(",");
                    StringBuilder logItemText = new StringBuilder();
                    for (String sympRowItem : sympRowData) {
                        String[] sympItem = sympRowItem.split(":");
                        logItemText.append(sympLabels[Integer.parseInt(sympItem[0]) - 1]).append(": ")
                                .append(sympItem[1]).append("\n");
                    }
                    // Trim the trailing "\n"
                    text = logItemText.delete(logItemText.length() - 1, logItemText.length()).toString();
                } else if (record instanceof StressFactorRecordModel) {
                    title = getString(R.string.log_env_label);
                    String envRow = ((StressFactorRecordModel) record).getStressFactors();
                    String[] envLabels = getResources().getStringArray(R.array.stress_factors);
                    String[] envRowData = envRow.split(",");
                    StringBuilder logItemText = new StringBuilder();
                    for (String envRowItem : envRowData) {
                        String[] envItem = envRowItem.split(":");
                        logItemText.append(envLabels[Integer.parseInt(envItem[0]) - 1]).append(": ")
                                .append(envItem[1]).append("\n");
                    }
                    // Trim the trailing "\n"
                    text = logItemText.delete(logItemText.length() - 1, logItemText.length()).toString();
                }
                mLogItems.add(new LogItem(time, title, text));
            } catch (Exception e) {
                *//*
                We may have exceptions if our data is malformatted or the like, but the need to
                not crash outweighs the need to figure out the data type and move it around, since
                that would create high code complexity, so we'll just skip over the offending data.
                 *//*
                e.printStackTrace();
            }
        }
        // Sort in descending order then tell the LogAdapter to refresh
        Collections.sort(mLogItems);
        mAdapter.notifyDataSetChanged();
    }*/


    private void setupList(ArrayList<MedicationDataModel> medicationDataList) {
        System.out.println("Debug## :: In setupList size() " + medicationDataList.size());
        mMedsDataList = medicationDataList;
        mMedsNames = new String[medicationDataList.size()];
        mMedicationIds = new int[medicationDataList.size()];

        // Populate adapter.
        //    boolean[] addedMedsIds = new boolean[mMeds.length];
        boolean[] addedMedsIds = new boolean[mMedsNames.length];
        for (int i = 0; i < medicationDataList.size(); i++) {
            mMedsNames[i] = medicationDataList.get(i).getName();
            mMedicationIds[i] = medicationDataList.get(i).getId();
            System.out.println("Debug## :: " + mMedsNames[i] +  " ::: " + mMedicationIds[i]);
            List<SubscribeDataModel> subscriptions =  medicationDataList.get(i).getSubscriptions();
            for (int j = 0; j < subscriptions.size(); j++) {
                if (mUser.getUserId() == subscriptions.get(j).getUserId()) {
                    mCurrentSubscribedMedIds.add(subscriptions.get(j).getMedicationId());
                    oldSubscriptionsMap.put(subscriptions.get(j).getId(), subscriptions.get(j));
                    addedMedsIds[i] = true;
                    break;
                }
            }
        }

        // Get complete list.
        mMedsListView = (ListView) findViewById(R.id.listMeds);
        mMeds = getResources().getStringArray(R.array.medications);


        // Set the listview to use the med names array
        mMedsListView.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_multiple_choice, mMedsNames));
        // Check the checkboxes of the already added meds.
        for (int j = 0; j < addedMedsIds.length; j++) {
            mMedsListView.setItemChecked(j, addedMedsIds[j]);
        }
    }

    /**
     * Populates the listview with all of the meds that are available. Pulls list from a string
     * array in the strings.xml file. Each entry in that array has the format:
     * "[ID#]:[Name]"
     */
    private void setupList() {
        // Get complete list.
        mMedsListView = (ListView) findViewById(R.id.listMeds);
        mMeds = getResources().getStringArray(R.array.medications);
        // Populate adapter.
        boolean[] addedMedsIds = new boolean[mMeds.length];
        mMedsNames = new String[mMeds.length];
        mMedsIds = new String[mMeds.length];
        for (int i = 0; i < mMeds.length; i++) {
            // See format in javadoc above
            String[] med = mMeds[i].split(":");
            mMedsIds[i] = med[0]; // The number representing a med
            mMedsNames[i] = med[1]; // The med's name
            // Evaluates to true if this med has already been added for the user
            addedMedsIds[i] = mCurrentMedIds.contains(mMedsIds[i]);
        }
        // Set the listview to use the med names array
        mMedsListView.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_multiple_choice, mMedsNames));
        // Check the checkboxes of the already added meds.
        for (int j = 0; j < addedMedsIds.length; j++) {
            mMedsListView.setItemChecked(j, addedMedsIds[j]);
        }
    }

    /**
     * Sets up the Next button so that it gets the checked medications
     */
    private void setupButton() {
        next = (Button) findViewById(R.id.nextButton);
        Util.makeGreen(next, this);
        next.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Activity ctx = SetupMedicationActivity.this;
                SparseBooleanArray checked = mMedsListView.getCheckedItemPositions();
                Set<String> selectedItems = new HashSet<>();
                Set<Integer> selectedIds = new HashSet<>();
                for (int i = 0; i < checked.size(); i++) {
                    // Item position in adapter
                    int position = checked.keyAt(i);
                    if (checked.valueAt(i)) {
                        selectedItems.add(mMeds[position]);
                        selectedIds.add(mMedicationIds[position]);
                    }
                }
                // Ensure at least one drug is selected.
                if (selectedIds.size() > 0) {
                    // Save to sharedPrefs only meds were changed from before.
                    //    if (!mCurrentMedIds.equals(selectedIds)) {
                    sendMedSubscriptionData(selectedIds);
                    mUser.recordAddedMedications(mMedsDataList, selectedIds);
                    //   if (!mIsInitialSetup) {
                    Util.toast(SetupMedicationActivity.this, R.string.toast_changes_saved);
                    //  }
                    //      }
                    //   setResult(RESULT_OK);
                    //   finish();
                } else {
                    Util.toast(ctx, R.string.toast_must_select_med);
                }
            }
        });
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        // Don't display up button if in initial setup mode.
        getActionBar().setHomeButtonEnabled(!mIsInitialSetup);
        getActionBar().setDisplayHomeAsUpEnabled(!mIsInitialSetup);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.add_meds, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private JSONObject prepareMedSubscriptionData(Set<Integer> selectedIds) {
        for (Integer selectedKey : selectedIds)  {
            System.out.println("selectedId : " + selectedKey);
        }
        Set<Integer> deletedSubscribeIds = new HashSet<Integer>();
        Set<Integer> unchangedSubscribeIds = new HashSet<Integer>();
        Set<Integer> addedMedIds = new HashSet<Integer>();
        Set<Integer> unchangedMedIds = new HashSet<Integer>();
        Set<Integer> deletedMedIds = new HashSet<Integer>();

        Set<Integer> oldSubscribeKeys = oldSubscriptionsMap.keySet();

        for (Integer key : oldSubscribeKeys) {
            System.out.println("oldSubscribeKey : " + key);
            Integer medId = oldSubscriptionsMap.get(key).getMedicationId();
            System.out.println("oldSubscribeKey medId : " + medId);
            if (!selectedIds.contains(medId)) {
                deletedSubscribeIds.add(key);
                deletedMedIds.add(medId);
                System.out.println("-- deleted medId : " + medId);
            } else {
                unchangedSubscribeIds.add(key);
                unchangedMedIds.add(medId);
                System.out.println("-- unchanged medId : " + medId);
            }
        }

        for (Integer selectedKey : selectedIds)  {
            if (!deletedMedIds.contains(selectedKey) && !unchangedMedIds.contains(selectedKey)) {
                addedMedIds.add(selectedKey);
                System.out.println("-- added medId : " + selectedKey);
            }
        }
        JSONObject updatedJson = new JSONObject();
        JSONObject userJson = new JSONObject();
        JSONObject json = new JSONObject();
        JSONArray jsonArray = new JSONArray();
        for (Integer medId : addedMedIds) {
            SubscribeDataModel subscribeData = new SubscribeDataModel();
            subscribeData.setUserId(mUser.getUserId());
            subscribeData.setMedicationId(medId);
            JSONObject subscribeJson = SubscribeDataModel.toJson(subscribeData);
            jsonArray.put(subscribeJson);
        }
        for (Integer id : deletedSubscribeIds) {
            SubscribeDataModel subscribeData = new SubscribeDataModel();
            subscribeData.setId(id);
            subscribeData.setUserId(0);
            subscribeData.setMedicationId(0);
            JSONObject subscribeJson = SubscribeDataModel.toJson(subscribeData);
            jsonArray.put(subscribeJson);
        }
        try {
            userJson.put("id", mUser.getUserId());
            userJson.put("subscribes_by_uid", jsonArray);
            JSONArray userJsonArray = new JSONArray();
            userJsonArray.put(userJson);
            updatedJson.put("record", userJsonArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println("UpdatedJson :: " + updatedJson);
        return updatedJson;
    }

    /**
     * Starts Jobs that send data to backends.
     */
    private void sendMedSubscriptionData(Set<Integer> selectedIds) {
        // Change UI
        setProgressBarIndeterminateVisibility(true);
        next.setText(getString(R.string.sending));
        next.setEnabled(false);
        //    mMedsList.setEnabled(false);
        // Get data
        JSONObject lastReportedData = prepareMedSubscriptionData(selectedIds);
        lastReportedTime = Calendar.getInstance().getTimeInMillis();
        // Store the fact that we're running jobs
        jobsRunning.add("SUBSCRIBE_SEND_DF");
        // Start the jobs
        mJobManager.addJobInBackground(new DreamFactorySendJob(User.SUBSCRIBE_DATA_TYPE, lastReportedData.toString(), lastReportedTime));
    }

    private void finishedSending() {
        // Change UI
        setProgressBarIndeterminateVisibility(false);
        next.setText(getString(R.string.send));
        next.setEnabled(true);
        //   mMedsList.setEnabled(true);
        // Toast
        Util.toast(SetupMedicationActivity.this, R.string.toast_changes_saved);
        setResult(RESULT_OK);
        finish();
       /* // Dismiss notification if needed then finish
        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.cancel(MS101Receiver.Notif.MEDS.ordinal());
        finish();*/
    }

}
