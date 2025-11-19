package com.example.project;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.jitsi.meet.sdk.JitsiMeetActivity;
import org.jitsi.meet.sdk.JitsiMeetConferenceOptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VideoCallActivity extends AppCompatActivity {

    private TextView titleText, sessionInfoText, endedText;
    private View doctorNotesContainer, userReportContainer;
    private EditText doctorNotesEditText;
    private Button uploadReportButton, downloadReportButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private String appointmentId;
    private String userId;
    private String doctorId;
    private String joinUrl; // from appointment.meeting.joinUrl
    private boolean isDoctor;
    private long startAt;
    private long endAt;
    private boolean launched = false;
    private ListenerRegistration reg;

    private final SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        titleText = findViewById(R.id.titleText);
        sessionInfoText = findViewById(R.id.sessionInfoText);
        endedText = findViewById(R.id.endedText);
        doctorNotesContainer = findViewById(R.id.doctorNotesContainer);
        userReportContainer = findViewById(R.id.userReportContainer);
        doctorNotesEditText = findViewById(R.id.doctorNotesEditText);
        uploadReportButton = findViewById(R.id.uploadReportButton);
        downloadReportButton = findViewById(R.id.downloadReportButton);

        appointmentId = getIntent().getStringExtra("appointment_id");
        if (TextUtils.isEmpty(appointmentId) || auth.getCurrentUser() == null) { finish(); return; }
        String myUid = auth.getCurrentUser().getUid();

        reg = db.collection("appointments").document(appointmentId).addSnapshotListener((snap, err) -> {
            if (err != null || snap == null || !snap.exists()) { finish(); return; }
            userId = String.valueOf(snap.get("userId"));
            doctorId = String.valueOf(snap.get("doctorId"));
            String status = String.valueOf(snap.get("status"));
            Object sObj = snap.get("startAt");
            Object eObj = snap.get("endAt");
            startAt = sObj instanceof Number ? ((Number) sObj).longValue() : 0L;
            endAt = eObj instanceof Number ? ((Number) eObj).longValue() : 0L;
            Map<String, Object> meeting = (Map<String, Object>) snap.get("meeting");
            if (meeting != null) joinUrl = (String) meeting.get("joinUrl");
            String reportUrl = snap.getString("reportUrl");

            isDoctor = myUid.equals(doctorId);
            titleText.setText("Video Consultation");
            sessionInfoText.setText("Time: " + fmt.format(new Date(startAt)));

            long now = System.currentTimeMillis();
            boolean inWindow = now >= startAt && (endAt == 0L || now <= endAt);

            // Doctor can join when accepted/in_progress; user only when in_progress
            boolean accepted = "accepted".equalsIgnoreCase(status);
            boolean inProgress = "in_progress".equalsIgnoreCase(status);
            boolean canJoin = (isDoctor && (accepted || inProgress) && inWindow) || (!isDoctor && inProgress && inWindow);

            if (canJoin && !launched) {
                launched = true;
                launchJitsi(joinUrl);
                scheduleAutoHangup();
            }

            boolean ended = !inWindow && endAt > 0L;
            endedText.setVisibility(ended ? View.VISIBLE : View.GONE);
            doctorNotesContainer.setVisibility(isDoctor && ended ? View.VISIBLE : View.GONE);
            userReportContainer.setVisibility(!isDoctor && !TextUtils.isEmpty(reportUrl) ? View.VISIBLE : View.GONE);
            if (!TextUtils.isEmpty(reportUrl)) {
                downloadReportButton.setOnClickListener(v -> {
                    try { startActivity(new android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(reportUrl))); } catch (Exception ignored) {}
                });
            }
        });

        uploadReportButton.setOnClickListener(v -> uploadDoctorNotesPdf());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reg != null) reg.remove();
    }

    private void launchJitsi(String url) {
        if (TextUtils.isEmpty(url)) return;
        try {
            Uri uri = Uri.parse(url);
            String room = uri.getLastPathSegment();
            String serverStr = uri.getScheme() + "://" + uri.getHost();
            URL serverURL = new URL(serverStr);
            JitsiMeetConferenceOptions.Builder b = new JitsiMeetConferenceOptions.Builder()
                    .setServerURL(serverURL)
                    .setRoom(room);
            // Disable features
            b.setFeatureFlag("welcomepage.enabled", false);
            b.setFeatureFlag("invite.enabled", false);
            b.setFeatureFlag("live-streaming.enabled", false);
            b.setFeatureFlag("recording.enabled", false);
            b.setFeatureFlag("chat.enabled", false);
            b.setFeatureFlag("meeting-name.enabled", false);
            JitsiMeetActivity.launch(this, b.build());
        } catch (MalformedURLException e) {
            // ignore
        }
    }

    private void scheduleAutoHangup() {
        if (endAt <= 0L) return;
        long now = System.currentTimeMillis();
        long delay = Math.max(0, endAt - now);
        new android.os.Handler(getMainLooper()).postDelayed(() -> {
            try {
                sendBroadcast(new android.content.Intent("org.jitsi.meet.HANG_UP"));
            } catch (Exception ignored) {}
            endedText.setVisibility(View.VISIBLE);
            doctorNotesContainer.setVisibility(isDoctor ? View.VISIBLE : View.GONE);
            // Optionally show a dialog
            android.app.AlertDialog.Builder dlg = new android.app.AlertDialog.Builder(this)
                    .setTitle("Session Complete")
                    .setMessage("The scheduled time has ended.")
                    .setPositiveButton("OK", null);
            dlg.show();
        }, delay);
    }

    private void uploadDoctorNotesPdf() {
        if (!isDoctor || TextUtils.isEmpty(appointmentId) || TextUtils.isEmpty(userId)) return;
        String notes = doctorNotesEditText != null ? doctorNotesEditText.getText().toString().trim() : "";
        if (TextUtils.isEmpty(notes)) return;
        // Build a simple PDF using Android PdfDocument
        android.graphics.pdf.PdfDocument pdf = new android.graphics.pdf.PdfDocument();
        android.graphics.pdf.PdfDocument.PageInfo pageInfo = new android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create();
        android.graphics.pdf.PdfDocument.Page page = pdf.startPage(pageInfo);
        android.graphics.Canvas canvas = page.getCanvas();
        android.graphics.Paint paint = new android.graphics.Paint();
        paint.setTextSize(14f);
        int x = 40, y = 60;
        canvas.drawText("Video Consultation Notes", x, y, paint); y += 24;
        canvas.drawText("Appointment: " + appointmentId, x, y, paint); y += 20;
        canvas.drawText("User: " + userId, x, y, paint); y += 20;
        canvas.drawText("Doctor: " + doctorId, x, y, paint); y += 20;
        canvas.drawText("Start: " + startAt, x, y, paint); y += 20;
        canvas.drawText("End: " + endAt, x, y, paint); y += 28;
        paint.setTextSize(12f);
        for (String line : notes.split("\n")) { canvas.drawText(line, x, y, paint); y += 18; if (y > 800) break; }
        pdf.finishPage(page);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try { pdf.writeTo(bos); } catch (IOException e) { pdf.close(); return; }
        pdf.close();
        byte[] bytes = bos.toByteArray();

        StorageReference ref = FirebaseStorage.getInstance().getReference()
                .child("reports").child(userId).child(appointmentId + ".pdf");
        ref.putBytes(bytes).continueWithTask(t -> ref.getDownloadUrl())
                .addOnSuccessListener(uri -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("reportUrl", uri.toString());
                    updates.put("reportReadyAt", FieldValue.serverTimestamp());
                    db.collection("appointments").document(appointmentId).update(updates);
                });
    }
}
