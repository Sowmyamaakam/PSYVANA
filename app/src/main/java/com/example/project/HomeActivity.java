package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private TextView welcomeText;
    private CardView selfAssessmentCard;
    private CardView myProgressCard;
    private CardView recommendationsCard;
    private CardView professionalHelpCard;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // ✅ Check if user is logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Redirect to login/main activity
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Hide the action bar for a cleaner look
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        initViews();

        // Set user welcome message
        setWelcomeMessage(currentUser);

        // Set click listeners
        setClickListeners();
    }

    private void initViews() {
        welcomeText = findViewById(R.id.welcomeText);
        selfAssessmentCard = findViewById(R.id.selfAssessmentCard);
        myProgressCard = findViewById(R.id.myProgressCard);
        recommendationsCard = findViewById(R.id.recommendationsCard);
        professionalHelpCard = findViewById(R.id.professionalHelpCard);
    }

    private void setWelcomeMessage(FirebaseUser currentUser) {
        String displayName = currentUser.getDisplayName();
        if (displayName != null && !displayName.isEmpty()) {
            welcomeText.setText("Hello " + displayName.split(" ")[0] + "!");
        } else if (currentUser.getEmail() != null) {
            welcomeText.setText("Hello " + currentUser.getEmail().split("@")[0] + "!");
        } else {
            welcomeText.setText("Hello there!");
        }
    }

    private void setClickListeners() {
        selfAssessmentCard.setOnClickListener(v -> handleSelfAssessment());
        myProgressCard.setOnClickListener(v -> handleMyProgress());
        recommendationsCard.setOnClickListener(v -> handleRecommendations());
        professionalHelpCard.setOnClickListener(v -> handleProfessionalHelp());
    }

    private boolean isUserLoggedIn() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void handleSelfAssessment() {
        if (!isUserLoggedIn()) return;
        Intent intent = new Intent(HomeActivity.this, SelfAssessmentActivity.class);
        startActivity(intent);
    }

    private void handleMyProgress() {
        if (!isUserLoggedIn()) return;
        Intent intent = new Intent(HomeActivity.this, ProgressActivity.class);
        startActivity(intent);
    }

    private void handleRecommendations() {
        if (!isUserLoggedIn()) return;
        Intent intent = new Intent(HomeActivity.this, RecommendationsActivity.class);
        startActivity(intent);
    }

    private void handleProfessionalHelp() {
        if (!isUserLoggedIn()) return;
        Intent intent = new Intent(HomeActivity.this, ProfessionalHelpActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        // Optional: confirm before logout
        super.onBackPressed();
        mAuth.signOut();
        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
