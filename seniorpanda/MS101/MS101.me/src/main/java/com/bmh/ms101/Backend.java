package com.bmh.ms101;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.bmh.ms101.ex.DFCredentialsInvalidException;
import com.bmh.ms101.models.LogDataModel;
import com.bmh.ms101.models.BaseDataModel;
import com.bmh.ms101.models.BaseRecordModel;
import com.bmh.ms101.models.LoginModel;
import com.bmh.ms101.models.MedRecordModel;
import com.bmh.ms101.models.MedicationDataModel;
import com.bmh.ms101.models.StressFactorRecordModel;
import com.bmh.ms101.models.SubscribeDataModel;
import com.bmh.ms101.models.SymptomRecordModel;

import org.droidparts.net.http.HTTPException;
import org.droidparts.net.http.HTTPResponse;
import org.droidparts.net.http.RESTClient2;
import org.droidparts.persist.serializer.JSONSerializer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Handles interaction with the back end. Currently supports the old Google Sheets backend for everyone
 * as well as the new RESTful backend for specified dev accounts.
 */
public class Backend {

/*    // For the Dreamfactory Backend
    public static final String DF_APP_NAME = "ms101-android";
    public static final String DF_URL = "http://54.210.77.98:80/rest";
    public static final String DF_SESSION_SUFIX = "/user/session";
    public static final String DF_DB_SUFIX = "/db";
    public static final String DF_MEDS_TABLE = "ms_medication";
    public static final String DF_SYMP_TABLE = "ms_symptom";
    public static final String DF_STRESS_TABLE = "ms_stress_factor";
    public static final String DF_LATEST_DATA_TABLE = "ms_latest_data";
    public static final String DF_GET_DATA_SUFFIX = DF_DB_SUFIX + "/%s?filter=uid%%3D\"%s\"%%20AND%%20date_created%%20BETWEEN%%20\"%s%%2000%%3A00%%3A00\"%%20AND%%20\"%s%%2023%%3A59%%3A59\"&order=date_created%%20DESC&fields=*";
    public static final String DF_POST_DATA_SUFIX = DF_DB_SUFIX + "/%s?fields=*";
    public static final String VER_MED = "MS101:med:1";
    public static final String VER_SYMP = "MS101:symp:1";
    public static final String VER_STRESS = "MS101:env:1";

    // Support contacts. Tel # is used by widget
    public static final String SUPPORT_PHONE = "9282882430";
    public static final String SUPPORT_EMAIL = "help@ms101.me";*/

    // For the Dreamfactory Backend
    public static final String DF_APP_NAME = "DukeSeniorPanda";
    public static final String DF_URL = "http://54.86.181.99:80/rest";
    public static final String DF_SESSION_SUFIX = "/user/session";
    public static final String DF_DB_SUFIX = "/db";
    public static final String DF_USER_TABLE = "user";
    public static final String DF_SUBSCRIBE_TABLE = "subscribe";
    public static final String DF_SYMPTOM_TABLE = "symptom";
    public static final String DF_MEDICATION_TABLE = "medication";
    public static final String DF_TAKEN_TABLE = "taken";
    public static final String DF_HAS_TABLE = "has";
    public static final String DF_AUDIO_RECORD_TABLE = "audio_record";
    public static final String DF_FAMILY_SHARE_TABLE = "family_sharing";

    public static final String DF_MEDS_TABLE = "ms_medication";
    public static final String DF_SYMP_TABLE = "ms_symptom";
    public static final String DF_STRESS_TABLE = "ms_stress_factor";
    public static final String DF_LATEST_DATA_TABLE = "ms_latest_data";
    public static final String DF_GET_DATA_SUFFIX = DF_DB_SUFIX + "/%s?filter=uid%%3D\"%s\"%%20AND%%20date_created%%20BETWEEN%%20\"%s%%2000%%3A00%%3A00\"%%20AND%%20\"%s%%2023%%3A59%%3A59\"&order=date_created%%20DESC&fields=*";
    public static final String DF_POST_DATA_SUFIX = DF_DB_SUFIX + "/%s?fields=*";
    public static final String DF_PUT_DATA_SUFIX = DF_DB_SUFIX + "/%s?fields=*&related=subscribes_by_uid,medications_by_subscribe";
    public static final String DF_RELATED_SUBSCRIBE_BY_UID = "subscribes_by_uid";
    public static final String DF_RELATED_MED_BY_SUBSCIBE = "medications_by_subscribe";
    public static final String DF_RELATED_TAKEN_BY_UID = "takens_by_uid";
    public static final String DF_RELATED_SYMPTOM_BY_UID = "symptoms_by_uid";
    public static final String DF_RELATED_MEDS_BY_TAKEN = "medications_by_taken";

