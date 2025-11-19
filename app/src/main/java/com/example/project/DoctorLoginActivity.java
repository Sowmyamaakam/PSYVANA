package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.TextView;
import android.util.Patterns;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class DoctorLoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton, goSignupButton;
    private ProgressBar progressBar;
    private TextView forgotPasswordText;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_login);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        goSignupButton = findViewById(R.id.goSignupButton);
        progressBar = findViewById(R.id.progressBar);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
        View backButton = findViewById(R.id.backButton);
        if (backButton != null) backButton.setOnClickListener(v -> finish());

        // Prefill email if passed from signup flow
        String prefill = getIntent().getStringExtra("prefill_email");
        if (prefill != null && !prefill.isEmpty()) {
            emailEditText.setText(prefill);
            passwordEditText.requestFocus();
        }

        loginButton.setOnClickListener(v -> handleLogin());
        goSignupButton.setOnClickListener(v -> startActivity(new Intent(this, DoctorSignupActivity.class)));
        if (forgotPasswordText != null) {
            forgotPasswordText.setOnClickListener(v -> handleForgotPassword());
        }
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) { emailEditText.setError("Required"); emailEditText.requestFocus(); return; }
        if (TextUtils.isEmpty(password)) { passwordEditText.setError("Required"); passwordEditText.requestFocus(); return; }

        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);
                        if (task.isSuccessful() && auth.getCurrentUser() != null) {
                            String uid = auth.getCurrentUser().getUid();
                            // Ensure role is doctor
                            db.collection("users").document(uid).get()
                                    .addOnSuccessListener(snap -> {
                                        if (snap.exists()) {
                                            String role = snap.getString("role");
                                            if ("doctor".equals(role)) {
                                                Toast.makeText(DoctorLoginActivity.this, "Welcome, Doctor", Toast.LENGTH_SHORT).show();
                                                boolean complete = false;
                                                try {
                                                    Object modalitiesObj = snap.get("modalities");
                                                    boolean hasAnyMod = false;
                                                    if (modalitiesObj instanceof java.util.Map) {
                                                        java.util.Map<?,?> m = (java.util.Map<?,?>) modalitiesObj;
                                                        hasAnyMod = java.lang.Boolean.TRUE.equals(m.get("video")) ||
                                                                java.lang.Boolean.TRUE.equals(m.get("call")) ||
                                                                java.lang.Boolean.TRUE.equals(m.get("text"));
                                                    }

                                                    Object rateObj = snap.get("rate");
                                                    boolean hasRate = false;
                                                    if (rateObj instanceof java.util.Map) {
                                                        Object v = ((java.util.Map<?,?>) rateObj).get("video");
                                                        hasRate = v instanceof Number && ((Number) v).intValue() > 0;
                                                    }

                                                    Object availObj = snap.get("availability");
                                                    boolean hasWeekly = false;
                                                    if (availObj instanceof java.util.Map) {
                                                        Object weekly = ((java.util.Map<?,?>) availObj).get("weekly");
                                                        if (weekly instanceof java.util.Map) {
                                                            hasWeekly = !((java.util.Map<?,?>) weekly).isEmpty();
                                                        }
                                                    }

                                                    complete = hasAnyMod && hasRate && hasWeekly;
                                                } catch (Exception ignore) { }

                                                Class<?> dest = complete ? DoctorAppointmentsActivity.class : DoctorProfileActivity.class;
                                                Intent intent = new Intent(DoctorLoginActivity.this, dest);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                            } else {
                                                Toast.makeText(DoctorLoginActivity.this, "This account is not a doctor account", Toast.LENGTH_LONG).show();
                                            }
                                        } else {
                                            Toast.makeText(DoctorLoginActivity.this, "User record not found", Toast.LENGTH_LONG).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(DoctorLoginActivity.this, e.getMessage(), Toast.LENGTH_LONG).show());
                        } else {
                            Toast.makeText(DoctorLoginActivity.this, task.getException() != null ? task.getException().getMessage() : "Login failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void handleForgotPassword() {
        String email = emailEditText.getText().toString().trim();
        if (TextUtils.isEmpty(email)) { emailEditText.setError("Enter your email to reset password"); emailEditText.requestFocus(); return; }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) { emailEditText.setError("Enter a valid email"); emailEditText.requestFocus(); return; }

        progressBar.setVisibility(View.VISIBLE);
        auth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    Toast.makeText(DoctorLoginActivity.this, "Password reset email sent", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(DoctorLoginActivity.this, task.getException() != null ? task.getException().getMessage() : "Failed to send reset email", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
