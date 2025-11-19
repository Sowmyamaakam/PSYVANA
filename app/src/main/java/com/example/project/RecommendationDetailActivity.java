package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class RecommendationDetailActivity extends AppCompatActivity {

    private ImageView typeIcon;
    private TextView titleText;
    private TextView typeText;
    private TextView durationText;
    private TextView difficultyText;
    private TextView descriptionText;
    private CardView instructionsCard;
    private TextView instructionsText;
    private CardView timerCard;
    private TextView timerText;
    private ProgressBar timerProgress;
    private Button startButton;
    private Button pauseButton;
    private Button completeButton;
    private Button skipButton;

    private String recommendationType;
    private String recommendationTitle;
    private String recommendationDescription;
    private String recommendationDuration;

    private CountDownTimer activityTimer;
    private long totalTimeMs = 0;
    private long remainingTimeMs = 0;
    private boolean isTimerRunning = false;
    private boolean isActivityCompleted = false;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendation_detail);

        // Hide action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get data from intent
        getIntentData();

        initViews();
        setupRecommendation();
        setClickListeners();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        recommendationTitle = intent.getStringExtra("recommendation_title");
        recommendationType = intent.getStringExtra("recommendation_type");
        recommendationDescription = intent.getStringExtra("recommendation_description");
        recommendationDuration = intent.getStringExtra("recommendation_duration");

        // Parse duration to milliseconds
        if (recommendationDuration != null) {
            totalTimeMs = parseDurationToMs(recommendationDuration);
            remainingTimeMs = totalTimeMs;
        }
    }

    private long parseDurationToMs(String duration) {
        try {
            String[] parts = duration.toLowerCase().split(" ");
            int minutes = Integer.parseInt(parts[0]);
            return minutes * 60 * 1000; // Convert to milliseconds
        } catch (Exception e) {
            return 10 * 60 * 1000; // Default 10 minutes
        }
    }

    private void initViews() {
        typeIcon = findViewById(R.id.typeIcon);
        titleText = findViewById(R.id.titleText);
        typeText = findViewById(R.id.typeText);
        durationText = findViewById(R.id.durationText);
        difficultyText = findViewById(R.id.difficultyText);
        descriptionText = findViewById(R.id.descriptionText);
        instructionsCard = findViewById(R.id.instructionsCard);
        instructionsText = findViewById(R.id.instructionsText);
        timerCard = findViewById(R.id.timerCard);
        timerText = findViewById(R.id.timerText);
        timerProgress = findViewById(R.id.timerProgress);
        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        completeButton = findViewById(R.id.completeButton);
        skipButton = findViewById(R.id.skipButton);
    }

    private void setupRecommendation() {
        // Set basic info
        titleText.setText(recommendationTitle);
        typeText.setText(recommendationType.toUpperCase());
        durationText.setText(recommendationDuration);
        descriptionText.setText(recommendationDescription);

        // Set icon and color based on type
        int iconRes = getTypeIcon(recommendationType);
        int colorRes = getTypeColor(recommendationType);
        typeIcon.setImageResource(iconRes);
        typeIcon.setColorFilter(colorRes);

        // Set type-specific instructions
        String instructions = getInstructionsForType(recommendationType);
        instructionsText.setText(instructions);

        // Setup timer
        updateTimerDisplay();
        timerProgress.setMax(100);

        // Show/hide timer based on activity type
        if (recommendationType.equals("crisis_support")) {
            timerCard.setVisibility(View.GONE);
            startButton.setText("Get Help Now");
        } else {
            timerCard.setVisibility(View.VISIBLE);
            startButton.setText("Start " + recommendationType);
        }
    }

    private int getTypeIcon(String type) {
        switch (type.toLowerCase()) {
            case "meditation": return android.R.drawable.ic_menu_compass;
            case "breathing": return android.R.drawable.ic_menu_recent_history;
            case "journaling": return android.R.drawable.ic_menu_edit;
            case "exercise": return android.R.drawable.ic_menu_directions;
            case "crisis_support": return android.R.drawable.ic_dialog_alert;
            default: return android.R.drawable.ic_menu_info_details;
        }
    }

    private int getTypeColor(String type) {
        switch (type.toLowerCase()) {
            case "meditation": return 0xFFA855F7; // Purple
            case "breathing": return 0xFF3B82F6; // Blue
            case "journaling": return 0xFF10B981; // Green
            case "exercise": return 0xFFF59E0B; // Orange
            case "crisis_support": return 0xFFEF4444; // Red
            default: return 0xFF6B7280; // Gray
        }
    }

    private String getInstructionsForType(String type) {
        switch (type.toLowerCase()) {
            case "meditation":
                return "1. Find a comfortable seated position\n" +
                        "2. Close your eyes or soften your gaze\n" +
                        "3. Focus on your natural breathing\n" +
                        "4. When thoughts arise, gently return to your breath\n" +
                        "5. Stay present and be kind to yourself";

            case "breathing":
                return "1. Sit or lie down comfortably\n" +
                        "2. Place one hand on chest, one on belly\n" +
                        "3. Breathe in slowly through your nose for 4 counts\n" +
                        "4. Hold your breath for 4 counts\n" +
                        "5. Exhale slowly through your mouth for 6 counts\n" +
                        "6. Repeat this cycle";

            case "journaling":
                return "1. Find a quiet space with pen and paper\n" +
                        "2. Write about your current thoughts and feelings\n" +
                        "3. Don't worry about grammar or structure\n" +
                        "4. Be honest and authentic with yourself\n" +
                        "5. Reflect on what you've written";

            case "exercise":
                return "1. Choose a physical activity you enjoy\n" +
                        "2. Start with gentle movements to warm up\n" +
                        "3. Gradually increase intensity as comfortable\n" +
                        "4. Focus on how your body feels\n" +
                        "5. Cool down with stretching";

            case "crisis_support":
                return "If you're experiencing a mental health crisis:\n\n" +
                        "🚨 Emergency: Call 911\n" +
                        "📞 Suicide Prevention: Call 988\n" +
                        "💬 Crisis Text Line: Text HOME to 741741\n" +
                        "🆘 SAMHSA Helpline: 1-800-662-4357\n\n" +
                        "You are not alone. Professional help is available.";

            default:
                return "Follow the guidance provided and take your time with this activity. Remember to be gentle with yourself throughout the process.";
        }
    }

    private void setClickListeners() {
        startButton.setOnClickListener(v -> {
            if (recommendationType.equals("crisis_support")) {
                handleCrisisSupport();
            } else {
                startActivity();
            }
        });

        pauseButton.setOnClickListener(v -> pauseActivity());

        completeButton.setOnClickListener(v -> completeActivity());

        skipButton.setOnClickListener(v -> skipActivity());
    }

    private void handleCrisisSupport() {
        // Show crisis support options
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Crisis Support")
                .setMessage("Choose how you'd like to get help:")
                .setPositiveButton("Call 988 (Suicide Prevention)", (dialog, which) -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(android.net.Uri.parse("tel:988"));
                    startActivity(callIntent);
                })
                .setNeutralButton("Text Crisis Line", (dialog, which) -> {
                    Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                    smsIntent.setData(android.net.Uri.parse("sms:741741"));
                    smsIntent.putExtra("sms_body", "HOME");
                    startActivity(smsIntent);
                })
                .setNegativeButton("Emergency 911", (dialog, which) -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(android.net.Uri.parse("tel:911"));
                    startActivity(callIntent);
                })
                .show();
    }

    private void startActivity() {
        if (!isTimerRunning) {
            isTimerRunning = true;
            startButton.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            completeButton.setVisibility(View.VISIBLE);

            activityTimer = new CountDownTimer(remainingTimeMs, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    remainingTimeMs = millisUntilFinished;
                    updateTimerDisplay();
                    updateProgressBar();
                }

                @Override
                public void onFinish() {
                    isTimerRunning = false;
                    completeActivity();
                }
            }.start();

            Toast.makeText(this, "Activity started!", Toast.LENGTH_SHORT).show();
        }
    }

    private void pauseActivity() {
        if (isTimerRunning && activityTimer != null) {
            activityTimer.cancel();
            isTimerRunning = false;
            startButton.setVisibility(View.VISIBLE);
            startButton.setText("Resume");
            pauseButton.setVisibility(View.GONE);
            Toast.makeText(this, "Activity paused", Toast.LENGTH_SHORT).show();
        }
    }

    private void completeActivity() {
        if (activityTimer != null) {
            activityTimer.cancel();
        }

        isActivityCompleted = true;
        isTimerRunning = false;

        // Save completion to Firebase
        saveCompletionToFirebase();

        // Show completion message
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Activity Completed!")
                .setMessage("Great job! You've completed this activity. Keep up the good work on your mental wellness journey.")
                .setPositiveButton("View More Recommendations", (dialog, which) -> {
                    finish();
                })
                .setNegativeButton("Done", (dialog, which) -> {
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void skipActivity() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Skip Activity?")
                .setMessage("Are you sure you want to skip this activity? You can always come back to it later.")
                .setPositiveButton("Yes, Skip", (dialog, which) -> {
                    if (activityTimer != null) {
                        activityTimer.cancel();
                    }
                    finish();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private void updateTimerDisplay() {
        if (remainingTimeMs > 0) {
            long minutes = (remainingTimeMs / 1000) / 60;
            long seconds = (remainingTimeMs / 1000) % 60;
            String timeString = String.format("%02d:%02d", minutes, seconds);
            timerText.setText(timeString);
        } else {
            timerText.setText("00:00");
        }
    }

    private void updateProgressBar() {
        if (totalTimeMs > 0) {
            int progress = (int) (((totalTimeMs - remainingTimeMs) * 100) / totalTimeMs);
            timerProgress.setProgress(progress);
        }
    }

    private void saveCompletionToFirebase() {
        String userId = auth.getCurrentUser().getUid();

        java.util.Map<String, Object> completion = new java.util.HashMap<>();
        completion.put("title", recommendationTitle);
        completion.put("type", recommendationType);
        completion.put("completedAt", System.currentTimeMillis());
        completion.put("duration", recommendationDuration);

        db.collection("users").document(userId)
                .collection("completed_activities")
                .add(completion)
                .addOnSuccessListener(documentReference -> {
                    // Successfully saved
                })
                .addOnFailureListener(e -> {
                    // Handle error silently
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (activityTimer != null) {
            activityTimer.cancel();
        }
    }

    @Override
    public void onBackPressed() {
        if (isTimerRunning) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("Activity in Progress")
                    .setMessage("You have an activity in progress. Do you want to pause and go back?")
                    .setPositiveButton("Yes, Go Back", (dialog, which) -> {
                        if (activityTimer != null) {
                            activityTimer.cancel();
                        }
                        super.onBackPressed();
                    })
                    .setNegativeButton("Continue Activity", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}