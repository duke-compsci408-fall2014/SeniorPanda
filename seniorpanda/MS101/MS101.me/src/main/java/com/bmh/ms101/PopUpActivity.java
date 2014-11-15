package com.bmh.ms101;

import android.app.Activity;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bmh.ms101.ex.DFCredentialsInvalidException;

import org.droidparts.net.http.HTTPException;
import org.json.JSONException;

import java.util.ArrayList;

/**
 * Activity that acts like a dialog, used so widgets can show "dialogs".
 */
public class PopUpActivity extends Activity {
    public static final String POPUP_LAYOUT = "popupLayout";

    public static final int MD_PILL_CLICK = 1;
    public static final int TEC_DOSE_MISS = 2;

    private static Backend mBackend;
    private static User mUser;
    private static int mWidgetId;
    private static boolean mIsMorningDose;

    TextView mTitle;
    TextView mPrompt;
    Button mDismiss;
    RadioGroup mReason;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBackend = new Backend(this);
        mUser = mBackend.getUser();
        mWidgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        mIsMorningDose = getIntent().getBooleanExtra(MS101Receiver.MD_TEC_IS_MORNING_DOSE, true);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_pop_up);
        // Get views
        mTitle = (TextView) findViewById(R.id.popup_title);
        mPrompt = (TextView) findViewById(R.id.popup_text);
        mDismiss = (Button) findViewById(R.id.popup_dismiss);
        mReason = (RadioGroup) findViewById(R.id.popup_miss_reason);
        // Set up views
        setupUi(getIntent().getIntExtra(POPUP_LAYOUT, MD_PILL_CLICK));
    }

    /**
     * Sets up the UI
     *
     * @param which Which layout to use
     */
    private void setupUi(int which) {
        switch (which) {
            case MD_PILL_CLICK:
                mReason.setVisibility(View.GONE);
                mTitle.setText(R.string.tec_dose_reported_title);
                mPrompt.setText(R.string.tec_dose_reported_text);
                mDismiss.setEnabled(true);
                mDismiss.setText(R.string.okay);
                mDismiss.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finish();
                    }
                });
                break;
            case TEC_DOSE_MISS:
                String promptStr = getString(R.string.md_miss_prompt);
                promptStr = String.format(promptStr, mIsMorningDose ? "morning" : "evening");
                mPrompt.setText(promptStr);
                mTitle.setText(R.string.tec_miss_dose_title);
                mReason.setVisibility(View.VISIBLE);
                mDismiss.setText(R.string.confirm);
                mDismiss.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        handleTecReason(mReason.getCheckedRadioButtonId());
                    }
                });
                break;
        }
    }

    /**
     * Handles when user says that they missed a dose of Tecfidera.
     * <p/>
     * TODO use jobs!
     * TODO move to TecfideraWidgetHandler class
     *
     * @param reason The reason the user missed a dose
     */
    private void handleTecReason(int reason) {
        int medKey = Integer.parseInt(mUser.getMedKey(mWidgetId));
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
                switch (reason) {
                    case R.id.popup_rad_yes:
                        // Increment if this is the med the widget is tracking, may take us from -1 to 0...
                        dosesForToday++;
                        // ...in which case, increment again to get to 1
                        if (dosesForToday == 0) dosesForToday++;
                        break;
                    default:
                        // Only ever bring the number of doses from -1 up to 0 since user didn't take dose
                        if (dosesForToday == -1) dosesForToday++;
                        break;
                }
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
        // Send doses
        new AsyncTask<String, Void, Object>() {
            @Override
            protected void onPreExecute() {
                // Disable changes
                mReason.setEnabled(false);
                mDismiss.setEnabled(false);
                mDismiss.setText(R.string.sending);
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
                    NotificationManager notifManager = (NotificationManager) PopUpActivity.this
                            .getSystemService(Context.NOTIFICATION_SERVICE);
                    notifManager.cancel(MS101Receiver.TEC_ALARM_1);
                    notifManager.cancel(MS101Receiver.TEC_ALARM_2);
                    setupUi(MD_PILL_CLICK);
                }
            }
        }.execute(medsRow);
    }

}
