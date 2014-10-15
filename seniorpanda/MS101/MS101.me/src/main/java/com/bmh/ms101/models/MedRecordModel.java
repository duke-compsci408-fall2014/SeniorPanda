package com.bmh.ms101.models;

import org.droidparts.annotation.serialize.JSON;

/**
 * Model for DF Medication schema
 */
public class MedRecordModel extends BaseRecordModel {

    @JSON(key = "medication")
    private String medication = null;

    public String getMedication() {
        return medication;
    }

    public void setMedication(String medication) {
        this.medication = medication;
    }

    @Override
    public String toString()  {
        StringBuilder sb = new StringBuilder();
        sb.append("class RecordRequest {\n");
        sb.append("  id: ").append(getId()).append("\n");
        sb.append("  uid: ").append("\"").append(getUid()).append("\"").append("\n");
        sb.append("  time: ").append("\"").append(getTime()).append("\"").append("\n");
        sb.append("  medication: ").append("\"").append(getMedication()).append("\"").append("\n");
        sb.append("  data_ver: ").append("\"").append(getDataVer()).append("\"").append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
