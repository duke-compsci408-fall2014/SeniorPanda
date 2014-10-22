package com.bmh.ms101.models;


import org.json.JSONException;
import org.json.JSONObject;

public class SymptomDataModel {

    // Key strings to get values from the pref file
    public static final String SYMPTOM_TYPE_TREMOR = "tremor";
    public static final String SYMPTOM_TYPE_SLOW_MOVEMENT = "slowmovement";
    public static final String SYMPTOM_TYPE_RIGIDITY = "rigidity";
    public static final String SYMPTOM_TYPE_FREEZING = "freezing";

    public JSONObject getJsonData(String type, String bodyLocation, String duration, int uid, String date) {
        JSONObject json = new JSONObject();
        try {
            json.put("symptom_type", type);
            json.put("body_location", bodyLocation);
            json.put("duration", duration);
            json.put("uid", uid);
            json.put("date", date);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }


}
