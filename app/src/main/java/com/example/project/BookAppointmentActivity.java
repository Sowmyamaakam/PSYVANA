package com.example.project;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.content.Intent;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class BookAppointmentActivity extends AppCompatActivity {

    private ImageButton backButton;
    private TextView doctorNameText, modalityText, priceText, appointmentStatusText;
    private EditText dateEdit, durationEdit;
    private Spinner timeSpinner;
    private Button bookButton;
    private Button joinButton;
    private ProgressBar progressBar;
    private View statusRow;

    private String doctorId;
    private String modality; // video|call|text
    private String appointmentId; // if viewing existing appointment
    private String meetingJoinUrl;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private Calendar selectedCal = Calendar.getInstance();
    private Map<String, Object> weeklyAvailability = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_appointment);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        doctorId = getIntent().getStringExtra("doctor_id");
        modality = getIntent().getStringExtra("modality");
        appointmentId = getIntent().getStringExtra("appointment_id");
        if (TextUtils.isEmpty(modality)) modality = "video";

        backButton = findViewById(R.id.backButton);
        doctorNameText = findViewById(R.id.doctorNameText);
        modalityText = findViewById(R.id.modalityText);
        priceText = findViewById(R.id.priceText);
        dateEdit = findViewById(R.id.dateEdit);
        timeSpinner = findViewById(R.id.timeSpinner);
        durationEdit = findViewById(R.id.durationEdit);
        bookButton = findViewById(R.id.bookButton);
        joinButton = findViewById(R.id.joinButton);
        progressBar = findViewById(R.id.progressBar);
        statusRow = findViewById(R.id.statusRow);
        appointmentStatusText = findViewById(R.id.appointmentStatusText);

        backButton.setOnClickListener(v -> finish());

        if (!TextUtils.isEmpty(appointmentId)) {
            // View mode for existing appointment
            enterViewMode();
            loadExistingAppointment(appointmentId);
        } else {
            modalityText.setText(capitalize(modality));
            durationEdit.setText("45");

            // Pickers
            dateEdit.setOnClickListener(v -> openDatePicker());

            bookButton.setOnClickListener(v -> createAppointment());

            loadDoctor();
        }
    }

    private void loadDoctor() {
        db.collection("users").document(doctorId).get().addOnSuccessListener(doc -> {
            String name = doc.getString("name");
            doctorNameText.setText(name != null ? name : "Doctor");
            // price
            Object rateObj = doc.get("rate");
            String currency = "INR";
            String amount = "-";
            if (rateObj instanceof Map) {
                Object price = ((Map<?,?>) rateObj).get(modality);
                Object cur = ((Map<?,?>) rateObj).get("currency");
                if (cur != null) currency = cur.toString();
                if (price instanceof Number) amount = String.valueOf(((Number) price).intValue());
            }
            priceText.setText(currency + " " + amount);
            // load weekly availability
            try {
                Object availObj = doc.get("availability");
                if (availObj instanceof Map) {
                    Object weekly = ((Map<?,?>) availObj).get("weekly");
                    if (weekly instanceof Map) {
                        //noinspection unchecked
                        weeklyAvailability = (Map<String, Object>) weekly;
                    }
                }
            } catch (Exception ignore) {}
            // If a date is already selected, populate time options
            if (!TextUtils.isEmpty(dateEdit.getText().toString().trim())) {
                populateTimeOptionsForSelectedDate();
            }
        });
    }

    private void openDatePicker() {
        final Calendar now = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this, (DatePicker view, int year, int month, int dayOfMonth) -> {
            selectedCal.set(Calendar.YEAR, year);
            selectedCal.set(Calendar.MONTH, month);
            selectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            dateEdit.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCal.getTime()));
            populateTimeOptionsForSelectedDate();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        dp.show();
    }

    private void enterViewMode() {
        if (statusRow != null) statusRow.setVisibility(View.VISIBLE);
        dateEdit.setEnabled(false);
        if (timeSpinner != null) timeSpinner.setEnabled(false);
        durationEdit.setEnabled(false);
        bookButton.setVisibility(View.GONE);
        joinButton.setVisibility(View.GONE);
    }

    private void loadExistingAppointment(String id) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("appointments").document(id).get().addOnSuccessListener((DocumentSnapshot snap) -> {
            progressBar.setVisibility(View.GONE);
            if (snap.exists()) {
                String status = snap.getString("status");
                appointmentStatusText.setText(!TextUtils.isEmpty(status) ? capitalize(status) : "-");

                Object d = snap.get("doctorId");
                if (d != null) doctorId = d.toString();
                Object m = snap.get("modality");
                if (m != null) modality = m.toString();
                modalityText.setText(capitalize(modality));

                Object priceObj = snap.get("price");
                if (priceObj instanceof Map) {
                    Object cur = ((Map<?,?>) priceObj).get("currency");
                    Object amt = ((Map<?,?>) priceObj).get("amount");
                    String currency = cur != null ? cur.toString() : "INR";
                    String amount = amt instanceof Number ? String.valueOf(((Number) amt).intValue()) : "-";
                    priceText.setText(currency + " " + amount);
                }

                Object startAt = snap.get("startAt");
                if (startAt instanceof Number) {
                    long millis = ((Number) startAt).longValue();
                    selectedCal.setTimeInMillis(millis);
                    dateEdit.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedCal.getTime()));
                    // Populate spinner with the exact time from appointment
                    ArrayList<String> single = new ArrayList<>();
                    single.add(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(selectedCal.getTime()));
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, single);
                    timeSpinner.setAdapter(adapter);
                    timeSpinner.setSelection(0);
                }

                // Meeting link (for video)
                meetingJoinUrl = null;
                Object meetingObj = snap.get("meeting");
                if (meetingObj instanceof Map) {
                    Object link = ((Map<?,?>) meetingObj).get("joinUrl");
                    if (link != null) meetingJoinUrl = link.toString();
                }
                // Show Join when accepted video with link and within time window
                long now = System.currentTimeMillis();
                long startMs = selectedCal.getTimeInMillis();
                long windowStart = startMs - 15L*60L*1000L;
                long windowEnd = startMs + 60L*60L*1000L;
                boolean inWindow = now >= windowStart && now <= windowEnd;
                boolean showJoin = "accepted".equalsIgnoreCase(status) && "video".equalsIgnoreCase(modality) && !TextUtils.isEmpty(meetingJoinUrl) && inWindow;
                if (showJoin) {
                    joinButton.setVisibility(View.VISIBLE);
                    joinButton.setOnClickListener(v -> {
                        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(meetingJoinUrl))); }
                        catch (Exception e) { Toast.makeText(this, "Cannot open meeting link", Toast.LENGTH_LONG).show(); }
                    });
                } else {
                    joinButton.setVisibility(View.GONE);
                }

                loadDoctor();
            } else {
                Toast.makeText(this, "Appointment not found", Toast.LENGTH_LONG).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void createAppointment() {
        if (auth.getCurrentUser() == null) { Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show(); return; }
        String dateStr = dateEdit.getText().toString().trim();
        String timeStr = timeSpinner != null && timeSpinner.getSelectedItem() != null ? timeSpinner.getSelectedItem().toString() : "";
        String durationStr = durationEdit.getText().toString().trim();
        if (TextUtils.isEmpty(dateStr) || TextUtils.isEmpty(timeStr) || "No slots available".equalsIgnoreCase(timeStr)) { Toast.makeText(this, "Pick date and an available time", Toast.LENGTH_SHORT).show(); return; }
        int duration = TextUtils.isEmpty(durationStr) ? 45 : Integer.parseInt(durationStr);

        progressBar.setVisibility(View.VISIBLE);
        bookButton.setEnabled(false);

        String uid = auth.getCurrentUser().getUid();
        // Apply selected time (HH:mm) to selectedCal
        try {
            int hour = Integer.parseInt(timeStr.substring(0,2));
            int min = Integer.parseInt(timeStr.substring(3,5));
            selectedCal.set(Calendar.HOUR_OF_DAY, hour);
            selectedCal.set(Calendar.MINUTE, min);
        } catch (Exception ignore) {}
        long startAtMillis = selectedCal.getTimeInMillis();
        long endAtMillis = startAtMillis + duration * 60L * 1000L;

        // Prepare basic appointment
        Map<String, Object> appt = new HashMap<>();
        appt.put("userId", uid);
        appt.put("doctorId", doctorId);
        appt.put("status", "pending");
        appt.put("modality", modality);
        appt.put("startAt", startAtMillis);
        appt.put("endAt", endAtMillis);
        Map<String, Object> price = new HashMap<>();
        price.put("currency", priceText.getText().toString().split(" ")[0]);
        try {
            price.put("amount", Integer.parseInt(priceText.getText().toString().split(" ")[1]));
        } catch (Exception e) { price.put("amount", 0); }
        appt.put("price", price);
        appt.put("createdAt", FieldValue.serverTimestamp());
        appt.put("updatedAt", FieldValue.serverTimestamp());

        db.collection("appointments").add(appt).addOnSuccessListener(ref -> {
            progressBar.setVisibility(View.GONE);
            bookButton.setEnabled(true);
            Toast.makeText(this, "Appointment requested", Toast.LENGTH_SHORT).show();
            // Schedule local reminder 4 hours before
            scheduleReminder(ref.getId(), startAtMillis - 4L * 60L * 60L * 1000L);
            finish();
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            bookButton.setEnabled(true);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void populateTimeOptionsForSelectedDate() {
        ArrayList<String> times = new ArrayList<>();
        try {
            if (weeklyAvailability != null) {
                // Map Java Calendar day to our keys: 1=Sun..7=Sat
                String[] keys = {"sun","mon","tue","wed","thu","fri","sat"};
                int dow = selectedCal.get(Calendar.DAY_OF_WEEK);
                String key = keys[dow - 1];
                Object v = weeklyAvailability.get(key);
                if (v instanceof String) {
                    String range = (String) v; // e.g., 09:00-17:30
                    String[] parts = range.split("-");
                    if (parts.length == 2) {
                        String start = parts[0].trim();
                        String end = parts[1].trim();
                        int sh = Integer.parseInt(start.substring(0, 2));
                        int sm = Integer.parseInt(start.substring(3, 5));
                        int eh = Integer.parseInt(end.substring(0, 2));
                        int em = Integer.parseInt(end.substring(3, 5));
                        Calendar cursor = (Calendar) selectedCal.clone();
                        cursor.set(Calendar.HOUR_OF_DAY, sh);
                        cursor.set(Calendar.MINUTE, sm);
                        Calendar endCal = (Calendar) selectedCal.clone();
                        endCal.set(Calendar.HOUR_OF_DAY, eh);
                        endCal.set(Calendar.MINUTE, em);
                        // 30-minute steps, inclusive of end time
                        while (!cursor.after(endCal)) {
                            times.add(String.format(Locale.getDefault(), "%02d:%02d",
                                    cursor.get(Calendar.HOUR_OF_DAY), cursor.get(Calendar.MINUTE)));
                            cursor.add(Calendar.MINUTE, 30);
                        }
                    }
                }
            }
        } catch (Exception ignore) {}

        if (times.isEmpty()) {
            times.add("No slots available");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, times);
        timeSpinner.setAdapter(adapter);
        timeSpinner.setSelection(0);
    }

    private void scheduleReminder(String apptId, long triggerAtMillis) {
        if (triggerAtMillis <= System.currentTimeMillis()) return; // too late
        try {
            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("appointment_id", apptId);
            PendingIntent pi = PendingIntent.getBroadcast(this, apptId.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            }
        } catch (Exception ignore) {}
    }

    private String capitalize(String s) {
        if (TextUtils.isEmpty(s)) return s;
        return s.substring(0,1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }
}
