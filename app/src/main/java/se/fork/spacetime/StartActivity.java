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
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlacePicker;

import java.util.ArrayList;
import java.util.List;

import se.fork.spacetime.model.LoggablePlaceList;
import se.fork.spacetime.model.MyPlaceLists;
import se.fork.spacetime.utils.LocalStorage;

public class StartActivity extends FragmentActivity
        implements GoogleApiClient.OnConnectionFailedListener {

    private static final int PLACE_PICKER_REQUEST = 1;
    private Spinner listSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        listSpinner = findViewById(R.id.placelist_spinner);
        if(!isInitialized()) {
            initialize();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalStorage.getInstance().logAll(this);
        populatePlaceListSpinner();
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
            }
        }
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
        LocalStorage.getInstance().saveMyPlaceLists(this, myPlaceLists);
        LocalStorage.getInstance().logAll(this);
        LocalStorage.getInstance().saveLoggablePlaceList(loggablePlaceList, this);
    }

}
