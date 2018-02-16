package se.fork.spacetime.model;

import java.util.List;

import se.fork.spacetime.database.PlaceLogEntry;

/**
 * Created by per.fork on 2018-02-12.
 */

public class PlaceReport {
    private LoggablePlace place;
    private long fromTimestamp;
    private long toTimeStamp;
    private long totalDuration;
    private List<TimeSpan> timeSpans;

    public PlaceReport(LoggablePlace place, long fromTimestamp, long toTimeStamp, long totalDuration, List<TimeSpan> timeSpans) {
        this.place = place;
        this.fromTimestamp = fromTimestamp;
        this.toTimeStamp = toTimeStamp;
        this.totalDuration = totalDuration;
        this.timeSpans = timeSpans;
    }

    public LoggablePlace getPlace() {
        return place;
    }

    public long getFromTimestamp() {
        return fromTimestamp;
    }

    public long getToTimeStamp() {
        return toTimeStamp;
    }

    public long getTotalDuration() {
        return totalDuration;
    }

    public List<TimeSpan> getTimeSpans() {
        return timeSpans;
    }
}
