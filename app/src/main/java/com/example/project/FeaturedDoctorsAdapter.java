package com.example.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class FeaturedDoctorsAdapter extends RecyclerView.Adapter<FeaturedDoctorsAdapter.ViewHolder> {

    private List<ProfessionalHelpActivity.FeaturedDoctor> doctors;


    public FeaturedDoctorsAdapter(List<ProfessionalHelpActivity.FeaturedDoctor> doctors) {
        this.doctors = doctors;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_featured_doctor, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProfessionalHelpActivity.FeaturedDoctor doctor = doctors.get(position);

        holder.doctorAvatar.setText(doctor.getAvatar());
        holder.doctorName.setText(doctor.getName());
        holder.doctorSpecialty.setText(doctor.getSpecialty());
        holder.doctorRating.setText(String.valueOf(doctor.getRating()));
        holder.doctorExperience.setText(doctor.getExperience());
    }

    @Override
    public int getItemCount() {
        return doctors.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView doctorAvatar;
        TextView doctorName;
        TextView doctorSpecialty;
        TextView doctorRating;
        TextView doctorExperience;

        ViewHolder(View itemView) {
            super(itemView);
            doctorAvatar = itemView.findViewById(R.id.doctorAvatar);
            doctorName = itemView.findViewById(R.id.doctorName);
            doctorSpecialty = itemView.findViewById(R.id.doctorSpecialty);
            doctorRating = itemView.findViewById(R.id.doctorRating);
            doctorExperience = itemView.findViewById(R.id.doctorExperience);
        }
    }
}