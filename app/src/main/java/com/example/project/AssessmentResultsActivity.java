package com.example.project;



import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AssessmentResultsActivity extends AppCompatActivity {

    private TextView depressionResult;
    private TextView anxietyResult;
    private TextView stressResult;
    private TextView overallSeverityText;
    private Button getRecommendationsButton;
    private Button backHomeButton;

    private int depressionScore;
    private int anxietyScore;
    private int stressScore;







    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assessment_results);

        // Hide action bar for consistency
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        initViews();
        loadScoresFromIntent();
        displayResults();
        setClickListeners();
    }

    private void initViews() {
        depressionResult = findViewById(R.id.depressionResult);
        anxietyResult = findViewById(R.id.anxietyResult);
        stressResult = findViewById(R.id.stressResult);
        overallSeverityText = findViewById(R.id.overallSeverityText);
        getRecommendationsButton = findViewById(R.id.getRecommendationsButton);
        backHomeButton = findViewById(R.id.backHomeButton);
    }

    private void loadScoresFromIntent() {
        Intent intent = getIntent();
        depressionScore = intent.getIntExtra("depressionScore", -1);
        anxietyScore = intent.getIntExtra("anxietyScore", -1);
        stressScore = intent.getIntExtra("stressScore", -1);
    }

    private void displayResults() {
        depressionResult.setText("Depression Score: " + depressionScore + " → " + calculateDepressionSeverity(depressionScore));
        anxietyResult.setText("Anxiety Score: " + anxietyScore + " → " + calculateAnxietySeverity(anxietyScore));
        stressResult.setText("Stress Score: " + stressScore + " → " + calculateStressSeverity(stressScore));

        // Overall severity based on highest
        AssessmentData assessmentData = new AssessmentData(
                depressionScore,
                anxietyScore,
                stressScore,
                calculateDepressionSeverity(depressionScore),
                calculateAnxietySeverity(anxietyScore),
                calculateStressSeverity(stressScore)
        );

        overallSeverityText.setText("Overall Severity: " + assessmentData.getOverallSeverity().toUpperCase());
    }

    private void setClickListeners() {
        getRecommendationsButton.setOnClickListener(v -> {
            // Go to AIRecommendationsActivity with refresh flag
            Intent intent = new Intent(AssessmentResultsActivity.this, AIRecommendationsActivity.class);
            intent.putExtra("refresh_after_assessment", true);
            startActivity(intent);
            finish();
        });

        backHomeButton.setOnClickListener(v -> {
            Intent intent = new Intent(AssessmentResultsActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    // Same logic as SelfAssessmentActivity
    private String calculateDepressionSeverity(int score) {
        if (score == -1) return "Not Assessed";
        if (score <= 4) return "Minimal";
        if (score <= 9) return "Mild";
        if (score <= 14) return "Moderate";
        if (score <= 19) return "Moderately Severe";
        return "Severe";
    }

    private String calculateAnxietySeverity(int score) {
        if (score == -1) return "Not Assessed";
        if (score <= 4) return "Minimal";
        if (score <= 9) return "Mild";
        if (score <= 14) return "Moderate";
        return "Severe";
    }

    private String calculateStressSeverity(int score) {
        if (score == -1) return "Not Assessed";
        if (score <= 13) return "Low";
        if (score <= 26) return "Moderate";
        return "High";
    }
}
