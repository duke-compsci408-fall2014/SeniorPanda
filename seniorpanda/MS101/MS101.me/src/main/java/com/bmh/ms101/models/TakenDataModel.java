package com.bmh.ms101.models;

import com.amazonaws.org.joda.time.DateTime;

import org.json.JSONException;
import org.json.JSONObject;

public class TakenDataModel {
    private int id = 0;
    private int userId = 0;
    private int medicationId = 0;
    private int pillsTaken = 0;
    private DateTime dateTimeTaken = null;

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

    public int getPillsTaken() {
        return pillsTaken;
    }

    public void setPillsTaken(int pillsTaken) {
        this.pillsTaken = pillsTaken;
    }

    public DateTime getDateTimeTaken() {
        return dateTimeTaken;
    }

    public void setDateTimeTaken(DateTime dateTimeTaken) {
        this.dateTimeTaken = dateTimeTaken;
    }

    public static TakenDataModel fromJson(JSONObject json) {
        TakenDataModel takenDataModel = new TakenDataModel();
        try {
            if (json != null) {
                if (json.has("id")) {
                    takenDataModel.setId(json.getInt("id"));
                }
                if (json.has("uid")) {
                    takenDataModel.setUserId(json.getInt("uid"));
                }
                if (json.has("medication_id")) {
                    takenDataModel.setMedicationId(json.getInt("medication_id"));
                }
                if (json.has("pills_taken")) {
                    takenDataModel.setPillsTaken(json.getInt("pills_taken"));
                }
                /*if (json.has("data_taken")) {
                    takenDataModel.setDateTimeTaken(json.get("date_taken"));
                }*/
            }
        } catch (JSONException e) {

        }
        return takenDataModel;
    }

    public static JSONObject toJson(TakenDataModel data) {
        JSONObject json = new JSONObject();

        try {
            if (data != null) {
                json.put("uid", data.getUserId());
                json.put("medication_id", data.getMedicationId());
                json.put("pills_taken", data.pillsTaken);
            }
        } catch (JSONException e) {

        }
        return json;
    }
}
