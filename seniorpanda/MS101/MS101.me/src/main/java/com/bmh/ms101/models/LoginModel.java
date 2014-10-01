package com.bmh.ms101.models;

import org.droidparts.annotation.serialize.JSON;
import org.droidparts.model.Model;

/**
 * Models a login request for dreamfactory
 */
public class LoginModel extends Model {

    @JSON(key = "email")
    private String email = null;
    @JSON(key = "password")
    private String password = null;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
