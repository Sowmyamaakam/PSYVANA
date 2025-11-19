package com.example.project;

import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DoctorAppointmentsActivity extends AppCompatActivity implements DoctorAppointmentsAdapter.Listener {

    private ImageView backButton;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyText;
    private TextView profileName, profileModalities, profileRates, profileAvailability;
    private View editProfileButton;
    private Button doctorHistoryButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private DoctorAppointmentsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_appointments);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.backButton);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);
        profileName = findViewById(R.id.profileName);
        profileModalities = findViewById(R.id.profileModalities);
        profileRates = findViewById(R.id.profileRates);
        profileAvailability = findViewById(R.id.profileAvailability);
        editProfileButton = findViewById(R.id.editProfileButton);
        doctorHistoryButton = findViewById(R.id.doctorHistoryButton);

        adapter = new DoctorAppointmentsAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        backButton.setOnClickListener(v -> finish());
        editProfileButton.setOnClickListener(v -> startActivity(new Intent(this, DoctorProfileActivity.class)));
        if (doctorHistoryButton != null) {
            doctorHistoryButton.setOnClickListener(v -> startActivity(new Intent(this, DoctorHistoryActivity.class)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadHeader();
        load();
    }

    private void load() {
        if (auth.getCurrentUser() == null) { finish(); return; }
        String uid = auth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);
        db.collection("appointments")
                .whereEqualTo("doctorId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    List<DoctorAppointmentsAdapter.Item> items = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) {
                        DoctorAppointmentsAdapter.Item it = new DoctorAppointmentsAdapter.Item();
                        it.id = d.getId();
                        it.userId = d.getString("userId");
                        it.status = d.getString("status");
                        it.modality = String.valueOf(d.get("modality"));
                        Object start = d.get("startAt");
                        it.startAt = start instanceof Number ? ((Number) start).longValue() : 0L;
                        Object end = d.get("endAt");
                        it.endAt = end instanceof Number ? ((Number) end).longValue() : 0L;
                        Map<String, Object> price = (Map<String, Object>) d.get("price");
                        if (price != null) {
                            Object amt = price.get("amount");
                            Object cur = price.get("currency");
                            it.price = (cur != null ? cur.toString() : "INR") + " " + (amt instanceof Number ? String.valueOf(((Number) amt).intValue()) : "-");
                        } else it.price = "";
                        items.add(it);
                    }
                    adapter.setItems(items);
                    progressBar.setVisibility(View.GONE);
                    emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    emptyText.setVisibility(View.VISIBLE);
                    emptyText.setText("Failed to load: " + e.getMessage());
                });
    }

    private void loadHeader() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            setHeaderFromDoc(doc.getData());
        });
    }

    private void setHeaderFromDoc(Map<String, Object> user) {
        if (user == null) return;
        Object name = user.get("name");
        profileName.setText(name != null ? name.toString() : "");

        // Modalities line
        StringBuilder mods = new StringBuilder();
        Object modalities = user.get("modalities");
        if (modalities instanceof Map) {
            Map<?,?> m = (Map<?,?>) modalities;
            if (asBool(m.get("video"))) mods.append("Video, ");
            if (asBool(m.get("call"))) mods.append("Call, ");
            if (asBool(m.get("text"))) mods.append("Text, ");
            if (mods.length() > 2) mods.setLength(mods.length()-2);
        }
        profileModalities.setText(mods.length()>0 ? mods.toString() : "Modalities not set");

        // Rates line
        String rateLine = "";
        Object rateObj = user.get("rate");
        if (rateObj instanceof Map) {
            Map<?,?> r = (Map<?,?>) rateObj;
            String cur = r.get("currency") != null ? r.get("currency").toString() : "INR";
            rateLine = String.format(Locale.getDefault(),
                    "Rates: Video %s %s • Call %s %s • Text %s %s",
                    cur, num(r.get("video")), cur, num(r.get("call")), cur, num(r.get("text")));
        }
        profileRates.setText(rateLine.isEmpty()?"Rates not set":rateLine);

        // Availability line (compact)
        String availLine = "";
        Object avail = user.get("availability");
        if (avail instanceof Map) {
            Object weekly = ((Map<?,?>) avail).get("weekly");
            if (weekly instanceof Map) {
                String[] days = {"mon","tue","wed","thu","fri","sat","sun"};
                String[] labels = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
                StringBuilder sb = new StringBuilder("Availability: ");
                for (int i=0;i<days.length;i++) {
                    Object val = ((Map<?,?>) weekly).get(days[i]);
                    if (val instanceof String && !((String) val).isEmpty()) {
                        if (sb.length()>14) sb.append(", ");
                        sb.append(labels[i]).append(" ").append(formatRangeToAmPm((String) val));
                    }
                }
                availLine = sb.toString();
            }
        }
        profileAvailability.setText(availLine.isEmpty()?"Availability not set":availLine);
    }

    private boolean asBool(Object o) {
        return (o instanceof Boolean) ? (Boolean) o : false;
    }

    private String num(Object o) {
        return (o instanceof Number) ? String.valueOf(((Number) o).intValue()) : "-";
    }

    private String formatRangeToAmPm(String raw) {
        if (TextUtils.isEmpty(raw)) return raw;
        String[] parts = raw.split(",");
        StringBuilder out = new StringBuilder();
        for (String p : parts) {
            String seg = p.trim();
            if (seg.contains("-")) {
                String[] lr = seg.split("-");
                if (lr.length == 2) {
                    String left = toAmPm(lr[0].trim());
                    String right = toAmPm(lr[1].trim());
                    if (out.length()>0) out.append(", ");
                    out.append(left).append("-").append(right);
                    continue;
                }
            }
            if (out.length()>0) out.append(", ");
            out.append(seg);
        }
        return out.toString();
    }

    private String toAmPm(String hhmm) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date d = in.parse(hhmm);
            SimpleDateFormat out = new SimpleDateFormat("h:mm a", Locale.getDefault());
            return d != null ? out.format(d) : hhmm;
        } catch (ParseException e) {
            return hhmm;
        }
    }

    @Override
    public void onAccept(@NonNull DoctorAppointmentsAdapter.Item item) {
        Map<String, Object> extra = new HashMap<>();
        if ("video".equalsIgnoreCase(item.modality)) {
            String roomId = "psyvana-call-" + item.id;
            Map<String, Object> meeting = new HashMap<>();
            meeting.put("provider", "jitsi");
            meeting.put("joinUrl", "https://meet.jit.si/" + roomId);
            extra.put("meeting", meeting);
        } else if ("call".equalsIgnoreCase(item.modality) || "phone".equalsIgnoreCase(item.modality)) {
            String roomId = "call-" + item.id;
            extra.put("callUrl", "https://meet.jit.si/" + roomId);
        }
        updateStatus(item.id, "accepted", extra);
    }

    @Override
    public void onDecline(@NonNull DoctorAppointmentsAdapter.Item item) {
        updateStatus(item.id, "declined", null);
    }

    @Override
    public void onStart(@NonNull DoctorAppointmentsAdapter.Item item) {
        updateStatus(item.id, "in_progress", null);
    }

    @Override
    public void onComplete(@NonNull DoctorAppointmentsAdapter.Item item) {
        updateStatus(item.id, "completed", null);
    }

    @Override
    public void onOpenChat(@NonNull DoctorAppointmentsAdapter.Item item) {
        Intent chat = new Intent(this, ChatActivity.class);
        chat.putExtra(ChatActivity.EXTRA_APPOINTMENT_ID, item.id);
        startActivity(chat);
    }

    @Override
    public void onJoinCall(@NonNull DoctorAppointmentsAdapter.Item item) {
        Intent call = new Intent(this, AudioCallActivity.class);
        call.putExtra("appointment_id", item.id);
        startActivity(call);
    }

    @Override
    public void onJoinVideo(@NonNull DoctorAppointmentsAdapter.Item item) {
        Intent vid = new Intent(this, VideoCallActivity.class);
        vid.putExtra("appointment_id", item.id);
        startActivity(vid);
    }

    private void updateStatus(String apptId, String status, Map<String, Object> extra) {
        DocumentReference ref = db.collection("appointments").document(apptId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", FieldValue.serverTimestamp());
        if (extra != null) updates.putAll(extra);
        if ("completed".equals(status)) updates.put("completedAt", FieldValue.serverTimestamp());
        ref.update(updates).addOnSuccessListener(a -> {
            Toast.makeText(this, "Updated", Toast.LENGTH_SHORT).show();
            load();
        }).addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