    public static final String VER_MED = "MS101:med:1";
    public static final String VER_SYMP = "MS101:symp:1";
    public static final String VER_STRESS = "MS101:env:1";

    // Support contacts. Tel # is used by widget
    public static final String SUPPORT_PHONE = "9282882430";
    public static final String SUPPORT_EMAIL = "help@ms101.me";

    private final Context mCtx;
    private final User mUser;

    // For RESTful backends
    private static RESTClient2 mDFRestClient = null;

    /**
     * Create a new Backend object using the given context. Some instance variables are static.
     * @param ctx Context used to create this Backend
     */
    public Backend(Context ctx) {
        mCtx = ctx;
        mUser = new User(ctx);
        if (!mUser.getDFSessionId().equals("")) {
            mDFRestClient = new RESTClient2(mCtx);
            mDFRestClient.setHeader("Accept", "text/plain,application/json");
            mDFRestClient.setHeader("X-DreamFactory-Application-Name", Backend.DF_APP_NAME);
            mDFRestClient.setHeader("X-DreamFactory-Session-Token", mUser.getDFSessionId());
        }
    }

    /**
     * @return User object for current user
     */
    public User getUser() {
        return mUser;
    }

    /**
     * Logs out from all backends
     */
    public void logoutFromAll() {
        if (needsGoogleDisconnect()) disconnectGoogle();
        dreamfactoryLogout();
        Util.toast(mCtx, R.string.toast_logout_success);
    }

    /* ******************************* Methods for Google backend ******************************* */

    /**
     * Checks to see if we need to remove the Google MS101.me accounts from User's phones.
     * @return True if we need to remove the account.
     */
    public boolean needsGoogleDisconnect() {
        return mUser.getPrefsUtil().getPrefString("key_account", null) != null;
    }

    /**
     * Logs out the user. Invalidates the access token, clears all values from the pref file, then
     * shows a toast when finished
     */
    public void disconnectGoogle() {
        Account[] accounts = AccountManager.get(mCtx).getAccountsByType("com.google");
        for (Account account : accounts) {
            if (account.name.endsWith("ms101.me")) {
                AccountManager.get(mCtx).removeAccount(account, new AccountManagerCallback<Boolean>() {
                    @Override
                    public void run(AccountManagerFuture<Boolean> future) {
                        new User(MS101.getInstance()).getPrefsUtil().getPrefs().edit().remove("key_account").commit();
                    }
                }, null);
            }
        }
        mUser.getPrefsUtil().getPrefs().edit().remove("has_done_transfer").remove("key_seen_symp_tutorial")
                .remove("key_sheet").remove("key_seen_main_tutorial").remove("key_list_feed_url").commit();
    }

    /* **************************** Methods for Dreamfactory backend **************************** */

    /**
     * Attempt to get a valid Session ID by logging into the Dreamfactory DSP.
     * @param isRefreshing If we're ignoring the existing token in order to get a new one.
     */
    public void tryDFLogin(boolean isRefreshing) throws Exception {
        // Check to see if we're already logged in, and return if we are
        if (!mUser.getDFSessionId().equals("") && !isRefreshing) {
            return;
        }
        // Init our rest client
        mDFRestClient = new RESTClient2(mCtx);
        mDFRestClient.setHeader("Accept", "text/plain,application/json");
        mDFRestClient.setHeader("X-DreamFactory-Application-Name", Backend.DF_APP_NAME);
        // Try to login
        String email = mUser.getDFEmail();
        String password = mUser.getDFPass();
        // Create a login request
        LoginModel loginRequest = new LoginModel();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        // Serialize login request
        JSONObject login = new JSONSerializer<>(LoginModel.class, mCtx).serialize(loginRequest);
        // Try to login
        HTTPResponse httpResponse = mDFRestClient.post(DF_URL + DF_SESSION_SUFIX, login);
        // Get the session ID if login was a success
        mUser.recordDFSessionId(new JSONObject(httpResponse.body).getString("session_id"));
        mDFRestClient.setHeader("X-DreamFactory-Session-Token", mUser.getDFSessionId());
    }

