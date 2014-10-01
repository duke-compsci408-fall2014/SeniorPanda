package com.bmh.ms101.models;

import org.droidparts.annotation.serialize.JSON;

/**
 * Model for DF Stress Factor schema
 */
public class StressFactorRecordModel extends BaseRecordModel {

    @JSON(key = "environment")
    private String stressFactors = null;

    public String getStressFactors() {
        return stressFactors;
    }

    public void setStressFactors(String stressFactor) {
        this.stressFactors = stressFactor;
    }

    @Override
    public String toString()  {
        StringBuilder sb = new StringBuilder();
        sb.append("class RecordRequest {\n");
        sb.append("  id: ").append(getId()).append("\n");
        sb.append("  uid: ").append("\"").append(getUid()).append("\"").append("\n");
        sb.append("  time: ").append("\"").append(getTime()).append("\"").append("\n");
        sb.append("  environment: ").append("\"").append(getStressFactors()).append("\"").append("\n");
        sb.append("  data_ver: ").append("\"").append(getDataVer()).append("\"").append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}