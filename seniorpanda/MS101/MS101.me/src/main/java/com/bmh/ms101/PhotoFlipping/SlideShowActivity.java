package com.bmh.ms101.PhotoFlipping;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ViewFlipper;

import com.bmh.ms101.R;

public class SlideShowActivity extends Activity implements OnClickListener {

    private static final Integer FLIP_INTERVAL = 6000;

    private ViewFlipper myFlipper;
    private Button myPreviousButton;
    private Button myNextButton;
    private Button myStartButton;
    private Button myPauseButton;
    private Button myDeleteButton;
    private int myPhotoIndex;

    private Animation slide_in_left, slide_in_right, slide_out_left, slide_out_right;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_show);
        myFlipper = (ViewFlipper) findViewById(R.id.photoFlipper);
        myFlipper.setAutoStart(true);
        myPreviousButton = (Button) findViewById(R.id.previousSlideButton);
        myNextButton = (Button) findViewById(R.id.nextSlideButton);
        myStartButton = (Button) findViewById(R.id.startSlideButton);
        myPauseButton = (Button) findViewById(R.id.pauseSlideButton);
        myDeleteButton = (Button) findViewById(R.id.deletePhotoButton);

        myPauseButton.setOnClickListener(this);
        myNextButton.setOnClickListener(this);
        myStartButton.setOnClickListener(this);
        myPauseButton.setOnClickListener(this);
        myDeleteButton.setOnClickListener(this);

        slide_in_left = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        slide_in_right = AnimationUtils.loadAnimation(this, R.anim.silde_in_right);
        slide_out_left = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        slide_out_right = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();
        getMenuInflater().inflate(R.menu.slide_show, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.upload_photo:
                //TODO
                return true;
            case R.id.action_settings:
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.nextSlideButton:
                myFlipper.setInAnimation(slide_in_right);
                myFlipper.setInAnimation(slide_out_left);
                myFlipper.showNext();
                break;
            case R.id.previousSlideButton:
                myFlipper.setInAnimation(slide_in_left);
                myFlipper.setInAnimation(slide_out_right);
                myFlipper.showPrevious();
                break;
            case R.id.startSlideButton:
                myFlipper.setFlipInterval(FLIP_INTERVAL);
                myFlipper.startFlipping();
                break;
            case R.id.pauseSlideButton:
                myFlipper.stopFlipping();
                break;
            case R.id.deletePhotoButton:
                //TODO
                break;
        }
    }
}
