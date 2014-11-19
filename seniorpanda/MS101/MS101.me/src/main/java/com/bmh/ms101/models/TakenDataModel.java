package com.bmh.ms101.models;

import com.amazonaws.org.joda.time.DateTime;
import com.amazonaws.org.joda.time.DateTimeZone;
import com.amazonaws.org.joda.time.tz.DateTimeZoneBuilder;

import org.json.JSONException;
import org.json.JSONObject;

public class TakenDataModel {
    private int id = 0;
    private int userId = 0;
    private int medicationId = 0;
    private int pillsTaken = 0;
    private String dateTimeTaken = null;
    private String medicationName;

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

    public String getDateTimeTaken() {
        return dateTimeTaken;
    }

    public void setDateTimeTaken(String dateTimeTaken) {
        this.dateTimeTaken = dateTimeTaken;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public static TakenDataModel fromJson(JSONObject json) {
        TakenDataModel takenDataModel = new TakenDataModel();
        try {
            if (json != null) {
                if (json.has(ModelConstants.ID)) {
                    takenDataModel.setId(json.getInt(ModelConstants.ID));
                }
                if (json.has(ModelConstants.UID)) {
                    takenDataModel.setUserId(json.getInt(ModelConstants.UID));
                }
                if (json.has(ModelConstants.MEDICATION_ID)) {
                    takenDataModel.setMedicationId(json.getInt(ModelConstants.MEDICATION_ID));
                }
                if (json.has(ModelConstants.PILLS_TAKEN)) {
                    takenDataModel.setPillsTaken(json.getInt(ModelConstants.PILLS_TAKEN));
                }
                if (json.has(ModelConstants.DATE_TAKEN)) {
                    takenDataModel.setDateTimeTaken(json.getString(ModelConstants.DATE_TAKEN));
                }
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
