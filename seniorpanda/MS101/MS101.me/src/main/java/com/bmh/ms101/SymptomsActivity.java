package com.bmh.ms101;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bmh.ms101.events.DFLoginResponseEvent;
import com.bmh.ms101.events.SendStressDFEvent;
import com.bmh.ms101.events.SendSympDFEvent;
import com.bmh.ms101.ex.DFCredentialsInvalidException;
import com.bmh.ms101.jobs.DreamFactoryLoginJob;
import com.bmh.ms101.jobs.DreamFactorySendJob;
import com.bmh.ms101.models.SymptomDataModel;
import com.path.android.jobqueue.JobManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.List;


import de.greenrobot.event.EventBus;

/**
 * FragmentActivity that contains the symptoms and environment reporting UI. Called both from main
 * activity and from a notification that fires every day at 12 noon to ask for symptoms (On Sunday,
 * asks for environmental stress factors).
 */
public class SymptomsActivity extends FragmentActivity implements ActionBar.TabListener {

    private final EventBus eventBus = EventBus.getDefault();

    private static final int NO_NOTIF = -1;
    // Section numbers to be passed to fragments
    public static final int SECTION_SYMP = 0;
    public static final int SECTION_STRESS = 1;

    private Backend mBackend;
    private User mUser;
    private JobManager mJobManager;
    private boolean mIsUnlocked = false;

    private ArrayList<String> jobsRunning = new ArrayList<>();
    private int currSection;
    private String lastReportedSympData;
    private String lastReportedStressData;
    private long lastReportedTime;

