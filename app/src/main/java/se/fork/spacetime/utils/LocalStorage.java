package se.fork.spacetime.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.SyncStateContract;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Iterator;
import java.util.Map;

import se.fork.spacetime.BuildConfig;
import se.fork.spacetime.model.LoggablePlace;
import se.fork.spacetime.model.LoggablePlaceList;
import se.fork.spacetime.model.MyPlaceLists;

public class LocalStorage {

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String MY_PLACE_LISTS = "MY_PLACE_LISTS";
    private static LocalStorage ourInstance;

    public static LocalStorage getInstance() {
        if(ourInstance == null) {
            ourInstance = new LocalStorage();
        }
        return ourInstance;
    }

    private LocalStorage() {
    }

    public void clearAll(Context context) {
        SharedPreferences.Editor edit = getSharedPreferences(context).edit();
        edit.clear();
        edit.commit();
    }

    public void saveLoggablePlaceList(LoggablePlaceList list, Context context) {
        saveToSharedPreferences(context, list.getKey(), list);
        MyPlaceLists lists = getMyPlaceLists(context);
        if(!lists.getKeys().contains(list.getKey())) {
            lists.addKey(list.getKey());
            saveMyPlaceLists(context, lists);
        }
    }

    public LoggablePlace getPlaceFromId(String id, Context context) {
        LoggablePlace place = null;
        MyPlaceLists placeLists = getMyPlaceLists(context);
        if (placeLists != null) {
            for (String key: placeLists.getKeys()) {
                LoggablePlaceList list = getLoggablePlaceList(context, key);
                if (list.getLoggablePlaces().containsKey(id)) {
                    place = list.getLoggablePlaces().get(id);
                }
            }
        }
        return place;
    }

    public LoggablePlaceList getLoggablePlaceList(Context context, String key) {
        return (LoggablePlaceList)getObjectFromSharedPreferences(context, key, LoggablePlaceList.class);
    }

    public MyPlaceLists getMyPlaceLists(Context context) {
        return (MyPlaceLists)getObjectFromSharedPreferences(context, MY_PLACE_LISTS, MyPlaceLists.class);
    }

    public void saveMyPlaceLists(Context context, MyPlaceLists lists) {
        saveToSharedPreferences(context, MY_PLACE_LISTS, lists);
    }

    public void logAll(Context context){
        Log.d(this.getClass().getSimpleName(), "All local storage:\n" + listPreferences(context));
    }

    public SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(BuildConfig.APPLICATION_ID, 0);
    }


    public void saveToSharedPreferences(Context context, String key, String value) {
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(key, value);
        editor.apply();
    }

    public void saveToSharedPreferences(Context context, String key, Object object) {
        SharedPreferences prefs = getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.setDateFormat(DEFAULT_DATE_FORMAT).create();
        String jsonObject = gson.toJson(object);
        editor.putString(key, jsonObject);

        editor.apply();
    }

    public Object getObjectFromSharedPreferences(Context context, String key, Class<?> type) {
        SharedPreferences prefs = getSharedPreferences(context);
        String json = prefs.getString(key, "");
        if (json.length() == 0) return null;
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.setDateFormat(DEFAULT_DATE_FORMAT).create();
        return type.cast(gson.fromJson(json, type));
    }

    public String getFromSharedPreferences(Context context, String key, String defaultValue) {
        SharedPreferences prefs = getSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public String listPreferences(Context context) {
        StringBuffer resultBuffer = new StringBuffer("SharedPreferences");
        SharedPreferences prefs = getSharedPreferences(context);
        Map<String,?> map = prefs.getAll();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            Object value = map.get(key);
            resultBuffer.append(":\n");
            resultBuffer.append(key);
            resultBuffer.append(":\t");
            resultBuffer.append(value.toString());
        }
        return resultBuffer.toString();
    }

    public Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        return builder.setDateFormat(DEFAULT_DATE_FORMAT).create();
    }
}
