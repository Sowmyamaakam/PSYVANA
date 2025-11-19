package com.example.project;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.google.android.material.card.MaterialCardView;

public class ConsultationOptionsAdapter extends RecyclerView.Adapter<ConsultationOptionsAdapter.ViewHolder> {

    private List<ProfessionalHelpActivity.ConsultationOption> options;
    private OnConsultationClickListener listener;
    private String selectedId = "";

    public interface OnConsultationClickListener {
        void onConsultationClick(ProfessionalHelpActivity.ConsultationOption option);
    }

    public ConsultationOptionsAdapter(List<ProfessionalHelpActivity.ConsultationOption> options,
                                      OnConsultationClickListener listener) {
        this.options = options;
        this.listener = listener;
    }

    public void setSelectedId(String selectedId) {
        this.selectedId = selectedId;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_consultation_option, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProfessionalHelpActivity.ConsultationOption option = options.get(position);
        boolean isSelected = option.getId().equals(selectedId);

        holder.consultationTitle.setText(option.getTitle());
        holder.consultationDescription.setText(option.getDescription());
        holder.consultationDuration.setText("⏱️ " + option.getDuration());
        holder.consultationAvailability.setText("📅 " + option.getAvailability());
        holder.consultationIcon.setImageResource(option.getIconResId());

        // Update styling based on selection
        if (isSelected) {
            holder.consultationCard.setCardBackgroundColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.teal_50)
            );
            holder.consultationCard.setStrokeColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.teal_500)
            );
            holder.consultationCard.setStrokeWidth(8); // Use dp converted value

            holder.iconContainer.setBackgroundResource(R.drawable.circle_teal_bg_solid);
            holder.consultationIcon.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white)
            );
        } else {
            holder.consultationCard.setCardBackgroundColor(Color.parseColor("#CCFFFFFF"));
            holder.consultationCard.setStrokeWidth(0);

            holder.iconContainer.setBackgroundResource(R.drawable.circle_gray_bg);
            holder.consultationIcon.setColorFilter(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.gray_600)
            );
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConsultationClick(option);
            }
        });
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView consultationCard;
        FrameLayout iconContainer;
        ImageView consultationIcon;
        TextView consultationTitle;
        TextView consultationDescription;
        TextView consultationDuration;
        TextView consultationAvailability;

        ViewHolder(View itemView) {
            super(itemView);
            consultationCard = itemView.findViewById(R.id.consultationCard);
            iconContainer = itemView.findViewById(R.id.iconContainer);
            consultationIcon = itemView.findViewById(R.id.consultationIcon);
            consultationTitle = itemView.findViewById(R.id.consultationTitle);
            consultationDescription = itemView.findViewById(R.id.consultationDescription);
            consultationDuration = itemView.findViewById(R.id.consultationDuration);
            consultationAvailability = itemView.findViewById(R.id.consultationAvailability);
        }
    }
}