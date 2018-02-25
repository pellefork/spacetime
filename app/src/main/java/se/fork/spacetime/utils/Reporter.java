package se.fork.spacetime.utils;

import android.provider.Settings;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.Calendar;
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
        PlaceReport report = null;
        if (rawData != null && rawData.size() > 0) {
            List<PlaceLogEntry> filteredData = getFilteredLogentryList(rawData);
            List<TimeSpan> timeSpans = new LinkedList<>();
            long totalDuration = 0;
            boolean dataInconsistent = false;

            // Find out if we start in or out pf place
            // If we start inside (first record is Out), add a record at the head with In state at time of report start
            if (!filteredData.get(0).isInside()) {
                PlaceLogEntry firstRealEntry = filteredData.get(0);
                PlaceLogEntry dummyEntry = new PlaceLogEntry(firstRealEntry.getPlaceId(), firstRealEntry.getPlaceName(), firstRealEntry.getListName(), true, reportStartTime);
                filteredData.add(0, dummyEntry);
                Log.d(this.getClass().getSimpleName(), "getPlaceReport, adding first record = " + dummyEntry);
            }

            // If we end inside (last record is In), add a record at the tail with Out state at time of report end
            if (filteredData.get(filteredData.size()-1).isInside() ) {
                PlaceLogEntry lastRealEntry = filteredData.get(filteredData.size()-1);
                PlaceLogEntry dummyEntry = new PlaceLogEntry(lastRealEntry.getPlaceId(), lastRealEntry.getPlaceName(), lastRealEntry.getListName(), false, reportStopTime);
                filteredData.add(dummyEntry);
                Log.d(this.getClass().getSimpleName(), "getPlaceReport, adding last record = " + dummyEntry);
            }

            Log.d(this.getClass().getSimpleName(), "getPlaceReport, filteredData.size() = " + filteredData.size());
            for (PlaceLogEntry entry: filteredData) {
                Log.d(this.getClass().getSimpleName(), "entry = " + entry);
            }

            if (filteredData.size() % 2 != 0 ) {
                dataInconsistent = true;
            }

            for (int i = 0; i < filteredData.size()-1; i += 2) {
                PlaceLogEntry entry = filteredData.get(i);
                PlaceLogEntry exit = filteredData.get(i+1);
                if (entry.isInside() && !exit.isInside()) {
                    TimeSpan timeSpan = new TimeSpan(entry.getTimestamp(), exit.getTimestamp());
                    timeSpans.add(timeSpan);
                    totalDuration += timeSpan.getDuration();
                } else {
                    dataInconsistent = true;
                }
            }
            report = new PlaceReport(place, reportStartTime, reportStopTime, totalDuration, timeSpans, dataInconsistent);
        }

        return report;
    }

    public static TimeSpan getPeriod(int selection) {
        TimeSpan timeSpan = null;
        switch (selection) {
            case Constants.THIS_WEEK:
                timeSpan = getWeek(0,true);
                break;
            case Constants.LAST_WEEK:
                timeSpan = getWeek(-1, false);
                break;
            case Constants.THIS_MONTH:
                timeSpan = getMonth(0, true);
                break;
            case Constants.LAST_MONTH:
                timeSpan = getMonth(-1, false);
                break;
            default:
                throw new IllegalArgumentException("Method accepts only fixed period arguments 0 -3");
        }
        return timeSpan;
    }

    public static TimeSpan getWeek(int offset, boolean truncateAtNow) {
        long periodStart;
        long periodStop;
        // get today and clear time of day
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        // get start of this week in milliseconds

        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        cal.add(Calendar.WEEK_OF_YEAR, offset);
        Log.d("Reporter", "Start of this week:       " + cal.getTime());
        Log.d("Reporter", "... in milliseconds:      " + cal.getTimeInMillis());
        periodStart = cal.getTimeInMillis();

        // start of the next week

        cal.add(Calendar.WEEK_OF_YEAR, 1);
        Log.d("Reporter", "Start of the next week:   " + cal.getTime());
        Log.d("Reporter", "... in milliseconds:      " + cal.getTimeInMillis());
        periodStop = cal.getTimeInMillis();
        long now = System.currentTimeMillis();
        if (truncateAtNow) {
            if (periodStop > now) periodStop = now;
        }
        return new TimeSpan(periodStart, periodStop);
    }

    public static TimeSpan getMonth(int offset, boolean truncateAtNow) {
        long periodStart;
        long periodStop;
        // get today and clear time of day
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of day !
        cal.clear(Calendar.MINUTE);
        cal.clear(Calendar.SECOND);
        cal.clear(Calendar.MILLISECOND);

        // get start of this week in milliseconds

        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MONTH, offset);
        Log.d("Reporter", "Start of this month:       " + cal.getTime());
        Log.d("Reporter", "... in milliseconds:      " + cal.getTimeInMillis());
        periodStart = cal.getTimeInMillis();

        // start of the next week

        cal.add(Calendar.MONTH, 1);
        Log.d("Reporter", "Start of the next month:   " + cal.getTime());
        Log.d("Reporter", "... in milliseconds:      " + cal.getTimeInMillis());
        periodStop = cal.getTimeInMillis();
        long now = System.currentTimeMillis();
        if (truncateAtNow) {
            if (periodStop > now) periodStop = now;
        }
        return new TimeSpan(periodStart, periodStop);
    }

}
