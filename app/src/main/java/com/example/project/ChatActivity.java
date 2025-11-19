package com.example.project;

import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_DOCTOR_ID = "doctor_id";
    public static final String EXTRA_USER_ID = "user_id"; // optional if doctor opens chat to a user
    public static final String EXTRA_APPOINTMENT_ID = "appointment_id";

    private ImageButton backButton, sendButton;
    private TextView chatTitle;
    private EditText messageInput;
    private RecyclerView messagesRecycler;
    private TextView sessionEndedText;
    private View doctorNotesContainer;
    private EditText doctorNotesEditText;
    private Button uploadReportButton;
    private View userReportContainer;
    private Button downloadReportButton;

    private ChatMessageAdapter adapter;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String myUid;
    private String otherUid;
    private String chatId;
    private String appointmentId;
    private String apptUserId;
    private String apptDoctorId;
    private boolean isDoctor = false;
    private long startAtMs = 0L;
    private long endAtMs = 0L;
    private boolean chatEnabled = true;

    private ListenerRegistration messageReg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        backButton = findViewById(R.id.backButton);
        sendButton = findViewById(R.id.sendButton);
        chatTitle = findViewById(R.id.chatTitle);
        messageInput = findViewById(R.id.messageInput);
        messagesRecycler = findViewById(R.id.messagesRecycler);
        sessionEndedText = findViewById(R.id.sessionEndedText);
        doctorNotesContainer = findViewById(R.id.doctorNotesContainer);
        doctorNotesEditText = findViewById(R.id.doctorNotesEditText);
        uploadReportButton = findViewById(R.id.uploadReportButton);
        userReportContainer = findViewById(R.id.userReportContainer);
        downloadReportButton = findViewById(R.id.downloadReportButton);

        backButton.setOnClickListener(v -> finish());

        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        messagesRecycler.setLayoutManager(lm);
        adapter = new ChatMessageAdapter();
        messagesRecycler.setAdapter(adapter);

        myUid = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        String doctorId = getIntent().getStringExtra(EXTRA_DOCTOR_ID);
        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        appointmentId = getIntent().getStringExtra(EXTRA_APPOINTMENT_ID);

        // Determine counterpart and optionally gate by appointment
        if (TextUtils.isEmpty(myUid)) { finish(); return; }
        if (!TextUtils.isEmpty(appointmentId)) {
            // Listen to appointment for gating and identities
            db.collection("appointments").document(appointmentId).addSnapshotListener((snap, err) -> {
                if (err != null || snap == null || !snap.exists()) { disableChat("Appointment not found"); return; }
                apptUserId = String.valueOf(snap.get("userId"));
                apptDoctorId = String.valueOf(snap.get("doctorId"));
                String status = String.valueOf(snap.get("status"));
                startAtMs = snap.get("startAt") instanceof Number ? ((Number) snap.get("startAt")).longValue() : 0L;
                endAtMs = snap.get("endAt") instanceof Number ? ((Number) snap.get("endAt")).longValue() : 0L;
                String reportUrl = snap.getString("reportUrl");

                // Resolve other party based on who I am
                if (myUid.equals(apptUserId)) { otherUid = apptDoctorId; isDoctor = false; }
                else { otherUid = apptUserId; isDoctor = true; }
                // Appointment-scoped chat room id
                chatId = appointmentId;

                // Title
                db.collection("users").document(otherUid).get().addOnSuccessListener(u -> {
                    Object name = u.get("name");
                    if (name != null) chatTitle.setText(String.valueOf(name));
                });

                // Gate: only accepted and within window
                boolean accepted = "accepted".equalsIgnoreCase(status) || "in_progress".equalsIgnoreCase(status);
                long now = System.currentTimeMillis();
                boolean inWindow = now >= startAtMs && (endAtMs == 0L || now <= endAtMs);
                setChatEnabled(accepted && inWindow);
                sessionEndedText.setVisibility(!inWindow && endAtMs > 0 ? View.VISIBLE : View.GONE);
                // Doctor notes visible to doctor when session ended
                doctorNotesContainer.setVisibility(isDoctor && !inWindow && endAtMs > 0 ? View.VISIBLE : View.GONE);
                // User report UI visible when reportUrl present
                userReportContainer.setVisibility(!isDoctor && !TextUtils.isEmpty(reportUrl) ? View.VISIBLE : View.GONE);
                if (!TextUtils.isEmpty(reportUrl)) {
                    downloadReportButton.setOnClickListener(v -> {
                        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(reportUrl))); } catch (Exception ignored) {}
                    });
                }

                // Attach messages if not already
                if (messageReg == null) attachMessagesListener();
            });
        } else {
            if (!TextUtils.isEmpty(doctorId)) otherUid = doctorId; else if (!TextUtils.isEmpty(userId)) otherUid = userId; else { finish(); return; }
            chatId = buildChatId(myUid, otherUid);
            // Title
            db.collection("users").document(otherUid).get().addOnSuccessListener(snap -> {
                Object name = snap.get("name");
                if (name != null) chatTitle.setText(String.valueOf(name));
            });
            attachMessagesListener();
        }

        sendButton.setOnClickListener(v -> doSend());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageReg != null) messageReg.remove();
    }

    private void attachMessagesListener() {
        DocumentReference chatRef = db.collection("chats").document(chatId);
        messageReg = chatRef.collection("messages")
                .orderBy("createdAtMs", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) return;
                        List<Map<String, Object>> list = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot d : value) {
                                Map<String, Object> m = new HashMap<>();
                                m.put("text", d.get("text"));
                                m.put("senderId", d.get("senderId"));
                                long ms = 0L;
                                Object msObj = d.get("createdAtMs");
                                if (msObj instanceof Number) {
                                    ms = ((Number) msObj).longValue();
                                } else {
                                    Object ts = d.get("createdAt");
                                    if (ts instanceof Timestamp) ms = ((Timestamp) ts).toDate().getTime();
                                    else if (ts instanceof Number) ms = ((Number) ts).longValue();
                                }
                                m.put("createdAt", ms);
                                list.add(m);
                            }
                        }
                        adapter.addOrUpdateAll(list);
                        messagesRecycler.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
                    }
                });
    }

    private void doSend() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty() || myUid == null || !chatEnabled) return;
        messageInput.setText("");

        DocumentReference chatRef = db.collection("chats").document(chatId);

        // Ensure chat doc exists
        Map<String, Object> chat = new HashMap<>();
        List<String> users = new ArrayList<>();
        users.add(myUid);
        users.add(otherUid);
        chat.put("users", users);
        chat.put("lastMessage", text);
        chat.put("lastAt", FieldValue.serverTimestamp());
        chat.put("lastAtMs", System.currentTimeMillis());
        chatRef.set(chat, com.google.firebase.firestore.SetOptions.merge());

        Map<String, Object> msg = new HashMap<>();
        msg.put("text", text);
        msg.put("senderId", myUid);
        msg.put("createdAt", FieldValue.serverTimestamp());
        msg.put("createdAtMs", System.currentTimeMillis());
        chatRef.collection("messages").add(msg);
    }

    private void setChatEnabled(boolean enabled) {
        this.chatEnabled = enabled;
        if (messageInput != null) messageInput.setEnabled(enabled);
        if (sendButton != null) sendButton.setEnabled(enabled);
        if (!enabled && chatTitle != null) {
            CharSequence t = chatTitle.getText();
            String s = t != null ? t.toString() : "Chat";
            if (!s.endsWith(" (chat disabled)")) chatTitle.setText(String.format(Locale.getDefault(), "%s (chat disabled)", s));
        }
    }

    private void disableChat(String reason) {
        setChatEnabled(false);
    }

    private String buildChatId(String u1, String u2) { if (u1.compareTo(u2) < 0) return u1 + "_" + u2; else return u2 + "_" + u1; }

    @Override
    protected void onStart() {
        super.onStart();
        if (uploadReportButton != null) {
            uploadReportButton.setOnClickListener(v -> uploadDoctorNotesPdf());
        }
    }

    private void uploadDoctorNotesPdf() {
        if (!isDoctor || TextUtils.isEmpty(appointmentId) || TextUtils.isEmpty(apptUserId)) return;
        String notes = doctorNotesEditText != null ? doctorNotesEditText.getText().toString().trim() : "";
        if (TextUtils.isEmpty(notes)) return;
        // Build a simple PDF using Android PdfDocument
        PdfDocument pdf = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 @ 72dpi
        PdfDocument.Page page = pdf.startPage(pageInfo);
        android.graphics.Canvas canvas = page.getCanvas();
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setTextSize(14f);
        int x = 40, y = 60;
        canvas.drawText("Session Report", x, y, paint); y += 24;
        canvas.drawText("Appointment: " + appointmentId, x, y, paint); y += 20;
        canvas.drawText("User: " + apptUserId, x, y, paint); y += 20;
        canvas.drawText("Doctor: " + apptDoctorId, x, y, paint); y += 20;
        canvas.drawText("Start: " + startAtMs, x, y, paint); y += 20;
        canvas.drawText("End: " + endAtMs, x, y, paint); y += 28;
        paint.setTextSize(12f);
        for (String line : notes.split("\n")) { canvas.drawText(line, x, y, paint); y += 18; if (y > 800) break; }
        pdf.finishPage(page);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try { pdf.writeTo(bos); } catch (IOException e) { pdf.close(); return; }
        pdf.close();
        byte[] bytes = bos.toByteArray();

        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("reports").child(apptUserId).child(appointmentId + ".pdf");
        ref.putBytes(bytes).continueWithTask(t -> ref.getDownloadUrl())
                .addOnSuccessListener(uri -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("reportUrl", uri.toString());
                    updates.put("reportReadyAt", FieldValue.serverTimestamp());
                    FirebaseFirestore.getInstance().collection("appointments").document(appointmentId)
                            .update(updates);
                });
    }
}
