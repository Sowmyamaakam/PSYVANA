package com.example.project;

import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class DoctorNotesActivity extends AppCompatActivity {
    private ImageButton backButton;
    private EditText notesEdit;
    private Button saveButton;
    private ProgressBar progressBar;

    private String appointmentId;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_notes);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        db = FirebaseFirestore.getInstance();
        appointmentId = getIntent().getStringExtra("appointment_id");

        backButton = findViewById(R.id.backButton);
        notesEdit = findViewById(R.id.notesEdit);
        saveButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar);

        backButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveNotesAndComplete());
    }

    private void saveNotesAndComplete() {
        final String notes = notesEdit.getText().toString().trim();
        if (TextUtils.isEmpty(appointmentId)) { Toast.makeText(this, "Missing appointment", Toast.LENGTH_LONG).show(); return; }

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        // Generate a simple PDF locally (client-side)
        String pdfPath = null;
        try {
            pdfPath = generatePdf(notes);
        } catch (IOException e) {
            // continue without PDF
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("notes", notes);
        Map<String, Object> pdf = new HashMap<>();
        if (pdfPath != null) {
            // In a real app, upload to Cloud Storage and store URL; we store local path as a placeholder
            pdf.put("doctorLocalPath", pdfPath);
            pdf.put("userLocalPath", pdfPath);
        }
        updates.put("pdf", pdf);
        updates.put("status", "completed");
        updates.put("completedAt", FieldValue.serverTimestamp());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("appointments").document(appointmentId).update(updates)
                .addOnSuccessListener(a -> {
                    progressBar.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    Toast.makeText(this, "Saved & completed", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    saveButton.setEnabled(true);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String generatePdf(String notes) throws IOException {
        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size in points
        PdfDocument.Page page = doc.startPage(pageInfo);

        Paint paint = new Paint();
        paint.setTextSize(16);

        int x = 40; int y = 60;
        page.getCanvas().drawText("Appointment Summary", x, y, paint);
        y += 30;
        paint.setTextSize(12);
        String dateStr = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
        page.getCanvas().drawText("Generated: " + dateStr, x, y, paint);
        y += 30;

        page.getCanvas().drawText("Doctor Notes:", x, y, paint);
        y += 20;
        for (String line : wrapText(notes, 80)) {
            page.getCanvas().drawText(line, x, y, paint);
            y += 18;
        }

        doc.finishPage(page);

        File out = new File(getCacheDir(), "appointment_" + appointmentId + ".pdf");
        FileOutputStream fos = new FileOutputStream(out);
        doc.writeTo(fos);
        fos.close();
        doc.close();
        return out.getAbsolutePath();
    }

    private java.util.List<String> wrapText(String text, int maxChars) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null) return lines;
        String[] words = text.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String w : words) {
            if (line.length() + w.length() + 1 > maxChars) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(w);
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }
}
