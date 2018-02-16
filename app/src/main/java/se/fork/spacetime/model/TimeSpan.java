package se.fork.spacetime.model;

import java.util.UUID;

import se.fork.spacetime.utils.Reporter;

/**
 * Created by per.fork on 2018-02-12.
 */

public class TimeSpan {
    private long fromTimestamp;
    private long toTimeStamp;
    private long id;

    public TimeSpan(long fromTimestamp, long toTimeStamp) {
        this.fromTimestamp = fromTimestamp;
        this.toTimeStamp = toTimeStamp;
        this.id = UUID.randomUUID().hashCode();
    }

    public long getId() {
        return id;
    }

    public long getFromTimestamp() {
        return fromTimestamp;
    }

    public long getToTimeStamp() {
        return toTimeStamp;
    }

    public long getDuration() {
        return toTimeStamp - fromTimestamp;
    }

    public String getFormattedDuration() {
        return Reporter.getFormattedDuration(getDuration());
    }
}