package com.bmh.ms101;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.bmh.ms101.events.DFLoginResponseEvent;
import com.bmh.ms101.jobs.DreamFactoryLoginJob;
import com.path.android.jobqueue.JobManager;

import de.greenrobot.event.EventBus;

/**
 * Simple login activity.
 */
public class LoginActivity extends Activity {

    private final EventBus eventBus = EventBus.getDefault();

    private Backend mBackend;
    private User mUser;
    private JobManager mJobManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);
        mBackend = new Backend(this);
        mUser = mBackend.getUser();
        mJobManager = MS101.getInstance().getJobManager();
        setContentView(R.layout.activity_login);
        setupButtons();
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
//        startActivity(new Intent(LoginActivity.this, MainActivity.class));
    }

    /**
     * Make buttons pretty colors and sets their onClick() listeners
     */
    private void setupButtons() {
        ImageButton yesButton = (ImageButton) findViewById(R.id.credentialsYes);
        Util.makeGreen(yesButton, this);
        yesButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Util.showDFLoginDialog(LoginActivity.this, mUser, "");
            }
        });

        ImageButton noButton = (ImageButton) findViewById(R.id.credentialsNo);
        Util.makeRed(noButton, this);
        noButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Util.toast(LoginActivity.this, R.string.toast_no_credentials);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
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
                    Util.toast(this, "Login authentication processing in background");
                    break;
                case "JSON Exception":
                    Util.toast(this, R.string.toast_json_error);
                case "Server Problems":
                case "Cancelled":
                    mBackend.destroyDF();
                    Util.toast(this, R.string.toast_login_cancelled);
                    finish();
                    break;
                case "Success":
                    finish();
                case "Already Logged In":
                    setResult(RESULT_OK);
                    finish();
                    break;
            }

        } else {
            // Handle it if it's an exception of some kind
            Util.handleDFLoginError(this, mUser, (Exception) event.response);
        }
    }
}
