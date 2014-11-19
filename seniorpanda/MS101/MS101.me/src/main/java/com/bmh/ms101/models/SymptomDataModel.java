package com.bmh.ms101.models;


import org.json.JSONException;
import org.json.JSONObject;

public class SymptomDataModel {
    private int id = 0;
    private int userId = 0;
    String bodyLocation = null;
    String symptomType = null;
    String duration = null;
    String dateTime = null;


    // Key strings to get values from the pref file
    public static final String SYMPTOM_TYPE_TREMOR = "tremor";
    public static final String SYMPTOM_TYPE_SLOW_MOVEMENT = "slowmovement";
    public static final String SYMPTOM_TYPE_RIGIDITY = "rigidity";
    public static final String SYMPTOM_TYPE_FREEZING = "freezing";

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getBodyLocation() {
        return bodyLocation;
    }

    public void setBodyLocation(String bodyLocation) {
        this.bodyLocation = bodyLocation;
    }

    public String getSymptomType() {
        return symptomType;
    }

    public void setSymptomType(String symptomType) {
        this.symptomType = symptomType;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public static SymptomDataModel fromJson(JSONObject json) {
        SymptomDataModel symptomDataModel = new SymptomDataModel();
        try {
            if (json != null) {
                if (json.has("id")) {
                    symptomDataModel.setId(json.getInt("id"));
                }
                if (json.has("uid")) {
                    symptomDataModel.setUserId(json.getInt("uid"));
                }
                if (json.has("symptomType")) {
                    symptomDataModel.setSymptomType(json.getString("symptom_type"));
                }
                if (json.has("body_location")) {
                    symptomDataModel.setBodyLocation(json.getString("body_location"));
                }
                if (json.has("duration")) {
                    symptomDataModel.setDuration(json.getString("duration"));
                }
                if (json.has("date")) {
                    symptomDataModel.setDateTime(json.getString("date"));
                }
            }
         } catch (JSONException e) {

         }
         return symptomDataModel;
    }

    public static JSONObject toJson(SymptomDataModel data) {
        JSONObject json = new JSONObject();

        try {
            if (data != null) {
                json.put("uid", data.getUserId());
                json.put("symptom_type", data.getSymptomType());
                json.put("body_location", data.getBodyLocation());
                json.put("duration", data.getDuration());
            }
        } catch (JSONException e) {

        }
        System.out.println("symptom json obj " + json);
        return json;
    }

    public JSONObject getJsonData(String type, String bodyLocation, String duration, int uid, String date) {
        JSONObject json = new JSONObject();
        try {
            json.put("symptom_type", type);
            json.put("body_location", bodyLocation);
            json.put("duration", duration);
            json.put("uid", uid);
        //    json.put("date", date);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }


}
