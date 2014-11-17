package com.bmh.ms101.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UserDataModel extends BaseDataModel {
    private int id = 0;

    private JSONObject json = null;

    public UserDataModel() {
        super();
    }

    public UserDataModel(JSONObject json) {
        super();
        this.json = json;
        System.out.println("user json " + json);
    }

    public List<SubscribeDataModel> getSubscriptionData() {
        List<SubscribeDataModel> subscriptions = new ArrayList<SubscribeDataModel>();
        try {
            JSONArray subscribeUidJsonArray = json.getJSONArray("subscribes_by_uid");
            JSONArray medicationsSubscribeJsonArray = json.getJSONArray("medications_by_subscribe");
            for (int i = 0; i < subscribeUidJsonArray.length(); i++) {
                SubscribeDataModel subscribeData = new SubscribeDataModel();
                JSONObject subscribeDataJson = subscribeUidJsonArray.getJSONObject(i);
                subscribeData.setId(subscribeDataJson.getInt("id"));
                subscribeData.setMedicationId(subscribeDataJson.getInt("medication_id"));
                JSONObject medDataJson = getMedicationDataJson(medicationsSubscribeJsonArray, subscribeDataJson.getInt("medication_id"));
                subscribeData.setMedicationName(medDataJson.getString("name"));
                subscribeData.setPillsPerDose(medDataJson.getInt("pills_per_dose"));
                subscribeData.setDosesPerDay((medDataJson.getInt("doses_per_day")));
                subscriptions.add(subscribeData);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } {

        }
        return subscriptions;
    }

    JSONObject getMedicationDataJson(JSONArray medicationsArray, int medId) {
        JSONObject medicationJson = null;
        for (int i = 0; i < medicationsArray.length(); i++) {
            try {
                int mId = medicationsArray.getJSONObject(i).getInt("id");
                if (mId == medId) {
                    medicationJson = medicationsArray.getJSONObject(i);
                    break;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return medicationJson;
    }

    public static SubscribeDataModel fromJson(JSONObject json) {
        SubscribeDataModel subscribeDataModel = new SubscribeDataModel();
        try {
            if (json != null) {
                if (json.has("id")) {
                    subscribeDataModel.setId(json.getInt("id"));
                }
                if (json.has("uid")) {
                    subscribeDataModel.setUserId(json.getInt("uid"));
                }
                if (json.has("medication_id")) {
                    subscribeDataModel.setMedicationId(json.getInt("medication_id"));
                }
                if (json.has("medications_by_subscribe")) {
                    JSONObject medJson = json.getJSONObject("medications_by_subscribe");
                    if (medJson != null) {
                        if (medJson.has("name")) {
                            subscribeDataModel.setMedicationName(medJson.getString("name"));
                        }
                        if (medJson.has("doses_per_day")) {
                            subscribeDataModel.setDosesPerDay(medJson.getInt("doses_per_day"));
                        }
                        if (medJson.has("pills_per_dose")) {
                            subscribeDataModel.setPillsPerDose(medJson.getInt("pills_per_dose"));
                        }
                    }
                }
            }
        } catch (JSONException e) {

        }
        return subscribeDataModel;
    }

}
