package com.bmh.ms101.models;

import org.droidparts.annotation.serialize.JSON;

/**
 * Model for DF Symptom schema
 */
public class SymptomRecordModel extends BaseRecordModel {

    @JSON(key = "symptoms")
    private String symptoms = null;

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    @Override
    public String toString()  {
        StringBuilder sb = new StringBuilder();
        sb.append("class RecordRequest {\n");
        sb.append("  id: ").append(getId()).append("\n");
        sb.append("  uid: ").append("\"").append(getUid()).append("\"").append("\n");
        sb.append("  time: ").append("\"").append(getTime()).append("\"").append("\n");
        sb.append("  symptoms: ").append("\"").append(getSymptoms()).append("\"").append("\n");
        sb.append("  data_ver: ").append("\"").append(getDataVer()).append("\"").append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}