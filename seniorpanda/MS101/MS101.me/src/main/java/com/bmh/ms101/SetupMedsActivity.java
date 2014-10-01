package com.bmh.ms101;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.HashSet;
import java.util.Set;

/**
 * Shows all of the available meds that this app can track so we can set up the app to track the
 * meds the user takes
 */
public class SetupMedsActivity extends Activity {

    private ListView mMedsListView;
    private String[] mMeds;
    private String[] mMedsNames;
    private String[] mMedsIds;
    private boolean mIsInitialSetup;
    private User mUser;
    private Set<String> mCurrentMedIds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_meds);
        // Get the user and their current meds
        mUser = new User(this);
        mCurrentMedIds = mUser.getMedsIds();
        // Show the Up button in the action bar if not in tutorial.
        mIsInitialSetup = getIntent().getBooleanExtra(MainActivity.IS_INITIAL_SETUP, false);
        setupActionBar();
        setupList();
        setupButton();
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
        Button next = (Button) findViewById(R.id.nextButton);
        Util.makeGreen(next, this);
        next.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Activity ctx = SetupMedsActivity.this;
                SparseBooleanArray checked = mMedsListView.getCheckedItemPositions();
                Set<String> selectedItems = new HashSet<>();
                Set<String> selectedIds = new HashSet<>();
                for (int i = 0; i < checked.size(); i++) {
                    // Item position in adapter
                    int position = checked.keyAt(i);
                    if (checked.valueAt(i)) {
                        selectedItems.add(mMeds[position]);
                        selectedIds.add(mMedsIds[position]);
                    }
                }
                // Ensure at least one drug is selected.
                if (selectedItems.size() > 0) {
                    // Save to sharedPrefs only meds were changed from before.
                    if (!mCurrentMedIds.equals(selectedIds)) {
                        mUser.recordAddedMeds(selectedItems);
                        if (!mIsInitialSetup) {
                            Util.toast(SetupMedsActivity.this, R.string.toast_changes_saved);
                        }
                    }
                    setResult(RESULT_OK);
                    finish();
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

}
