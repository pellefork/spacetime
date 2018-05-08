package se.fork.spacetime;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import se.fork.spacetime.model.LoggablePlace;
import se.fork.spacetime.model.LoggablePlaceList;
import se.fork.spacetime.model.MyPlaceLists;
import se.fork.spacetime.model.Presence;
import se.fork.spacetime.utils.Constants;
import se.fork.spacetime.utils.LocalStorage;

public class StartActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener, NavigationView.OnNavigationItemSelectedListener, OnCompleteListener<Void> {

    private Spinner listSpinner;
    // private FloatingActionButton onOffButton;
    private ImageButton onOffButton;
    private String currentListKey;
    private LoggablePlaceList currentList;
    private List<String> currentListKeys;
    private Presence presence;
    private SwipeMenuListView listView;
    private PlaceListAdapter listAdapter;
    private boolean requestingLocation;
    private boolean isListDirty;

    private MyReceiver myReceiver;

    private List<String> listList;  // The whole list of place lists

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    // Geofencing data

    private enum PendingGeofenceTask {
        ADD, REMOVE, NONE
    }

    /**
     * Provides access to the Geofencing API.
     */
    private GeofencingClient mGeofencingClient;

    /**
     * The list of geofences used in this sample.
     */
    private List<Geofence> mGeofenceList;

    /**
     * Used when requesting to add or remove geofences.
     */
    private PendingIntent mGeofencePendingIntent;

    private PendingGeofenceTask mPendingGeofenceTask = PendingGeofenceTask.NONE;


    // TODO Remove from here

/*
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

*/
    // TODO to here

    private Geofence createGeofenceFromPlace(LoggablePlace place) {
        return new Geofence.Builder()
                .setRequestId(place.getId())
                .setCircularRegion(place.getLatitude(), place.getLongitude(), (float) place.getRadius())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    private List<Geofence> createGeofenceList(LoggablePlaceList placeList) {
        Log.d(this.getClass().getSimpleName(), "createGeofenceList from list: " + placeList);
        List<Geofence> resultList = null;
        int size = placeList.getLoggablePlaces().size();
        if ( size != 0) {
            resultList = new ArrayList<>(size);
            for (LoggablePlace place: placeList.getLoggablePlaces().values()) {
                Log.d(this.getClass().getSimpleName(), "createGeofenceList from place: " + place);
                Geofence geofence = createGeofenceFromPlace(place);
                resultList.add(geofence);
            }
        }
        return resultList;
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }


    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest(List<Geofence> geofenceList) {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    @SuppressWarnings("MissingPermission")
    private void populateGeofenceLists() {
        fetchPlaceLists();

        if (!checkPermissions()) {
            requestPermissions();
        } else {
            performPendingGeofenceTask();
        }


        // TODO Maybe create one PendingIntent for each LoggablePlaceList. Max is five, and total number of places max is 100

        if (mGeofenceList != null) {
            mGeofenceList.clear();
        } else {
            mGeofenceList = new ArrayList<>();
        }
        for (String key: listList) {
            Log.d(this.getClass().getSimpleName(), "populateGeofenceLists: In loop, key = " + key);
            LoggablePlaceList placeList = LocalStorage.getInstance().getLoggablePlaceList(this, key);
            mGeofenceList.addAll(createGeofenceList(placeList));
        }
    }

    /**
     * Return the current state of the permissions needed.
     */
    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(this.getClass().getSimpleName(), "Displaying permission rationale to provide additional context.");
            showSnackbar(R.string.permission_rationale, android.R.string.ok,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(StartActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    });
        } else {
            Log.i(this.getClass().getSimpleName(), "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(StartActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Performs the geofencing task that was pending until location permission was granted.
     */
    private void performPendingGeofenceTask() {
        if (mPendingGeofenceTask == PendingGeofenceTask.ADD) {
            addGeofences();
        } else if (mPendingGeofenceTask == PendingGeofenceTask.REMOVE) {
            removeGeofences();
        }
    }

    /**
     * Returns true if geofences were added, otherwise false.
     */
    private boolean getGeofencesAdded() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                Constants.GEOFENCES_ADDED_KEY, false);
    }

    /**
     * Stores whether geofences were added ore removed in {@link SharedPreferences};
     *
     * @param added Whether geofences were added or removed.
     */
    private void updateGeofencesAdded(boolean added) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(Constants.GEOFENCES_ADDED_KEY, added)
                .apply();
    }



