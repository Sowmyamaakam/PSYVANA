package com.example.project;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Patterns;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;

import com.example.project.HomeActivity;
import com.example.project.SignUpActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText phoneEditText;
    private Button loginButton;
    private Button signUpButton;
    private ProgressBar progressBar;
    private TextView forgotPasswordText;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Hide the action bar for a cleaner look
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Initialize views
        initViews();

        // Set click listeners
        setClickListeners();
    }

    @Override
public void onStart() {
    super.onStart();
    // Only auto-login if you explicitly saved a preference
    if (getSharedPreferences("auth", MODE_PRIVATE).getBoolean("keepSignedIn", false)
            && mAuth.getCurrentUser() != null) {
        navigateToMainActivity();
    }
}

    private void initViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        phoneEditText = findViewById(R.id.phoneEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpButton = findViewById(R.id.signUpButton);
        progressBar = findViewById(R.id.progressBar);
        forgotPasswordText = findViewById(R.id.forgotPasswordText);
    }

    private void setClickListeners() {
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleLogin();
            }
        });

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSignUp();
            }
        });

        if (forgotPasswordText != null) {
            forgotPasswordText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleForgotPassword();
                }
            });
        }
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        progressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);

                        if (task.isSuccessful()) {
                            // Login success
                            Toast.makeText(MainActivity.this, "Welcome back!", Toast.LENGTH_SHORT).show();
                            navigateToMainActivity();
                        } else {
                            // Login failed
                            Exception e = task.getException();
                            if (e != null && e.getMessage().contains("no user record")) {
                                // User does not exist → redirect to SignUpActivity
                                Toast.makeText(MainActivity.this, "Account not found. Please sign up.", Toast.LENGTH_LONG).show();
                                Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
                                startActivity(intent);
                            } else {
                                // Other errors (wrong password, network, etc.)
                                Toast.makeText(MainActivity.this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }

    private void handleForgotPassword() {
        String email = emailEditText.getText().toString().trim();
        if (email.isEmpty()) {
            emailEditText.setError("Enter your email to reset password");
            emailEditText.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email");
            emailEditText.requestFocus();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            Toast.makeText(MainActivity.this, "Password reset email sent", Toast.LENGTH_LONG).show();
                        } else {
                            Exception e = task.getException();
                            Toast.makeText(MainActivity.this, e != null ? e.getMessage() : "Failed to send reset email", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }


    private void navigateToMainActivity() {
        // Navigate to Home activity after successful login
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void handleSignUp() {
        // Navigate to sign up activity
        Intent intent = new Intent(MainActivity.this, SignUpActivity.class);
        startActivity(intent);
    }
}