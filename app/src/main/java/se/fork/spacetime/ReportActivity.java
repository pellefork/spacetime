package se.fork.spacetime;

import android.app.Activity;
import android.app.DatePickerDialog;
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
import android.widget.DatePicker;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
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
import se.fork.spacetime.model.TimeSpan;
import se.fork.spacetime.utils.Constants;
import se.fork.spacetime.utils.LocalStorage;
import se.fork.spacetime.utils.Reporter;

public class ReportActivity extends Activity {

    private Spinner listSpinner;
    private Spinner periodSpinner;
    private String currentListKey;
    private LoggablePlaceList currentList;
    private List<String> currentListKeys;
    private ListView listView;
    // private LogListAdapter listAdapter;
    private ExpandableListAdapter expandableListAdapter;
    private ExpandableListView expandableListView;
    private List<PlaceReport> reportList;

    final String dateFormatString = "yyyy-MM-dd HH:mm:ss";
    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);

    private int currentPeriodSelection;
    private TimeSpan currentPeriod;


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
        currentPeriodSelection = 0;
        periodSpinner = findViewById(R.id.report_period_spinner);
        periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPeriodSelection = position;
                if (currentPeriodSelection == Constants.OTHER_PERIOD) {
                    letUserSelectPeriod();
                } else {
                    currentPeriod = getCurrentPeriod();
                    updatePeriodView();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                resetPeriod();
            }
        });
        resetPeriod();
    }

    private void resetPeriod() {
        currentPeriodSelection = 0;
        periodSpinner.setSelection(currentPeriodSelection);
        currentPeriod = getCurrentPeriod();
        updatePeriodView();
    }

    private TimeSpan getCurrentPeriod() {
        TimeSpan period = Reporter.getPeriod(currentPeriodSelection);
        return period;
    }

    private void letUserSelectPeriod() {
        letUserSelectStartDate();
    }

    private void letUserSelectStartDate() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener fromDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.clear(Calendar.MINUTE);
                calendar.clear(Calendar.SECOND);
                calendar.clear(Calendar.MILLISECOND);
                currentPeriod = new TimeSpan(calendar.getTimeInMillis(), System.currentTimeMillis());
                letUserSelectStopDate();
            }
        };
        DatePickerDialog fromDateDialog = new DatePickerDialog(this, fromDateListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        fromDateDialog.setMessage(getString(R.string.report_select_start_date));
        fromDateDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        fromDateDialog.show();
    }

    private void letUserSelectStopDate () {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog.OnDateSetListener fromDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.clear(Calendar.MINUTE);
                calendar.clear(Calendar.SECOND);
                calendar.clear(Calendar.MILLISECOND);
                calendar.add(Calendar.DATE, 1);    // User wants inclusive dates so e.g. a single day report could be Jan 23 -> Jan 23
                currentPeriod = new TimeSpan(currentPeriod.getFromTimestamp(), calendar.getTimeInMillis());
                updatePeriodView();
            }
        };
        DatePickerDialog toDateDialog = new DatePickerDialog(this, fromDateListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        toDateDialog.setMessage(getString(R.string.report_select_end_date));
        toDateDialog.getDatePicker().setMinDate(currentPeriod.getFromTimestamp());
        toDateDialog.show();
    }

    private void updatePeriodView() {
        TextView periodStartView = findViewById(R.id.period_start);
        TextView periodEndView = findViewById(R.id.period_end);
        String periodStartString = dateFormat.format(new Date(currentPeriod.getFromTimestamp()));
        String periodEndString = dateFormat.format(new Date(currentPeriod.getToTimeStamp()));
        periodStartView.setText(periodStartString);
        periodEndView.setText(periodEndString);
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
}
