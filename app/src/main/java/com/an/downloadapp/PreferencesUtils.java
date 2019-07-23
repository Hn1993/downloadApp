package com.an.downloadapp;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesUtils {
    private static PreferencesUtils preferencesUtils = null;
    private SharedPreferences sharedPreferences;
    private static final String PREFERENCES_NAME = "preferences_lua";
    private PreferencesUtils() {
        sharedPreferences = App.getInstance().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static PreferencesUtils getInstance() {
        if (preferencesUtils == null) {
            preferencesUtils = new PreferencesUtils();
        }
        return preferencesUtils;
    }

    public void putBoolean(String key,boolean value){
        sharedPreferences.edit()
                .putBoolean(key, value)
                .apply();

    }

    public boolean getBoolean(String key){
        return sharedPreferences.getBoolean(key,false);
    }

    public void putString(String key,String value){
        sharedPreferences.edit()
                .putString(key, value)
                .apply();
    }

    public String getString(String key){
        return sharedPreferences.getString(key, "");
    }


    public void putInt(String key,int value){
        sharedPreferences.edit()
                .putInt(key, value)
                .apply();
    }

    public int getInt(String key){
        return sharedPreferences.getInt(key, 0);
    }

    public void putLong(String key,long value){
        sharedPreferences.edit()
                .putLong(key, value)
                .apply();
    }

    public Long getLong(String key){
        return sharedPreferences.getLong(key, 0);
    }

    public void clearSp(String key){
        sharedPreferences.edit().remove(key).apply();
    }
}
