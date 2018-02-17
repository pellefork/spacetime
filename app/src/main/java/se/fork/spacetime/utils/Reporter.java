package se.fork.spacetime.utils;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import se.fork.spacetime.database.PlaceLogEntry;
import se.fork.spacetime.model.LoggablePlace;
import se.fork.spacetime.model.PlaceReport;
import se.fork.spacetime.model.TimeSpan;

/**
 * Created by per.fork on 2018-02-12.
 */

public class Reporter {
    public static final long MS_PER_HOUR = 1000 * 60 * 60;
    public static final long MS_PER_MINUTE = 1000 * 60;
    public static final long MS_PER_SECOND = 1000;


    private static final Reporter ourInstance = new Reporter();

    public static Reporter getInstance() {
        return ourInstance;
    }

    private Reporter() {
    }

    public List<PlaceLogEntry> getFilteredLogentryList(List<PlaceLogEntry> rawDataList) {
        // TODO Create filter algorithm here
        return rawDataList;
    }

    public static String getFormattedDuration(long duration) {
        long ms = duration;
        long remainder = 0;
        long hours = ms / MS_PER_HOUR;
        remainder = ms - (hours * MS_PER_HOUR);
        long minutes = remainder / MS_PER_MINUTE;
        remainder = remainder - (minutes * MS_PER_MINUTE);
        long seconds = remainder / MS_PER_SECOND;

        StringBuilder sb = new StringBuilder();
        sb.append(new DecimalFormat("#00").format(hours));
        sb.append(":");
        sb.append(new DecimalFormat("00").format(minutes));
        sb.append(":");
        sb.append(new DecimalFormat("00").format(seconds));
        return  sb.toString();
    }

    public PlaceReport getPlaceReport(LoggablePlace place, List<PlaceLogEntry> rawData, long reportStartTime, long reportStopTime) {
        List<PlaceLogEntry> filteredData = getFilteredLogentryList(rawData);
        List<TimeSpan> timeSpans = new LinkedList<>();
        long totalDuration = 0;

        // Find out if we start in or out pf place
        // If we start inside (first record is Out), add a record at the head with In state at time of report start
        if (!filteredData.get(0).isInside()) {
            PlaceLogEntry firstRealEntry = filteredData.get(0);
            PlaceLogEntry dummyEntry = new PlaceLogEntry(firstRealEntry.getPlaceId(), firstRealEntry.getPlaceName(), firstRealEntry.getListName(), true, reportStartTime);
            filteredData.add(0, dummyEntry);
        }

        // If we end inside (last record is In), add a record at the tail with Out state at time of report end
        if (filteredData.get(filteredData.size()-1).isInside() ) {
            PlaceLogEntry firstRealEntry = filteredData.get(0);
            PlaceLogEntry dummyEntry = new PlaceLogEntry(firstRealEntry.getPlaceId(), firstRealEntry.getPlaceName(), firstRealEntry.getListName(), false, reportStopTime);
            filteredData.add(dummyEntry);
        }

        for (int i = 0; i < filteredData.size(); i += 2) {
            PlaceLogEntry entry = filteredData.get(i);
            PlaceLogEntry exit = filteredData.get(i+1);
            TimeSpan timeSpan = new TimeSpan(entry.getTimestamp(), exit.getTimestamp());
            timeSpans.add(timeSpan);
            totalDuration += timeSpan.getDuration();
        }
        PlaceReport report = new PlaceReport(place, reportStartTime, reportStopTime, totalDuration, timeSpans);

        return report;
    }
}
