package com.example.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AppointmentHistoryAdapter extends RecyclerView.Adapter<AppointmentHistoryAdapter.VH> {

    private final List<Map<String, Object>> items;
    private final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public AppointmentHistoryAdapter(List<Map<String, Object>> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment_history, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Map<String, Object> m = items.get(position);
        String modality = safeString(m.get("modality"));
        String status = safeString(m.get("status"));

        long startAt = asLong(m.get("startAt"));
        Date d = new Date(startAt);
        String dateStr = dateFmt.format(d);
        String timeStr = timeFmt.format(d);

        String priceStr = "";
        Object priceObj = m.get("price");
        if (priceObj instanceof Map) {
            Object cur = ((Map<?,?>) priceObj).get("currency");
            Object amt = ((Map<?,?>) priceObj).get("amount");
            String curS = cur != null ? cur.toString() : "INR";
            String amtS = amt != null ? String.valueOf(amt) : "-";
            priceStr = curS + " " + amtS;
        }

        h.titleText.setText("Consultation • " + capitalize(modality));
        h.subtitleText.setText(dateStr + " • " + timeStr + " • " + capitalize(modality));
        h.statusText.setText(capitalize(status));
        h.priceText.setText(priceStr);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView titleText, subtitleText, statusText, priceText;
        VH(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.titleText);
            subtitleText = itemView.findViewById(R.id.subtitleText);
            statusText = itemView.findViewById(R.id.statusText);
            priceText = itemView.findViewById(R.id.priceText);
        }
    }

    private static String safeString(Object o) {
        return o == null ? "" : o.toString();
    }

    private static long asLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0L; }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0,1).toUpperCase(Locale.getDefault()) + s.substring(1);
    }
}
