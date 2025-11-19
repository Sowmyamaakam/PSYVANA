package com.example.project;



import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

public class ProfessionalHelpActivity extends AppCompatActivity {

    private ImageButton backButton;
    private RecyclerView consultationOptionsRecyclerView;
    private Button scheduleButton;
    private Button returnDashboardButton;
    private Button historyButton;
    private Button upcomingButton;

    private ConsultationOptionsAdapter consultationAdapter;
    // Removed featured doctors adapter

    private List<ConsultationOption> consultationOptions;
    private String selectedOptionId = "";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private boolean didRedirect = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_professional_help);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupData();
        setupRecyclerViews();
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // If user has an existing non-cancelled appointment, redirect to its detail view
        if (didRedirect) return;
        if (auth != null && auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            db.collection("appointments").whereEqualTo("userId", uid).get()
                    .addOnSuccessListener(snaps -> {
                        String pickId = null;
                        long bestTs = Long.MIN_VALUE;
                        for (QueryDocumentSnapshot d : snaps) {
                            String status = d.getString("status");
                            if (status == null) continue;
                            if ("cancelled".equals(status)) continue;
                            // consider pending/accepted/rejected
                            Object tsObj = d.get("updatedAt");
                            long ts = 0L;
                            if (tsObj instanceof Timestamp) ts = ((Timestamp) tsObj).toDate().getTime();
                            else {
                                Object cObj = d.get("createdAt");
                                if (cObj instanceof Timestamp) ts = ((Timestamp) cObj).toDate().getTime();
                            }
                            if (ts > bestTs) {
                                bestTs = ts;
                                pickId = d.getId();
                            }
                        }
                        if (pickId != null) {
                            didRedirect = true;
                            Intent intent = new Intent(ProfessionalHelpActivity.this, BookAppointmentActivity.class);
                            intent.putExtra("appointment_id", pickId);
                            startActivity(intent);
                        }
                    });
        }
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        consultationOptionsRecyclerView = findViewById(R.id.consultationOptionsRecyclerView);
        scheduleButton = findViewById(R.id.scheduleButton);
        returnDashboardButton = findViewById(R.id.returnDashboardButton);
        historyButton = findViewById(R.id.historyButton);
        upcomingButton = findViewById(R.id.upcomingButton);
    }

    private void setupData() {
        // Consultation Options
        consultationOptions = new ArrayList<>();
        consultationOptions.add(new ConsultationOption(
                "video",
                "Video Consultation",
                "Face-to-face session with a licensed therapist",
                R.drawable.ic_video,
                "50 min",
                "Available today"
        ));
        consultationOptions.add(new ConsultationOption(
                "phone",
                "Phone Consultation",
                "Voice call with a mental health professional",
                R.drawable.ic_phone,
                "45 min",
                "Available now"
        ));
        consultationOptions.add(new ConsultationOption(
                "chat",
                "Text Therapy",
                "Secure messaging with your therapist",
                R.drawable.ic_message_circle,
                "Ongoing",
                "24/7 response"
        ));

        // Featured doctors section removed
    }

    private void setupRecyclerViews() {
        // Consultation Options
        consultationAdapter = new ConsultationOptionsAdapter(consultationOptions,
                new ConsultationOptionsAdapter.OnConsultationClickListener() {
                    @Override
                    public void onConsultationClick(ConsultationOption option) {
                        selectedOptionId = option.getId();
                        updateScheduleButton();
                        consultationAdapter.setSelectedId(selectedOptionId);
                        consultationAdapter.notifyDataSetChanged();
                    }
                });
        consultationOptionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        consultationOptionsRecyclerView.setAdapter(consultationAdapter);

        // Featured doctors section removed
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        returnDashboardButton.setOnClickListener(v -> finish());

        if (historyButton != null) {
            historyButton.setOnClickListener(v -> {
                Intent i = new Intent(ProfessionalHelpActivity.this, AppointmentHistoryActivity.class);
                startActivity(i);
            });
        }

        if (upcomingButton != null) {
            upcomingButton.setOnClickListener(v -> {
                Intent i = new Intent(ProfessionalHelpActivity.this, UpcomingAppointmentsActivity.class);
                startActivity(i);
            });
        }

        scheduleButton.setOnClickListener(v -> {
            if (!selectedOptionId.isEmpty()) {
                Intent intent = new Intent(this, DoctorListActivity.class);
                intent.putExtra("consultation_type", selectedOptionId);
                startActivity(intent);
            }
        });
    }

    private void updateScheduleButton() {
        if (selectedOptionId.isEmpty()) {
            scheduleButton.setEnabled(false);
            scheduleButton.setText("Select an option above");
            scheduleButton.setBackgroundResource(R.drawable.button_disabled_bg);
        } else {
            scheduleButton.setEnabled(true);
            scheduleButton.setText("📅  Schedule Consultation");
            scheduleButton.setBackgroundResource(R.drawable.button_teal_bg);
        }
    }

    // Model classes
    public static class ConsultationOption {
        private String id;
        private String title;
        private String description;
        private int iconResId;
        private String duration;
        private String availability;

        public ConsultationOption(String id, String title, String description, int iconResId,
                                  String duration, String availability) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.iconResId = iconResId;
            this.duration = duration;
            this.availability = availability;
        }

        // Getters
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public int getIconResId() { return iconResId; }
        public String getDuration() { return duration; }
        public String getAvailability() { return availability; }
    }

    public static class FeaturedDoctor {
        private String name;
        private String specialty;
        private float rating;
        private String experience;
        private String avatar;

        public FeaturedDoctor(String name, String specialty, float rating,
                              String experience, String avatar) {
            this.name = name;
            this.specialty = specialty;
            this.rating = rating;
            this.experience = experience;
            this.avatar = avatar;
        }

        // Getters
        public String getName() { return name; }
        public String getSpecialty() { return specialty; }
        public float getRating() { return rating; }
        public String getExperience() { return experience; }
        public String getAvatar() { return avatar; }
    }
}