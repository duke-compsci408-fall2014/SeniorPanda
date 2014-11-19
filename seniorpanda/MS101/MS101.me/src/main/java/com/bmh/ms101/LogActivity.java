package com.bmh.ms101;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.bmh.ms101.PhotoFlipping.SlideShowActivity;
import com.bmh.ms101.events.DFLoginResponseEvent;
import com.bmh.ms101.events.GetDataDFEvent;
import com.bmh.ms101.ex.DFCredentialsInvalidException;
import com.bmh.ms101.jobs.DreamFactoryGetJob;
import com.bmh.ms101.jobs.DreamFactoryLoginJob;
import com.bmh.ms101.models.BaseRecordModel;
import com.bmh.ms101.models.MedRecordModel;
import com.bmh.ms101.models.StressFactorRecordModel;
import com.bmh.ms101.models.SymptomRecordModel;
import com.path.android.jobqueue.JobManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * Shows a log of medication doses, symptoms, and stress factors for the selected time frame
 */
public class LogActivity extends Activity {

    private final EventBus eventBus = EventBus.getDefault();

    // Week, month, 3 months, all time.
    private static final int[] TIME_RANGES = { 7, 30, 90, 3650 };

    private ProgressBar mLoading;
    private Button mReloadBtn;
    private Button mVisBtn;
    private Spinner mTimeRange;
    private FrameLayout mListContainer;

    private LogAdapter mAdapter;
    private List<LogItem> mLogItems;
    private Backend mBackend;
    private User mUser;
    private JobManager mJobManager;

