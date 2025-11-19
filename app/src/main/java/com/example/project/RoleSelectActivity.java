package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class RoleSelectActivity extends AppCompatActivity {

    private Button loginUserButton;
    private Button loginDoctorButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_select);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        loginUserButton = findViewById(R.id.loginUserButton);
        loginDoctorButton = findViewById(R.id.loginDoctorButton);

        loginUserButton.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });

        loginDoctorButton.setOnClickListener(v -> {
            startActivity(new Intent(this, DoctorLoginActivity.class));
        });
    }
}
