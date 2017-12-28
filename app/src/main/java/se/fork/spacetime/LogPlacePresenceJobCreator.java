package se.fork.spacetime;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by per.fork on 2017-12-28.
 */

public class LogPlacePresenceJobCreator implements JobCreator {
    @Override
    public Job create(String tag) {
        switch (tag) {
            case LogPlacePresenceJob.TAG:
                return new LogPlacePresenceJob();
            default:
                return null;
        }
    }
}
