package se.fork.spacetime;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import se.fork.spacetime.model.LoggablePlace;
import se.fork.spacetime.model.LoggablePlaceList;
import se.fork.spacetime.utils.LocalStorage;

/**
 * Created by per.fork on 2018-02-03.
 */

public class EditPlaceActivity extends Activity {
    private String currentListKey;
    private String currentPlaceKey;
    private LoggablePlaceList currentPlaceList;
    private LoggablePlace currentPlace;

    private EditText nameView;
    private EditText addressView;
    private TextView latitudeView;
    private TextView longitudeView;
    private EditText radiusView;
    private Button saveButton;
    private Button cancelButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_place);
        setupViews();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            currentListKey = extras.getString("current_list");
            currentPlaceKey = extras.getString("place");
            currentPlaceList = LocalStorage.getInstance().getLoggablePlaceList(this, currentListKey);
            currentPlace = currentPlaceList.getLoggablePlaces().get(currentPlaceKey);
            populateForm();
        }
    }

    private void setupViews() {
        nameView = findViewById(R.id.name_view);
        addressView = findViewById(R.id.address_view);
        latitudeView = findViewById(R.id.latitude_view);
        longitudeView = findViewById(R.id.longitude_view);
        radiusView = findViewById(R.id.radius_view);
        saveButton = findViewById(R.id.save_button);
        cancelButton = findViewById(R.id.cancel_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveAndExit();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelAndExit();
            }
        });
    }

    private void populateForm() {
        nameView.setText(currentPlace.getName());
        addressView.setText(currentPlace.getAddress());
        latitudeView.setText(Double.toString(currentPlace.getLatitude()));
        longitudeView.setText(Double.toString(currentPlace.getLongitude()));
        radiusView.setText( Integer.toString((int)currentPlace.getRadius()));
    }

    private void setPlaceFields() {
        currentPlace.setName(nameView.getText().toString());
        currentPlace.setAddress(addressView.getText().toString());
        currentPlace.setRadius(Double.parseDouble(radiusView.getText().toString()));
    }

    private void savePlace() {
        currentPlaceList.getLoggablePlaces().put(currentPlaceKey, currentPlace);
        LocalStorage.getInstance().saveLoggablePlaceList(currentPlaceList, this);
    }

    private void saveAndExit() {
        setPlaceFields();
        savePlace();
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }

    private void cancelAndExit() {
        Intent returnIntent = new Intent();
        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }
}
