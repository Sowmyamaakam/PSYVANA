package com.example.project;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class DoctorProfileActivity extends AppCompatActivity {

    private Switch videoSwitch, callSwitch, textSwitch;
    private EditText rateVideoEdit, rateCallEdit, rateTextEdit, currencyEdit, timezoneEdit;
    private EditText monStart, monEnd, tueStart, tueEnd, wedStart, wedEnd, thuStart, thuEnd, friStart, friEnd, satStart, satEnd, sunStart, sunEnd;
    private Button saveButton;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_profile);
        if (getSupportActionBar() != null) getSupportActionBar().hide();

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        videoSwitch = findViewById(R.id.videoSwitch);
        callSwitch = findViewById(R.id.callSwitch);
        textSwitch = findViewById(R.id.textSwitch);
        rateVideoEdit = findViewById(R.id.rateVideoEdit);
        rateCallEdit = findViewById(R.id.rateCallEdit);
        rateTextEdit = findViewById(R.id.rateTextEdit);
        currencyEdit = findViewById(R.id.currencyEdit);
        timezoneEdit = findViewById(R.id.timezoneEdit);
        monStart = findViewById(R.id.monStart); monEnd = findViewById(R.id.monEnd);
        tueStart = findViewById(R.id.tueStart); tueEnd = findViewById(R.id.tueEnd);
        wedStart = findViewById(R.id.wedStart); wedEnd = findViewById(R.id.wedEnd);
        thuStart = findViewById(R.id.thuStart); thuEnd = findViewById(R.id.thuEnd);
        friStart = findViewById(R.id.friStart); friEnd = findViewById(R.id.friEnd);
        satStart = findViewById(R.id.satStart); satEnd = findViewById(R.id.satEnd);
        sunStart = findViewById(R.id.sunStart); sunEnd = findViewById(R.id.sunEnd);
        saveButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar);
        View backButton = findViewById(R.id.backButton);
        if (backButton != null) backButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> saveProfile());
        loadProfile();
    }

    private void loadProfile() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) return;
            Boolean v = doc.getBoolean("modalities.video");
            Boolean c = doc.getBoolean("modalities.call");
            Boolean t = doc.getBoolean("modalities.text");
            if (v != null) videoSwitch.setChecked(v);
            if (c != null) callSwitch.setChecked(c);
            if (t != null) textSwitch.setChecked(t);
            Map<String, Object> rate = (Map<String, Object>) doc.get("rate");
            if (rate != null) {
                Object rv = rate.get("video"); if (rv != null) rateVideoEdit.setText(String.valueOf(rv));
                Object rc = rate.get("call"); if (rc != null) rateCallEdit.setText(String.valueOf(rc));
                Object rt = rate.get("text"); if (rt != null) rateTextEdit.setText(String.valueOf(rt));
                Object cur = rate.get("currency"); if (cur != null) currencyEdit.setText(String.valueOf(cur));
            }
            Object tz = doc.get("timezone"); if (tz != null) timezoneEdit.setText(String.valueOf(tz));
            Map<String, Object> availability = (Map<String, Object>) doc.get("availability.weekly");
            if (availability != null) {
                setIfPresent(availability, "mon", monStart, monEnd);
                setIfPresent(availability, "tue", tueStart, tueEnd);
                setIfPresent(availability, "wed", wedStart, wedEnd);
                setIfPresent(availability, "thu", thuStart, thuEnd);
                setIfPresent(availability, "fri", friStart, friEnd);
                setIfPresent(availability, "sat", satStart, satEnd);
                setIfPresent(availability, "sun", sunStart, sunEnd);
            }
        });
    }

    private void setIfPresent(Map<String, Object> weekly, String key, EditText start, EditText end) {
        Object v = weekly.get(key);
        if (v instanceof String) {
            String val = (String) v;
            String[] parts = val.split("-");
            if (parts.length == 2) { start.setText(parts[0]); end.setText(parts[1]); }
        }
    }

    private void saveProfile() {
        if (auth.getCurrentUser() == null) { Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show(); return; }
        String uid = auth.getCurrentUser().getUid();

        boolean video = videoSwitch.isChecked();
        boolean call = callSwitch.isChecked();
        boolean text = textSwitch.isChecked();

        String rateVideo = rateVideoEdit.getText().toString().trim();
        String rateCall = rateCallEdit.getText().toString().trim();
        String rateText = rateTextEdit.getText().toString().trim();
        String currency = currencyEdit.getText().toString().trim();
        String tz = timezoneEdit.getText().toString().trim();

        Map<String, Object> modalities = new HashMap<>();
        modalities.put("video", video);
        modalities.put("call", call);
        modalities.put("text", text);

        Map<String, Object> rate = new HashMap<>();
        rate.put("video", TextUtils.isEmpty(rateVideo) ? 0 : Integer.parseInt(rateVideo));
        rate.put("call", TextUtils.isEmpty(rateCall) ? 0 : Integer.parseInt(rateCall));
        rate.put("text", TextUtils.isEmpty(rateText) ? 0 : Integer.parseInt(rateText));
        rate.put("currency", TextUtils.isEmpty(currency) ? "INR" : currency);

        Map<String, Object> weekly = new HashMap<>();
        putRange(weekly, "mon", monStart, monEnd);
        putRange(weekly, "tue", tueStart, tueEnd);
        putRange(weekly, "wed", wedStart, wedEnd);
        putRange(weekly, "thu", thuStart, thuEnd);
        putRange(weekly, "fri", friStart, friEnd);
        putRange(weekly, "sat", satStart, satEnd);
        putRange(weekly, "sun", sunStart, sunEnd);

        Map<String, Object> update = new HashMap<>();
        update.put("modalities", modalities);
        update.put("rate", rate);
        update.put("timezone", TextUtils.isEmpty(tz) ? "Asia/Kolkata" : tz);
        Map<String, Object> availWrap = new HashMap<>();
        availWrap.put("weekly", weekly);
        update.put("availability", availWrap);

        progressBar.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);

        DocumentReference doc = db.collection("users").document(uid);
        doc.set(update, SetOptions.merge()).addOnSuccessListener(a -> {
            progressBar.setVisibility(View.GONE);
            saveButton.setEnabled(true);
            Toast.makeText(this, "Profile saved", Toast.LENGTH_SHORT).show();
            Intent i = new Intent(DoctorProfileActivity.this, DoctorAppointmentsActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }).addOnFailureListener(e -> {
            progressBar.setVisibility(View.GONE);
            saveButton.setEnabled(true);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void putRange(Map<String, Object> map, String key, EditText start, EditText end) {
        String s = start.getText().toString().trim();
        String e = end.getText().toString().trim();
        if (!TextUtils.isEmpty(s) && !TextUtils.isEmpty(e)) {
            map.put(key, s + "-" + e);
        }
    }
}