    /**
     * Removes geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    private void removeGeofences() {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions));
            return;
        }
        Log.d(this.getClass().getSimpleName(), "removeGeofences");
        mGeofencingClient.removeGeofences(getGeofencePendingIntent()).addOnCompleteListener(this);
    }

    /**
     * Runs when the result of calling {@link #addGeofences()} and/or {@link #removeGeofences()}
     * is available.
     * @param task the resulting Task, containing either a result or error.
     */
    @Override
    public void onComplete(@NonNull Task<Void> task) {
        mPendingGeofenceTask = PendingGeofenceTask.NONE;
        if (task.isSuccessful()) {
            updateGeofencesAdded(!getGeofencesAdded());

            int messageId = getGeofencesAdded() ? R.string.geofences_added :
                    R.string.geofences_removed;
            Toast.makeText(this, getString(messageId), Toast.LENGTH_SHORT).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this, task.getException());
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            Log.w(this.getClass().getSimpleName(), errorMessage);
        }
    }


    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        Log.i(this.getClass().getSimpleName(), "onRequestPermissionResult");
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(this.getClass().getSimpleName(), "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(this.getClass().getSimpleName(), "Permission granted.");
                performPendingGeofenceTask();
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                showSnackbar(R.string.permission_denied_explanation, R.string.settings,
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        });
                mPendingGeofenceTask = PendingGeofenceTask.NONE;
            }
        }
    }

    /**
     * Shows a {@link Snackbar} using {@code text}.
     *
     * @param text The Snackbar text.
     */
    private void showSnackbar(final String text) {
        View container = findViewById(android.R.id.content);
        if (container != null) {
            Snackbar.make(container, text, Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Shows a {@link Snackbar}.
     *
     * @param mainTextStringId The id for the string resource for the Snackbar text.
     * @param actionStringId   The text of the action item.
     * @param listener         The listener associated with the Snackbar action.
     */
    private void showSnackbar(final int mainTextStringId, final int actionStringId,
                              View.OnClickListener listener) {
        Snackbar.make(
                findViewById(android.R.id.content),
                getString(mainTextStringId),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(actionStringId), listener).show();
    }


    private void setupGeofencing() {

    }



    /**
        * Adds geofences. This method should be called after the user has granted the location
     * permission.
     */
    @SuppressWarnings("MissingPermission")
    private void addGeofences() {
        if (!checkPermissions()) {
            showSnackbar(getString(R.string.insufficient_permissions));
            return;
        }

        Log.d(this.getClass().getSimpleName(), "addGeofences: " + mGeofenceList);

        mGeofencingClient.addGeofences(getGeofencingRequest(mGeofenceList), getGeofencePendingIntent())
            .addOnCompleteListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        myReceiver = new MyReceiver();

        checkPermission();
        requestingLocation = false;

        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<>();

        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;
        mGeofencingClient = LocationServices.getGeofencingClient(this);

        populateGeofenceLists();
        updateGeofencesAdded(false);

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


        onOffButton = findViewById(R.id.onoff);
        setOnOffButtonColor(R.color.powerFabOff);
        onOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestingLocation) {
                    removeGeofences();
                    setOnOffButtonColor(R.color.powerFabOff);
                    requestingLocation = false;
                } else {
                    addGeofences();
                    setOnOffButtonColor(R.color.powerFabOn);
                    requestingLocation = true;
                }
            }
        });

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

    private void setupSwipeList() {
        SwipeMenuCreator swipeCreator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                // create "open" item
                SwipeMenuItem opItem = new SwipeMenuItem(
                        getApplicationContext());
                // set item background
                opItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                        0xCE)));
                opItem.setWidth((250));
                opItem.setTitle("Edit");
                opItem.setTitleSize(20);
                opItem.setTitleColor(Color.WHITE);
                menu.addMenuItem(opItem);
                SwipeMenuItem delItem = new SwipeMenuItem(
                        getApplicationContext());
                delItem.setBackground(new ColorDrawable(Color.rgb(0xC9, 0xC9,
                        0xCE)));
                delItem.setTitleSize(20);
                delItem.setBackground(R.color.colorAccent);
                delItem.setWidth((250));
                delItem.setTitleColor(Color.WHITE);
                delItem.setTitle("Delete");
                menu.addMenuItem(delItem);
            }
        };
