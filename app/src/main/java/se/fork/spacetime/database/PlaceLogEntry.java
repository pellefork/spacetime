package se.fork.spacetime.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import java.util.Date;

/**
 * Created by per.fork on 2018-02-03.
 */

@Entity(tableName = "place_log_entry")
public class PlaceLogEntry {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "place_id")
    private String placeId;

    @ColumnInfo(name = "place_name")
    private String placeName;

    @ColumnInfo(name = "list_name")
    private String listName;

    @ColumnInfo(name = "inside")
    private boolean inside;

    @ColumnInfo(name = "time_stamp")
    private long timestamp;     // TODO Change to long for timestamp and convert outside

    public PlaceLogEntry(String placeId, String placeName, String listName, boolean inside, long timestamp) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.listName = listName;
        this.inside = inside;
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getListName() {
        return listName;
    }

    public void setListName(String listName) {
        this.listName = listName;
    }

    public boolean isInside() {
        return inside;
    }

    public void setInside(boolean inside) {
        this.inside = inside;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "PlaceLogEntry{" +
                "placeId='" + placeId + '\'' +
                ", placeName='" + placeName + '\'' +
                ", listName='" + listName + '\'' +
                ", inside=" + inside +
                ", timestamp=" + new Date(timestamp) +
                '}';
    }
}
