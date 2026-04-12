package com.everlog.managers.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import com.everlog.application.ELApplication;

import java.util.Set;

public abstract class PreferencesManager {

    private static final String PREFERENCE_TITLE = "EverlogPreferences";

    protected void savePreference(boolean value, String key) {
        SharedPreferences sharedPref = getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    protected void savePreference(String value, String key) {
        SharedPreferences sharedPref = getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(key, value);
        editor.apply();
    }

    protected void savePreference(float value, String key) {
        SharedPreferences sharedPref = getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(key, value);
        editor.apply();
    }

    protected void savePreference(int value, String key) {
        SharedPreferences sharedPref = getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    protected void savePreference(long value, String key) {
        SharedPreferences sharedPref = getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putLong(key, value);
        editor.apply();
    }

    protected void savePreference(Set<String> value, String key) {
        SharedPreferences sharedPref = getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putStringSet(key, value);
        editor.apply();
    }

    protected boolean getPreference(String key, boolean defaultValue) {
        SharedPreferences sharedPref = getPreferences();
        return sharedPref.getBoolean(key, defaultValue);
    }

    protected String getPreference(String key, String defaultValue) {
        SharedPreferences sharedPref = getPreferences();
        return sharedPref.getString(key, defaultValue);
    }

    protected int getPreference(String key, int defaultValue) {
        SharedPreferences sharedPref = getPreferences();
        return sharedPref.getInt(key, defaultValue);
    }

    protected float getPreference(String key, float defaultValue) {
        SharedPreferences sharedPref = getPreferences();
        return sharedPref.getFloat(key, defaultValue);
    }

    protected long getPreference(String key, long defaultValue) {
        SharedPreferences sharedPref = getPreferences();
        return sharedPref.getLong(key, defaultValue);
    }

    protected Set<String> getPreference(String key, Set<String> defaultValue) {
        SharedPreferences sharedPref = getPreferences();
        return sharedPref.getStringSet(key, defaultValue);
    }

    protected void removePreference(String key) {
        SharedPreferences sharedPref = getPreferences();
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.remove(key);
        editor.apply();
    }

    protected SharedPreferences getPreferences() {
        return ELApplication.getInstance().getSharedPreferences(PREFERENCE_TITLE, Context.MODE_PRIVATE);
    }
}