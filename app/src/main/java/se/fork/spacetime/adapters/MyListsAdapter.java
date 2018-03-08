package se.fork.spacetime.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import pl.pawelkleczkowski.customgauge.CustomGauge;
import se.fork.spacetime.R;
import se.fork.spacetime.model.LoggablePlaceList;

/**
 * Created by per.fork on 2018-03-07.
 */

public class MyListsAdapter extends RecyclerView.Adapter<MyListsAdapter.MyViewHolder> {

    private List<LoggablePlaceList> placeListsList;

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_list, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        LoggablePlaceList loggablePlaceList = placeListsList.get(position);
        holder.listNameView.setText(loggablePlaceList.getName());
        // TODO Calculate rest...
        holder.gauge.setValue(98);
        holder.valueView.setText("98");
        holder.thisWeeksTotalView.setText("44");
        holder.goalHoursView.setText("40");
    }

    @Override
    public int getItemCount() {
        return placeListsList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView listNameView;
        public TextView goalHoursView;
        public TextView thisWeeksTotalView;
        public CustomGauge gauge;
        public TextView valueView;

        public MyViewHolder(View itemView) {
            super(itemView);
            listNameView = itemView.findViewById(R.id.list_name);
            goalHoursView = itemView.findViewById(R.id.goal_hours);
            thisWeeksTotalView = itemView.findViewById(R.id.this_week_total);
            valueView = itemView.findViewById(R.id.value);
            gauge = itemView.findViewById(R.id.gauge);
        }
    }

    public MyListsAdapter(List<LoggablePlaceList> placeListsList) {
        this.placeListsList = placeListsList;
    }


}
