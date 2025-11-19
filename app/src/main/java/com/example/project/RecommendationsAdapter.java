// RecommendationsAdapter.java
package com.example.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RecommendationsAdapter extends RecyclerView.Adapter<RecommendationsAdapter.RecommendationViewHolder> {

    private List<Recommendation> recommendations;
    private OnRecommendationClickListener listener;

    public interface OnRecommendationClickListener {
        void onRecommendationClick(Recommendation recommendation);
        void onMarkCompleted(Recommendation recommendation);
        void onRateRecommendation(Recommendation recommendation, int rating);
    }

    public RecommendationsAdapter(List<Recommendation> recommendations, OnRecommendationClickListener listener) {
        this.recommendations = recommendations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecommendationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recomendations, parent, false);
        return new RecommendationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecommendationViewHolder holder, int position) {
        Recommendation recommendation = recommendations.get(position);
        holder.bind(recommendation, listener);
    }

    @Override
    public int getItemCount() {
        return recommendations.size();
    }

    static class RecommendationViewHolder extends RecyclerView.ViewHolder {

        private CardView cardView;
        private ImageView typeIcon;
        private TextView titleText;
        private TextView typeText;
        private TextView durationText;
        private TextView difficultyText;
        private TextView descriptionText;
        private TextView reasonText;
        private Button startButton;
        private Button completedButton;
        private RatingBar ratingBar;
        private View severityIndicator;

        public RecommendationViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            typeIcon = itemView.findViewById(R.id.typeIcon);
            titleText = itemView.findViewById(R.id.titleText);
            typeText = itemView.findViewById(R.id.typeText);
            durationText = itemView.findViewById(R.id.durationText);
            difficultyText = itemView.findViewById(R.id.difficultyText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            reasonText = itemView.findViewById(R.id.reasonText);
            startButton = itemView.findViewById(R.id.startButton);
            completedButton = itemView.findViewById(R.id.completedButton);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            severityIndicator = itemView.findViewById(R.id.severityIndicator);
        }

        public void bind(Recommendation recommendation, OnRecommendationClickListener listener) {
            titleText.setText(recommendation.getTitle());
            typeText.setText(recommendation.getType().toUpperCase());
            durationText.setText(recommendation.getDuration());
            difficultyText.setText(recommendation.getDifficulty());
            descriptionText.setText(recommendation.getDescription());
            reasonText.setText(recommendation.getReason());

            // Set type icon and color
            typeIcon.setImageResource(recommendation.getTypeIcon());
            typeIcon.setColorFilter(recommendation.getTypeColor());

            // Set severity indicator color
            int severityColor = getSeverityColor(recommendation.getSeverityMatch());
            severityIndicator.setBackgroundColor(severityColor);

            // Handle completed state
            if (recommendation.isCompleted()) {
                startButton.setVisibility(View.GONE);
                completedButton.setVisibility(View.VISIBLE);
                completedButton.setText("✓ Completed");
                completedButton.setEnabled(false);
                ratingBar.setVisibility(View.VISIBLE);
                ratingBar.setRating(recommendation.getRating());
            } else {
                startButton.setVisibility(View.VISIBLE);
                completedButton.setVisibility(View.GONE);
                ratingBar.setVisibility(View.GONE);
            }

            // Click listeners
            cardView.setOnClickListener(v -> listener.onRecommendationClick(recommendation));

            startButton.setOnClickListener(v -> listener.onRecommendationClick(recommendation));

            completedButton.setOnClickListener(v -> {
                if (!recommendation.isCompleted()) {
                    listener.onMarkCompleted(recommendation);
                }
            });

            ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
                if (fromUser) {
                    listener.onRateRecommendation(recommendation, (int) rating);
                }
            });
        }

        private int getSeverityColor(String severity) {
            switch (severity.toLowerCase()) {
                case "crisis": return 0xFFEF4444; // Red
                case "moderate": return 0xFFF59E0B; // Orange
                case "mild": return 0xFF10B981; // Green
                case "maintenance": return 0xFF3B82F6; // Blue
                default: return 0xFF6B7280; // Gray
            }
        }
    }
}