    /**
     * CALLED ONLY FROM NON-UI THREADS
     * Tries to log in using stored credentials. Won't ask user for credentials upon error.
     * @return True if we were able to login successfully
     */
    private boolean tryDFQuickLogin() {
        // Init a new rest client
        mDFRestClient = new RESTClient2(mCtx);
        mDFRestClient.setHeader("Accept", "text/plain,application/json");
        mDFRestClient.setHeader("X-DreamFactory-Application-Name", Backend.DF_APP_NAME);
        // Get things
        String email = mUser.getDFEmail();
        String password = mUser.getDFPass();
        if (email.equals("") || password.equals("")) return false;
        // Create a login request
        LoginModel loginRequest = new LoginModel();
        loginRequest.setEmail(email);
        loginRequest.setPassword(password);
        try {
            // Serialize login request
            JSONObject login = new JSONSerializer<>(LoginModel.class, mCtx).serialize(loginRequest);
            // Try to login
            HTTPResponse httpResponse = mDFRestClient.post(DF_URL + DF_SESSION_SUFIX, login);
            // Get the session ID if login was a success
            mUser.recordDFSessionId(new JSONObject(httpResponse.body).getString("session_id"));
            mDFRestClient.setHeader("X-DreamFactory-Session-Token", mUser.getDFSessionId());
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        } catch (HTTPException e) {
            // Thrown if we had trouble logging in
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<BaseDataModel> getFromDF(int dataType) throws HTTPException, JSONException {
        if (mUser.getDFSessionId().equals("") || !mUser.isDFSessionIdValid()) {
            if (!tryDFQuickLogin()) return null; // Don't do this if we aren't logged in
        }

        List<BaseDataModel> data = null;
        switch (dataType) {

            // TODO: REFINE THE FAMILY DATA MODEL
            case User.FAMILY_SHARE_DATA_TYPE:
                data = new ArrayList<BaseDataModel>();

                String getFamShareURL = DF_URL + DF_DB_SUFIX + "/" + DF_FAMILY_SHARE_TABLE + "?related=*";
                System.out.println("getURL :: " + getFamShareURL);
                JSONObject familySharingData = mDFRestClient.getJSONObject(getFamShareURL);
                List<LogDataModel> familyData = DataUtil.getLogsFromUserData(familySharingData, mUser.getUserId());
                for (int i = 0; i < familyData.size(); i++) {
                    data.add(familyData.get(i));
                }
                break;

            case User.MEDICATION_DATA_TYPE:
                data = new ArrayList<BaseDataModel>();
                String getMedsURL = DF_URL + DF_DB_SUFIX + "/" + DF_MEDICATION_TABLE + "?related=*";
                System.out.println("getURL :: " + getMedsURL);
                JSONArray medsDataObjectsJson = mDFRestClient.getJSONObject(getMedsURL).getJSONArray("record");
                System.out.println("medication dataObjects :: " + medsDataObjectsJson);
                for (int i = 0; i < medsDataObjectsJson.length(); i++) {
                    JSONObject json = medsDataObjectsJson.getJSONObject(i);
                    MedicationDataModel medsDataObject = MedicationDataModel.fromJson(json);
                    data.add(medsDataObject);
                }
                break;
            case User.SUBSCRIBE_DATA_TYPE:
                data = new ArrayList<BaseDataModel>();

                String getSubscribeURL = DF_URL + DF_DB_SUFIX + "/" + DF_USER_TABLE + "?related="
                        + DF_RELATED_SUBSCRIBE_BY_UID + "," + DF_RELATED_MED_BY_SUBSCIBE;
                System.out.println("getURL :: " + getSubscribeURL);
                JSONObject userData = mDFRestClient.getJSONObject(getSubscribeURL);
                List<SubscribeDataModel> subscribeData = DataUtil.getSubsciptionsFromUserData(userData, mUser.getUserId());
                for (int i = 0; i < subscribeData.size(); i++) {
                    data.add(subscribeData.get(i));
                }
                break;
            case User.LOGS_DATA_TYPE:
                data = new ArrayList<BaseDataModel>();
                String getLogsURL = DF_URL + DF_DB_SUFIX + "/" + DF_USER_TABLE + "?related="
                        + DF_RELATED_TAKEN_BY_UID + "," + DF_RELATED_SYMPTOM_BY_UID + ","
                        + DF_RELATED_MEDS_BY_TAKEN;
                System.out.println("getURL :: " + getLogsURL);
                JSONObject userActivityData = mDFRestClient.getJSONObject(getLogsURL);
                List<LogDataModel> logsData = DataUtil.getLogsFromUserData(userActivityData, mUser.getUserId());
                for (int i = 0; i < logsData.size(); i++) {
                    data.add(logsData.get(i));
                }
                break;

        }

        return data;
    }

    /**
     * Gets a user's data from the DF backend within the specified time range.
     * @param from Earliest time stamp we want items to have
     * @param to Latest time stamp we want items to have
     * @return An ArrayList of BaseRecordRequest items
     * @throws org.droidparts.net.http.HTTPException
     * @throws org.json.JSONException
     */
    public ArrayList<BaseRecordModel> getFromDF(Date from, Date to) throws HTTPException, JSONException {
        // If the session is blank or it could be invalid, try to login
        if (mUser.getDFSessionId().equals("") || !mUser.isDFSessionIdValid()) {
            if (!tryDFQuickLogin()) return null; // Don't do this if we aren't logged in
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        // Create URL for getting data and try to get it
        String getURL = DF_URL + String.format(DF_GET_DATA_SUFFIX, DF_LATEST_DATA_TABLE, mUser.getAccountName(), sdf.format(from), sdf.format(to));
        JSONArray dataObjects = mDFRestClient.getJSONObject(getURL).getJSONArray("record");
        // Create JSON (de)serializers
        JSONSerializer<MedRecordModel> medsJSON = new JSONSerializer<>(MedRecordModel.class, mCtx);
        JSONSerializer<SymptomRecordModel> sympJSON = new JSONSerializer<>(SymptomRecordModel.class, mCtx);
        JSONSerializer<StressFactorRecordModel> stressJSON = new JSONSerializer<>(StressFactorRecordModel.class, mCtx);
        // Iterate through records from DB and deserialize them
        ArrayList<BaseRecordModel> data = new ArrayList<>();
        try {
            for (int i = 0; i < dataObjects.length() - 1; i++) {
                JSONObject dataObject = dataObjects.getJSONObject(i);
                if (dataObject.getString("data_ver").equals(VER_MED)) {
                    data.add(medsJSON.deserialize(dataObject));
                } else if (dataObject.getString("data_ver").equals(VER_SYMP)) {
                    data.add(sympJSON.deserialize(dataObject));
                } else if (dataObject.getString("data_ver").equals(VER_STRESS)) {
                    data.add(stressJSON.deserialize(dataObject));
                }
            }
        } catch (Exception e) {
            if (e instanceof JSONException) {
                throw (JSONException) e;
            } else if (e instanceof HTTPException) {
                throw (HTTPException) e;
            } else {
                e.printStackTrace();
            }
        }
        return data;
    }

    /**
     * Sends user data to the Dreamfactory backend
     * @param type What type of data we're sending
     * @param data The data to send
     * @throws JSONException
     * @throws HTTPException
     */
    public void sendToDF(int type, String data) throws JSONException, HTTPException,
            DFCredentialsInvalidException {
        sendToDF(type, data, String.valueOf(Calendar.getInstance().getTimeInMillis()));
    }

    /**
     * Sends user data to the Dreamfactory backend, manually specifying time data was reported.
     * @param type What type of data we're sending
     * @param data The data to send
     * @param time The time the data was reported by user
     */
    public Object sendToDF(int type, String data, String time) throws JSONException, HTTPException,
            DFCredentialsInvalidException {
        HTTPResponse httpResponse = null;
        // If session is blank or it could be invalid, try to login
        if (mUser.getDFSessionId().equals("") || !mUser.isDFSessionIdValid()) {
            if (!tryDFQuickLogin()) return httpResponse; // Don't do this if we aren't logged in
        }
        String uid = mUser.getAccountName();
        String postURL = DF_URL;
        JSONObject recordToCreate = null;
        try {
            switch (type) {
                case User.MED:
                    postURL += String.format(DF_POST_DATA_SUFIX, DF_MEDS_TABLE);
                    MedRecordModel medRecordRequest = new MedRecordModel();
                    medRecordRequest.setUid(uid);
                    medRecordRequest.setTime(time);
                    medRecordRequest.setDataVer(VER_MED);
                    medRecordRequest.setMedication(data);
                    recordToCreate = new JSONSerializer<>(MedRecordModel.class, mCtx).serialize(medRecordRequest);
                    break;
                case User.SYMP:
                    postURL += String.format(DF_POST_DATA_SUFIX, DF_SYMP_TABLE);
                    SymptomRecordModel symptomRecordRequest = new SymptomRecordModel();
                    symptomRecordRequest.setUid(uid);
                    symptomRecordRequest.setTime(time);
                    symptomRecordRequest.setDataVer(VER_SYMP);
                    symptomRecordRequest.setSymptoms(data);
                    recordToCreate = new JSONSerializer<>(SymptomRecordModel.class, mCtx).serialize(symptomRecordRequest);
                    break;
                case User.STRESS:
                    postURL += String.format(DF_POST_DATA_SUFIX, DF_STRESS_TABLE);
                    StressFactorRecordModel stressFactorRecordRequest = new StressFactorRecordModel();
                    stressFactorRecordRequest.setUid(uid);
                    stressFactorRecordRequest.setTime(time);
                    stressFactorRecordRequest.setDataVer(VER_STRESS);
                    stressFactorRecordRequest.setStressFactors(data);
                    recordToCreate = new JSONSerializer<>(StressFactorRecordModel.class, mCtx).serialize(stressFactorRecordRequest);
                    break;
                case User.SYMP_DATA_TYPE:
                    postURL += String.format(DF_POST_DATA_SUFIX, DF_SYMPTOM_TABLE);

                    /*SymptomDataModel symptomDataModel = new SymptomDataModel();

                    JSONObject json = symptomDataModel.getJsonData(SymptomDataModel.SYMPTOM_TYPE_RIGIDITY,
                                                       "right foot", "55", 1, time);
                    recordToCreate = json;
*/
                    recordToCreate = new JSONObject(data);
                    //  recordToCreate = new JSONSerializer<>(SymptomRecordModel.class, mCtx).serialize(symptomRecordRequest);
                    break;
                case User.TAKEN_DATA_TYPE:
                    postURL += String.format(DF_POST_DATA_SUFIX, DF_TAKEN_TABLE);

                    /*SymptomDataModel symptomDataModel = new SymptomDataModel();

                    JSONObject json = symptomDataModel.getJsonData(SymptomDataModel.SYMPTOM_TYPE_RIGIDITY,
                                                       "right foot", "55", 1, time);
                    recordToCreate = json;
*/
                    recordToCreate = new JSONObject(data);
                    //  recordToCreate = new JSONSerializer<>(SymptomRecordModel.class, mCtx).serialize(symptomRecordRequest);
                    break;
                case User.SUBSCRIBE_DATA_TYPE:
                    postURL += String.format(DF_PUT_DATA_SUFIX, DF_USER_TABLE);

                    /*SymptomDataModel symptomDataModel = new SymptomDataModel();

                    JSONObject json = symptomDataModel.getJsonData(SymptomDataModel.SYMPTOM_TYPE_RIGIDITY,
                                                       "right foot", "55", 1, time);
                    recordToCreate = json;
*/
                    recordToCreate = new JSONObject(data);
                    //  recordToCreate = new JSONSerializer<>(SymptomRecordModel.class, mCtx).serialize(symptomRecordRequest);
                    System.out.println("Subscribe send json : " + recordToCreate);
                    break;
            }
        } catch (Exception e) {
            if (e instanceof JSONException) {
                throw (JSONException) e;
            } else if (e instanceof HTTPException) {
                throw (HTTPException) e;
            } else {
                e.printStackTrace();
            }
        }
        System.out.println("Calling server");
        if (type ==  User.SUBSCRIBE_DATA_TYPE) {
            System.out.println("calling put");
            httpResponse = mDFRestClient.put(postURL, recordToCreate);
        } else {
            System.out.println("post");
            httpResponse = mDFRestClient.post(postURL, recordToCreate);
        }
        System.out.println("http response : " + httpResponse);
        System.out.println("http response body : " + httpResponse.body);

        return httpResponse;
    }

    /**
     * Logs out of Dreamfactory if need be and disposes of REST client object.
     */
    public void destroyDF() {
        if (mDFRestClient != null) {
            // Logout and get rid of rest client object
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        // Try to log out
                        mDFRestClient.delete(DF_URL + DF_SESSION_SUFIX);
                    } catch (Exception e) {
                        // Really, it doesn't matter at all
                        e.printStackTrace();
                    }
                    return null;
                }
                @Override
                protected void onPostExecute(Void result) {
                    mDFRestClient = null;
                    mUser.recordDFSessionId("");
                }
            }.execute();
        }
    }

