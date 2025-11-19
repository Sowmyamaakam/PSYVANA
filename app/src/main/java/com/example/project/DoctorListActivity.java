package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DoctorListActivity extends AppCompatActivity implements DoctorListAdapter.OnDoctorClickListener {

    private ImageButton backButton;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;

    private DoctorListAdapter adapter;
    private FirebaseFirestore db;

    private String modality; // "video" | "phone" | "chat" (mapped to video/call/text)

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_list);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();

        modality = getIntent().getStringExtra("consultation_type");
        if (modality == null) modality = "video";
        if (modality.equals("phone")) modality = "call";
        if (modality.equals("chat")) modality = "text";

        backButton = findViewById(R.id.backButton);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);

        adapter = new DoctorListAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> finish());

        loadDoctors();
    }

    private void loadDoctors() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").whereEqualTo("role", "doctor").get().addOnSuccessListener(snap -> {
            List<DoctorListAdapter.Doctor> list = new ArrayList<>();
            for (QueryDocumentSnapshot d : snap) {
                Boolean supports = false;
                Object modalities = d.get("modalities");
                if (modalities instanceof Map) {
                    Object val = ((Map<?,?>) modalities).get(modality);
                    if (val instanceof Boolean) supports = (Boolean) val;
                }
                if (!supports) continue;

                DoctorListAdapter.Doctor item = new DoctorListAdapter.Doctor();
                item.id = d.getId();
                item.name = d.getString("name");
                Object specs = d.get("specialties");
                if (specs instanceof List) item.specialties = joinList((List<?>) specs);
                else item.specialties = d.getString("bio") != null ? d.getString("bio") : "";
                Object ratingAvg = d.get("rating.avg");
                item.rating = ratingAvg instanceof Number ? ((Number) ratingAvg).floatValue() : 4.8f;
                Object rateObj = d.get("rate");
                if (rateObj instanceof Map) {
                    item.rate = (Map<String, Object>) rateObj;
                    Object price = ((Map<?,?>) rateObj).get(modality);
                    Object cur = ((Map<?,?>) rateObj).get("currency");
                    String currency = cur != null ? cur.toString() : "INR";
                    String amount = price instanceof Number ? String.valueOf(((Number) price).intValue()) : "-";
                    item.displayPrice = currency + " " + amount;
                } else {
                    item.displayPrice = "INR -";
                }
                list.add(item);
            }
            adapter.setItems(list);
            progressBar.setVisibility(View.GONE);
            emptyText.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            emptyText.setText("Failed to load doctors: " + e.getMessage());
        });
    }

    private String joinList(List<?> arr) {
        List<String> s = new ArrayList<>();
        for (Object o : arr) s.add(String.valueOf(o));
        return String.join(", ", s);
    }

    @Override
    public void onDoctorClick(DoctorListAdapter.Doctor item) {
        Intent i = new Intent(this, BookAppointmentActivity.class);
        i.putExtra("doctor_id", item.id);
        i.putExtra("modality", modality);
        startActivity(i);
    }
}
