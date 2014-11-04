package com.bmh.ms101.jobs;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.util.IOUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 */
public class S3FetchPhotoIntentService extends IntentService {

    private static final String ACTION_UPLOAD_S3 = "com.bmh.ms101.jobs.action.UPLOAD_S3";
    private static final String ACTION_FETCH_S3 = "com.bmh.ms101.jobs.action.FETCH_S3"; // ?? no such class ??

    private static final String EXTRA_PARAM1 = "com.bmh.ms101.jobs.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "com.bmh.ms101.jobs.extra.PARAM2";
    /**
     * Consider putting the credential elsewhere:
     *  1. Try Amazon Cognito
     *  2. Try creating a user for app access specifically: dynamics ones vs static ones
     *      http://docs.aws.amazon.com/IAM/latest/APIReference/API_CreateAccessKey.html
     *      http://docs.aws.amazon.com/AWSAndroidSDK/latest/javadoc/
     *      http://docs.aws.amazon.com/mobile/sdkforandroid/developerguide/s3transfermanager.html
     */
    private static final String AWS_KEY = "AKIAJ4AM2SBTB6TRYJDQ";
    private static final String AWS_SECRET = "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";
    private static final String BUCKET_NAME = "seniorpandadevnew"; // think about alternative

    private static List<String> KeyList;
    private static Map<String, Bitmap> BitmapMap;

    // singleton
    private static AmazonS3Client s3Client = null;
    public static synchronized AmazonS3Client getS3ClientInstance() {
        if (null == s3Client) {
            s3Client = new AmazonS3Client(new BasicAWSCredentials(AWS_KEY, AWS_SECRET));
        }
        return s3Client;
    }

    //purpose of this one???
    /**
     * Starts this service to perform action X with the given parameters.
     * If the service is already performing a task this action will be queued:
     *      ex: checking and downloading the recent added photos
     * @see IntentService
     */
    public static void startActionFetchS3(Context context, String param1, String param2) {
        Intent intent = new Intent(context, S3FetchPhotoIntentService.class);
        intent.setAction(ACTION_FETCH_S3);
        intent.putExtra(EXTRA_PARAM1, param1);
        intent.putExtra(EXTRA_PARAM2, param2);
        context.startService(intent);
    }

    public S3FetchPhotoIntentService() {
        super("S3FetchPhotoJob");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FETCH_S3.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionFetchS3(AWS_KEY, AWS_SECRET);

            } else if (ACTION_UPLOAD_S3.equals(action)) {
                final String param1 = intent.getStringExtra(EXTRA_PARAM1);
                final String param2 = intent.getStringExtra(EXTRA_PARAM2);
                handleActionUploadS3(AWS_KEY, AWS_SECRET);
            }
        }
    }

    /**
     * Handle action UploadS3 in the provided background thread with the provided parameters.
     * @param awsKey, awsSecret
     */
    private void handleActionUploadS3(String awsKey, String awsSecret) {
        // 1. check against database 2. use Amazon's credential
    }

    /**
     * Handle action FetchS3 in the provided background thread with the provided parameters.
     */
    private void handleActionFetchS3(String key, String secret) {
        try {
//            AWS_KEY = key; AWS_SECRET = key; // in case those credentials are fed from outsides. i.e. properties file
//            AmazonS3 s3 = new AmazonS3Client(AWSCredentials);
            KeyList = new ArrayList<String>(); // for later usage;
            BitmapMap = new ConcurrentHashMap<String, Bitmap>();

            AmazonS3Client s3Client = getS3ClientInstance();

            List<S3ObjectSummary> summaries = s3Client.listObjects(BUCKET_NAME).getObjectSummaries();
            String[] keysNames = new String[summaries.size()]; // think about update issue:

            for(int i = 0; i < keysNames.length; i++) {
                keysNames[i] = summaries.get(i).getKey();
            }

            for (String picName: keysNames){
                BitmapMap.put(picName, fetchImageAsBitMap(BUCKET_NAME, picName));
            }
        }
        catch (Exception T) {
            throw new UnsupportedOperationException("Cannot handle Fetch S3 Action");
        }
    }

    /**
     * Fetch Image from S3 in the format of Bitmap to be displayed
     * @return
     */
    private Bitmap fetchImageAsBitMap(String bucketName, String picName) throws IOException{
        S3ObjectInputStream content = s3Client.getObject(bucketName, picName).getObjectContent();
        byte[] bytes = IOUtils.toByteArray(content);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        return bitmap;
    }

    /**
     * Getter for the ConcurrentHashMap
     * @return
     */
    public static Map<String, Bitmap> getBitmapMap() {
        return BitmapMap;
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