    /**
     * Destroys DF session and REST client, and clears prefs file
     */
    public void dreamfactoryLogout() {
        destroyDF();
        mUser.getPrefsUtil().getDFPrefs().edit().clear().commit();
    }

    /* ************************** Methods for En/Decoding Data Strings ************************** */

    /**
     * Takes two int arrays, one containing med IDs and the other containing the number of doses of
     * each med the user has had today, and encodes them into a string to be posted on the backend.
     * Format is as follows:
     *
     * "[medID]:[#doses*];[medID]:[#doses];..."
     * *NOTE (-1 for N/A)
     *
     * @param medIDs Int array of med IDs
     * @param doses Int array of dosages
     * @return String in the format above
     */
    public String encodeMedsRow(int[] medIDs, int[] doses) {
        StringBuilder medRowBuilder = new StringBuilder();
        for (int i = 0; i < medIDs.length; i++) {
            medRowBuilder.append(medIDs[i]).append(":").append(doses[i]).append(";");
        }
        // Trim the trailing semi-colon and then return the string
        return medRowBuilder.deleteCharAt(medRowBuilder.length() - 1).toString();
    }

    /**
     * Take a med string from the backend and decodes it into a 2D int array, where the first array
     * contains med IDs and the second array contains the number of doses of each med the user took.
     * @param medRow String from the backend with med dosage data in it
     * @return 2D array with med IDs and dosages for each med
     */
    public int[][] decodeMedsRow(String medRow) {
        String[] medItems = medRow.split(";");
        int[][] medInfo = new int[2][medItems.length];
        for (int i = 0; i < medItems.length; i++) {
            String[] medItem = medItems[i].split(":");
            medInfo[0][i] = Integer.valueOf(medItem[0]);
            medInfo[1][i] = Integer.valueOf(medItem[1]);
        }
        return medInfo;
    }

