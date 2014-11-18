package com.bmh.ms101.PhotoFlipping;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.bmh.ms101.Constants;
import com.bmh.ms101.PhotoSharing.S3PhotoIntentService;
import com.bmh.ms101.R;
import com.bmh.ms101.Util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SlideShowActivity extends Activity implements OnClickListener {

    private static final Integer FLIP_INTERVAL = 20000;
    private static final Integer SELECT_PHOTO_FROM_GALLERY_REQUEST = 100;
    private static final Integer DELETE_PHOTO_REQUEST = 101;
    private static final Integer TAKE_PHOTO_REQUEST = 102;
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;

    private ViewFlipper myFlipper;
    private TextView myDateTime;
    private Thread myDateTimeThread;
    private ResponseReceiver myResponseReceiver;
    private GestureDetector myTouchDetector;
    private Set<String> visitedBitMaps;
    private Animation slide_in_left, slide_in_right, slide_out_left, slide_out_right;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_show);
        myFlipper = (ViewFlipper) findViewById(R.id.photoFlipper);
        myFlipper.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                myTouchDetector.onTouchEvent(motionEvent);
                return true;
            }
        });

        initButton(R.id.previousSlideButton);
        initButton(R.id.nextSlideButton);
        initButton(R.id.startSlideButton);
        initButton(R.id.pauseSlideButton);
        initButton(R.id.deletePhotoButton);

        visitedBitMaps = new HashSet<>();
        S3PhotoIntentService.startActionFetchS3(this, null, null);

        slide_in_left = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        slide_in_right = AnimationUtils.loadAnimation(this, R.anim.silde_in_right);
        slide_out_left = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        slide_out_right = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);

        IntentFilter statusIntentFilter = new IntentFilter(Constants.BROADCAST_ACTION);
        statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        myResponseReceiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(myResponseReceiver, statusIntentFilter);
        myTouchDetector = new GestureDetector(myFlipper.getContext(), new SwipeGestureDetector());

        //set up the date time text views
        myDateTime = (TextView) findViewById(R.id.slide_show_display_date);
        myDateTime.setTextColor(getResources().getColor(R.color.app_green));
        myDateTime.setTypeface(Typeface.DEFAULT_BOLD);
        myDateTimeThread = new Thread(new DateTimeRunner());
        myDateTimeThread.start();
    }

    private void initButton(int resId) {
        Button button = (Button) findViewById(resId);
        Util.makeGreen(button, this);
        button.setOnClickListener(this);
    }

    @Override
    public void onDestroy() {
        if (myResponseReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(myResponseReceiver);
            myResponseReceiver = null;
        }
        //TODO: destroy threads
        super.onDestroy();
    }

    public void addPhoto(Bitmap bitmap) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        myFlipper.addView(imageView);
    }

    private void dispatchTakePhotoIntent() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Log.w("PhotoShowActivity", "The current app has no camera");
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Alert");
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(R.string.no_camera_message);
            builder.setView(input);
            builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            builder.show();
            return;
        }
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {
            ImageFilePath imageFilePath = null;
            try {
                imageFilePath = createImageFile();
            } catch (IOException e) {
                //TODO: pop up dialog
                return;
            }
            if (imageFilePath.getFile() != null) {
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFilePath.getFile()));
                startActivityForResult(takePhotoIntent, TAKE_PHOTO_REQUEST);

                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File f = new File(imageFilePath.getPath());
                Uri contentUri = Uri.fromFile(f);
                mediaScanIntent.setData(contentUri);
                sendBroadcast(mediaScanIntent);
            }
        }
    }
