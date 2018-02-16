package se.fork.spacetime.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Created by per.fork on 2018-02-03.
 */

@Dao
public interface PlaceLogEntryDao {

    @Query("select * from place_log_entry")
    List<PlaceLogEntry> getAll();

    @Query("select * " +
            "from place_log_entry" +
            " where place_id = :placeId" +
            " order by time_stamp"
    )
    List<PlaceLogEntry> getAllByPlace(String placeId);

    @Query("select count(*) from place_log_entry")
    long countRecords();

    @Insert
    void insertAll(PlaceLogEntry... entry);

    @Delete
    void delete(PlaceLogEntry entry);
}
