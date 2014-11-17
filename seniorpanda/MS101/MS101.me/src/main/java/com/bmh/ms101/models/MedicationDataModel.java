package com.bmh.ms101.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class MedicationDataModel extends BaseDataModel {

    private int id = 0;
    private String name = null;
    private String treatment = null;
    private int dosesPerDay = 0;
    private int pillsPerDose = 0;
    private List<SubscribeDataModel> subscriptions = null;

    MedicationDataModel() {
        super();
        subscriptions = new ArrayList<SubscribeDataModel>();
    }


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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

    public List<SubscribeDataModel> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<SubscribeDataModel> subscriptions) {
        this.subscriptions = subscriptions;
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
                if (json.has("pills_per_dose")) {
                    medicationDataModel.setPillsPerDose(json.getInt("pills_per_dose"));
                }
                if (json.has("subscribes_by_medication_id")) {
                    JSONArray jsonArray = json.getJSONArray("subscribes_by_medication_id");
                    List<SubscribeDataModel> subscribeDataModelList = new ArrayList<SubscribeDataModel>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject subscribeJson = jsonArray.getJSONObject(i);
                        SubscribeDataModel subscribe = SubscribeDataModel.fromJson(subscribeJson);
                        subscribeDataModelList.add(subscribe);
                    }
                    medicationDataModel.setSubscriptions(subscribeDataModelList);
                }
            }
        } catch (JSONException e) {

        }
        return medicationDataModel;
    }


}
