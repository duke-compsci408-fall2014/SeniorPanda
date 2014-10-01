package com.bmh.ms101;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Allows users to record a short audio note.
 */
public class RecordAudioActivity extends Activity {

    private final String KEY_AUDIO_PRESENT = "audio_present";
    private final String KEY_FILE_PATH = "file_path";
    private final String KEY_FILE_NAME = "file_name";
    private final int MAX_LENGTH = 20; // Max recording length in seconds

    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;
    private Thread mProgressThread = null;
    private android.os.Handler mProgressHandler = new android.os.Handler();

    private TextView mAudioStatus;
    private ProgressBar mAudioPos;
    private Button mSSRecord;
    private Button mPSAudio;
    private LinearLayout mAudioControls;

    private boolean mAudioPresent = false;
    private String BASE_FILE_PATH;
    private String mFilePath = null;
    private String mFileName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_audio);
        BASE_FILE_PATH = getFilesDir() + "/audio/";
        if (savedInstanceState != null) {
            // Restore the file path and name if present
            mAudioPresent = savedInstanceState.getBoolean(KEY_AUDIO_PRESENT, false);
            mFilePath = savedInstanceState.getString(KEY_FILE_PATH, null);
            mFileName = savedInstanceState.getString(KEY_FILE_NAME, null);
        }
        // No need to call this if we had a stored file path and name
        if (mFilePath == null) getFilePath();
        setupUi();
    }

    @Override
    public void onBackPressed() {
        if (mRecorder != null) {
            // If the user is recording, we assume they changed their mind and don't want to keep it
            discardRecording();
            super.onBackPressed();
        } else if (mAudioPresent) {
            // If there's an audio file present, first stop it if it's playing
            if (mPlayer != null) stopAudio();
            // Then ask user if they want to save or delete the file and go back, or cancel going back
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.alert_unsaved_audio_title)
                    .setMessage(R.string.alert_unsaved_audio_message)
                    .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveAudio();
                            RecordAudioActivity.this.finish();
                        }
                    })
                    .setNegativeButton(R.string.discard, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            discardFile();
                            RecordAudioActivity.this.finish();
                        }
                    })
                    .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAudioPresent) {
            // Only save these if there is an audio file present
            outState.putBoolean(KEY_AUDIO_PRESENT, mAudioPresent);
            outState.putString(KEY_FILE_PATH, mFilePath);
            outState.putString(KEY_FILE_NAME, mFileName);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // We were interrupted, just release the recorder and delete the file
        if (mRecorder != null) discardRecording();
        // Playback interrupted, release player
        if (mPlayer != null) stopAudio();
    }

    /**
     * Initializes the UI
     */
    private void setupUi() {
        // Get all of our views
        mAudioStatus = (TextView) findViewById(R.id.audio_status);
        mAudioPos = (ProgressBar) findViewById(R.id.audio_position);
        mSSRecord = (Button) findViewById(R.id.record_start_stop);
        mAudioControls = (LinearLayout) findViewById(R.id.audio_controls_cont);
        mPSAudio = (Button) mAudioControls.findViewById(R.id.audio_play_stop);
        Button delAudio = (Button) mAudioControls.findViewById(R.id.audio_delete);
        Button saveAudio = (Button) mAudioControls.findViewById(R.id.audio_save);
        // Set up our views
        mSSRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSSRecord.getText().equals(getString(R.string.start_record))) startRecording();
                else stopRecording();
            }
        });
        mPSAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPSAudio.getText().equals(getString(R.string.play))) playAudio();
                else stopAudio();
            }
        });
        delAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAudio();
            }
        });
        saveAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAudio();
            }
        });
        // If we had an audio file from a saved instance state, be sure that we switch our layout
        setAudioIsPresent(mAudioPresent);
    }

    /**
     * Convenience method for changing the layout
     * @param isPresent True if an audio file is present
     */
    private void setAudioIsPresent(boolean isPresent) {
        mAudioPresent = isPresent;
        if (mAudioPresent) {
            mSSRecord.setVisibility(View.GONE);
            mAudioControls.setVisibility(View.VISIBLE);
            mAudioStatus.setText("Audio Note: " + mFileName);
        } else {
            mAudioControls.setVisibility(View.GONE);
            mSSRecord.setVisibility(View.VISIBLE);
            mAudioStatus.setText(R.string.audio_not_present);
        }
    }

    /**
     * Start recording audio note
     */
    private void startRecording() {
        mAudioPos.setMax(MAX_LENGTH);
        // Init the recorder
        initRecorder();
        // Start recording, start the progress updater, and change button text to "Stop Recording"
        recordPosTimer.start();
        mRecorder.start();
        mAudioStatus.setText("Recording: " + mFileName);
        mSSRecord.setText(R.string.stop_record);
    }

    /**
     * Initializes the MediaRecorder
     */
    private void initRecorder() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setAudioSamplingRate(44100);
        mRecorder.setAudioEncodingBitRate(96000);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRecorder.setOutputFile(mFilePath);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mRecorder.setMaxDuration(1000 * MAX_LENGTH);
        mRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) stopRecording();
            }
        });
        try {
            mRecorder.prepare();
        } catch (IOException e) {
            // TODO handle this correctly!
            e.printStackTrace();
        }
    }

    /**
     * Called if the recording finished correctly. Stops the MediaRecorder and takes care of cleanup,
     * while marking that there is now an audio file present.
     */
    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        recordPosTimer.cancel();
        mAudioPos.setProgress(0);
        mSSRecord.setText(R.string.start_record);
        setAudioIsPresent(true);
    }

    /**
     * Plays the audio note
     */
    private void playAudio() {
        // Init the player first
        initPlayer();
        // Start the player, progress updater thread, and change the button's text to "Stop"
        mPlayer.start();
        mProgressThread.start();
        mPSAudio.setText(R.string.stop);
    }

    /**
     * Initializes the MediaPlayer
     */
    private void initPlayer() {
        // Create the Player and init it
        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopAudio();
            }
        });
        try {
            mPlayer.setDataSource(mFilePath);
            mPlayer.prepare();
        } catch (IOException e) {
            // TODO handle correctly
            e.printStackTrace();
        }
        // Set up the progress bar and thread that updates it
        mAudioPos.setMax(mPlayer.getDuration() / 1000);
        mProgressThread = new Thread(audioPosRunnable);
    }

    /**
     * Stops the audio playback.
     */
    private void stopAudio() {
        // Stop updating progress bar, release and null the player, and set button text to "Play"
        if (mProgressThread != null) {
            mProgressThread.interrupt();
            mProgressThread = null;
        }
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        mPSAudio.setText(R.string.play);
    }

    /**
     * Deletes the audio file and handles the layout and background changes needed
     */
    private void deleteAudio() {
        // Make sure the audio file isn't in use
        stopAudio();
        discardFile();
        getFilePath();
        setAudioIsPresent(false);
    }

    /**
     * "Saves" audio file and closes activity
     * TODO technically the audio is already saved, but this may be used in the future for sending it to server
     */
    private void saveAudio() {
        // Make sure the audio file isn't in use
        stopAudio();
        finish();
    }

    /**
     * Called if the recording was interrupted before finished. Stops MediaRecorder and cleans up,
     * but deletes the file.
     */
    private void discardRecording() {
        recordPosTimer.cancel();
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        discardFile();
    }

    /**
     * Called to actually delete the file
     */
    private void discardFile() {
        File file = new File(mFilePath);
        file.delete();
    }

    /**
     * Gets a file path and name for the recorder/player to use
     */
    private void getFilePath() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMddyyyy");
        String fileName = sdf.format(Calendar.getInstance().getTime());
        String fileAppendage = "";
        String fileExt = ".m4a";
        File file = new File(BASE_FILE_PATH + fileName + fileAppendage + fileExt);
        // Ensure the directory is present
        File directory = file.getParentFile();
        if (!directory.exists() && !directory.mkdirs()) try {
            throw new IOException("Path to file could not be created.");
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Figure out if we need to add on to the name
        int num = 1;
        while (file.exists()) {
            // If the file exists, then append an XXX number on the end, starting with 001
            fileAppendage = String.format("%03d", num);
            num++;
            file = new File(BASE_FILE_PATH + fileName + fileAppendage + fileExt);
        }
        mFileName = fileName + fileAppendage + fileExt;
        mFilePath = BASE_FILE_PATH + mFileName;
    }

    /**
     * Timer that counts down the (max) length of time a recording can be, and increments the
     * progress bar every second
     *
     * TODO Add a textview with time??
     */
    private CountDownTimer recordPosTimer = new CountDownTimer(1000 * MAX_LENGTH, 1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            mAudioPos.incrementProgressBy(1);
        }
        @Override
        public void onFinish() {
            Log.w("Timer", "Finished!");
        }
    };

    /**
     * Runnable for thread used to update progress bar, or reset it to 0 if interrupted
     */
    private Runnable audioPosRunnable = new Runnable() {
        @Override
        public void run() {
            while (true) {
                try {
                    // Update the progress bar every 1 second
                    Thread.sleep(1000);
                    mProgressHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // If media player isn't null, then update the progress bar
                            if (mPlayer != null) mAudioPos.setProgress(mPlayer.getCurrentPosition() / 1000);
                        }
                    });
                } catch (InterruptedException e) {
                    // We've stopped the audio, so reset the progress bar, then exit thread
                    mProgressHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mAudioPos.setProgress(0);
                        }
                    });
                    return;
                }
            }
        }
    };
}
