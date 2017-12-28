package se.fork.spacetime;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.List;

import se.fork.spacetime.model.LoggablePlace;
import se.fork.spacetime.model.LoggablePlaceList;
import se.fork.spacetime.model.MyPlaceLists;
import se.fork.spacetime.utils.LocalStorage;

public class StartActivity extends FragmentActivity
        implements GoogleApiClient.OnConnectionFailedListener {

    private static final int PLACE_PICKER_REQUEST = 1;
    private Spinner listSpinner;
    private String currentListKey;
    private LoggablePlaceList currentList;
    private ListView listView;
    private PlaceListAdapter listAdapter;
    private boolean isListDirty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
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
        if(!isInitialized()) {
            initialize();
        } else {
            currentListKey = LocalStorage.getInstance().getMyPlaceLists(this).getKeys().get(0);
            currentList = LocalStorage.getInstance().getLoggablePlaceList(this, currentListKey);
        }
    }

    private void setupPlaceList() {
        listView = findViewById(R.id.place_list);
        listAdapter = new PlaceListAdapter();
        listView.setAdapter(listAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalStorage.getInstance().logAll(this);
        populatePlaceListSpinner();
        setupPlaceList();
        isListDirty = false;
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

    public void invokeMap(View view) {
        startActivity(new Intent(this, MapsActivity.class));
    }

    public void invokePlacepicker(View view) {
        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();

        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);
                Log.d(this.getClass().getSimpleName(), "onActivityResult Place: " + place.getName() + ", latlong: " + place.getLatLng() + ", types: " + place.getPlaceTypes());
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                saveNewLoggablePlace(place);
            }
        }
    }

    private void saveNewLoggablePlace(Place place) {
        currentList.getLoggablePlaces().add(new LoggablePlace(place));
        LocalStorage.getInstance().saveLoggablePlaceList(currentList, this);
        LocalStorage.getInstance().logAll(this);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    private boolean isInitialized() {
        return LocalStorage.getInstance().getMyPlaceLists(this) != null;
    }

    private void initialize() {
        LoggablePlaceList loggablePlaceList = new LoggablePlaceList("Work");
        MyPlaceLists myPlaceLists = new MyPlaceLists();
        myPlaceLists.addKey(loggablePlaceList.getKey());
        currentListKey = loggablePlaceList.getKey();
        LocalStorage.getInstance().saveMyPlaceLists(this, myPlaceLists);
        LocalStorage.getInstance().logAll(this);
        LocalStorage.getInstance().saveLoggablePlaceList(loggablePlaceList, this);
        currentList = loggablePlaceList;
    }

    private class PlaceListAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return currentList.getLoggablePlaces().size();
        }

        @Override
        public LoggablePlace getItem(int i) {
            return currentList.getLoggablePlaces().get(i);
        }

        @Override
        public long getItemId(int i) {
            return currentList.getLoggablePlaces().get(i).getId().hashCode();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null)
                view = getLayoutInflater().inflate(R.layout.listitem_place, viewGroup, false);
            final LoggablePlace loggablePlace = getItem(i);
            if(loggablePlace != null) {
                TextView nameView = view.findViewById(R.id.name);
                TextView addressView = view.findViewById(R.id.address);
                final Switch enabledView = view.findViewById(R.id.enabled);
                nameView.setText(loggablePlace.getName());
                addressView.setText(loggablePlace.getAddress());
                enabledView.setChecked(loggablePlace.isEnabled());
                enabledView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        loggablePlace.setEnabled(enabledView.isChecked());
                        isListDirty = true;
                    }
                });
            }


            return view;
        }
    }
}
