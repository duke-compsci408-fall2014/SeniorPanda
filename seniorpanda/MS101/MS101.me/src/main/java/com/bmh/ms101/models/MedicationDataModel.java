package com.bmh.ms101.models;

import org.json.JSONException;
import org.json.JSONObject;

public class MedicationDataModel extends BaseDataModel {

    private int id = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    private String name = null;
    private String treatment = null;
    private int dosesPerDay = 0;
    private int pillsPerDose = 0;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public static MedicationDataModel fromJson(JSONObject json) {
        MedicationDataModel medicationDataModel = new MedicationDataModel();
        try {
            if (json != null) {
                if (json.has("id")) {
                    medicationDataModel.setId(json.getInt("id"));
                }
                if (json.has("name")) {
                    medicationDataModel.setName(json.getString("name"));
                }
                if (json.has("doses_per_day")) {
                    medicationDataModel.setDosesPerDay(json.getInt("doses_per_day"));
                }
                if (json.has("doses_per_day")) {
                    medicationDataModel.setPillsPerDose(json.getInt("pills_per_dose"));
                }
            }
        } catch (JSONException e) {

        }
        return medicationDataModel;
    }


}
