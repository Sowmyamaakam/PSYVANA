// RecommendationsActivity.java
package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RecommendationsActivity extends AppCompatActivity {

    private RecyclerView recommendationsRecyclerView;
    private RecommendationsAdapter adapter;
    private ProgressBar progressBar;
    private List<Recommendation> recommendations;

    private RecommendationService recommendationService;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendations);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        setupRecyclerView();

        recommendationService = new RecommendationService();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Load recommendations
        loadRecommendations();
    }

    private void initViews() {
        recommendationsRecyclerView = findViewById(R.id.recommendationsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupRecyclerView() {
        recommendations = new ArrayList<>();
        adapter = new RecommendationsAdapter(recommendations, new RecommendationsAdapter.OnRecommendationClickListener() {
            @Override
            public void onRecommendationClick(Recommendation recommendation) {
                handleRecommendationClick(recommendation);
            }

            @Override
            public void onMarkCompleted(Recommendation recommendation) {
                markRecommendationCompleted(recommendation);
            }

            @Override
            public void onRateRecommendation(Recommendation recommendation, int rating) {
                rateRecommendation(recommendation, rating);
            }
        });

        recommendationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recommendationsRecyclerView.setAdapter(adapter);
    }

    private void loadRecommendations() {
        progressBar.setVisibility(View.VISIBLE);

        // First try to load cached recommendations
        loadCachedRecommendations();

        // Then generate new ones based on latest assessment
        generateNewRecommendations();
    }

    private void loadCachedRecommendations() {
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .collection("recommendations")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        List<Recommendation> cachedRecs = new ArrayList<>();
                        queryDocumentSnapshots.forEach(doc -> {
                            Recommendation rec = doc.toObject(Recommendation.class);
                            cachedRecs.add(rec);
                        });

                        recommendations.clear();
                        recommendations.addAll(cachedRecs);
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    // Continue with generating new recommendations
                });
    }

    private void generateNewRecommendations() {
        getUserLatestAssessment(new AssessmentCallback() {
            @Override
            public void onAssessmentLoaded(UserAssessment assessment) {
                recommendationService.getPersonalizedRecommendations(assessment,
                        new RecommendationService.GPTRecommendationCallback() {
                            @Override
                            public void onSuccess(RecommendationResponse response) {
                                runOnUiThread(() -> {
                                    recommendations.clear();
                                    recommendations.addAll(response.getRecommendations());
                                    adapter.notifyDataSetChanged();
                                    progressBar.setVisibility(View.GONE);
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(RecommendationsActivity.this,
                                            "Failed to load recommendations: " + error,
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                        });
            }

            @Override
            public void onError(String error) {
                // Use default assessment if none found
                UserAssessment defaultAssessment = createDefaultAssessment();
                recommendationService.getPersonalizedRecommendations(defaultAssessment,
                        new RecommendationService.GPTRecommendationCallback() {
                            @Override
                            public void onSuccess(RecommendationResponse response) {
                                runOnUiThread(() -> {
                                    recommendations.clear();
                                    recommendations.addAll(response.getRecommendations());
                                    adapter.notifyDataSetChanged();
                                    progressBar.setVisibility(View.GONE);
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> {
                                    progressBar.setVisibility(View.GONE);
                                    showDefaultRecommendations();
                                });
                            }
                        });
            }
        });
    }

    private void getUserLatestAssessment(AssessmentCallback callback) {
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .collection("assessments")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        UserAssessment assessment = queryDocumentSnapshots.getDocuments()
                                .get(0).toObject(UserAssessment.class);
                        callback.onAssessmentLoaded(assessment);
                    } else {
                        callback.onError("No assessment found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private UserAssessment createDefaultAssessment() {
        List<String> defaultPreferences = Arrays.asList("meditation", "breathing", "journaling");
        return new UserAssessment(5, 5, 5, 5, 15, defaultPreferences);
    }

    private void showDefaultRecommendations() {
        recommendations.clear();

        recommendations.add(new Recommendation(
                "Quick Breathing Exercise",
                "breathing",
                "5 minutes",
                "beginner",
                "Simple 4-7-8 breathing technique",
                "Great for immediate stress relief",
                "moderate"
        ));

        recommendations.add(new Recommendation(
                "Mindful Moment",
                "meditation",
                "10 minutes",
                "beginner",
                "Brief mindfulness practice",
                "Perfect for busy schedules",
                "mild"
        ));

        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Showing default recommendations", Toast.LENGTH_SHORT).show();
    }

    private void handleRecommendationClick(Recommendation recommendation) {
        Toast.makeText(this, "Opening: " + recommendation.getTitle(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(RecommendationsActivity.this, RecommendationDetailActivity.class);
        intent.putExtra("recommendation_title", recommendation.getTitle());
        intent.putExtra("recommendation_type", recommendation.getType());
        intent.putExtra("recommendation_description", recommendation.getDescription());
        intent.putExtra("recommendation_duration", recommendation.getDuration());
        startActivity(intent);
    }

    private void markRecommendationCompleted(Recommendation recommendation) {
        recommendation.setCompleted(true);

        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .collection("recommendations")
                .document(String.valueOf(recommendation.getTimestamp()))
                .update("completed", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Marked as completed!", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show());
    }

    private void rateRecommendation(Recommendation recommendation, int rating) {
        recommendation.setRating(rating);

        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .collection("recommendations")
                .document(String.valueOf(recommendation.getTimestamp()))
                .update("rating", rating)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to save rating", Toast.LENGTH_SHORT).show());
    }

    // Callback interface for loading assessments
    private interface AssessmentCallback {
        void onAssessmentLoaded(UserAssessment assessment);
        void onError(String error);
    }
}