    /**
     * Translates a linear layout with radio buttons for symptoms into a string of data
     * @param symptomsContainer Layout with radio buttons. to be processed to get data
     * @return String of data to be put into the user's sheet
     */
    public String encodeSymptoms(LinearLayout symptomsContainer) {
        StringBuilder encoded = new StringBuilder("");
        // Loops through each RadioGroup to find which RadioButton is checked
        for (int i = 0; i < symptomsContainer.getChildCount(); i++) {
            LinearLayout labelAndSlider = (LinearLayout) symptomsContainer.getChildAt(i);
            // Get the radio group
            RadioGroup value = (RadioGroup) labelAndSlider.findViewById(R.id.symptomRatings);
            encoded.append(i + 1).append(":");
            // Get the checked RadioButton
            RadioButton checkedRadio = (RadioButton) labelAndSlider.findViewById(value
                    .getCheckedRadioButtonId());
            // Get the severity number by getting the checked RadioButton's label
            int severity = Integer.parseInt((String) checkedRadio.getText());
            encoded.append(severity).append(",");
            // Each time this loops, it adds on a piece of string in this format:
            // "[SymptomID]:[SeverityLevel]" where SymptomID is just whether the symptom is the 1st
            // in the list, 2nd in the list, etc.
        }
        // Delete trailing comma.
        encoded.deleteCharAt(encoded.length() - 1);
        return encoded.toString();
    }
}
