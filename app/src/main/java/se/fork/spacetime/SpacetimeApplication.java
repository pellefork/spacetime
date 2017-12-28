package se.fork.spacetime;

import android.app.Application;

import com.evernote.android.job.JobManager;

/**
 * Created by per.fork on 2017-12-28.
 */

public class SpacetimeApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // JobManager.create(this).addJobCreator(new LogPlacePresenceJobCreator());
        // JobManager.instance().getConfig().setAllowSmallerIntervalsForMarshmallow(true); // Don't use this in production

    }
}
