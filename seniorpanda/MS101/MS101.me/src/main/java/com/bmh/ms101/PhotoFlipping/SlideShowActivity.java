package com.bmh.ms101.PhotoFlipping;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.bmh.ms101.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SlideShowActivity extends Activity implements OnClickListener {

    private static final Integer FLIP_INTERVAL = 6000;

    private ViewFlipper myFlipper;
    private Button myPreviousButton;
    private Button myNextButton;
    private Button myStartButton;
    private Button myPauseButton;
    private Button myDeleteButton;
    private TextView myDateTime;
    private int myPhotoIndex;//TODO

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
        myDateTime = (TextView) findViewById(R.id.date_time);
        DateTimeRunner timeThread = new DateTimeRunner();
        myDateTime.setVisibility(View.VISIBLE);
        myDateTime.setTextColor(Color.YELLOW);
        timeThread.run();

        //to DELETE: dummy content
        ImageView image1 = new ImageView(getApplicationContext());
        image1.setBackgroundResource(R.drawable.sanmay_dog);
        image1.setScaleType(ImageView.ScaleType.FIT_XY);
        myFlipper.addView(image1);
        ImageView image2 = new ImageView(getApplicationContext());
        image2.setBackgroundResource(R.drawable.steve);
        image2.setScaleType(ImageView.ScaleType.FIT_XY);
        myFlipper.addView(image2);

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
        menu.clear();
        getMenuInflater().inflate(R.menu.slide_show, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

    public class DateTimeRunner implements Runnable {
        public void run() {
            while (myFlipper.isShown()) {
                Calendar c = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss a");
                String formatted = dateFormat.format(c.getTime());
                myDateTime.setText(formatted);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
