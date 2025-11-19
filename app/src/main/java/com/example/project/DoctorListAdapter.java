package com.example.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DoctorListAdapter extends RecyclerView.Adapter<DoctorListAdapter.ViewHolder> {

    public interface OnDoctorClickListener {
        void onDoctorClick(Doctor item);
    }

    private List<Doctor> items = new ArrayList<>();
    private final OnDoctorClickListener listener;

    public DoctorListAdapter(OnDoctorClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Doctor> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_doctor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Doctor d = items.get(position);
        holder.name.setText(d.name);
        holder.specialty.setText(d.specialties);
        holder.price.setText(d.displayPrice);
        holder.ratingBar.setRating(d.rating);
        holder.itemView.setOnClickListener(v -> listener.onDoctorClick(d));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class Doctor {
        public String id;
        public String name;
        public String specialties;
        public float rating;
        public String displayPrice;
        public Map<String, Object> rate;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, specialty, price;
        RatingBar ratingBar;
        TextView avatar;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            specialty = itemView.findViewById(R.id.specialty);
            price = itemView.findViewById(R.id.price);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            avatar = itemView.findViewById(R.id.doctorAvatar);
        }
    }
}
