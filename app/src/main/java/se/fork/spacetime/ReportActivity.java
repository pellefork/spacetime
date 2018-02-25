package se.fork.spacetime;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import se.fork.spacetime.adapters.PlaceReportExpandableListAdapter;
import se.fork.spacetime.database.PlaceLogEntry;
import se.fork.spacetime.database.SpacetimeDatabase;
import se.fork.spacetime.model.LoggablePlace;
import se.fork.spacetime.model.LoggablePlaceList;
import se.fork.spacetime.model.MyPlaceLists;
import se.fork.spacetime.model.PlaceReport;
import se.fork.spacetime.utils.LocalStorage;
import se.fork.spacetime.utils.Reporter;

public class ReportActivity extends Activity {

    private Spinner listSpinner;
    private String currentListKey;
    private LoggablePlaceList currentList;
    private List<String> currentListKeys;
    private ListView listView;
    // private LogListAdapter listAdapter;
    private ExpandableListAdapter expandableListAdapter;
    private ExpandableListView expandableListView;
    private List<PlaceReport> reportList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
        expandableListView = findViewById(R.id.expandable_list_view);
        listSpinner = findViewById(R.id.placelist_spinner);
        listSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                MyPlaceLists listLists = LocalStorage.getInstance().getMyPlaceLists(getApplicationContext());
                currentListKey = listLists.getKeys().get(i);
                Log.d(this.getClass().getSimpleName(), "onItemSelected, i = " + i + ", key = " + currentListKey);
                currentList = LocalStorage.getInstance().getLoggablePlaceList(getApplicationContext(), currentListKey);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        populatePlaceListSpinner();
        SpacetimeDatabase db = SpacetimeDatabase.getSpacetimeDatabase(this);
        MyPlaceLists listLists = LocalStorage.getInstance().getMyPlaceLists(getApplicationContext());
        currentListKey = listLists.getKeys().get(0);    // TODO Get value from spinner
        currentList = LocalStorage.getInstance().getLoggablePlaceList(getApplicationContext(), currentListKey);

        new ReadLogDataTask(this, db, currentList).execute();
    }

    private void populatePlaceListSpinner() {
        List<String> keys = LocalStorage.getInstance().getMyPlaceLists(this).getKeys();
        List<String> listList = new ArrayList<>();
        for(String key: keys) {
            listList.add(LocalStorage.getInstance().getLoggablePlaceList(this, key).getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        listSpinner.setAdapter(adapter);
    }

    private class ReadLogDataTask extends AsyncTask<Void, Void, Void> {

        private final SpacetimeDatabase db;
        private Context context;
        private LoggablePlaceList placeList;

        private ReadLogDataTask(Context context, SpacetimeDatabase db, LoggablePlaceList placeList) {
            this.db = db;
            this.context = context;
            this.placeList = placeList;
        }


        @Override
        protected Void doInBackground(Void... voids) {
            reportList = new LinkedList<>();
            for (LoggablePlace place: currentList.getLoggablePlaces().values()) {
                List<PlaceLogEntry> logEntryList = db.placeLogEntryDao().getAllByPlace(place.getId());  // TODO Replace with query by timespan
                PlaceReport report = Reporter.getInstance().getPlaceReport(place, logEntryList, new Date(0).getTime(), new Date().getTime());
                if (report != null) {
                    reportList.add(report);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            expandableListAdapter = new PlaceReportExpandableListAdapter(context, new Date(0), new Date(), reportList);
            expandableListView.setAdapter(expandableListAdapter);
        }
    }

/*
    private class LogListAdapter extends BaseAdapter {
        final String dateFormatString = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
        @Override
        public int getCount() {
            return logEntryList.size();
        }

        @Override
        public PlaceLogEntry getItem(int position) {
            return logEntryList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return logEntryList.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = getLayoutInflater().inflate(R.layout.listitem_report, parent, false);

            TextView placeNameView = convertView.findViewById(R.id.name);
            TextView statusView = convertView.findViewById(R.id.status);
            TextView timestampView = convertView.findViewById(R.id.timestamp);
            PlaceLogEntry entry = getItem(position);
            placeNameView.setText(entry.getPlaceName());
            if (entry.isInside()) {
                statusView.setText("In");
            } else {
                statusView.setText("Out");
            }
            Date timestamp = new Date(entry.getTimestamp());
            String timeStampStr = dateFormat.format(timestamp);
            timestampView.setText(timeStampStr);

            return convertView;
        }
    }
*/
}
