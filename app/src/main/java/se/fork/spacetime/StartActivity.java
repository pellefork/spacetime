package se.fork.spacetime;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import se.fork.spacetime.model.LoggablePlace;
import se.fork.spacetime.model.LoggablePlaceList;
import se.fork.spacetime.model.MyPlaceLists;
import se.fork.spacetime.model.Presence;
import se.fork.spacetime.utils.LocalStorage;

public class StartActivity extends FragmentActivity
        implements GoogleApiClient.OnConnectionFailedListener {

    private static final int PLACE_PICKER_REQUEST = 1;
    private static final int EDIT_PLACE_REQUEST = 2;
    private Spinner listSpinner;
    private FloatingActionButton onOffButton;
    private String currentListKey;
    private LoggablePlaceList currentList;
    private List<String> currentListKeys;
    private Presence presence;
    private ListView listView;
    private PlaceListAdapter listAdapter;
    private boolean requestingLocation;
    private boolean isListDirty;
    // A reference to the service used to get location updates.
    private LogPlacesByLocationService mService = null;
    // The BroadcastReceiver used to listen from broadcasts from the service.
    private MyReceiver myReceiver;
    // Tracks the bound state of the service.
    private boolean mBound = false;
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogPlacesByLocationService.LocalBinder binder = (LogPlacesByLocationService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        checkPermission();
        requestingLocation = false;
        BottomNavigationView bottomNavigationView = (BottomNavigationView)
                findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.action_list);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_list:

                                break;
                            case R.id.action_map:
                                startActivity(new Intent(getApplicationContext(), MapsActivity.class));
                                break;
                            case R.id.action_report:
                                startActivity(new Intent(getApplicationContext(), ReportActivity.class));
                                break;
                        }
                        return false;
                    }
                });
        FloatingActionButton addPlaceButton = findViewById(R.id.fab_add);
        addPlaceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invokePlacepicker(v);
            }
        });

        onOffButton = findViewById(R.id.fab_onoff);
        onOffButton.setBackgroundTintList(ColorStateList.valueOf( getResources().getColor(R.color.powerFabOff, null)));
        onOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestingLocation) {
                    stopRequestingLocations(v);
                    onOffButton.setBackgroundTintList(ColorStateList.valueOf( getResources().getColor(R.color.powerFabOff, null)));
                    requestingLocation = false;
                } else {
                    startRequestingLocations(v);
                    onOffButton.setBackgroundTintList(ColorStateList.valueOf( getResources().getColor(R.color.powerFabOn, null)));
                    requestingLocation = true;
                }
            }
        });

        myReceiver = new MyReceiver();
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
        // LogPlacePresenceJob.schedulePeriodic();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, LogPlacesByLocationService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }


    public void checkPermission(){
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                ){//Can add more as per requirement

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH},
                    123);
        }
    }

    /**
     * Receiver for broadcasts sent by {@link LogPlacesByLocationService}.
     */
    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Location location = intent.getParcelableExtra(LogPlacesByLocationService.EXTRA_LOCATION);
            if (location != null) {
                Toast.makeText(StartActivity.this, location.toString(),
                        Toast.LENGTH_SHORT).show();
            }
            presence = intent.getParcelableExtra(LogPlacesByLocationService.EXTRA_PRESENCE);
            if (location != null) {
                Toast.makeText(StartActivity.this, presence.toString(),
                        Toast.LENGTH_SHORT).show();
            }
            listAdapter.notifyDataSetChanged();
        }
    }

    public void startRequestingLocations(View view) {
        mService.requestLocationUpdates();
    }

    public void stopRequestingLocations(View view) {
        mService.removeLocationUpdates();
    }

    private void setupPlaceList() {
        currentListKeys = new LinkedList();
        for (String key: currentList.getLoggablePlaces().keySet()) {
            currentListKeys.add(key);
        }
        listView = findViewById(R.id.place_list);
        listAdapter = new PlaceListAdapter();
        listView.setAdapter(listAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
                new IntentFilter(LogPlacesByLocationService.ACTION_BROADCAST));
        LocalStorage.getInstance().logAll(this);
        populatePlaceListSpinner();
        setupPlaceList();
        isListDirty = false;
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection);
            mBound = false;
        }
        super.onStop();
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

    public void invokeReport (View view) {
        startActivity(new Intent(this, ReportActivity.class));
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
        } else if (requestCode == EDIT_PLACE_REQUEST) {
            if (resultCode == RESULT_OK) {
                currentList = LocalStorage.getInstance().getLoggablePlaceList(this, currentListKey);
                listAdapter.notifyDataSetChanged();
            }
        }
    }

    private void saveNewLoggablePlace(Place place) {
        currentList.getLoggablePlaces().put(place.getId(), new LoggablePlace(place));
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
            return currentList.getLoggablePlaces().get(currentListKeys.get(i));

        }

        @Override
        public long getItemId(int i) {
            return getItem(i).getId().hashCode();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null)
                view = getLayoutInflater().inflate(R.layout.listitem_place, viewGroup, false);
            final LoggablePlace loggablePlace = getItem(i);

            if(loggablePlace != null) {
                View row = view.findViewById(R.id.row);
                TextView nameView = view.findViewById(R.id.name);
                TextView addressView = view.findViewById(R.id.address);
                final Switch enabledView = view.findViewById(R.id.enabled);
                if (presence != null) {
                    if(presence.getPresentPlaces().contains(loggablePlace.getId())) {
                        row.setBackgroundColor(getResources().getColor(R.color.colorPresentRowPositive));
                    } else {
                        row.setBackgroundColor(getResources().getColor(R.color.colorPresentRowNegative));
                    }
                }
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

                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(StartActivity.this, EditPlaceActivity.class);
                        intent.putExtra("current_list", currentListKey);
                        intent.putExtra("place", loggablePlace.getId());
                        startActivityForResult(intent, EDIT_PLACE_REQUEST);
                    }
                });
            }


            return view;
        }
    }
}
