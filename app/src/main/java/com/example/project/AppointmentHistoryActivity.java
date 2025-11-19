package com.example.project;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppointmentHistoryActivity extends AppCompatActivity {

    private RecyclerView historyRecycler;
    private ProgressBar progressBar;
    private TextView emptyText;
    private ImageButton backButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<Map<String, Object>> data = new ArrayList<>();
    private AppointmentHistoryAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_history);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        historyRecycler = findViewById(R.id.historyRecycler);
        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        historyRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AppointmentHistoryAdapter(data);
        historyRecycler.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        if (auth.getCurrentUser() == null) {
            showEmpty();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        String uid = auth.getCurrentUser().getUid();
        db.collection("appointments")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "completed")
                .orderBy("startAt", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        progressBar.setVisibility(View.GONE);
                        data.clear();
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (DocumentSnapshot d : task.getResult().getDocuments()) {
                                data.add(d.getData());
                            }
                            adapter.notifyDataSetChanged();
                            toggleEmpty();
                            Toast.makeText(AppointmentHistoryActivity.this, "History items: " + data.size(), Toast.LENGTH_SHORT).show();
                        } else {
                            toggleEmpty();
                            String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                            Toast.makeText(AppointmentHistoryActivity.this, "Load failed: " + msg, Toast.LENGTH_LONG).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    toggleEmpty();
                    Toast.makeText(AppointmentHistoryActivity.this, "Load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void toggleEmpty() {
        boolean isEmpty = data.isEmpty();
        emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        historyRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        data.clear();
        adapter.notifyDataSetChanged();
        toggleEmpty();
    }
}
