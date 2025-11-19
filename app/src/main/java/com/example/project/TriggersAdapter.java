package com.example.project;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TriggersAdapter extends RecyclerView.Adapter<TriggersAdapter.ViewHolder> {

    private List<ProgressActivity.TriggerItem> triggers;

    public TriggersAdapter(List<ProgressActivity.TriggerItem> triggers) {
        this.triggers = triggers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trigger, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProgressActivity.TriggerItem trigger = triggers.get(position);

        holder.triggerName.setText(trigger.getName());
        holder.triggerImprovement.setText(trigger.getImprovement());
        holder.triggerImprovement.setTextColor(Color.parseColor(trigger.getColor()));
    }

    @Override
    public int getItemCount() {
        return triggers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView triggerName;
        TextView triggerImprovement;

        ViewHolder(View itemView) {
            super(itemView);
            triggerName = itemView.findViewById(R.id.triggerName);
            triggerImprovement = itemView.findViewById(R.id.triggerImprovement);
        }
    }
}