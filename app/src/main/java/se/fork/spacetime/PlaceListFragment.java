package se.fork.spacetime;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by per.fork on 2018-05-08.
 */

public class PlaceListFragment extends Fragment {

    public static PlaceListFragment newInstance() {
        PlaceListFragment fragment = new PlaceListFragment();
        return fragment;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_placelist, container, false);



        return view;
    }
}
