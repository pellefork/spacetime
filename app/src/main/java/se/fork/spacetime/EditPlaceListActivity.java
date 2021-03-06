package se.fork.spacetime;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;

import se.fork.spacetime.model.LoggablePlaceList;
import se.fork.spacetime.utils.Constants;

public class EditPlaceListActivity extends AppCompatActivity {

    private boolean creatingNew;
    private TextView titleView;
    private EditText nameView;
    private EditText goalHoursView;

    private LoggablePlaceList currentList;
    private List<String> takenNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupViews();
    }

    private void setupViews() {
        setContentView(R.layout.activity_edit_place_list);
        titleView = findViewById(R.id.title);
        nameView = findViewById(R.id.name_view);
        goalHoursView = findViewById(R.id.goalhours_view);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            creatingNew = extras.getBoolean(Constants.EDIT_PLACELIST_CREATING_NEW);
        } else {
            creatingNew = false;
        }
        if (creatingNew) {
            titleView.setText(R.string.editplacelist_title_new);
        }
    }

    private void saveNewListAndExit() {
        currentList = new LoggablePlaceList(nameView.getText().toString());
    }

    private void saveOldListAndExit() {

    }

    public void save(View view) {
        if (creatingNew) {
            saveNewListAndExit();
        } else {
            saveOldListAndExit();
        }

    }

    public void cancel() {

    }
}