//    @Override
//    protected void onStop() {
//        myDateTimeThread.interrupt();
//        super.onStop();
//    }
//
//    @Override
//    protected void onResume() {
//        myDateTimeThread = new Thread(new DateTimeRunner());
//        myDateTimeThread.start();
//        super.onResume();
//    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.slide_show, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.take_photo:
                dispatchTakePhotoIntent();
                return true;
            case R.id.upload_photo:
                uploadPhotoFromGallery();
                return true;
            case R.id.update_slide_show:
                Intent fetchedIntent = new Intent(Constants.BROADCAST_ACTION).putExtra(Constants.EXTENDED_DATA_STATUS, Constants.STATE_ACTION_COMPLETE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(fetchedIntent);
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.change_city:
                showInputDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change city");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("Go", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                changeCity(input.getText().toString());
            }
        });
        builder.show();
    }

    public void changeCity(String city) {
        WeatherFragment weatherFragment = (WeatherFragment) getFragmentManager()
                .findFragmentById(R.id.slide_show_weather_display);
        weatherFragment.changeCity(city);
        new CityPreference(this).setCity(city);
    }

    private void uploadPhotoFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PHOTO_FROM_GALLERY_REQUEST);
    }

    private ImageFilePath createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        String path = "file:" + image.getAbsolutePath();
        ImageFilePath imageFilePath = new ImageFilePath(image, path);
        return imageFilePath;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_PHOTO_FROM_GALLERY_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri imageURL = data.getData();
                Map<String, String> imageMap = new HashMap<String, String>();
                imageMap.put(imageURL.toString().substring(imageURL.toString().lastIndexOf("/") + 1), imageURL.toString());
                S3PhotoIntentService.startActionUploadS3(this, imageMap, null);
            }
        } else if (requestCode == TAKE_PHOTO_REQUEST) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                Bitmap bitmap = (Bitmap) extras.get("data");
                addPhoto(bitmap);
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
                flipperShowNext();
                break;
            case R.id.previousSlideButton:
                flipperShowPrevious();
                break;
            case R.id.startSlideButton:
                flipperStartFlipping();
                break;
            case R.id.pauseSlideButton:
                flipperStopFlipping();
                break;
            case R.id.deletePhotoButton:
                ImageView currentView = (ImageView) myFlipper.getCurrentView();
                myFlipper.removeView(currentView);
                String imageName = S3PhotoIntentService.getImageName(((BitmapDrawable) currentView.getDrawable()).getBitmap());
                Log.w("PhotoShowActivity", "Delete photo is called " + imageName);
                S3PhotoIntentService.startActionDeleteS3(this, imageName);
                break;
        }
    }

    private void flipperStopFlipping() {
        Log.w("PhotoShowActivity", "ViewFlipper stops flipping");
        myFlipper.stopFlipping();
        myFlipper.setAutoStart(false);
    }

    private void flipperStartFlipping() {
        myFlipper.setAutoStart(true);
        myFlipper.setFlipInterval(FLIP_INTERVAL);
        if (myFlipper.isAutoStart() && !myFlipper.isFlipping()) {
            Log.w("PhotoShowActivity", "ViewFlipper starts to flip");
            myFlipper.setInAnimation(slide_in_right);
            myFlipper.setOutAnimation(slide_out_left);
            myFlipper.startFlipping();
        }
    }

    private void flipperShowPrevious() {
        Log.w("PhotoShowActivity", "ViewFlipper shows previous");
        flipperStopFlipping();
        myFlipper.setInAnimation(slide_in_left);
        myFlipper.setOutAnimation(slide_out_right);
        myFlipper.showPrevious();
    }

    private void flipperShowNext() {
        Log.w("PhotoShowActivity", "ViewFlipper shows next");
        flipperStopFlipping();
        myFlipper.setInAnimation(slide_in_right);
        myFlipper.setOutAnimation(slide_out_left);
        myFlipper.showNext();
    }

    public void doUpdateTimeWork() {
        runOnUiThread(new Runnable() {
            public void run() {
                try {
                    Calendar c = Calendar.getInstance();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm a");
                    String formatted = dateFormat.format(c.getTime());
                    myDateTime.setText(formatted);
                } catch (Exception e) {
                }
            }
        });
    }

    public synchronized void doFetchPhotoWork() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Map<String, Bitmap> bitmapMap = S3PhotoIntentService.getBitmapMap();
                for (Bitmap bitmap : bitmapMap.values()) {
                    if (!visitedBitMaps.contains(bitmap)) {
                        addPhoto(bitmap);
                    }
                }
                flipperStartFlipping();
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

    public class ResponseReceiver extends BroadcastReceiver {
        private ResponseReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(Constants.EXTENDED_DATA_STATUS, Constants.STATE_ACTION_COMPLETE)) {
                case Constants.STATE_ACTION_COMPLETE:
//                    handler.post(new Runnable() {
//                        public void run() {
//                            doFetchPhotoWork();
//                        }
//                    });
                    doFetchPhotoWork();
            }
        }
    }

    public class SwipeGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    flipperShowNext();
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    flipperShowPrevious();
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

}
