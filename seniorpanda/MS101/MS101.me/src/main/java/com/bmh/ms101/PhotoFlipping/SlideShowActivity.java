package com.bmh.ms101.PhotoFlipping;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.text.InputType;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
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
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.bmh.ms101.Constants;
import com.bmh.ms101.MS101;
import com.bmh.ms101.MainActivity;
import com.bmh.ms101.PhotoSharing.S3PhotoIntentService;
import com.bmh.ms101.R;
import com.bmh.ms101.User;
import com.bmh.ms101.Util;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SlideShowActivity extends Activity implements OnClickListener {

    private static final Integer FLIP_INTERVAL = 20000;
    private static final Integer SELECT_PHOTO_FROM_GALLERY_REQUEST = 100;
    private static final Integer TAKE_PHOTO_REQUEST = 102;
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_THRESHOLD_VELOCITY = 200;
    private static final int TIME_UPDATE_INTERVAL = 10000;
    private static final int DELETE_PHOTO_INTERVAL = 100000;
    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";
    private static final String DATE_TIME_FORMAT = "EEE, MMM dd yyy HH:mm a";

    private ProgressBar myProgressBar;
    private ViewFlipper myFlipper;
    private TextView myDateTextView;
    private TextView myTimeTextView;
    private FetchReceiver myFetchReceiver;
    private DeleteReceiver myDeleteReceiver;
    private GestureDetector myTouchDetector;
    private Animation slide_in_left, slide_in_right, slide_out_left, slide_out_right;
    private Map<Integer, String> counterToImageNameMap;
    private int imageCounter = 0;
    private String myCurrentPhotoName = null;
    private String myCurrentPhotoPath = null;
    private Integer deleteTimes = 0;
    private boolean deletingPhoto = false;
    private boolean updatingTime = false;

    private String defaultFamShareName = null;

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
        setTitle(R.string.title_activity_photo_slideshow);
        initButton(R.id.uploadPhotoButton, Constants.COLOR_GREEN);
        initButton(R.id.takePhotoButton, Constants.COLOR_GREEN);
        initButton(R.id.startSlideButton, Constants.COLOR_GREEN);
        initButton(R.id.pauseSlideButton, Constants.COLOR_GREEN);
        initButton(R.id.deletePhotoButton, Constants.COLOR_GREEN);
        initButton(R.id.slide_show_weather_change_city, Constants.COLOR_GREEN);
        initButton(R.id.temperature_convert_button, Constants.COLOR_GREEN);
        counterToImageNameMap = new HashMap<Integer, String>();
        slide_in_left = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        slide_in_right = AnimationUtils.loadAnimation(this, R.anim.silde_in_right);
        slide_out_left = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
        slide_out_right = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
        myTouchDetector = new GestureDetector(myFlipper.getContext(), new SwipeGestureDetector());
        myDateTextView = (TextView) findViewById(R.id.slide_show_display_date);
        myTimeTextView = (TextView) findViewById(R.id.slide_show_display_time);
        initTextView(myDateTextView);
        initTextView(myTimeTextView);

        defaultFamShareName = getIntent().getExtras().getString(MainActivity.DEFAULT_FAMSHARENAME);
    }

    private void initDeletePhotoThread() {
        new DeletePhotoAsync().execute();
    }

    private void registerReceiver() {
        IntentFilter actionFetchedIntentFilter = new IntentFilter(Constants.ACTION_FETCHED_PHOTO);
        actionFetchedIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        IntentFilter actionDeletedIntentFilter = new IntentFilter(Constants.ACTION_DELETED_PHOTO);
        actionDeletedIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        myFetchReceiver = new FetchReceiver();
        myDeleteReceiver = new DeleteReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(myFetchReceiver, actionFetchedIntentFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(myDeleteReceiver, actionDeletedIntentFilter);
    }

    private void initTextView(TextView textView) {
        textView.setTextColor(getResources().getColor(R.color.white));
        textView.setTypeface(Typeface.DEFAULT_BOLD);
    }

    private void initButton(int resId, String color) {
        Button button = (Button) findViewById(resId);
        switch (color) {
            case Constants.COLOR_GREEN:
                Util.makeGreen(button, this);
                break;
        }
        button.setOnClickListener(this);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver();
        S3PhotoIntentService.clearPhotos();
        deletingPhoto = false;
        updatingTime = false;
        super.onDestroy();
    }

    public void addPhoto(Bitmap bitmap, String imageName) {
        if (!counterToImageNameMap.containsValue(imageName)) {
            ImageView imageView = new ImageView(this);
            imageView.setTag(imageCounter);
            counterToImageNameMap.put(imageCounter, imageName);
            imageView.setImageBitmap(bitmap);
            myFlipper.addView(imageView);
            imageCounter++;
        }
    }

    private void dispatchTakePhotoIntent() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Log.w(this.getClass().getName(), "The current app has no camera");
            showInfoDialog(R.string.dialog_alert_title, R.string.no_camera_message, R.string.dialog_ok_button_text);
            return;
        }
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {
            ImageFilePath imageFilePath = null;
            try {
                imageFilePath = createImageFile();
            } catch (IOException e) {
                Log.w(this.getClass().getName(), "IOException occurs while dispatching take photo intent!");
                showInfoDialog(R.string.dialog_alert_title, R.string.fail_create_image_file, R.string.dialog_ok_button_text);
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

    private void showToast(String text, int duration) {
        Toast.makeText(getApplicationContext(), text, duration).show();
    }

    private void showInfoDialog(int title, int inputText, int buttonText) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        final TextView input = new TextView(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(inputText);
        builder.setView(input);
        builder.setPositiveButton(buttonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    @Override
    protected void onStop() {
        unregisterReceiver();
        stopFlipping();
        S3PhotoIntentService.clearPhotos();
        deletingPhoto = false;
        updatingTime = false;
        super.onStop();
    }

    private void unregisterReceiver() {
        if (myFetchReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(myFetchReceiver);
            myFetchReceiver = null;
        }
        if (myDeleteReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(myDeleteReceiver);
            myDeleteReceiver = null;
        }
    }

    @Override
    protected void onResume() {
        deletingPhoto = true;
        updatingTime = true;
        initDateTimeThread();
        initDeletePhotoThread();
        registerReceiver();
        S3PhotoIntentService.startActionFetchS3(this);
        showToast("Start loading pictures", Toast.LENGTH_SHORT);
        startFlipping();
        if (myFlipper.getChildCount() == 0) {
            myProgressBar = (ProgressBar) findViewById(R.id.slide_show_progress_bar);
            myProgressBar.setIndeterminate(true);
            myProgressBar.setBackgroundColor(getResources().getColor(R.color.app_green));
            myProgressBar.setVisibility(View.VISIBLE);
        }
        super.onResume();
    }

    private void initDateTimeThread() {
        new DateTimeAsync().execute();
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
            case R.id.update_slide_show:
                S3PhotoIntentService.startActionFetchS3(this);
                showToast("Start loading pictures", Toast.LENGTH_SHORT);
                return true;
            case R.id.change_family_sharing: // change the bucket for family sharing
                S3PhotoIntentService.clearPhotos();
                // Create and show the dialog
                // Get our views that will be used in the dialog
                LinearLayout dialogView = (LinearLayout) ((LayoutInflater) this.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.dialog_bucket_change, null);
                final EditText famShareName = (EditText) dialogView.findViewById(R.id.current_bucket);
                famShareName.setText(defaultFamShareName);
                AlertDialog.Builder builder = new AlertDialog.Builder(SlideShowActivity.this);
                builder.setTitle(R.string.change_bucket_message)
                        .setView(dialogView)
                        .setPositiveButton(R.string.change, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Record the email and password that the user provided
                                User innerUser = new User(MS101.getInstance());
                                innerUser.recordFamShareName(famShareName.getText().toString());
                                deleteAllPhotos();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
                S3PhotoIntentService.startActionFetchS3(this);
                showToast("Family photo sharing bucket changed", Toast.LENGTH_LONG);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAllPhotos() {
        myFlipper.removeAllViews();
        counterToImageNameMap.clear();
        deleteTimes = 0;
        imageCounter = 0;
    }

    private void showChangeCityDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_change_city_title);
        final EditText cityField = new EditText(this);
        final EditText countryField = new EditText(this);
        cityField.setHint(R.string.city_field_hint);
        countryField.setHint(R.string.country_field_hint);
        cityField.setInputType(InputType.TYPE_CLASS_TEXT);
        countryField.setInputType(InputType.TYPE_CLASS_TEXT);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.addView(cityField);
        layout.addView(countryField);
        builder.setView(layout);
        builder.setPositiveButton("Go", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = cityField.getText().toString().trim() + "," + countryField.getText().toString().trim();
                changeCity(text);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
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
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), SELECT_PHOTO_FROM_GALLERY_REQUEST);
    }

    private ImageFilePath createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        myCurrentPhotoName = JPEG_FILE_PREFIX + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(myCurrentPhotoName, JPEG_FILE_SUFFIX, storageDir);
//        String path = "file:" + image.getAbsolutePath();
        String path = image.getAbsolutePath();
        ImageFilePath imageFilePath = new ImageFilePath(image, path);
        myCurrentPhotoPath = image.getAbsolutePath();
        Log.w(this.getClass().getName(), "Create image path for camera photo: " + myCurrentPhotoPath);
        return imageFilePath;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_PHOTO_FROM_GALLERY_REQUEST) {
            if (resultCode == RESULT_OK) {
                Uri imageUri = data.getData();
                Log.w(this.getClass().getName(), "Uri from gallery: " + imageUri.toString());
                Map<String, String> imageMap = new HashMap<String, String>();

                String imagePath = null;
                if (isMediaDocument(imageUri)) {
                    imagePath = getPathFromURI(imageUri);
                } else if (isExternalStorageDocument(imageUri)) {
                    imagePath = getRealPathFromURI(imageUri);
                }
                Log.w(this.getClass().getName(), "Received image path from gallery: " + imagePath);
                String imageName = imagePath.substring(imagePath.lastIndexOf("/") + 1);
                imageMap.put(imageName, imagePath);

                S3PhotoIntentService.startActionUploadS3(this, imageMap);
            }
        } else if (requestCode == TAKE_PHOTO_REQUEST) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                Bitmap bitmap = (Bitmap) extras.get("data");
                addPhoto(bitmap, myCurrentPhotoName);
                Map<String, String> imageMap = new HashMap<String, String>();
                imageMap.put(myCurrentPhotoName, myCurrentPhotoPath);
                S3PhotoIntentService.startActionUploadS3(this, imageMap);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private String getPathFromURI(Uri uri) {
        final String docId = DocumentsContract.getDocumentId(uri);
        final String[] split = docId.split(":");
        final String type = split[0];
        Uri contentUri = null;
        if ("image".equals(type)) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
        final String selection = "_id=?";
        final String[] selectionArgs = new String[]{
                split[1]
        };
        return getDataColumn(this, contentUri, selection, selectionArgs);
    }

    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };
        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    private boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }


    private String getRealPathFromURI(Uri uri) {
        String res = null;
        String[] projection = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(this, uri, projection, null, null, null);
        Cursor cursor = loader.loadInBackground();
        if (cursor != null) {
            cursor.moveToFirst();
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            Log.w(this.getClass().getName(), "column index is " + column_index);
            return cursor.getString(column_index);
        }
        cursor.close();
        return res;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.takePhotoButton:
                dispatchTakePhotoIntent();
                break;
            case R.id.uploadPhotoButton:
                uploadPhotoFromGallery();
                break;
            case R.id.startSlideButton:
                startFlipping();
                break;
            case R.id.pauseSlideButton:
                stopFlipping();
                break;
            case R.id.slide_show_weather_change_city:
                showChangeCityDialog();
                break;
            case R.id.temperature_convert_button:
                WeatherFragment weatherFragment = (WeatherFragment) getFragmentManager()
                        .findFragmentById(R.id.slide_show_weather_display);
                String unit = weatherFragment.changeUnit();
                Button changeUnitButton = (Button) findViewById(R.id.temperature_convert_button);
                if (unit.equals(WeatherFragment.CELSIUS_DEGREE)) {
                    changeUnitButton.setText(R.string.fahren_unit);
                } else {
                    changeUnitButton.setText(R.string.celsius_unit);
                }
                break;
            case R.id.deletePhotoButton:
                ImageView currentView = (ImageView) myFlipper.getCurrentView();
                int counter = ((Integer) currentView.getTag()).intValue();
                String imageName = counterToImageNameMap.get(counter);
                counterToImageNameMap.remove(counter);
                myFlipper.removeView(currentView);
                S3PhotoIntentService.startActionDeleteS3(this, imageName);
                break;
        }
    }

    private void stopFlipping() {
        Log.w(this.getClass().getName(), "ViewFlipper stops flipping");
        showToast("Slideshow stops flipping", Toast.LENGTH_SHORT);
        myFlipper.stopFlipping();
        myFlipper.setAutoStart(false);
    }

    private void startFlipping() {
        myFlipper.setAutoStart(true);
        myFlipper.setFlipInterval(FLIP_INTERVAL);
        if (myFlipper.isAutoStart() && !myFlipper.isFlipping()) {
            Log.w(this.getClass().getName(), "ViewFlipper starts to flip");
            showToast("Slideshow starts to flip", Toast.LENGTH_SHORT);
            myFlipper.setInAnimation(slide_in_right);
            myFlipper.setOutAnimation(slide_out_left);
            myFlipper.startFlipping();
        }
    }

    private void showPreviousInFlipper() {
        Log.w(this.getClass().getName(), "ViewFlipper shows previous");
        stopFlipping();
        myFlipper.setInAnimation(slide_in_left);
        myFlipper.setOutAnimation(slide_out_right);
        myFlipper.showPrevious();
    }

    private void showNextInFlipper() {
        Log.w(this.getClass().getName(), "ViewFlipper shows next");
        stopFlipping();
        myFlipper.setInAnimation(slide_in_right);
        myFlipper.setOutAnimation(slide_out_left);
        myFlipper.showNext();
    }

    public void doUpdateTimeWork(final String date, final String time) {
        runOnUiThread(new Runnable() {
            public void run() {
                myDateTextView.setText(date);
                myTimeTextView.setText(time);
            }
        });
    }

    public void doFetchPhotoWork(final Bitmap bitmap, final String imageName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                addPhoto(bitmap, imageName);
                startFlipping();
                if (myProgressBar.isShown()) {
                    myProgressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception e) {

        }
    }

    public class DeletePhotoAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            while (deletingPhoto) {
                sleep(DELETE_PHOTO_INTERVAL);
                synchronized (deleteTimes) {
                    while (deleteTimes > 0 && myFlipper.getChildCount() > 0) {
                        myFlipper.removeViewAt(0);
                        deleteTimes--;
                    }
                    try {
                        Thread.sleep(TIME_UPDATE_INTERVAL);
                    } catch (InterruptedException e) {
                        Log.w(this.getClass().getName(), "DeletePhotoFromFlipperThread is interrupted");
                        Thread.currentThread().interrupt();
                    }
                }
            }
            return null;
        }
    }

    public class DateTimeAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            while (updatingTime) {
                Calendar c = Calendar.getInstance();
                SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_TIME_FORMAT);
                String formatted = dateFormat.format(c.getTime());
                doUpdateTimeWork(formatted.substring(0, 10), formatted.substring(11));
                sleep(TIME_UPDATE_INTERVAL);
            }
            return null;
        }
    }

    public class FetchReceiver extends BroadcastReceiver {
        private FetchReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.ACTION_FETCHED_PHOTO:
                    Bundle extras = intent.getExtras();
                    Bitmap bitmap = (Bitmap) extras.get(Constants.INTENT_FETCHED_PHOTO);
                    String imageName = (String) extras.get(Constants.INTENT_PHOTO_NAME);
                    Log.w(this.getClass().getName(), "Received photo from Intent Service " + imageName);
                    doFetchPhotoWork(bitmap, imageName);
            }
        }
    }

    public class DeleteReceiver extends BroadcastReceiver {
        private DeleteReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Constants.ACTION_DELETED_PHOTO:
                    String name = (String) intent.getExtras().get(Constants.INTENT_PHOTO_NAME);
                    Log.w(this.getClass().getName(), "Deleted photo from Intent Service " + name);
                    int counter = -1;
                    for (Integer i : counterToImageNameMap.keySet()) {
                        if (counterToImageNameMap.get(i).equals(name)) {
                            counter = i;
                            break;
                        }
                    }
                    if (counter != -1) {
                        counterToImageNameMap.remove(counter);
                    }
                    synchronized (deleteTimes) {
                        deleteTimes++;
                    }
            }
        }
    }

    public class SwipeGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            try {
                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    showNextInFlipper();
                } else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                    showPreviousInFlipper();
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }

}
