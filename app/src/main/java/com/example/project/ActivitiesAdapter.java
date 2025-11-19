package com.example.project;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ActivitiesAdapter extends RecyclerView.Adapter<ActivitiesAdapter.ViewHolder> {

    private List<ProgressActivity.ActivityItem> activities;

    public ActivitiesAdapter(List<ProgressActivity.ActivityItem> activities) {
        this.activities = activities;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProgressActivity.ActivityItem activity = activities.get(position);

        holder.activityName.setText(activity.getName());
        holder.activityTime.setText(activity.getTime());

        // Set status dot color
        GradientDrawable dotDrawable = (GradientDrawable) holder.statusDot.getBackground();
        dotDrawable.setColor(Color.parseColor(activity.getDotColor()));
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View statusDot;
        TextView activityName;
        TextView activityTime;

        ViewHolder(View itemView) {
            super(itemView);
            statusDot = itemView.findViewById(R.id.statusDot);
            activityName = itemView.findViewById(R.id.activityName);
            activityTime = itemView.findViewById(R.id.activityTime);
        }
    }
}