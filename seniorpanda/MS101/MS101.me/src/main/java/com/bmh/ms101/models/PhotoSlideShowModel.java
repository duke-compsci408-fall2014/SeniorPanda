package com.bmh.ms101.models;

import org.droidparts.annotation.serialize.JSON;
import org.droidparts.model.Model;

/**
 * Created by Main on 11/4/14.
 */
public class PhotoSlideShowModel extends Model {

    @JSON(key = "id")
    private Integer id = null;
    @JSON(key = "picture_url")
    private String pictureURL = null;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getPictureURL() {
        return pictureURL;
    }

    public void setPictureURL(String picture_url) {
        this.pictureURL = picture_url;
    }

    //TODO

}
