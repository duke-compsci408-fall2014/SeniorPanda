package com.bmh.ms101.PhotoFlipping;

import android.app.Activity;
import android.content.SharedPreferences;

public class CityPreference {

    private static final String CITY_PREF = "city";
    SharedPreferences prefs;

    public CityPreference(Activity activity) {
        prefs = activity.getPreferences(Activity.MODE_PRIVATE);
    }

    //default City
    String getCity() {
        return prefs.getString(CITY_PREF, "Durham, US");
    }

    void setCity(String city) {
        prefs.edit().putString(CITY_PREF, city).commit();
    }

}