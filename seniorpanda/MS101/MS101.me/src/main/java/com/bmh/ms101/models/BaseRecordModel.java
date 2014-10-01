package com.bmh.ms101.models;

import org.droidparts.annotation.serialize.JSON;
import org.droidparts.model.Model;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Model for base record request for our Dreamfactory DSP Schemas for MS101
 */
public class BaseRecordModel extends Model implements Comparable<BaseRecordModel> {

    @JSON(key = "id", optional = true)
    private Integer id = null;
    @JSON(key = "uid")
    private String uid = null;
    @JSON(key = "time")
    private String time = null;
    @JSON(key = "data_ver")
    private String dataVer = null;
    @JSON(key = "date_created")
    private String dateCreated = null;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String id) {
        this.uid = id;
    }

    public String getTime() {
        return time;
    }

    public long getLongTime() {
        return Long.parseLong(time);
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setTime(String time) {
        this.time = time;
        this.dateCreated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(this.getLongTime()));
    }

    public String getDataVer() {
        return dataVer;
    }

    public void setDataVer(String dataVer) {
        this.dataVer = dataVer;
    }

    @Override
    public int compareTo(BaseRecordModel other) {
        Long otherTime = other.getLongTime();
        // Set up so that when sorted, items end up with newest first.
        return otherTime.compareTo(this.getLongTime());
    }
}