// set creator
        listView.setMenuCreator(swipeCreator);
        listView.setSwipeDirection(SwipeMenuListView.DIRECTION_LEFT);
        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index) {
                    case 0:
                        // open
                        Toast.makeText(getApplicationContext(), "Index: " + index + " " + "Position: " + position, Toast.LENGTH_LONG).show();
                        //Toast.makeText(CustomerListActivity.this,"Position: "+index,Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(StartActivity.this, EditPlaceActivity.class);
                        intent.putExtra("current_list", currentListKey);
                        intent.putExtra("place",  currentList.getLoggablePlaces().get(currentListKeys.get(position)).getId());
                        startActivityForResult(intent, Constants.EDIT_PLACE_REQUEST);
                        break;
                    case 1:
                        // delete
                        String id = currentList.getLoggablePlaces().get(currentListKeys.get(position)).getId();
                        currentList.getLoggablePlaces().remove(id);
                        LocalStorage.getInstance().saveLoggablePlaceList(currentList, getApplicationContext());
                        listAdapter.notifyDataSetInvalidated();
                        Toast.makeText(getApplicationContext(), "Index: " + index + " " + "Position: " + position, Toast.LENGTH_LONG).show();
                        break;
                }
                return false;
            }
        });
    }


    private void setOnOffButtonColor() {
        requestingLocation = getGeofencesAdded();
        if (requestingLocation) {
            setOnOffButtonColor(R.color.powerFabOn);
        } else {
            setOnOffButtonColor(R.color.powerFabOff);
        }
    }

    private void setOnOffButtonColor(int colorId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            onOffButton.setBackgroundTintList(ColorStateList.valueOf( getResources().getColor(colorId, null)));
        } else {
            onOffButton.setBackgroundTintList(ColorStateList.valueOf( getResources().getColor(colorId)));
        }
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_my_lists) {
            // startActivity(new Intent(this, MyListsActivity.class));
            startActivity(new Intent(this, HomeActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_data_cleanup) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
/*

        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(new Intent(this, LogPlacesByLocationService.class), mServiceConnection,
                Context.BIND_AUTO_CREATE);
*/
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


    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(this.getClass().getSimpleName(), "onReceive: Receiced new transition");
            presence = intent.getParcelableExtra(GeofenceService.EXTRA_PRESENCE);
            Log.d(this.getClass().getSimpleName(), "onReceive: Receiced new presence: " + presence);
            listAdapter.notifyDataSetChanged();
        }
    }


    private void setupPlaceList() {
        currentListKeys = new LinkedList();
        for (String key: currentList.getLoggablePlaces().keySet()) {
            currentListKeys.add(key);
        }
        listView = findViewById(R.id.place_list);
        setupSwipeList();
        listAdapter = new PlaceListAdapter();
        listView.setAdapter(listAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
                new IntentFilter(GeofenceService.ACTION_BROADCAST));

        LocalStorage.getInstance().logAll(this);
        populatePlaceListSpinner();
        setupPlaceList();
        isListDirty = false;
        // setOnOffButtonColor();
    }

    // TODO Maybe remove

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver);
        super.onPause();
    }


    private void fetchPlaceLists() {
        listList = LocalStorage.getInstance().getMyPlaceLists(this).getKeys();
    }

    private void populatePlaceListSpinner() {
        List<String> nameList = new ArrayList<>();
        for(String key: listList) {
            nameList.add(LocalStorage.getInstance().getLoggablePlaceList(this, key).getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, nameList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        listSpinner.setAdapter(adapter);
    }

    public void invokePlacepicker(View view) {

        PlacePicker.IntentBuilder builder = new PlacePicker.IntentBuilder();
        try {
            startActivityForResult(builder.build(this), Constants.PLACE_PICKER_REQUEST);
        } catch (GooglePlayServicesRepairableException e) {
            e.printStackTrace();
        } catch (GooglePlayServicesNotAvailableException e) {
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                Place place = PlacePicker.getPlace(this, data);
                Log.d(this.getClass().getSimpleName(), "onActivityResult Place: " + place.getName() + ", latlong: " + place.getLatLng() + ", types: " + place.getPlaceTypes());
                String toastMsg = String.format("Place: %s", place.getName());
                Toast.makeText(this, toastMsg, Toast.LENGTH_LONG).show();
                saveNewLoggablePlace(place);
            }
        } else if (requestCode == Constants.EDIT_PLACE_REQUEST) {
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

/*
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(StartActivity.this, EditPlaceActivity.class);
                        intent.putExtra("current_list", currentListKey);
                        intent.putExtra("place", loggablePlace.getId());
                        startActivityForResult(intent, EDIT_PLACE_REQUEST);
                    }
                });
*/
            }


            return view;
        }
    }
}
