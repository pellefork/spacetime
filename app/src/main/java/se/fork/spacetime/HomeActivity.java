package se.fork.spacetime;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import se.fork.spacetime.adapters.MyListsAdapter;
import se.fork.spacetime.model.LoggablePlaceList;
import se.fork.spacetime.model.MyPlaceLists;
import se.fork.spacetime.utils.LocalStorage;

public class HomeActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    private List<LoggablePlaceList> placeLists;
    private MyListsAdapter myListsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        recyclerView = findViewById(R.id.recycler_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyPlaceLists myPlaceLists = LocalStorage.getInstance().getMyPlaceLists(this);
        placeLists = new ArrayList<>();
        List<String> keys = myPlaceLists.getKeys();
        for (String key: keys) {
            LoggablePlaceList loggablePlaceList = LocalStorage.getInstance().getLoggablePlaceList(this, key);
            placeLists.add(loggablePlaceList);
        }
        myListsAdapter = new MyListsAdapter(placeLists);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(myListsAdapter);
    }
}
