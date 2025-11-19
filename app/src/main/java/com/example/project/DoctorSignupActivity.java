package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class DoctorSignupActivity extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText;
    private EditText genderEditText, specialistEditText, phoneEditText;
    private Button signupButton;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_signup);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        genderEditText = findViewById(R.id.genderEditText);
        specialistEditText = findViewById(R.id.specialistEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        signupButton = findViewById(R.id.signupButton);
        progressBar = findViewById(R.id.progressBar);
        View backButton = findViewById(R.id.backButton);
        if (backButton != null) backButton.setOnClickListener(v -> finish());

        signupButton.setOnClickListener(v -> handleSignup());
    }

    private void handleSignup() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String gender = genderEditText != null ? genderEditText.getText().toString().trim() : "";
        String specialist = specialistEditText != null ? specialistEditText.getText().toString().trim() : "";
        String phone = phoneEditText != null ? phoneEditText.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) { nameEditText.setError("Required"); nameEditText.requestFocus(); return; }
        if (TextUtils.isEmpty(email)) { emailEditText.setError("Required"); emailEditText.requestFocus(); return; }
        if (TextUtils.isEmpty(password) || password.length() < 6) { passwordEditText.setError("Min 6 chars"); passwordEditText.requestFocus(); return; }
        if (TextUtils.isEmpty(gender)) { genderEditText.setError("Required"); genderEditText.requestFocus(); return; }
        if (TextUtils.isEmpty(specialist)) { specialistEditText.setError("Required"); specialistEditText.requestFocus(); return; }
        if (TextUtils.isEmpty(phone)) { phoneEditText.setError("Required"); phoneEditText.requestFocus(); return; }
        // Basic phone validation: digits and length >= 10
        String digitsOnly = phone.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 10) { phoneEditText.setError("Enter valid phone number"); phoneEditText.requestFocus(); return; }

        progressBar.setVisibility(View.VISIBLE);
        signupButton.setEnabled(false);

        auth.fetchSignInMethodsForEmail(email)
                .addOnSuccessListener(result -> {
                    boolean exists = result != null && result.getSignInMethods() != null && !result.getSignInMethods().isEmpty();
                    if (exists) {
                        progressBar.setVisibility(View.GONE);
                        signupButton.setEnabled(true);
                        Toast.makeText(DoctorSignupActivity.this, "Email already registered. Please login.", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(DoctorSignupActivity.this, DoctorLoginActivity.class);
                        intent.putExtra("prefill_email", email);
                        startActivity(intent);
                        return;
                    }

                    // Create account
                    auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(this, task -> {
                                progressBar.setVisibility(View.GONE);
                                signupButton.setEnabled(true);
                                if (task.isSuccessful() && auth.getCurrentUser() != null) {
                                    String uid = auth.getCurrentUser().getUid();

                                    // Store doctor profile in users/{uid}
                                    Map<String, Object> userDoc = new HashMap<>();
                                    userDoc.put("name", name);
                                    userDoc.put("email", email);
                                    userDoc.put("gender", gender);
                                    userDoc.put("specialist", specialist);
                                    userDoc.put("phone", digitsOnly);
                                    userDoc.put("role", "doctor");
                                    userDoc.put("modalities", new HashMap<String, Object>() {{
                                        put("video", true); put("call", false); put("text", false);
                                    }});
                                    userDoc.put("rate", new HashMap<String, Object>() {{
                                        put("video", 1000); put("call", 700); put("text", 500); put("currency", "INR");
                                    }});
                                    userDoc.put("timezone", "Asia/Kolkata");

                                    db.collection("users").document(uid).set(userDoc)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(DoctorSignupActivity.this, "Doctor account created", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(DoctorSignupActivity.this, DoctorProfileActivity.class);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(DoctorSignupActivity.this, "Failed to save user: " + e.getMessage(), Toast.LENGTH_LONG).show()
                                            );
                                } else {
                                    Toast.makeText(DoctorSignupActivity.this, task.getException() != null ? task.getException().getMessage() : "Signup failed", Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    signupButton.setEnabled(true);
                    Toast.makeText(DoctorSignupActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

}

