package com.bmh.ms101;

import com.bmh.ms101.models.LogDataModel;
import com.bmh.ms101.models.ModelConstants;
import com.bmh.ms101.models.SubscribeDataModel;
import com.bmh.ms101.models.SymptomDataModel;
import com.bmh.ms101.models.TakenDataModel;
import com.bmh.ms101.models.UserDataModel;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DataUtil {
    public static List<SubscribeDataModel> getSubsciptionsFromUserData(JSONObject userData, int userId) {
        List<SubscribeDataModel> subscribeData = null;
        JSONArray userDataObjectsJson = null;
        try {
            userDataObjectsJson = userData.getJSONArray("record");
            JSONObject userDataObjectJson = null;
            for (int i = 0; i < userDataObjectsJson.length(); i++) {
                JSONObject json = userDataObjectsJson.getJSONObject(i);
                int id = json.getInt("id");
                if (id == userId) {
                    userDataObjectJson = json;
                    break;
                }
            }
            UserDataModel userDataModel = new UserDataModel(userDataObjectJson);
            subscribeData = userDataModel.getSubscriptionData();
            System.out.println("subscribeData size" + subscribeData.size());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println("subscribeData " + subscribeData);
        return subscribeData;
    }

    public static List<LogDataModel> getLogsFromUserData(JSONObject userData, int userId) {
        List<LogDataModel> logDataList = new ArrayList<LogDataModel>();
        LogDataModel logData = new LogDataModel();
        List<TakenDataModel> medsTakenData = null;
        List<SymptomDataModel> symptomsData = null;
        JSONArray userDataObjectsJson = null;
        try {
            userDataObjectsJson = userData.getJSONArray("record");
            JSONObject userDataObjectJson = null;
            for (int i = 0; i < userDataObjectsJson.length(); i++) {
                JSONObject json = userDataObjectsJson.getJSONObject(i);
                int id = json.getInt(ModelConstants.ID);
                if (id == userId) {
                    userDataObjectJson = json;
                    break;
                }
            }
            UserDataModel userDataModel = new UserDataModel(userDataObjectJson);
            medsTakenData = userDataModel.getMedicationsTakenData();
            System.out.println("medsTakenData size" + medsTakenData.size());
            logData.setMedsTakenData(medsTakenData);

            symptomsData = userDataModel.getSymptomsData();
            System.out.println("symptomsData size" + symptomsData.size());
            logData.setSymptomsData(symptomsData);
            logDataList.add(logData);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        System.out.println("logDataList " + logDataList);
        return logDataList;
    }

}