    private Date lastFrom;
    private Date lastTo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBackend = new Backend(this);
        mUser = mBackend.getUser();
        mJobManager = MS101.getInstance().getJobManager();
        setContentView(R.layout.activity_log);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setupUi();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.log, menu);
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

    /**
     * Gets our views and sets needed onClick() listeners
     */
    private void setupUi() {
        mLoading = (ProgressBar) findViewById(R.id.log_loading);
        mReloadBtn = (Button) findViewById(R.id.log_reload);
        mVisBtn = (Button) findViewById(R.id.check_calendar);
        ListView listView = (ListView) findViewById(R.id.log_list);
        listView.setEmptyView(findViewById(R.id.empty_list));
        // Hide the listview for now because we don't want to show the empty view while loading.
        mListContainer = (FrameLayout) findViewById(R.id.log_list_container);
        mListContainer.setVisibility(View.GONE);
        // Set up the listview with a dummy adapter
        mLogItems = new ArrayList<>();
        mAdapter = new LogAdapter(this, mLogItems);
        listView.setAdapter(mAdapter);
        mReloadBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                loadLogData();
            }
        });
        setupTimeRangeSelector();

        mVisBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Util.trackUiEvent("click_visualization_button_calendar", LogActivity.this);
                startActivity(new Intent(LogActivity.this, WebCalendarVisualization.class));
            }
        });
    }

    /**
     * Sets up our spinner with selectable time ranges and defines the OnItemSelectedListener
     */
    private void setupTimeRangeSelector() {
        mTimeRange = (Spinner) findViewById(R.id.log_time_range);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.time_range_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTimeRange.setAdapter(adapter);
        mTimeRange.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                loadLogData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * Called to load log data.
     */
    private void loadLogData() {
        prepareLoadLogData();
        mJobManager.addJobInBackground(new DreamFactoryGetJob(lastFrom, lastTo));
    }

    /**
     * Prepares the UI and a few variables so that we can load log data.
     */
    private void prepareLoadLogData() {
        // Show the loading progress spinner
        mListContainer.setVisibility(View.GONE);
        mReloadBtn.setVisibility(View.GONE);
        mLoading.setVisibility(View.VISIBLE);
        // Get times
        int daysOffset = TIME_RANGES[mTimeRange.getSelectedItemPosition()];
        Calendar cal = Calendar.getInstance();
        lastTo = cal.getTime();
        cal.add(Calendar.DAY_OF_WEEK, -daysOffset);
        lastFrom = cal.getTime();
    }

    /**
     * Changes UI based on if we successfully loaded the Log data or not.
     * @param wasSuccess True on success
     */
    private void afterLoadLogData(boolean wasSuccess) {
        mLoading.setVisibility(View.GONE);
        if (wasSuccess) {
            mListContainer.setVisibility(View.VISIBLE);
        } else {
            Util.toast(LogActivity.this, R.string.toast_get_df_data_error);
            mReloadBtn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Takes objects that extend BaseRecordModel, filters out unwanted ones, then uses the remaining
     * ones to create a list of LogItems, then tells the LogAdapter to refresh.
     * Used with the DF backend.
     *
     * @param userRecords ArrayList of objects that extend BaseRecordRequest
     */
    private void populateLogDF(ArrayList<BaseRecordModel> userRecords) {
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
                /*
                We may have exceptions if our data is malformatted or the like, but the need to
                not crash outweighs the need to figure out the data type and move it around, since
                that would create high code complexity, so we'll just skip over the offending data.
                 */
                e.printStackTrace();
            }
        }
        // Sort in descending order then tell the LogAdapter to refresh
        Collections.sort(mLogItems);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Called when we get a response from DreamFactory after requesting some of the User's data.
     * @param event GetDataDFEvent
     */
    public void onEventMainThread(GetDataDFEvent event) {
        if (event.wasSuccess) {
            ArrayList<BaseRecordModel> userRecords = (ArrayList<BaseRecordModel>) event.response;
            populateLogDF(userRecords);
            afterLoadLogData(true);
        } else {
            if (event.response instanceof DFCredentialsInvalidException) {
                mJobManager.addJobInBackground(new DreamFactoryLoginJob());
            } else {
                Util.handleGetJobFailure(this, (Exception) event.response);
                afterLoadLogData(false);
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
                    afterLoadLogData(false);
                    break;
                case "Success":
                case "Already Logged In":
                    mJobManager.addJobInBackground(new DreamFactoryGetJob(lastFrom, lastTo));
                    break;
            }
        } else {
            // Handle it if it's an exception of some kind
            Util.handleDFLoginError(this, mUser, (Exception) event.response);
        }
    }

    /**
     * Custom ArrayAdapter for the log listview, takes a list of LogItems to populate the listview.
     * Takes care of handling whether the items need to show a header or not as well.
     */
    private static class LogAdapter extends ArrayAdapter<LogItem> {
        private final Activity mContext;
        private List<LogItem> mLogItems;

        static class ViewHolder {
            public TextView date;
            public TextView title;
            public TextView text;
        }

        public LogAdapter(Activity context, List<LogItem> logItems) {
            super(context, R.layout.log_item, logItems);
            this.mContext = context;
            this.mLogItems = logItems;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            // Reuse our views
            if (rowView == null) {
                LayoutInflater layoutInflater = mContext.getLayoutInflater();
                rowView = layoutInflater.inflate(R.layout.log_item, null);
                // Configure the view holder
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.date = (TextView) rowView.findViewById(R.id.log_item_separator);
                viewHolder.date.setVisibility(View.GONE);
                viewHolder.title = (TextView) rowView.findViewById(R.id.log_item_title);
                viewHolder.text = (TextView) rowView.findViewById(R.id.log_item_text);
                rowView.setTag(viewHolder);
            }
            // Fill in data
            ViewHolder viewHolder = (ViewHolder) rowView.getTag();
            viewHolder.title.setText(mLogItems.get(position).getTitle());
            viewHolder.text.setText(mLogItems.get(position).getText());
            viewHolder.date.setText(mLogItems.get(position).getDateString());
            // Figure out if we need to show the date header or not on this item
            long currItemDate = mLogItems.get(position).getDate();
            long prevItemDate = position == 0 ? 0 : mLogItems.get(position - 1).getDate();
            if (prevItemDate == 0) {
                // Show the date header if this is the first item
                viewHolder.date.setVisibility(View.VISIBLE);
            } else {
                // Or show the date header if this and the previous item have a different day of year
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(currItemDate);
                int currDayOfYear = c.get(Calendar.DAY_OF_YEAR);
                c.setTimeInMillis(prevItemDate);
                int prevDayOfYear = c.get(Calendar.DAY_OF_YEAR);
                if (currDayOfYear != prevDayOfYear) {
                    viewHolder.date.setVisibility(View.VISIBLE);
                } else {
                    // Otherwise hide the date header
                    viewHolder.date.setVisibility(View.GONE);
                }
            }
            return rowView;
        }
    }
}
