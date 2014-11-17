package com.bmh.ms101.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SubscribeDataModel extends BaseDataModel {
    private int id = 0;
    private int userId = 0;
    private int medicationId = 0;
    private String medicationName = null;
    private String treatment = null;
    private int dosesPerDay = 0;
    private int pillsPerDose = 0;

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

    public static JSONObject toJson(SubscribeDataModel data) {
        JSONObject json = new JSONObject();

        try {
            if (data != null) {
                if (data.getId() > 0) {
                    json.put("id", data.getId());
                }
                if (data.getUserId() == 0) {
                    System.out.println("Setting uid to null");
                    json.put("uid", JSONObject.NULL);
                } else {
                    json.put("uid", data.getUserId());
                }
                if (data.getMedicationId() == 0) {
                    System.out.println("Setting medication_id to null");
                    json.put("medication_id", JSONObject.NULL);
                } else {
                    json.put("medication_id", data.getMedicationId());
                }
            }
        } catch (JSONException e) {

        }
        System.out.println("json obj " + json);
        return json;
    }

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

    public int getMedicationId() {
        return medicationId;
    }

    public void setMedicationId(int medicationId) {
        this.medicationId = medicationId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }

    public int getDosesPerDay() {
        return dosesPerDay;
    }

    public void setDosesPerDay(int dosesPerDay) {
        this.dosesPerDay = dosesPerDay;
    }

    public int getPillsPerDose() {
        return pillsPerDose;
    }

    public void setPillsPerDose(int pillsPerDose) {
        this.pillsPerDose = pillsPerDose;
    }
}
