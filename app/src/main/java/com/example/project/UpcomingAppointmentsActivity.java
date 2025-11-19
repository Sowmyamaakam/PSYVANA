package com.example.project;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class UpcomingAppointmentsActivity extends AppCompatActivity {

    private RecyclerView upcomingRecycler;
    private ProgressBar progressBar;
    private TextView emptyText;
    private ImageButton backButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private final List<UpcomingAppointmentsAdapter.Item> data = new ArrayList<>();
    private UpcomingAppointmentsAdapter adapter;
    private final Set<String> prompted = new HashSet<>();
    private static final String CHANNEL_ID = "sessions_channel";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upcoming_appointments);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        upcomingRecycler = findViewById(R.id.upcomingRecycler);
        progressBar = findViewById(R.id.progressBar);
        emptyText = findViewById(R.id.emptyText);
        backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        upcomingRecycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UpcomingAppointmentsAdapter(new UpcomingAppointmentsAdapter.Listener() {
            @Override public void onStart(@NonNull UpcomingAppointmentsAdapter.Item item) {
                updateStatus(item.id, "in_progress");
            }
            @Override public void onComplete(@NonNull UpcomingAppointmentsAdapter.Item item) {
                updateStatus(item.id, "completed");
            }
            @Override public void onOpenChat(@NonNull UpcomingAppointmentsAdapter.Item item) {
                Intent chat = new Intent(UpcomingAppointmentsActivity.this, ChatActivity.class);
                chat.putExtra(ChatActivity.EXTRA_APPOINTMENT_ID, item.id);
                startActivity(chat);
            }
            @Override public void onJoinCall(@NonNull UpcomingAppointmentsAdapter.Item item) {
                Intent call = new Intent(UpcomingAppointmentsActivity.this, AudioCallActivity.class);
                call.putExtra("appointment_id", item.id);
                startActivity(call);
            }
            @Override public void onJoinVideo(@NonNull UpcomingAppointmentsAdapter.Item item) {
                Intent vid = new Intent(UpcomingAppointmentsActivity.this, VideoCallActivity.class);
                vid.putExtra("appointment_id", item.id);
                startActivity(vid);
            }
        });
        upcomingRecycler.setAdapter(adapter);

        loadUpcoming();
        startInProgressListener();
        createChannelIfNeeded();
    }

    private void loadUpcoming() {
        if (auth.getCurrentUser() == null) {
            showEmpty();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        String uid = auth.getCurrentUser().getUid();
        long now = System.currentTimeMillis();
        db.collection("appointments")
                .whereEqualTo("userId", uid)
                .whereIn("status", Arrays.asList("pending", "accepted"))
                .whereGreaterThanOrEqualTo("startAt", now)
                .orderBy("startAt", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        progressBar.setVisibility(View.GONE);
                        List<UpcomingAppointmentsAdapter.Item> list = new ArrayList<>();
                        if (task.isSuccessful() && task.getResult() != null) {
                            for (DocumentSnapshot d : task.getResult().getDocuments()) {
                                UpcomingAppointmentsAdapter.Item it = new UpcomingAppointmentsAdapter.Item();
                                it.id = d.getId();
                                it.status = d.getString("status");
                                it.modality = String.valueOf(d.get("modality"));
                                Object s = d.get("startAt");
                                it.startAt = s instanceof Number ? ((Number) s).longValue() : 0L;
                                Object e = d.get("endAt");
                                it.endAt = e instanceof Number ? ((Number) e).longValue() : 0L;
                                Map<String, Object> price = (Map<String, Object>) d.get("price");
                                if (price != null) {
                                    Object amt = price.get("amount");
                                    Object cur = price.get("currency");
                                    it.price = (cur != null ? cur.toString() : "INR") + " " + (amt instanceof Number ? String.valueOf(((Number) amt).intValue()) : "-");
                                } else it.price = "";
                                list.add(it);
                            }
                        }
                        data.clear();
                        data.addAll(list);
                        adapter.setItems(list);
                        toggleEmpty();
                    }
                });
    }

    private void startAcceptedCallListener() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("appointments")
                .whereEqualTo("userId", uid)
                .whereEqualTo("modality", "call")
                .whereEqualTo("status", "accepted")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null || value == null) return;
                        long now = System.currentTimeMillis();
                        for (DocumentSnapshot d : value.getDocuments()) {
                            String id = d.getId();
                            if (prompted.contains(id)) continue;
                            long startAt = d.get("startAt") instanceof Number ? ((Number) d.get("startAt")).longValue() : 0L;
                            long endAt = d.get("endAt") instanceof Number ? ((Number) d.get("endAt")).longValue() : Long.MAX_VALUE;
                            if (now >= startAt && now <= endAt) {
                                prompted.add(id);
                                String doctorId = String.valueOf(d.get("doctorId"));
                                promptCall(doctorId);
                            }
                        }
                    }
                });
    }

    private void startInProgressListener() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("appointments")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "in_progress")
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    long now = System.currentTimeMillis();
                    for (DocumentSnapshot d : value.getDocuments()) {
                        String id = d.getId();
                        if (prompted.contains(id)) continue;
                        long startAt = d.get("startAt") instanceof Number ? ((Number) d.get("startAt")).longValue() : 0L;
                        long endAt = d.get("endAt") instanceof Number ? ((Number) d.get("endAt")).longValue() : Long.MAX_VALUE;
                        if (now < startAt || now > endAt) continue;
                        String modality = String.valueOf(d.get("modality"));
                        prompted.add(id);
                        if ("video".equalsIgnoreCase(modality)) {
                            // Local notification
                            Intent vidIntent = new Intent(UpcomingAppointmentsActivity.this, VideoCallActivity.class);
                            vidIntent.putExtra("appointment_id", id);
                            vidIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            postLocalNotification("Doctor joined the video session", "Tap to join now.", vidIntent);
                            new AlertDialog.Builder(this)
                                    .setTitle("Doctor joined the session")
                                    .setMessage("Tap Join Video to enter the call.")
                                    .setPositiveButton("Join Video", (dlg, w) -> {
                                        Intent vid = new Intent(UpcomingAppointmentsActivity.this, VideoCallActivity.class);
                                        vid.putExtra("appointment_id", id);
                                        startActivity(vid);
                                    })
                                    .setNegativeButton("Later", null)
                                    .show();
                        } else if ("call".equalsIgnoreCase(modality) || "phone".equalsIgnoreCase(modality)) {
                            Intent callIntent = new Intent(UpcomingAppointmentsActivity.this, AudioCallActivity.class);
                            callIntent.putExtra("appointment_id", id);
                            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            postLocalNotification("Doctor joined the audio session", "Tap to join now.", callIntent);
                            new AlertDialog.Builder(this)
                                    .setTitle("Doctor joined the session")
                                    .setMessage("Tap Join Call to enter the call.")
                                    .setPositiveButton("Join Call", (dlg, w) -> {
                                        Intent call = new Intent(UpcomingAppointmentsActivity.this, AudioCallActivity.class);
                                        call.putExtra("appointment_id", id);
                                        startActivity(call);
                                    })
                                    .setNegativeButton("Later", null)
                                    .show();
                        } else if ("text".equalsIgnoreCase(modality) || "chat".equalsIgnoreCase(modality)) {
                            Intent chatIntent = new Intent(UpcomingAppointmentsActivity.this, ChatActivity.class);
                            chatIntent.putExtra(ChatActivity.EXTRA_APPOINTMENT_ID, id);
                            chatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            postLocalNotification("Doctor opened chat", "Tap to start messaging.", chatIntent);
                            new AlertDialog.Builder(this)
                                    .setTitle("Doctor opened chat")
                                    .setMessage("Tap Open Chat to start messaging.")
                                    .setPositiveButton("Open Chat", (dlg, w) -> {
                                        Intent chat = new Intent(UpcomingAppointmentsActivity.this, ChatActivity.class);
                                        chat.putExtra(ChatActivity.EXTRA_APPOINTMENT_ID, id);
                                        startActivity(chat);
                                    })
                                    .setNegativeButton("Later", null)
                                    .show();
                        }
                    }
                });
    }

    private void createChannelIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Sessions", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for session start prompts");
            nm.createNotificationChannel(channel);
        }
    }

    private void postLocalNotification(String title, String body, Intent tapIntent) {
        PendingIntent pi = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder b = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi);
        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), b.build());
    }

    private void promptCall(String doctorId) {
        db.collection("users").document(doctorId).get().addOnSuccessListener(doc -> {
            String phone = doc.getString("phone");
            if (phone == null || phone.trim().isEmpty()) return;
            new AlertDialog.Builder(this)
                    .setTitle("Doctor accepted")
                    .setMessage("Start the call now?\n" + phone)
                    .setPositiveButton("Call", (dlg, w) -> {
                        Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                        startActivity(dial);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void toggleEmpty() {
        boolean isEmpty = data.isEmpty();
        emptyText.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        upcomingRecycler.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void showEmpty() {
        progressBar.setVisibility(View.GONE);
        data.clear();
        adapter.setItems(new ArrayList<>());
        toggleEmpty();
    }

    private void updateStatus(String apptId, String status) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        if ("completed".equals(status)) {
            updates.put("completedAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
        }
        db.collection("appointments").document(apptId)
                .update(updates)
                .addOnSuccessListener(a -> loadUpcoming())
                .addOnFailureListener(e -> {});
    }
}
