package com.bmh.ms101.models;

import org.json.JSONException;
import org.json.JSONObject;

public class DeviceDataModel extends BaseDataModel {
    private int id = 0;
    private int userId = 0;
    private String regId = null;

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

    public String getRegId() {
        return regId;
    }

    public void setRegId(String regId) {
        this.regId = regId;
    }

    public static DeviceDataModel fromJson(JSONObject json) {
        DeviceDataModel deviceDataModel = new DeviceDataModel();
        try {
            if (json != null) {
                if (json.has(ModelConstants.ID)) {
                    deviceDataModel.setId(json.getInt(ModelConstants.ID));
                }
                if (json.has(ModelConstants.UID)) {
                    deviceDataModel.setUserId(json.getInt(ModelConstants.UID));
                }
                if (json.has("regid")) {
                    deviceDataModel.setRegId(json.getString("regid"));
                }
            }
        } catch (JSONException e) {

        }
        return deviceDataModel;
    }

    public static JSONObject toJson(DeviceDataModel data) {
        JSONObject json = new JSONObject();

        try {
            if (data != null) {
                json.put("uid", data.getUserId());
                json.put("regid", data.getRegId());
            }
        } catch (JSONException e) {

        }
        return json;
    }
}
