package se.fork.spacetime.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

/**
 * Created by per.fork on 2018-02-03.
 */

@Database(entities = {PlaceLogEntry.class}, version = 1)
public abstract class SpacetimeDatabase extends RoomDatabase {

    private static SpacetimeDatabase INSTANCE;

    public abstract PlaceLogEntryDao placeLogEntryDao();

    public static SpacetimeDatabase getSpacetimeDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE =
                    Room.databaseBuilder(context.getApplicationContext(), SpacetimeDatabase.class, "spacetime-database")
                            // allow queries on the main thread.
                            // Don't do this on a real app! See PersistenceBasicSample for an example.
                            .allowMainThreadQueries()
                            .build();
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }
}
