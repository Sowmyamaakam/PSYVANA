package com.example.project;


import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProgressActivity extends AppCompatActivity {

    private ImageButton backButton;
    private SimpleBarChartView progressChart;
    private TextView currentScoreText;
    private RecyclerView activitiesRecyclerView;
    private Button returnDashboardButton;
    private TextView weeklyCountText;
    private TextView streakText;
    private TextView topTypeText;
    

    private ActivitiesAdapter activitiesAdapter;

    private List<ActivityItem> activities;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        initViews();
        setupData();
        setupRecyclerViews();
        setupClickListeners();
        loadProgressData();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        progressChart = findViewById(R.id.progressChart);
        currentScoreText = findViewById(R.id.currentScoreText);
        activitiesRecyclerView = findViewById(R.id.activitiesRecyclerView);
        returnDashboardButton = findViewById(R.id.returnDashboardButton);
        weeklyCountText = findViewById(R.id.weeklyCountText);
        streakText = findViewById(R.id.streakText);
        topTypeText = findViewById(R.id.topTypeText);
        

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private void setupData() {
        List<ChartDataPoint> chartData = new ArrayList<>();
        progressChart.setData(chartData);

        activities = new ArrayList<>();
    }

    private void setupRecyclerViews() {
        // Activities RecyclerView
        activitiesAdapter = new ActivitiesAdapter(activities);
        activitiesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        activitiesRecyclerView.setAdapter(activitiesAdapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        returnDashboardButton.setOnClickListener(v -> finish());
    }

    private void loadProgressData() {
        // TODO: Load actual progress data from Firebase
        // This is a placeholder - you would fetch real data here
        if (auth.getCurrentUser() == null) return;
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId)
                .collection("progress")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(2)
                .get()
                .addOnSuccessListener(snapshots -> {
                    List<ChartDataPoint> chartPoints = new ArrayList<>();
                    if (!snapshots.isEmpty()) {
                        DocumentSnapshot latest = snapshots.getDocuments().get(0);
                        Number scoreNum = latest.getDouble("score");
                        if (scoreNum == null) scoreNum = latest.getLong("score");
                        if (scoreNum != null) currentScoreText.setText(scoreNum.intValue() + "%");

                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            String label = doc.getString("label");
                            if (label == null) label = doc.getString("month");
                            Number valNum = doc.getDouble("score");
                            if (valNum == null) valNum = doc.getLong("value");
                            int value = valNum != null ? valNum.intValue() : 0;
                            if (label == null) {
                                Timestamp ts = doc.getTimestamp("timestamp");
                                label = ts != null ? formatMonthLabel(ts.toDate()) : "";
                            }
                            chartPoints.add(new ChartDataPoint(label, value));
                        }
                        Collections.reverse(chartPoints);
                    }
                    progressChart.setData(chartPoints);
                });

        db.collection("users").document(userId)
                .collection("completed_activities")
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(snapshots -> {
                    activities.clear();
                    // For stats
                    long nowMs = System.currentTimeMillis();
                    long sevenDaysAgoMs = nowMs - 7L * 24L * 60L * 60L * 1000L;
                    int weeklyCount = 0;
                    Map<String, Integer> typeCounts = new HashMap<>();
                    Set<Long> dayBuckets = new HashSet<>();
                    
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String name = doc.getString("name");
                        if (name == null) name = doc.getString("title");
                        if (name == null) name = "";

                        String time = doc.getString("timeText");
                        if (time == null) time = doc.getString("duration");

                        String color = doc.getString("dotColor");
                        String type = doc.getString("type");

                        Timestamp ts = doc.getTimestamp("timestamp");
                        if (ts == null) {
                            Long completedAt = doc.getLong("completedAt");
                            if (completedAt != null) {
                                long ms = completedAt < 1_000_000_000_000L ? completedAt * 1000L : completedAt;
                                ts = new Timestamp(new Date(ms));
                            }
                        }

                        if (time == null && ts != null) time = toRelativeTime(ts.toDate());
                        if (time == null) time = "";

                        if (color == null) {
                            if (type == null) type = "";
                            switch (type.toLowerCase()) {
                                case "meditation":
                                    color = "#9C27B0"; // purple
                                    break;
                                case "journaling":
                                    color = "#2196F3"; // blue
                                    break;
                                case "exercise":
                                    color = "#4CAF50"; // green
                                    break;
                                default:
                                    color = "#4CAF50";
                            }
                        }

                        activities.add(new ActivityItem(name, time, color));

                        // Accumulate stats
                        if (ts != null) {
                            long ms = ts.toDate().getTime();
                            if (ms >= sevenDaysAgoMs) weeklyCount++;
                            long day = ms / (24L * 60L * 60L * 1000L);
                            dayBuckets.add(day);
                        }
                        if (type != null) {
                            typeCounts.put(type, typeCounts.getOrDefault(type, 0) + 1);
                        }
                        
                    }
                    activitiesAdapter.notifyDataSetChanged();

                    // Compute top type
                    String topType = "-";
                    int maxCount = 0;
                    for (Map.Entry<String, Integer> e : typeCounts.entrySet()) {
                        if (e.getValue() > maxCount) {
                            maxCount = e.getValue();
                            topType = e.getKey();
                        }
                    }

                    // Compute streak (consecutive days up to today)
                    long todayDay = System.currentTimeMillis() / (24L * 60L * 60L * 1000L);
                    int streak = 0;
                    long d = todayDay;
                    while (dayBuckets.contains(d)) {
                        streak++;
                        d--;
                    }

                    // Bind stats
                    weeklyCountText.setText(String.valueOf(weeklyCount));
                    streakText.setText(streak + (streak == 1 ? " day" : " days"));
                    topTypeText.setText(capitalize(topType));
                    
                });
    }

    // Model Classes
    public static class ChartDataPoint {
        private String label;
        private int value;

        public ChartDataPoint(String label, int value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public int getValue() { return value; }
    }

    public static class TriggerItem {
        private String name;
        private String improvement;
        private String color;

        public TriggerItem(String name, String improvement, String color) {
            this.name = name;
            this.improvement = improvement;
            this.color = color;
        }

        public String getName() { return name; }
        public String getImprovement() { return improvement; }
        public String getColor() { return color; }
    }

    public static class ActivityItem {
        private String name;
        private String time;
        private String dotColor;

        public ActivityItem(String name, String time, String dotColor) {
            this.name = name;
            this.time = time;
            this.dotColor = dotColor;
        }

        public String getName() { return name; }
        public String getTime() { return time; }
        public String getDotColor() { return dotColor; }
    }

    private String toRelativeTime(Date date) {
        long now = System.currentTimeMillis();
        long then = date.getTime();
        long diff = Math.max(0, now - then);
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";
        if (hours < 24) return hours + " hr ago";
        if (days == 1) return "Yesterday";
        return days + " days ago";
    }

    private String formatMonthLabel(Date date) {
        String[] months = {"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        @SuppressWarnings("deprecation")
        int m = date.getMonth();
        return months[Math.max(0, Math.min(11, m))];
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "-";
        return s.substring(0,1).toUpperCase() + s.substring(1);
    }
}