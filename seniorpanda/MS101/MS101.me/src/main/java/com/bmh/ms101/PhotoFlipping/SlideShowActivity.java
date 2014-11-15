package com.bmh.ms101.PhotoFlipping;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

    private static final Integer FLIP_INTERVAL = 50000;
    private static final Integer SELECT_PHOTO_FROM_GALLERY_REQUEST = 100;
    private static final Integer DELETE_PHOTO_REQUEST = 101;
    private static final Integer TAKE_PHOTO_REQUEST = 102;

    private ViewFlipper myFlipper;
    private Button myPreviousButton;
    private Button myNextButton;
    private Button myStartButton;
    private Button myPauseButton;
    private Button myDeleteButton;
    private TextView myDateTime;
    private Thread myDateTimeThread;
    private ResponseReceiver myResponseReceiver;

    private Set<String> visitedBitMaps;
    private boolean myFullScreen;

    private Animation slide_in_left, slide_in_right, slide_out_left, slide_out_right;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_show);
        myFlipper = (ViewFlipper) findViewById(R.id.photoFlipper);
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

        visitedBitMaps = new HashSet<>();
        myFullScreen = false;
        setUpDateTimeTextView();
        S3PhotoIntentService.startActionFetchS3(this, null, null);

        myPauseButton.setOnClickListener(this);
        myNextButton.setOnClickListener(this);
        myStartButton.setOnClickListener(this);
        myPauseButton.setOnClickListener(this);
        myDeleteButton.setOnClickListener(this);

        slide_in_left = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        slide_in_right = AnimationUtils.loadAnimation(this, R.anim.silde_in_right);
        slide_out_left = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        slide_out_right = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);

        IntentFilter statusIntentFilter = new IntentFilter(Constants.BROADCAST_ACTION);
        statusIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        myResponseReceiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(myResponseReceiver, statusIntentFilter);
    }

    @Override
    public void onDestroy() {
        if (myResponseReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(myResponseReceiver);
            myResponseReceiver = null;
        }
        myDateTimeThread.interrupt();
        super.onDestroy();
    }

    private void setUpDateTimeTextView() {
        myDateTime = (TextView) findViewById(R.id.slide_show_display_date);
        myDateTime.setTextColor(getResources().getColor(R.color.app_green));
        myDateTime.setTypeface(Typeface.DEFAULT_BOLD);
        myDateTimeThread = new Thread(new DateTimeRunner());
        myDateTimeThread.start();
    }

    public void addPhoto(Bitmap bitmap) {
        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bitmap);
        myFlipper.addView(imageView);
    }

    private void dispatchTakePhotoIntent() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            NoCameraDialogFragment noCameraDialog = new NoCameraDialogFragment();
            noCameraDialog.show(getFragmentManager(), "no_camera_dialog");
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

    @Override
    protected void onStop() {
        myDateTimeThread.interrupt();
        super.onStop();
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
            case R.id.take_photo:
                dispatchTakePhotoIntent();
                break;
            case R.id.upload_photo:
                uploadPhotoFromGallery();
                return true;
            case R.id.update_slide_show:
                doFetchPhotoWork();
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Bitmap resizeBitmap(Bitmap bitmap) {
        double width = bitmap.getWidth();
        double height = bitmap.getHeight();
        System.out.println("width: " + width + " height: " + height);//TODO: delete
        LinearLayout panel = (LinearLayout) findViewById(R.id.slide_show_panel);
//        panel.getViewTreeObserver()
        double ratio = Math.min(panel.getWidth() / width, panel.getHeight() / height);
        int newWidth = (int) (ratio * width);
        int newHeight = (int) (ratio * height);
        System.out.println("panel width: " + panel.getWidth() + " height: " + panel.getHeight());//TODO: delete
        System.out.println("ratio: " + ratio + " newWidth: " + newWidth + " newHeight: " + newHeight);//TODO: delete
        bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        return bitmap;
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
                myFlipper.stopFlipping();
                myFlipper.setInAnimation(slide_in_right);
                myFlipper.setOutAnimation(slide_out_left);
                myFlipper.showNext();
                break;
            case R.id.previousSlideButton:
                myFlipper.stopFlipping();
                myFlipper.setInAnimation(slide_in_left);
                myFlipper.setOutAnimation(slide_out_right);
                myFlipper.showPrevious();
                break;
            case R.id.startSlideButton:
                if (!myFlipper.isFlipping()) {
                    myFlipper.setFlipInterval(FLIP_INTERVAL);
                    myFlipper.startFlipping();
                }
                break;
            case R.id.pauseSlideButton:
                if (myFlipper.isFlipping()) {
                    myFlipper.stopFlipping();
                }
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
                    myDateTime.setText(formatted);
                } catch (Exception e) {
                }
            }
        });
    }

    public void doFetchPhotoWork() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                //TODO: delete dummy content
//                if (!fetched) {
//                    ImageView image1 = new ImageView(getApplicationContext());
//                    Bitmap bitmap1 = resizeBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.sanmay_dog));
//                    image1.setImageBitmap(bitmap1);
//                    myFlipper.addView(image1);
//                    ImageView image2 = new ImageView(getApplicationContext());
//                    Bitmap bitmap2 = resizeBitmap(BitmapFactory.decodeResource(getApplicationContext().getResources(), R.drawable.steve));
//                    image2.setImageBitmap(bitmap2);
//                    myFlipper.addView(image2);
//                }

                Map<String, Bitmap> bitmapMap = S3PhotoIntentService.getBitmapMap();
                for (Bitmap bitmap : bitmapMap.values()) {
                    if (!visitedBitMaps.contains(bitmap)) {
                        addPhoto(bitmap);
                    }
                }
                myFlipper.setAutoStart(true);
            }
        });
    }

    public void setFullScreen(boolean fullscreen) {
        getWindow().setFlags(
                fullscreen ? WindowManager.LayoutParams.FLAG_FULLSCREEN : 0,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        myFullScreen = fullscreen;

        if (Build.VERSION.SDK_INT >= 11) {
            // Sets the View to be "low profile". Status and navigation bar icons will be dimmed
            int flag = fullscreen ? View.SYSTEM_UI_FLAG_LOW_PROFILE : 0;
            if (Build.VERSION.SDK_INT >= 14 && fullscreen) {
                // Hides all of the navigation icons
                flag |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }
//           Applies the settings to the screen View
            View mainView = findViewById(R.id.slide_show_panel);
            mainView.setSystemUiVisibility(flag);

            if (fullscreen) {
                this.getActionBar().hide();
            } else {
                this.getActionBar().show();
            }
        }
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
                    doFetchPhotoWork();
            }
        }
    }
}