    private FragmentManager mFragMan;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private Button mSendSympStressButton;

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
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.activity_symptoms);
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
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.symptoms, menu);
        return true;
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
            // TODO - check this
            finish();
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
            case R.id.action_show_scale:
                switch(getActionBar().getSelectedTab().getPosition()) {
                    case SECTION_SYMP:
                        Util.buildInfoDialog(this, R.string.help_symptom_scales_title,
                                R.string.help_symptom_scales_content, R.string.okay);
                        break;
                    case SECTION_STRESS:
                        Util.buildInfoDialog(this, R.string.help_stress_factor_scales_title,
                                R.string.help_stress_factor_scales_content, R.string.okay);
                        break;
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
        currSection = tab.getPosition();
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * Sets up the UI
     */
    private void setupUI() {
        // Get views
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mSendSympStressButton = (Button) findViewById(R.id.sendSympStressButton);
        // Setup views
        setupViewPager();
        setupButtons();
    }

    /**
     * Sets up the action bar and view pager
     */
    private void setupViewPager() {
        // See if launched from reminder notification.
        int mNotifType = getIntent().getIntExtra(MS101Receiver.EXTRA_NOTIF_TYPE, NO_NOTIF);
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        actionBar.setDisplayHomeAsUpEnabled(mNotifType == NO_NOTIF);
        mFragMan = getSupportFragmentManager();
        mSectionsPagerAdapter = new SectionsPagerAdapter(mFragMan);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
                currSection = position;
                setButtonText();
            }
        });
        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i))
                    .setTabListener(this));
        }
        // If arrived from notification, open corresponding section.
        if (mNotifType != NO_NOTIF) mViewPager.setCurrentItem(mNotifType);
        currSection = actionBar.getSelectedTab().getPosition();
    }

    /**
     * Sets up the color and onClick() listener for our Send buttons
     */
    private void setupButtons() {
        Util.makeGreen(mSendSympStressButton, this);
        mSendSympStressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currSection == SECTION_SYMP) {
                    mViewPager.setCurrentItem(SECTION_STRESS);
                } else {
                    sendSympStress();
                }
            }
        });
        setButtonText();
    }

    /**
     * Convenience method for setting send buttons' text
     */
    private void setButtonText() {
        // Change button text depending on currently selected tab
        switch (currSection) {
            case SECTION_SYMP:
                if (jobsRunning.isEmpty()) mSendSympStressButton.setText(R.string.next);
                break;
            case SECTION_STRESS:
                if (jobsRunning.isEmpty()) mSendSympStressButton.setText(R.string.send_symp_stress);
                break;
        }
    }

    private void setRadioOptionsEnabled(boolean enabled) {
        SymptomsSectionFragment sympFragment = (SymptomsSectionFragment) mFragMan.findFragmentByTag(getFragTag(R.id.pager, SECTION_SYMP));
        SymptomsSectionFragment stressFragment = (SymptomsSectionFragment) mFragMan.findFragmentByTag(getFragTag(R.id.pager, SECTION_STRESS));
        sympFragment.getItems().setEnabled(enabled);
        stressFragment.getItems().setEnabled(enabled);
    }

    /**
     * Sends symptoms and stress factors to the backends
     */
  /*  private void sendSympStress() {
        // Change UI
  *//*      setProgressBarIndeterminateVisibility(true);
        mSendSympStressButton.setText(R.string.sending);
        mSendSympStressButton.setEnabled(false);
        setRadioOptionsEnabled(false);
        // Get data
        SymptomsSectionFragment sympFragment = (SymptomsSectionFragment) mFragMan.findFragmentByTag(getFragTag(R.id.pager, SECTION_SYMP));
        SymptomsSectionFragment stressFragment = (SymptomsSectionFragment) mFragMan.findFragmentByTag(getFragTag(R.id.pager, SECTION_STRESS));
        lastReportedSympData = mBackend.encodeSymptoms(sympFragment.getItems());
        lastReportedStressData = mBackend.encodeSymptoms(stressFragment.getItems());
        lastReportedTime = Calendar.getInstance().getTimeInMillis();
        // Store the fact that we're running jobs
        jobsRunning.add("DF_SYMP");
        jobsRunning.add("DF_STRESS");
        // Start the symptom jobs, which will then call the stress jobs when they complete
        mJobManager.addJobInBackground(new DreamFactorySendJob(User.SYMP, lastReportedSympData, lastReportedTime));*//*
    }*/

    /**
     * Sends symptoms and stress factors to the backends
     */
    private void sendSympStress() {
        // Change UI
        setProgressBarIndeterminateVisibility(true);
        mSendSympStressButton.setText(R.string.sending);
        mSendSympStressButton.setEnabled(false);
        setRadioOptionsEnabled(false);
        // Get data
        SymptomsSectionFragment sympFragment = (SymptomsSectionFragment) mFragMan.findFragmentByTag(getFragTag(R.id.pager, SECTION_SYMP));
    //    SymptomsSectionFragment stressFragment = (SymptomsSectionFragment) mFragMan.findFragmentByTag(getFragTag(R.id.pager, SECTION_STRESS));
     //   lastReportedSympData = mBackend.encodeSymptoms(sympFragment.getItems());
     //   lastReportedStressData = mBackend.encodeSymptoms(stressFragment.getItems());
    //    lastReportedSympData = getSymptomsData(sympFragment.getItems(), sympFragment.getCheckedBodyLocations());
        JSONArray jsonArray = getSymptomsData(sympFragment.getItems(), sympFragment.getCheckedBodyLocations());
        lastReportedTime = Calendar.getInstance().getTimeInMillis();
        // Store the fact that we're running jobs
   //     jobsRunning.add("DF_SYMP");

        for (int j = 0; j < jsonArray.length(); j++) {
            jobsRunning.add("DF_SYMP");
            //   jobsRunning.add("DF_STRESS");
            // Start the symptom jobs, which will then call the stress jobs when they complete
            //    mJobManager.addJobInBackground(new DreamFactorySendJob(User.SYMP_DATA_TYPE, lastReportedSympData, lastReportedTime));
           String data = "";
            try {
                data = jsonArray.get(j).toString();
                mJobManager.addJobInBackground(new DreamFactorySendJob(User.SYMP_DATA_TYPE, data, lastReportedTime));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }


    public JSONArray getSymptomsData(LinearLayout symptomsContainer, List<String> checkedBodyLocations) {
        List<String> types = new ArrayList<String>();
        // Loops through each RadioGroup to find which RadioButton is checked
        List<SymptomDataModel> symptomDataModels = new ArrayList<SymptomDataModel>();
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < symptomsContainer.getChildCount(); i++) {
            LinearLayout labelAndSlider = (LinearLayout) symptomsContainer.getChildAt(i);
            // Get the radio group
            RadioGroup value = (RadioGroup) labelAndSlider.findViewById(R.id.symptomRatings);
            // Get the checked RadioButton
            RadioButton checkedRadio = (RadioButton) labelAndSlider.findViewById(value
                    .getCheckedRadioButtonId());
            String duration = checkedRadio.getText().toString();
            String bodyLocation;
            switch (i) {
                case 0:
                    types.add(i, SymptomDataModel.SYMPTOM_TYPE_TREMOR);
                    break;
                case 1:
                    types.add(i, SymptomDataModel.SYMPTOM_TYPE_SLOW_MOVEMENT);
                    break;
                case 2:
                    types.add(i, SymptomDataModel.SYMPTOM_TYPE_RIGIDITY);
                    break;
                case 3:
                    types.add(i, SymptomDataModel.SYMPTOM_TYPE_FREEZING);
                    break;
                default:
                    break;
            }
            SymptomDataModel symptomDataModel = new SymptomDataModel();
            JSONObject jsonData = symptomDataModel.getJsonData(types.get(i), checkedBodyLocations.get(i), duration, 1, "2014-10-22");
            jsonArray.put(jsonData);

        }
        return jsonArray;
    }


    /**
     * Called when data has been sent successfully to all servers.
     */
    private void finishedSending() {
        Log.v("Debug##", "In finishSending");
        // Change UI
        setProgressBarIndeterminateVisibility(false);
        mSendSympStressButton.setText(getString(R.string.symp_stress_sent));
        mSendSympStressButton.setEnabled(true);
        setRadioOptionsEnabled(true);
        // Toast
        Util.toast(SymptomsActivity.this, getString(R.string.toast_thanks));
        // Dismiss notification if needed then finish
        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.cancel(MS101Receiver.Notif.SYMP.ordinal());
        notifManager.cancel(MS101Receiver.Notif.STRESS.ordinal());
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
                    jobsRunning.remove("DF_SYMP");
                    jobsRunning.remove("DF_STRESS");
                    mBackend.destroyDF();
                    if (jobsRunning.isEmpty()) finishedSending();
                    break;
                case "Success":
                case "Already Logged In":
                    // This is only gonna happen when we try to report symptoms most likely
                    if (jobsRunning.contains("DF_SYMP")) {
                        mJobManager.addJobInBackground(new DreamFactorySendJob(User.SYMP_DATA_TYPE, lastReportedSympData, lastReportedTime));
                    } else if (jobsRunning.contains("DF_STRESS")) {
                        // Because of the way we chain these calls normally, it's highly unlikely that we'll ever hit this
                        mJobManager.addJobInBackground(new DreamFactorySendJob(User.STRESS, lastReportedStressData, lastReportedTime));
                    }
                    break;
            }
        } else {
            // Handle it if it's an exception of some kind
            Util.handleDFLoginError(this, mUser, (Exception) event.response);
        }
    }

    /**
     * Called when we finish trying to send symptom data to Dreamfactory.
     * @param event SendSympDFEvent
     */
    public void onEventMainThread(SendSympDFEvent event) {
        if (event.wasSuccess) {
            jobsRunning.remove("DF_SYMP");
            // Now send stress factors
            mJobManager.addJobInBackground(new DreamFactorySendJob(User.STRESS, lastReportedStressData, lastReportedTime));
        } else {
            if (event.response instanceof DFCredentialsInvalidException) {
                mJobManager.addJobInBackground(new DreamFactoryLoginJob());
            } else {
                Util.handleSendJobFailure(this, (Exception) event.response);
            }
        }
    }

    /**
     * Called when we finish trying to send stress factor data to Dreamfactory.
     * @param event SendStressDFEvent
     */
    public void onEventMainThread(SendStressDFEvent event) {
        if (event.wasSuccess) {
            jobsRunning.remove("DF_STRESS");
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
     * Gets fragment's tag in viewpager so FragmentManager can find it
     * @param viewId ViewPager's ID
     * @param position Position of wanted fragment in viewpager
     * @return String tag for the fragment
     */
    private static String getFragTag(int viewId, int position) {
        return "android:switcher:" + viewId + ":" + position;
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a DummySectionFragment (defined as a static inner class
            // below) with the page number as its lone argument.
            Fragment fragment = new SymptomsSectionFragment();
            Bundle args = new Bundle();
            args.putInt(SymptomsSectionFragment.ARG_SECTION_NUMBER, position);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            // Show heath and environment.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.tab_title_symptoms).toUpperCase(l);
                case 1:
                    return getString(R.string.tab_title_stress_factors).toUpperCase(l);
            }
            return null;
        }
    }
}
