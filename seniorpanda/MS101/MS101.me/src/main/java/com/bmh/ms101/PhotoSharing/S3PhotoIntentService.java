package com.bmh.ms101.PhotoSharing;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.bmh.ms101.Constants;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class S3PhotoIntentService extends IntentService {

    private static final String ACTION_UPLOAD_S3 = "com.bmh.ms101.jobs.action.UPLOAD_S3";
    private static final String ACTION_FETCH_S3 = "com.bmh.ms101.jobs.action.FETCH_S3";
    private static final String ACTION_DELETE_S3 = "com.bmh.ms101.jobs.action.DELETE_S3";

    private static final String IMAGE_NAME = "com.bmh.ms101.jobs.extra.IMAGE_NAME";
    private static final String UPLOAD_MAP = "com.bmh.ms101.jobs.extra.UPLOAD_MAP";

    private static final String AWS_KEY = "";
    private static final String AWS_SECRET = "";
    private static final String BUCKET_NAME = "seniorpandadevnew"; // use the User.userName
    private static final String FOLDER_NAME = "PhotoSharing";

    private static List<String> KeyList = new ArrayList<String>(); // for later usage;
    private static Map<String, Bitmap> myBitmapMap = new ConcurrentHashMap<String, Bitmap>();
    private static Context myContext = null;

    private static CognitoCachingCredentialsProvider credentialsProvider;
    /**
     * Consider putting the credential elsewhere:
     * 1. Try Amazon Cognito4
     * 2. Try creating a user for app access specifically: dynamics ones vs static ones
     * http://docs.aws.amazon.com/IAM/latest/APIReference/API_CreateAccessKey.html
     * http://docs.aws.amazon.com/AWSAndroidSDK/latest/javadoc/
     * http://docs.aws.amazon.com/mobile/sdkforandroid/developerguide/s3transfermanager.html
     */
    // creating credential using COGNITO

    private static AmazonS3Client s3Client = null;

    public static synchronized AmazonS3Client getS3ClientInstance() {
        if (null == s3Client) {
            s3Client = new AmazonS3Client(new BasicAWSCredentials(AWS_KEY, AWS_SECRET));
//            s3Client = new AmazonS3Client(new CognitoCachingCredentialsProvider(
//                    myContext, // get the context for the current activity
//                    "123492269978",
//                    "us-east-1:0bf55fd1-baf0-4676-a290-ac9f07623024",
//                    "arn:aws:iam::123492269978:role/Cognito_SeniorPandaNewUnauth_DefaultRole",
//                    "arn:aws:iam::123492269978:role/Cognito_SeniorPandaNewAuth_DefaultRole",
//                    Regions.US_EAST_1
//            ));
        }
        return s3Client;
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

    //purpose ???
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

    public static void startActionDeleteS3(Context context, Bitmap bitmap) {
        Intent intent = new Intent(context, S3PhotoIntentService.class);
        intent.setAction(ACTION_DELETE_S3);
        intent.putExtra(IMAGE_NAME, bitmap);
        setContext(context);
        context.startService(intent);
    }

    private static void setContext(Context context) {
        myContext = context;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            final String bucketName = BUCKET_NAME;
            final String folderName = FOLDER_NAME;
            switch (action) {
                case ACTION_FETCH_S3:
                    handleActionFetchS3(AWS_KEY, AWS_SECRET);
                    return;
                case ACTION_UPLOAD_S3:
                    // TODO: automate this information fetching from DB
                    Map<String, String> imageMap =
                            ConcurrentUtils.DeserializeHashMap(intent.getSerializableExtra(UPLOAD_MAP));
                    handleActionUploadS3(AWS_KEY, AWS_SECRET, imageMap, bucketName, folderName);
                    return;
                case ACTION_DELETE_S3:
                    Bitmap bitmap = (Bitmap) intent.getExtras().get(IMAGE_NAME);
                    final String nameKey = folderName + Constants.SLASH + getImageName(bitmap);
                    Log.w("Delete photo", getImageName(bitmap));
                    handleActionDeleteS3(AWS_KEY, AWS_SECRET, nameKey, bucketName);
                    return;
            }
        }
    }

    private void handleActionDeleteS3(String awsKey, String awsSecret, String nameKey, String bucketName) {
        try {
            AmazonS3Client s3Client = getS3ClientInstance();
            s3Client.deleteObject(new DeleteObjectRequest(bucketName, nameKey));
        } catch (Exception T) {
            T.printStackTrace();
            throw new UnsupportedOperationException("Cannot handle Delete S3 Action");
        }
    }


    /**
     * Handle action UploadS3 in the provided background thread with the provided parameters.
     *
     * @param awsKey, awsSecret
     *                Bucket: imageSharing (should not change)
     *                // 1. check against database to determine the 2. use Amazon's credential
     *                caller must have Permission.Write permission to the bucket to upload an object.
     */
    private void handleActionUploadS3(String awsKey, String awsSecret, Map<String, String> imageMap, String bucketName, String folderName) {
        try {
            AmazonS3Client s3Client = getS3ClientInstance();
            s3Client.listBuckets();
            for (Map.Entry<String, String> entry : imageMap.entrySet()) {
                PutObjectRequest por = new PutObjectRequest(bucketName, entry.getKey(), new java.io.File(entry.getValue()));
                s3Client.putObject(por);
            }
        } catch (Exception T) {
            throw new UnsupportedOperationException("Cannot handle Upload S3 Action");
        }
    }


    /**
     * Handle action FetchS3 in the provided background thread with the provided parameters.
     */
    private void handleActionFetchS3(String key, String secret) {
//            AWS_KEY = key; AWS_SECRET = key; // in case those credentials are fed from outsides. i.e. properties file
//            AmazonS3 s3 = new AmazonS3Client(AWSCredentials);
        AmazonS3Client s3Client = getS3ClientInstance();
        List<S3ObjectSummary> summaries = s3Client.listObjects(BUCKET_NAME).getObjectSummaries();
        String[] keysNames = new String[summaries.size()]; // think about update issue:

        for (int i = 0; i < keysNames.length; i++) {
            keysNames[i] = summaries.get(i).getKey();
            Log.w("S3PhotoIntentService", summaries.get(i).getKey());
        }

        for (String picName : keysNames) {
            if (!checkEmptyDirectory(picName)) {
                try {
                    Bitmap bitmap = fetchImageAsBitMap(BUCKET_NAME, picName);
                    myBitmapMap.put(picName, bitmap);
                    Intent fetchedIntent = new Intent(Constants.ACTION_FETCHED_PHOTO).putExtra(Constants.INTENT_FETCHED_PHOTO, bitmap);
                    LocalBroadcastManager.getInstance(this).sendBroadcast(fetchedIntent);
                    Log.w("S3PhotoIntentService", "Bitmap of " + picName + " put in");
                } catch (Exception e) {
                    Log.w("S3PhotoIntentService", "current picName " + picName + " not found in fetching");
                    continue;
                }
            }
        }
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

    private String getImageName(Bitmap bitmap) {
        for (String name : myBitmapMap.keySet()) {
            if (myBitmapMap.get(name).equals(bitmap)) {
                return name;
            }
        }
        return null;
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
