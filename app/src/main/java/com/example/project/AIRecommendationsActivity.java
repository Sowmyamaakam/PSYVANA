package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class AIRecommendationsActivity extends AppCompatActivity {

    private TextView headerText, severityIndicator, emergencyMessage, generalAdvice, professionalHelpText;
    private CardView emergencyCard;
    private RecyclerView recommendationsRecyclerView;
    private Button takeAssessmentButton, refreshRecommendations;
    private ProgressBar progressBar;
    private LinearLayout contentLayout;

    private RecommendationService recommendationService;
    private RecommendationsAdapter adapter;
    private List<Recommendation> recommendations;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ai_recommendations);

        if (getSupportActionBar() != null) getSupportActionBar().hide();
        initViews();
        setupRecyclerView();

        recommendationService = new RecommendationService();
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        loadCachedRecommendations();
        setClickListeners();
    }

    private void initViews() {
        headerText = findViewById(R.id.headerText);
        severityIndicator = findViewById(R.id.severityIndicator);
        emergencyCard = findViewById(R.id.emergencyCard);
        emergencyMessage = findViewById(R.id.emergencyMessage);
        generalAdvice = findViewById(R.id.generalAdvice);
        professionalHelpText = findViewById(R.id.professionalHelpText);
        recommendationsRecyclerView = findViewById(R.id.recommendationsRecyclerView);
        takeAssessmentButton = findViewById(R.id.takeAssessmentButton);
        refreshRecommendations = findViewById(R.id.refreshRecommendations);
        progressBar = findViewById(R.id.progressBar);
        contentLayout = findViewById(R.id.contentLayout);
    }

    private void setupRecyclerView() {
        recommendations = new ArrayList<>();
        adapter = new RecommendationsAdapter(recommendations, new RecommendationsAdapter.OnRecommendationClickListener() {
            @Override
            public void onRecommendationClick(Recommendation rec) {
                Intent intent = new Intent(AIRecommendationsActivity.this, RecommendationDetailActivity.class);
                intent.putExtra("recommendation_title", rec.getTitle());
                intent.putExtra("recommendation_type", rec.getType());
                intent.putExtra("recommendation_description", rec.getDescription());
                intent.putExtra("recommendation_duration", rec.getDuration());
                startActivity(intent);
            }
            @Override
            public void onMarkCompleted(Recommendation rec) { rec.setCompleted(true); }
            @Override
            public void onRateRecommendation(Recommendation rec, int rating) { rec.setRating(rating); }
        });
        recommendationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        recommendationsRecyclerView.setAdapter(adapter);
    }

    private void setClickListeners() {
        takeAssessmentButton.setOnClickListener(v -> startActivity(new Intent(this, SelfAssessmentActivity.class)));

        refreshRecommendations.setOnClickListener(v -> generateRecommendations());
    }

    private void loadCachedRecommendations() {
        showLoading(true);
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .collection("gpt_recommendations")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        RecommendationResponse response = snapshots.getDocuments()
                                .get(0).toObject(RecommendationResponse.class);
                        if (response != null) displayRecommendations(response);
                        showLoading(false);
                    } else {
                        showAssessmentPrompt();
                    }
                })
                .addOnFailureListener(e -> showAssessmentPrompt());
    }

    private void generateRecommendations() {
        showLoading(true);

        // Load latest assessment
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .collection("assessments")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        UserAssessment assessment = snapshots.getDocuments()
                                .get(0).toObject(UserAssessment.class);

                        if (assessment != null) {
                            recommendationService.getPersonalizedRecommendations(assessment, new RecommendationService.GPTRecommendationCallback() {
                                @Override
                                public void onSuccess(RecommendationResponse response) {
                                    runOnUiThread(() -> displayRecommendations(response));
                                }
                                @Override
                                public void onError(String error) {
                                    runOnUiThread(() -> {
                                        showLoading(false);
                                        Toast.makeText(AIRecommendationsActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                                    });
                                }
                            });
                        } else showAssessmentPrompt();
                    } else showAssessmentPrompt();
                })
                .addOnFailureListener(e -> showAssessmentPrompt());
    }

    private void displayRecommendations(RecommendationResponse response) {
        showLoading(false);

        headerText.setText("AI Recommendations");
        severityIndicator.setText(response.getSeverityLevel().toUpperCase() + " LEVEL");
        severityIndicator.setTextColor(response.getSeverityColor());

        if (response.hasEmergencyMessage()) {
            emergencyCard.setVisibility(View.VISIBLE);
            emergencyMessage.setText(response.getEmergencyMessage());
            professionalHelpText.setVisibility(View.VISIBLE);
            professionalHelpText.setText(response.getProfessionalHelp());
        } else {
            emergencyCard.setVisibility(View.GONE);
            professionalHelpText.setVisibility(View.GONE);
        }

        generalAdvice.setText(response.getGeneralAdvice());
        recommendations.clear();
        if (response.getRecommendations() != null)
            recommendations.addAll(response.getRecommendations());
        adapter.notifyDataSetChanged();
        contentLayout.setVisibility(View.VISIBLE);
    }

    private void showAssessmentPrompt() {
        showLoading(false);
        headerText.setText("Take Assessment First");
        generalAdvice.setText("To get personalized AI recommendations, please complete your mental health assessment first.");
        emergencyCard.setVisibility(View.GONE);
        severityIndicator.setVisibility(View.GONE);
        professionalHelpText.setVisibility(View.GONE);
        recommendationsRecyclerView.setVisibility(View.GONE);
        contentLayout.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        contentLayout.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (getIntent().getBooleanExtra("refresh_after_assessment", false)) {
            generateRecommendations();
            getIntent().removeExtra("refresh_after_assessment");
        }
    }
}
