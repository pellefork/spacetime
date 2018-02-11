package se.fork.spacetime;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import se.fork.spacetime.database.PlaceLogEntry;
import se.fork.spacetime.database.SpacetimeDatabase;
import se.fork.spacetime.model.LoggablePlaceList;
import se.fork.spacetime.model.MyPlaceLists;
import se.fork.spacetime.utils.LocalStorage;

public class ReportActivity extends Activity {

    private Spinner listSpinner;
    private String currentListKey;
    private LoggablePlaceList currentList;
    private List<String> currentListKeys;
    private ListView listView;
    private LogListAdapter listAdapter;
    private List<PlaceLogEntry> logEntryList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);
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

        ListView logListView = findViewById(R.id.log_list);
        logEntryList = new LinkedList<>();
        listAdapter = new LogListAdapter();
        logListView.setAdapter(listAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        populatePlaceListSpinner();
        SpacetimeDatabase db = SpacetimeDatabase.getSpacetimeDatabase(this);
        new ReadLogDataTask(db).execute();
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

        private ReadLogDataTask(SpacetimeDatabase db) { // TODO Consider list name as param for later
            this.db = db;
        }


        @Override
        protected Void doInBackground(Void... voids) {
            logEntryList = db.placeLogEntryDao().getAll();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private class LogListAdapter extends BaseAdapter {
        final String dateFormatString = "yyyy-MM-dd'T'HH:mm:ss";
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
}
