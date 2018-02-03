package se.fork.spacetime.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by per.fork on 2017-12-27.
 */

public class LoggablePlaceList {
    private static final String KEY_PREFIX = "LOGGABLE_LIST_";
    private String name;
    private String key;
    private Date createdTimeStamp;
    private Map<String,LoggablePlace> loggablePlaces;

    public LoggablePlaceList() {
    }

    public LoggablePlaceList(String name) {
        this.name = name;
        this.key = KEY_PREFIX + name.replaceAll( " ", "_");
        loggablePlaces = new LinkedHashMap<>();
        createdTimeStamp = new Date();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Date getCreatedTimeStamp() {
        return createdTimeStamp;
    }

    public void setCreatedTimeStamp(Date createdTimeStamp) {
        this.createdTimeStamp = createdTimeStamp;
    }

    public Map<String, LoggablePlace> getLoggablePlaces() {
        return loggablePlaces;
    }

    public void setLoggablePlaces(Map<String, LoggablePlace> loggablePlaces) {
        this.loggablePlaces = loggablePlaces;
    }

    @Override
    public String toString() {
        return "LoggablePlaceList{" +
                "name='" + name + '\'' +
                ", createdTimeStamp=" + createdTimeStamp +
                ", loggablePlaces=" + loggablePlaces +
                '}';
    }
}
