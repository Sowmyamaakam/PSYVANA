//// SelfAssessmentActivity.java
//package com.example.project;
//
//import android.content.Intent;
//import android.os.Bundle;
//
//import android.widget.Button;
//import android.widget.Toast;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.viewpager2.widget.ViewPager2;
//import com.google.android.material.tabs.TabLayout;
//import com.google.android.material.tabs.TabLayoutMediator;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.firestore.FirebaseFirestore;
//import java.util.HashMap;
//import java.util.Map;
//
//public class SelfAssessmentActivity extends AppCompatActivity {
//
//    private TabLayout tabLayout;
//    private ViewPager2 viewPager;
//    private AssessmentPagerAdapter pagerAdapter;
//    private Button submitButton;
//
//    private FirebaseAuth auth;
//    private FirebaseFirestore db;
//
//    private int depressionScore = -1;
//    private int anxietyScore = -1;
//    private int stressScore = -1;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_self_assessment);
//
//        // Hide action bar
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().hide();
//        }
//
//        auth = FirebaseAuth.getInstance();
//        db = FirebaseFirestore.getInstance();
//
//        initViews();
//        setupViewPager();
//        setupSubmitButton();
//    }
//
//    private void initViews() {
//        tabLayout = findViewById(R.id.tabLayout);
//        viewPager = findViewById(R.id.viewPager);
//        submitButton = findViewById(R.id.submitButton);
//    }
//
//    private void setupViewPager() {
//        pagerAdapter = new AssessmentPagerAdapter(this);
//        viewPager.setAdapter(pagerAdapter);
//
//        // Connect TabLayout with ViewPager2
//        new TabLayoutMediator(tabLayout, viewPager,
//                (tab, position) -> {
//                    switch (position) {
//                        case 0:
//                            tab.setText("Depression (PHQ-9)");
//                            break;
//                        case 1:
//                            tab.setText("Anxiety (GAD-7)");
//                            break;
//                        case 2:
//                            tab.setText("Stress (PSS-10)");
//                            break;
//                    }
//                }
//        ).attach();
//    }
//
//    private void setupSubmitButton() {
//        submitButton.setOnClickListener(v -> handleSubmit());
//    }
//
//    private void handleSubmit() {
//        // Get scores from each fragment
//        FirebaseUser currentUser = auth.getCurrentUser();
//        if (currentUser == null) {
//            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//        DepressionFragment depressionFragment =
//                (DepressionFragment) getSupportFragmentManager().findFragmentByTag("f0");
//        AnxietyFragment anxietyFragment;
//        anxietyFragment = (AnxietyFragment) getSupportFragmentManager().findFragmentByTag("f1");
//        StressFragment stressFragment =
//                (StressFragment) getSupportFragmentManager().findFragmentByTag("f2");
//
//        // Calculate scores
//        if (depressionFragment != null) {
//            depressionScore = depressionFragment.calculateScore();
//        }
//        if (anxietyFragment != null) {
//            anxietyScore = anxietyFragment.calculateScore();
//        }
//        if (stressFragment != null) {
//            stressScore = stressFragment.calculateScore();
//        }
//
//        // Validate at least one assessment is completed
//        if (depressionScore == -1 && anxietyScore == -1 && stressScore == -1) {
//            Toast.makeText(this, "Please complete at least one assessment", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Save to Firebase and navigate to results
//        saveAssessmentResults();
//    }
//
//    private void saveAssessmentResults() {
//        if (auth.getCurrentUser() == null) {
//            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
//            finish();
//            return;
//        }
//
//
//        String userId = auth.getCurrentUser().getUid();
//
//        // Create assessment data
//        Map<String, Object> assessment = new HashMap<>();
//        assessment.put("timestamp", System.currentTimeMillis());
//        assessment.put("depressionScore", depressionScore);
//        assessment.put("anxietyScore", anxietyScore);
//        assessment.put("stressScore", stressScore);
//        assessment.put("depressionSeverity", calculateDepressionSeverity(depressionScore));
//        assessment.put("anxietySeverity", calculateAnxietySeverity(anxietyScore));
//        assessment.put("stressSeverity", calculateStressSeverity(stressScore));
//
//        // Save to Firestore
//        db.collection("users").document(userId)
//                .collection("assessments")
//                .add(assessment)
//                .addOnSuccessListener(documentReference -> {
//                    Toast.makeText(this, "Assessment saved successfully!", Toast.LENGTH_SHORT).show();
//
//                    // Navigate to results activity
//                    Intent intent = new Intent(SelfAssessmentActivity.this, AssessmentResultsActivity.class);
//                    intent.putExtra("depressionScore", depressionScore);
//                    intent.putExtra("anxietyScore", anxietyScore);
//                    intent.putExtra("stressScore", stressScore);
//                    startActivity(intent);
//                    finish();
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "Failed to save assessment: " + e.getMessage(),
//                            Toast.LENGTH_LONG).show();
//                });
//    }
//
//    // PHQ-9 Severity Calculation
//    private String calculateDepressionSeverity(int score) {
//        if (score == -1) return "Not Assessed";
//        if (score <= 4) return "Minimal";
//        if (score <= 9) return "Mild";
//        if (score <= 14) return "Moderate";
//        if (score <= 19) return "Moderately Severe";
//        return "Severe";
//    }
//
//    // GAD-7 Severity Calculation
//    private String calculateAnxietySeverity(int score) {
//        if (score == -1) return "Not Assessed";
//        if (score <= 4) return "Minimal";
//        if (score <= 9) return "Mild";
//        if (score <= 14) return "Moderate";
//        return "Severe";
//    }
//
//    // PSS-10 Severity Calculation
//    private String calculateStressSeverity(int score) {
//        if (score == -1) return "Not Assessed";
//        if (score <= 13) return "Low";
//        if (score <= 26) return "Moderate";
//        return "High";
//    }
//}
//
// SelfAssessmentActivity.java
package com.example.project;

