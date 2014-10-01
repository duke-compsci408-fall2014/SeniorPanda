package com.bmh.ms101;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RemoteViews;

/**
 * Allows user to set up the MD widget
 */
public class SetupMDWidgetActivity extends Activity {

    private User mUser;
    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private String[] mSpecialMeds;
    private String[] mMedIds;

    private ListView mListView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // If we don't get to the end, we obviously cancelled
        setResult(RESULT_CANCELED);
        // Now, let's get what we need
        mUser = new User(this);
        mSpecialMeds = mUser.getSpecialMeds();
        setContentView(R.layout.activity_setup_md_widget);
        Bundle extras = getIntent().getExtras();
        if (extras != null) mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        // See if we got an invalid ID; if so, just bail
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) finish();
        // We can't have a widget if none of our meds can use it
        if (mSpecialMeds.length == 0) showNotNeededPopup();
        else setupUI();
    }

    /**
     * None of our meds can use this widget, so tell the user that and then finish.
     */
    private void showNotNeededPopup() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.md_not_needed_prompt)
                .setNeutralButton(R.string.okay, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .show();
    }

    /**
     * Sets up the UI for the configurator if this user has set up meds that can be used with this
     * widget. If there are no meds that can be used, show a dialog and cancel.
     */
    private void setupUI() {
        mListView = (ListView) findViewById(R.id.md_list);
        final Button btnConfirm = (Button) findViewById(R.id.md_choose_med);
        setupList();
        Util.makeGreen(btnConfirm, this);
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = mListView.getCheckedItemPosition();
                if (pos != ListView.INVALID_POSITION) {
                    // Record this widget/med pair so that we keep track of it in the future
                    mUser.recordMedDoseWidgetId(mMedIds[pos], mAppWidgetId);
                    // Represent and refresh views and create alarms
                    RemoteViews views = new RemoteViews(SetupMDWidgetActivity.this.getPackageName(), R.layout.widget_md);
                    refreshRemoteViewsAndAlarms(SetupMDWidgetActivity.this, mAppWidgetId, views);
                    // Now create the widget
                    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(SetupMDWidgetActivity.this);
                    appWidgetManager.updateAppWidget(mAppWidgetId, views);
                    // Set our result to success and finish
                    Intent resultValue = new Intent();
                    resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
                    setResult(RESULT_OK, resultValue);
                    finish();
                }
            }
        });
    }

    /**
     * Sets up the listview which contains a list of all meds that this widget could be set up to
     * track.
     *
     * TODO ensure any meds that already have a widget aren't in this list
     */
    private void setupList() {
        // Populate adapter.
        String[] mMedNames = new String[mSpecialMeds.length];
        mMedIds = new String[mSpecialMeds.length];
        for (int i = 0; i < mSpecialMeds.length; i++) {
            String[] med = mSpecialMeds[i].split(":");
            mMedIds[i] = med[0]; // The med's ID #
            mMedNames[i] = med[1]; // The med's name
        }
        // Set the listview to use the med names array
        mListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, mMedNames));
    }

    /**
     * Refreshes a widget's views, also sets alarms to be active or inactive depending on if the med
     * tied to that widget is currently being tracked.
     * @param context The context to use
     * @param widgetId The widget to refresh
     */
    public static void refreshRemoteViewsAndAlarms(Context context, int widgetId, RemoteViews views) {
        User user = new User(context);
        String medKey = user.getMedKey(widgetId);
        // Get the med name as long as the med key doesn't contain "null"
        String medName = !medKey.contains("null") ? User.MED_NAMES[Integer.valueOf(medKey)] : null;
        if (!medKey.contains("null")) {
            // Trim off the generic part of the name, it makes it too long
            medName = medName.replace(medName.subSequence(medName.indexOf("(") - 1, medName.length()), "");
            views.setTextViewText(R.id.mdMedName, medName);
            views.setTextColor(R.id.mdMedName, context.getResources().getColor(R.color.black));
            Intent pillClickIntent = new Intent();
            pillClickIntent.setAction(MS101Receiver.MD_PILL_CLICK);
            pillClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, pillClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.mdPill, pendingIntent);
            // Set the special alarms
            MS101Receiver.createSpecialAlarms(context, medKey);
        } else {
            views.setTextViewText(R.id.mdMedName, context.getString(R.string.md_not_set_up));
            views.setTextColor(R.id.mdMedName, context.getResources().getColor(R.color.black));
            Intent intent = new Intent(context, com.bmh.ms101.SetupMDWidgetActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            views.setOnClickPendingIntent(R.id.mdPill, pendingIntent);
            // Cancels the special alarms
            MS101Receiver.cancelSpecialAlarms(context, medKey);
        }
    }

}
