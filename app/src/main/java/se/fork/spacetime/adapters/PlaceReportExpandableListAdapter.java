package se.fork.spacetime.adapters;

import android.arch.persistence.room.Dao;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import se.fork.spacetime.R;
import se.fork.spacetime.model.PlaceReport;
import se.fork.spacetime.model.TimeSpan;
import se.fork.spacetime.utils.Reporter;

/**
 * Created by per.fork on 2018-02-16.
 */

public class PlaceReportExpandableListAdapter extends BaseExpandableListAdapter {
    final String dateFormatString = "yyyy-MM-dd HH:mm:ss";
    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);

    private Context context;
    private Date fromDate;
    private Date toDate;
    private List<PlaceReport> placeReports;

    private Map<String, PlaceReport> placeReportMap;

    public PlaceReportExpandableListAdapter(Context context, Date fromDate, Date toDate, List<PlaceReport> placeReports) {
        this.context = context;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.placeReports = placeReports;
        placeReportMap = new LinkedHashMap<>(placeReports.size());
        for (PlaceReport placeReport: this.placeReports) {
            placeReportMap.put(placeReport.getPlace().getId(), placeReport);
        }
    }

    @Override
    public int getGroupCount() {
        return placeReports.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return placeReports.get(groupPosition).getTimeSpans().size();
    }

    @Override
    public PlaceReport getGroup(int groupPosition) {
        return placeReports.get(groupPosition);
    }

    @Override
    public TimeSpan getChild(int groupPosition, int childPosition) {
        return getGroup(groupPosition).getTimeSpans().get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return getGroup(groupPosition).getPlace().getId().hashCode();
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return getChild(groupPosition, childPosition).getId();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.listitem_report_group, null);
        }
        TextView nameView = convertView.findViewById(R.id.name);
        TextView addressView = convertView.findViewById(R.id.address);
        TextView durationView = convertView.findViewById(R.id.duration);
        PlaceReport report = getGroup(groupPosition);
        nameView.setText(report.getPlace().getName());
        addressView.setText(report.getPlace().getAddress());
        durationView.setText(Reporter.getFormattedDuration(report.getTotalDuration()));
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = layoutInflater.inflate(R.layout.listitem_report_child, null);
        }

        TextView totalDurationView = convertView.findViewById(R.id.duration);
        TextView inDateView = convertView.findViewById(R.id.in_date);
        TextView outDateView = convertView.findViewById(R.id.out_date);

        TimeSpan timeSpan = getChild(groupPosition, childPosition);
        totalDurationView.setText(timeSpan.getFormattedDuration());
        inDateView.setText(dateFormat.format(new Date(timeSpan.getFromTimestamp())));
        outDateView.setText(dateFormat.format(new Date(timeSpan.getToTimeStamp())));

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}
