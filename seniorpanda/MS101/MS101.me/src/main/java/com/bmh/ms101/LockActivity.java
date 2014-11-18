package com.bmh.ms101;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * A simple pin screen that will pop up every time the user opens the app to have them unlock it.
 * Also used when initially setting up the pin.
 */
public class LockActivity extends Activity {

    private User mUser;
    private CryptHelper mCryptHelper;
    private EditText mEtPin;
    private EditText mEtUserName;
    private Button mBtnConfirm;
    private Boolean mHasPin = false; //default value set to false
    private Boolean mHasName = false; //default value set to false

    private boolean mIsSetup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCryptHelper = new CryptHelper();
        mUser = new User(this);
        mIsSetup = getIntent().getBooleanExtra(MainActivity.IS_INITIAL_SETUP, false);
        setContentView(R.layout.activity_lock);
        setupUI();
    }

    /**
     * Initializes the UI
     */
    private void setupUI() {
        RelativeLayout content = (RelativeLayout) findViewById(R.id.lock_screen);
        LinearLayout pinAndButton = (LinearLayout) content.findViewById(R.id.pin_and_button);
        mEtPin = (EditText) pinAndButton.findViewById(R.id.pin);
        mBtnConfirm = (Button) pinAndButton.findViewById(R.id.confirm_pin);

        mEtUserName = (EditText) content.findViewById(R.id.userName);
        if (mIsSetup) {
            // Change the title of the activity
            setTitle(R.string.title_activity_create_pin);
            // Unmask the pin so user can see it when setting it up
            mEtPin.setTransformationMethod(null);
            // Change the text on some of the views
            TextView tvPrompt = (TextView) content.findViewById(R.id.lock_prompt);
            tvPrompt.setText(R.string.create_pin_instructions);
            mBtnConfirm.setText(R.string.confirm);
        }

        // We only want the button to be enabled if : 1. there are 4 digits in the pin field 2. The userName field is entered
        mEtPin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 4) {
                    mHasPin = true;
                    mBtnConfirm.setEnabled(mHasName && mHasPin);
                }
                else mBtnConfirm.setEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        mEtUserName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0) {
                    mHasName = true;
                    mBtnConfirm.setEnabled(mHasName && mHasPin);
                }
                else mBtnConfirm.setEnabled(false);
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Set the pin edittext's focus change listener so that the keypad will pop up automatically
        mEtPin.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN |
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
        // Set the userName edittext's focus change listener so that the keypad will pop up automatically
        mEtUserName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN |
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });

        // Set up the buttons' on click listeners
        mBtnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doUnlock();
            }
        });
        // Set up the enter key to act the same as the Unlock button
        mEtPin.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    // Don't even bother if the textview isn't 4 digits long yet
                    if (v.getText().length() == 4) doUnlock();
                    // We still return true even if it isn't 4 digits long so we don't mDismiss the keypad
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Called when the user either clicks the unlock button or the enter button on their keypad
     */
    private void doUnlock() {
        String pin = mEtPin.getText().toString();
        String userName = mEtUserName.getText().toString();

        if (mIsSetup) {
            // If we're setting up the pin, then do that and then unlock
            mUser.recordPin(encryptInfo(pin));
            mUser.recordUserName(encryptInfo(userName));
            setResult(RESULT_OK);
            finish();
        } else {
            if (mUser.verifyPin(encryptInfo(pin)) && mUser.verifyUserName(encryptInfo(userName))) {
                // Otherwise we need to check the pin to see if it's valid
                mUser.setUserName(userName);
                setResult(RESULT_OK);
                finish();
            } else {
                // If it isn't show the error symbol on the edittext
                mEtPin.setError(getString(R.string.wrong_pin));
            }
        }
    }

    /**
     * Encrypt the info
     * @param info Text from the particular text field
     * @return Encrypted info
     */
    private String encryptInfo(String info) {
        try {
            return mCryptHelper.encrypt(info, mUser.getSecretKey()).trim();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
