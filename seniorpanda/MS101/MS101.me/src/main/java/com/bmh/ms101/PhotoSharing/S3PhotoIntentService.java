package com.bmh.ms101.PhotoSharing;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.bmh.ms101.Constants;
import com.bmh.ms101.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class S3PhotoIntentService extends IntentService {
    private User mUser;
    private static final int CACHE_SIZE = 10;

    private static final String ACTION_UPLOAD_S3 = "com.bmh.ms101.jobs.action.UPLOAD_S3";
    private static final String ACTION_FETCH_S3 = "com.bmh.ms101.jobs.action.FETCH_S3";
    private static final String ACTION_DELETE_S3 = "com.bmh.ms101.jobs.action.DELETE_S3";

    private static final String IMAGE_NAME = "com.bmh.ms101.jobs.extra.IMAGE_NAME";
    private static final String UPLOAD_MAP = "com.bmh.ms101.jobs.extra.UPLOAD_MAP";
    private static final String USER_NAME = "com.bmh.ms101.jobs.extra.USER_NAME";

    private static final String BUCKET_NAME = "seniorpandadevnew"; // use the User.userName
    private static final String FOLDER_NAME = "PhotoSharing";

    private static List<String> KeyList = new ArrayList<String>(); // for later usage;
    private static Map<String, Bitmap> myBitmapMap = new ConcurrentHashMap<String, Bitmap>();
    private Queue<String> myPicNames = new LinkedList<>();
    private static Context myContext = null;

    private static CognitoCachingCredentialsProvider credentialsProvider;
    // creating credential using COGNITO
    private static AmazonS3Client s3Client = null;

    public static synchronized AmazonS3Client getS3ClientInstance() {
        if (null == s3Client) {
//            s3Client = new AmazonS3Client(new BasicAWSCredentials(AWS_KEY, AWS_SECRET));
            s3Client = new AmazonS3Client(new CognitoCachingCredentialsProvider(
                    myContext, // get the context for the current activity
                    "123492269978",
                    "us-east-1:0bf55fd1-baf0-4676-a290-ac9f07623024",
                    "arn:aws:iam::123492269978:role/Cognito_SeniorPandaNewUnauth_DefaultRole",
                    "arn:aws:iam::123492269978:role/Cognito_SeniorPandaNewAuth_DefaultRole",
                    Regions.US_EAST_1
            ));
        }
        return s3Client;
    }

    //purpose
    public S3PhotoIntentService() {
        super("S3FetchPhotoJob");
    }

    /**
     * Starts this service to perform action X with the given parameters.
     * If the service is already performing a task this action will be queued:
     * ex: checking and downloading the recent added photos
     *
     * @see IntentService
     */
    public static void startActionFetchS3(Context context) {
        if (myContext == null) {
            setContext(context);
        }
        Intent intent = new Intent(context, S3PhotoIntentService.class);
        intent.setAction(ACTION_FETCH_S3);
        context.startService(intent);
    }

    public static void startActionUploadS3(Context context, Map<String, String> imageMap) {
        Intent intent = new Intent(context, S3PhotoIntentService.class);
        intent.setAction(ACTION_UPLOAD_S3);
        intent.putExtra(UPLOAD_MAP, ConcurrentUtils.SerializeHashMap(imageMap));
        setContext(context);
        context.startService(intent);
    }

    public static void startActionDeleteS3(Context context, String imageName) {
        Intent intent = new Intent(context, S3PhotoIntentService.class);
        intent.setAction(ACTION_DELETE_S3);
        intent.putExtra(IMAGE_NAME, imageName);
        setContext(context);
        context.startService(intent);
    }

    private static void setContext(Context context) {
        myContext = context;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            mUser = new User(this);

            final String action = intent.getAction();
            final String bucketName = BUCKET_NAME;
            final String folderName = FOLDER_NAME;

            final String userName = mUser.getStoredUserName();
            switch (action) {
                case ACTION_FETCH_S3:
                    handleActionFetchS3(userName);
                    break;
                case ACTION_UPLOAD_S3:
                    // TODO: automate this information fetching from DB
                    Map<String, String> uploadImgMap =
                            ConcurrentUtils.DeserializeHashMap(intent.getSerializableExtra(UPLOAD_MAP));
                    handleActionUploadS3(uploadImgMap, bucketName, userName);
                    break;
                case ACTION_DELETE_S3:
                    String imageName = (String) intent.getExtras().get(IMAGE_NAME);

                    final String nameKey = imageName;  // the image name contains the folder name already
                    Log.w("Delete photo", imageName);

//                    final String nameKey = folderName + Constants.SLASH + imageName;
//                    Log.w(this.getClass().getName(), "Delete photo " + imageName);

                    handleActionDeleteS3(nameKey, userName);
                    break;
            }
        }
    }

    private void handleActionDeleteS3(String nameKey, String bucketName) {
        try {
            AmazonS3Client s3Client = getS3ClientInstance();
            s3Client.deleteObject(new DeleteObjectRequest(bucketName, nameKey));
            Log.w(this.getClass().getName(), "bName: " + bucketName + " and nameKey is " + nameKey);
        } catch (Throwable T) {
            //T.printStackTrace();
            //throw new UnsupportedOperationException("Cannot handle Delete S3 Action");
        }
    }

    public static void clearPhotos() {
        myBitmapMap.clear();
    }


    /**
     * Handle action UploadS3 in the provided background thread with the provided parameters.
     */
    private void handleActionUploadS3(Map<String, String> uploadImageMap, String bucketName, String folderName) {
        try {
            AmazonS3Client s3Client = getS3ClientInstance();
            s3Client.listBuckets();
            for (Map.Entry<String, String> entry : uploadImageMap.entrySet()) {
                PutObjectRequest por = new PutObjectRequest(bucketName, folderName + Constants.SLASH + entry.getKey(), new java.io.File(entry.getValue()));
                s3Client.putObject(por);
            }
        } catch (Exception T) {
            throw new UnsupportedOperationException("Cannot handle Upload S3 Action");
        }
    }


    /**
     * Handle action FetchS3 in the provided background thread with the provided parameters.
     *
     * @param folderName is derived from the userName
     */
    private void handleActionFetchS3(String folderName) {
//            AWS_KEY = key; AWS_SECRET = key; // in case those credentials are fed from outsides. i.e. properties file
//            AmazonS3 s3 = new AmazonS3Client(AWSCredentials);
        AmazonS3Client s3Client = getS3ClientInstance();
        Log.w("folderName here is ", folderName);
        List<S3ObjectSummary> summaries = s3Client.listObjects(BUCKET_NAME, folderName + Constants.SLASH).getObjectSummaries();

        String[] keysNames = new String[summaries.size()]; // think about update issue:

        for (int i = 0; i < keysNames.length; i++) {
            keysNames[i] = summaries.get(i).getKey();
            //Log.w(this.getClass().getName(), summaries.get(i).getKey());
        }

        for (String picName : keysNames) {
            if (!checkEmptyDirectory(picName)) {
                try {
                    if (!myBitmapMap.containsKey(picName)) {
                        if (myBitmapMap.size() > CACHE_SIZE) {
                            String name = myPicNames.remove();
                            myBitmapMap.remove(name);
                            sendRemovePictureIntent(name);
                        }
                        Bitmap bitmap = fetchImageAsBitMap(BUCKET_NAME, picName);
                        myBitmapMap.put(picName, bitmap);
                        Intent fetchedIntent = new Intent(Constants.ACTION_FETCHED_PHOTO);
                        fetchedIntent.putExtra(Constants.INTENT_FETCHED_PHOTO, bitmap);
                        fetchedIntent.putExtra(Constants.INTENT_PHOTO_NAME, picName);
                        LocalBroadcastManager.getInstance(this).sendBroadcast(fetchedIntent);
                        Log.w(this.getClass().getName(), "put in picture with name " + picName);
                    } else {
                        Log.w(this.getClass().getName(), "already contains picture with name " + picName);
                    }
                } catch (Exception e) {
                    Log.w(this.getClass().getName(), "current picName " + picName + " not found in fetching");
                    continue;
                }
            }
        }
    }

    private void sendRemovePictureIntent(String imageName) {
        Log.w(this.getClass().getName(), "remove picture with name " + imageName);
        Intent removeIntent = new Intent(Constants.ACTION_DELETED_PHOTO);
        removeIntent.putExtra(Constants.INTENT_PHOTO_NAME, imageName);
        LocalBroadcastManager.getInstance(this).sendBroadcast(removeIntent);
    }

    // check if it is the empty directory with /
    private boolean checkEmptyDirectory(String picName) {
        return (picName.charAt(picName.length() - 1) == Constants.SLASH.charAt(0));
    }

    /**
     * Fetch Image from S3 in the format of Bitmap to be displayed
     *
     * @return
     */
    private Bitmap fetchImageAsBitMap(String bucketName, String picName) throws IOException {
        S3ObjectInputStream content = s3Client.getObject(bucketName, picName).getObjectContent();
        byte[] bytes = IOUtils.toByteArray(content);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return bitmap;
    }

// //thinking of improving the sign-in:
    // http://docs.aws.amazon.com/mobile/sdkforandroid/developerguide/s3transfermanager.html

//    public static AWSCredentialsProvider getCredProvider(Context appContext) {
//        if(sCredProvider == null) {
//            sCredProvider = new CognitoCachingCredentialsProvider(
//                    appContext,
//                    Constants.AWS_ACCOUNT_ID,
//                    Constants.COGNITO_POOL_ID,
//                    Constants.COGNITO_ROLE_UNAUTH,
//                    null,
//                    Regions.US_EAST_1);
//            sCredProvider.refresh();
//        }
//        return sCredProvider;
//    }
}


/**
 * CognitoSyncManager syncClient = new CognitoSyncManager(
 * myActivity.getContext(),
 * "us-east-1:0bf55fd1-baf0-4676-a290-ac9f07623024",
 * Regions.US_EAST_1,
 * cognitoProvider);
 * <p/>
 * Dataset dataset = syncClient.openOrCreateDataset('myDataset');
 * dataset.put("myKey", "myValue");
 * dataset.synchronize(this, syncCallback);
 */
