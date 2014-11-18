package com.bmh.ms101;

import com.bmh.ms101.models.SubscribeDataModel;
import com.bmh.ms101.models.UserDataModel;

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

}
