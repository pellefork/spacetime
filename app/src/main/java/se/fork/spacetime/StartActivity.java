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
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
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
import se.fork.spacetime.utils.Constants;
import se.fork.spacetime.utils.LocalStorage;

public class StartActivity extends AppCompatActivity
        implements GoogleApiClient.OnConnectionFailedListener, NavigationView.OnNavigationItemSelectedListener {

    private Spinner listSpinner;
    private FloatingActionButton onOffButton;
    private String currentListKey;
    private LoggablePlaceList currentList;
    private List<String> currentListKeys;
    private Presence presence;
    private SwipeMenuListView listView;
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
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

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
        setOnOffButtonColor(R.color.powerFabOff);
        onOffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (requestingLocation) {
                    stopRequestingLocations(v);
                    setOnOffButtonColor(R.color.powerFabOff);
                    requestingLocation = false;
                } else {
                    startRequestingLocations(v);
                    setOnOffButtonColor(R.color.powerFabOn);
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
        if (mService != null) {
            requestingLocation = mService.isRequestingLocationUpdates();
        }
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
            Log.d(this.getClass().getSimpleName(), "onReceive: Activity receiving location " + location);
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
        setupSwipeList();
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
        setOnOffButtonColor();
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