import android.content.Intent;
import android.os.Bundle;

import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import java.util.Locale;

public class SelfAssessmentActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AssessmentPagerAdapter pagerAdapter;
    private Button submitButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private int depressionScore = -1;
    private int anxietyScore = -1;
    private int stressScore = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_assessment);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupViewPager();
        setupSubmitButton();
//        checkIfAssessmentDone(); // <-- check if user already did assessment
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        submitButton = findViewById(R.id.submitButton);
    }

    private void setupViewPager() {
        pagerAdapter = new AssessmentPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText("Depression (PHQ-9)");
                            break;
                        case 1:
                            tab.setText("Anxiety (GAD-7)");
                            break;
                        case 2:
                            tab.setText("Stress (PSS-10)");
                            break;
                    }
                }
        ).attach();
    }

//    private void checkIfAssessmentDone() {
//        FirebaseUser currentUser = auth.getCurrentUser();
//        if (currentUser == null) return;
//
//        String userId = currentUser.getUid();
//
//        db.collection("users").document(userId)
//                .collection("assessments")
//                .get()
//                .addOnSuccessListener(querySnapshot -> {
//                    if (!querySnapshot.isEmpty()) {
//                        // User already completed assessment
//                        Toast.makeText(this, "You have already completed the assessment", Toast.LENGTH_LONG).show();
//
//                        // Optionally, redirect to last results
//                        Map<String, Object> lastAssessment = querySnapshot.getDocuments()
//                                .get(querySnapshot.size() - 1).getData();
//
//                        Intent intent = new Intent(this, AssessmentResultsActivity.class);
//                        intent.putExtra("depressionScore", ((Long) lastAssessment.get("depressionScore")).intValue());
//                        intent.putExtra("anxietyScore", ((Long) lastAssessment.get("anxietyScore")).intValue());
//                        intent.putExtra("stressScore", ((Long) lastAssessment.get("stressScore")).intValue());
//                        startActivity(intent);
//                        finish();
//
//                    } else {
//                        // User hasn't done assessment yet
//                        setupSubmitButton(); // Enable submit button
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "Failed to check assessment: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                });
//    }

    private void setupSubmitButton() {
        submitButton.setOnClickListener(v -> handleSubmit());
    }

    private void handleSubmit() {
        // Get scores from each fragment
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        DepressionFragment depressionFragment =
                (DepressionFragment) getSupportFragmentManager().findFragmentByTag("f0");
        AnxietyFragment anxietyFragment;
        anxietyFragment = (AnxietyFragment) getSupportFragmentManager().findFragmentByTag("f1");
        StressFragment stressFragment =
                (StressFragment) getSupportFragmentManager().findFragmentByTag("f2");

        // Calculate scores
        if (depressionFragment != null) {
            depressionScore = depressionFragment.calculateScore();
        }
        if (anxietyFragment != null) {
            anxietyScore = anxietyFragment.calculateScore();
        }
        if (stressFragment != null) {
            stressScore = stressFragment.calculateScore();
        }

        // Validate at least one assessment is completed
        if (depressionScore == -1 && anxietyScore == -1 && stressScore == -1) {
            Toast.makeText(this, "Please complete at least one assessment", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save to Firebase and navigate to results
        saveAssessmentResults();
    }

    private void saveAssessmentResults() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        // Create assessment data
        Map<String, Object> assessment = new HashMap<>();
        assessment.put("timestamp", System.currentTimeMillis());
        assessment.put("depressionScore", depressionScore);
        assessment.put("anxietyScore", anxietyScore);
        assessment.put("stressScore", stressScore);
        assessment.put("depressionSeverity", calculateDepressionSeverity(depressionScore));
        assessment.put("anxietySeverity", calculateAnxietySeverity(anxietyScore));
        assessment.put("stressSeverity", calculateStressSeverity(stressScore));

        // Save to Firestore
        db.collection("users").document(userId)
                .collection("assessments")
                .add(assessment)
                .addOnSuccessListener(documentReference -> {
                    // Also save a wellness progress snapshot
                    int wellness = computeWellnessScore(depressionScore, anxietyScore, stressScore);
                    Map<String, Object> progress = new HashMap<>();
                    progress.put("score", wellness);
                    progress.put("timestamp", FieldValue.serverTimestamp());
                    // Optional label for chart (month)
                    String month = new SimpleDateFormat("MMM", Locale.getDefault()).format(new Date());
                    progress.put("label", month);

                    db.collection("users").document(userId)
                            .collection("progress")
                            .add(progress)
                            .addOnSuccessListener(ref -> {
                                Toast.makeText(this, "Assessment saved successfully!", Toast.LENGTH_SHORT).show();
                                // Navigate to results activity
                                Intent intent = new Intent(SelfAssessmentActivity.this, AssessmentResultsActivity.class);
                                intent.putExtra("depressionScore", depressionScore);
                                intent.putExtra("anxietyScore", anxietyScore);
                                intent.putExtra("stressScore", stressScore);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Even if progress write fails, continue to results
                                Intent intent = new Intent(SelfAssessmentActivity.this, AssessmentResultsActivity.class);
                                intent.putExtra("depressionScore", depressionScore);
                                intent.putExtra("anxietyScore", anxietyScore);
                                intent.putExtra("stressScore", stressScore);
                                startActivity(intent);
                                finish();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save assessment: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private int computeWellnessScore(int dep, int anx, int str) {
        // Normalize each available domain to 0-100 (higher is worse), then invert to wellness (higher is better)
        double totalWeight = 0.0;
        double sum = 0.0;
        if (dep >= 0) { sum += (dep * 100.0 / 27.0) * 0.4; totalWeight += 0.4; }
        if (anx >= 0) { sum += (anx * 100.0 / 21.0) * 0.3; totalWeight += 0.3; }
        if (str >= 0) { sum += (str * 100.0 / 40.0) * 0.3; totalWeight += 0.3; }
        if (totalWeight == 0) return 0;
        double severity = sum / totalWeight; // 0..100 (higher worse)
        int wellness = (int) Math.round(100.0 - severity);
        if (wellness < 0) wellness = 0;
        if (wellness > 100) wellness = 100;
        return wellness;
    }

    // PHQ-9 Severity Calculation
    private String calculateDepressionSeverity(int score) {
        if (score == -1) return "Not Assessed";
        if (score <= 4) return "Minimal";
        if (score <= 9) return "Mild";
        if (score <= 14) return "Moderate";
        if (score <= 19) return "Moderately Severe";
        return "Severe";
    }

    // GAD-7 Severity Calculation
    private String calculateAnxietySeverity(int score) {
        if (score == -1) return "Not Assessed";
        if (score <= 4) return "Minimal";
        if (score <= 9) return "Mild";
        if (score <= 14) return "Moderate";
        return "Severe";
    }

    // PSS-10 Severity Calculation
    private String calculateStressSeverity(int score) {
        if (score == -1) return "Not Assessed";
        if (score <= 13) return "Low";
        if (score <= 26) return "Moderate";
        return "High";
    }
}
