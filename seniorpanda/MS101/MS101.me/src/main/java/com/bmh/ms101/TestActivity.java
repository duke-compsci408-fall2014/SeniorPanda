package com.bmh.ms101;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

/**
 * Activity used to test features. Currently an audio test activity.
 */
public class TestActivity extends Activity {

    private MediaPlayer mMediaPlayer = null;
    private Thread mProgressThread;
    private android.os.Handler mProgressHandler = new android.os.Handler();

    private Button mPlayStopButton;
    private ProgressBar mProgress;
    private String BASE_FILE_PATH;
    private TextView mTVFileName;

    private File mFile = null;
    private String mFileName;
    private String[] mNames = null;
    private File[] mFiles = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        BASE_FILE_PATH = getFilesDir() + "/audio/";
        mFileName = getString(R.string.prompt_2);
        setupUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Run it here so it sees any that we add in the recording screen after backing out
        getFiles();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMediaPlayer != null) stopAudio();
    }

    /**
     * Gets a list of files
     */
    private void getFiles() {
        File dir = new File(BASE_FILE_PATH);
        dir.mkdirs();
        mFiles = dir.listFiles();
        mNames = dir.list();
        if (mNames.length == 0) {
            mFiles = null;
            mNames = null;
        }
    }

    /**
     * Sets up the test audio UI
     */
    private void setupUi() {
        mTVFileName = (TextView) findViewById(R.id.test_file_name);
        mProgress = (ProgressBar) findViewById(R.id.test_progress);
        mPlayStopButton = (Button) findViewById(R.id.test_play_stop);
        mPlayStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayStopButton.getText().equals(getString(R.string.test_play))) {
                    playAudio();
                } else {
                    stopAudio();
                }
            }
        });
        Button testRecord = (Button) findViewById(R.id.test_record_audio);
        testRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMediaPlayer!= null) stopAudio();
                startActivity(new Intent(TestActivity.this, RecordAudioActivity.class));
            }
        });
        Button testChoose = (Button) findViewById(R.id.test_choose);
        testChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mNames != null) {
                    if (mMediaPlayer!= null) stopAudio();
                    AlertDialog alertDialog = new AlertDialog.Builder(TestActivity.this)
                            .setTitle(R.string.test_choose)
                            .setItems(mNames, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    changeFile(which);
                                }
                            })
                            .create();
                    alertDialog.setCanceledOnTouchOutside(true);
                    alertDialog.show();
                } else {
                    Toast.makeText(TestActivity.this, R.string.toast_no_choice, Toast.LENGTH_SHORT).show();
                }
            }
        });
        Button testClear = (Button) findViewById(R.id.test_clear);
        testClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mNames != null) {
                    if (mMediaPlayer != null) stopAudio();
                    mFile = null;
                    for (File file : mFiles) {
                        file.delete();
                    }
                    mFiles = null;
                    mNames = null;
                    mFileName = getString(R.string.prompt_2);
                    mTVFileName.setText(mFileName);
                } else {
                    Toast.makeText(TestActivity.this, R.string.toast_no_choice, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Changes the current file when user selects a recorded file
     * @param which The index of the item chosen
     */
    private void changeFile(int which) {
        mFile = mFiles[which];
        mFileName = "File name: " + mNames[which];
        mTVFileName.setText(mFileName);
    }

    /**
     * Inits media player, starts audio and progress thread, and sets text to "Stop"
     */
    private void playAudio() {
        // Create media player with audio file
        if (mFileName.equals(getString(R.string.prompt_2))) {
            mMediaPlayer = MediaPlayer.create(this, R.raw.test_file);
        } else {
            mMediaPlayer = new MediaPlayer();
            try {
                mMediaPlayer.setDataSource(mFile.getAbsolutePath());
                mMediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Set progress bar max to be the duration of the audio file
        mProgress.setMax(mMediaPlayer.getDuration() / 1000);
        // Set media player's on completion listener, start the audio, and change button text to Stop
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                stopAudio();
            }
        });
        mMediaPlayer.start();
        mPlayStopButton.setText(R.string.stop);
        // Create thread used to update progress bar, then start it
        mProgressThread = new Thread(progressRunnable);
        mProgressThread.start();
    }

    /**
     * Stops audio playback (using .release()), stops and resets progress bar, and sets text to "Play"
     */
    private void stopAudio() {
        // Interrupt the progress updater thread, then release and null the media player
        mProgressThread.interrupt();
        mMediaPlayer.release();
        mMediaPlayer = null;
        mPlayStopButton.setText(R.string.test_play);
    }

    /**
     * Runnable for thread used to update progress bar, or reset it to 0 if interrupted
     */
    private Runnable progressRunnable = new Runnable() {
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
                            if (mMediaPlayer != null) mProgress.setProgress(mMediaPlayer.getCurrentPosition() / 1000);
                        }
                    });
                } catch (InterruptedException e) {
                    // We've stopped the audio, so reset the progress bar, then exit thread
                    mProgressHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mProgress.setProgress(0);
                        }
                    });
                    return;
                }
            }
        }
    };
}
