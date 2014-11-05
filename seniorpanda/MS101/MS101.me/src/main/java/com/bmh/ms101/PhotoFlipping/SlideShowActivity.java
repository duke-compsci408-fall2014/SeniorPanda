package com.bmh.ms101.PhotoFlipping;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.bmh.ms101.Util;
import com.bmh.ms101.jobs.S3FetchPhotoIntentService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SlideShowActivity extends Activity implements OnClickListener {

    private static final Integer FLIP_INTERVAL = 50000;
    private static final Integer SELECT_PHOTO_REQUEST = 100;
    private static final Integer FETCH_PHOTO_REQUEST = 101;
    private static final Integer DELETE_PHOTO_REQUEST = 102;

    private ViewFlipper myFlipper;
    private Button myPreviousButton;
    private Button myNextButton;
    private Button myStartButton;
    private Button myPauseButton;
    private Button myDeleteButton;
    private TextView myDateTime;
    private Set<String>

    private Animation slide_in_left, slide_in_right, slide_out_left, slide_out_right;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_show);
        myFlipper = (ViewFlipper) findViewById(R.id.photoFlipper);
        myFlipper.setAutoStart(true);
        myPreviousButton = (Button) findViewById(R.id.previousSlideButton);
        Util.makeGreen(myPreviousButton, this);
        myNextButton = (Button) findViewById(R.id.nextSlideButton);
        Util.makeGreen(myNextButton, this);
        myStartButton = (Button) findViewById(R.id.startSlideButton);
        Util.makeGreen(myStartButton, this);
        myPauseButton = (Button) findViewById(R.id.pauseSlideButton);
        Util.makeGreen(myPauseButton, this);
        myDeleteButton = (Button) findViewById(R.id.deletePhotoButton);
        Util.makeGreen(myDeleteButton, this);

        setUpDateTimeTextView();
        fetchPhotos();

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

    private void setUpDateTimeTextView() {
        myDateTime = (TextView) findViewById(R.id.slide_show_display_date);
        myDateTime.setTextColor(Color.WHITE);
        myDateTime.setTypeface(Typeface.DEFAULT_BOLD);
        Runnable timeRunnable = new DateTimeRunner();
        Thread timeThread = new Thread(timeRunnable);
        timeThread.start();
    }

    private void fetchPhotos() {
        //TODO: delete dummy content
        ImageView image1 = new ImageView(getApplicationContext());
        image1.setBackgroundResource(R.drawable.sanmay_dog);
        image1.setScaleType(ImageView.ScaleType.FIT_XY);
        myFlipper.addView(image1);
        ImageView image2 = new ImageView(getApplicationContext());
        image2.setBackgroundResource(R.drawable.steve);
        image2.setScaleType(ImageView.ScaleType.FIT_XY);
        myFlipper.addView(image2);
        ImageView image3 = new ImageView(getApplicationContext());
        image3.setBackgroundResource(R.drawable.null_pointer);
        image3.setScaleType(ImageView.ScaleType.FIT_XY);
        myFlipper.addView(image3);

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Fetch Picture"), FETCH_PHOTO_REQUEST);
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
                uploadPhoto();
                return true;
            case R.id.action_settings:
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void uploadPhoto() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PHOTO_REQUEST);
    }

    //TODO: public or protected
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_PHOTO_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri imageURL = data.getData();
                System.out.println("selected image path is: " + getRealPathFromURI(imageURL));//TODO: delete
                Map<String, Uri> imageMap = new HashMap<String, Uri>();
                imageMap.put(imageURL.toString().substring(imageURL.toString().lastIndexOf("/") + 1), imageURL);
                S3FetchPhotoIntentService.startActionUploadS3(this, imageMap, null);
            }
        } else if (requestCode == FETCH_PHOTO_REQUEST) {
            if (resultCode == RESULT_OK) {
                S3FetchPhotoIntentService.startActionFetchS3(this, null, null);
            }
        }
    }

    //Convert the image URI to the direct file system path of the image file
    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.nextSlideButton:
                myFlipper.setInAnimation(slide_in_right);
                myFlipper.setOutAnimation(slide_out_left);
                myFlipper.showNext();
                break;
            case R.id.previousSlideButton:
                myFlipper.setInAnimation(slide_in_left);
                myFlipper.setOutAnimation(slide_out_right);
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
//                myFlipper.getForeground()
                break;
        }
    }

    public void doUpdateTimeWork() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm a");
                    String formatted = dateFormat.format(c.getTime());
                    System.out.println("current time: " + formatted);//TODO:delete
                    myDateTime.setText(formatted);
                } catch (Exception e) {
                }
            }
        });
    }

    public class DateTimeRunner implements Runnable {
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    doUpdateTimeWork();
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                }
            }
        }
    }

    public void doFetchPhotoWork() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    public class FetchPhotoRunner implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    doFetchPhotoWork();
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                }
            }
        }
    }
}
