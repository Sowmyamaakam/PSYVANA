package com.example.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DoctorAppointmentsAdapter extends RecyclerView.Adapter<DoctorAppointmentsAdapter.VH> {

    interface Listener {
        void onAccept(@NonNull Item item);
        void onDecline(@NonNull Item item);
        void onStart(@NonNull Item item);
        void onComplete(@NonNull Item item);
        void onOpenChat(@NonNull Item item);
        void onJoinCall(@NonNull Item item);
        void onJoinVideo(@NonNull Item item);
    }

    static class Item {
        String id;
        String userId;
        String status;
        String modality;
        long startAt;
        long endAt;
        String price;
    }

    private final List<Item> items = new ArrayList<>();
    private final Listener listener;
    private final SimpleDateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault());

    DoctorAppointmentsAdapter(Listener listener) { this.listener = listener; }

    void setItems(List<Item> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appointment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Item it = items.get(pos);
        h.title.setText(cap(it.modality) + " session • " + it.price);
        h.subtitle.setText(df.format(new Date(it.startAt)));
        h.status.setText(cap(it.status));
        boolean pending = "pending".equalsIgnoreCase(it.status);
        boolean accepted = "accepted".equalsIgnoreCase(it.status);
        boolean inProgress = "in_progress".equalsIgnoreCase(it.status);
        long now = System.currentTimeMillis();
        long endAt = it.endAt > 0 ? it.endAt : Long.MAX_VALUE;
        boolean withinWindow = now >= it.startAt && now <= endAt;

        h.accept.setVisibility(pending ? View.VISIBLE : View.GONE);
        h.decline.setVisibility(pending ? View.VISIBLE : View.GONE);

        // Time-gated controls
        if (accepted) {
            h.start.setVisibility(View.VISIBLE);
            h.complete.setVisibility(View.VISIBLE);
            h.start.setEnabled(withinWindow);
            h.complete.setEnabled(withinWindow);
        } else if (inProgress) {
            h.start.setVisibility(View.GONE);
            h.complete.setVisibility(View.VISIBLE);
            h.complete.setEnabled(now >= it.startAt);
        } else {
            h.start.setVisibility(View.GONE);
            h.complete.setVisibility(View.GONE);
        }

        // Chat button only for text modality
        boolean isText = "text".equalsIgnoreCase(it.modality) || "chat".equalsIgnoreCase(it.modality);
        if (isText && (accepted || inProgress)) {
            h.chat.setVisibility(View.VISIBLE);
            h.chat.setEnabled(withinWindow || inProgress);
        } else {
            h.chat.setVisibility(View.GONE);
        }

        // Call button only for call modality
        boolean isCall = "call".equalsIgnoreCase(it.modality) || "phone".equalsIgnoreCase(it.modality);
        if (isCall && (accepted || inProgress)) {
            h.call.setVisibility(View.VISIBLE);
            h.call.setEnabled(withinWindow || inProgress);
        } else {
            h.call.setVisibility(View.GONE);
        }

        // Video button only for video modality
        boolean isVideo = "video".equalsIgnoreCase(it.modality);
        if (isVideo && (accepted || inProgress)) {
            h.video.setVisibility(View.VISIBLE);
            h.video.setEnabled(withinWindow || inProgress);
        } else {
            h.video.setVisibility(View.GONE);
        }
        h.accept.setOnClickListener(v -> listener.onAccept(it));
        h.decline.setOnClickListener(v -> listener.onDecline(it));
        h.start.setOnClickListener(v -> listener.onStart(it));
        h.complete.setOnClickListener(v -> listener.onComplete(it));
        h.chat.setOnClickListener(v -> listener.onOpenChat(it));
        h.call.setOnClickListener(v -> listener.onJoinCall(it));
        h.video.setOnClickListener(v -> listener.onJoinVideo(it));
        // Do not auto-start on item tap; explicit buttons only
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle, status;
        Button accept, decline, start, complete, chat, call, video;
        VH(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
            status = itemView.findViewById(R.id.status);
            accept = itemView.findViewById(R.id.acceptButton);
            decline = itemView.findViewById(R.id.declineButton);
            start = itemView.findViewById(R.id.startButton);
            complete = itemView.findViewById(R.id.completeButton);
            chat = itemView.findViewById(R.id.chatButton);
            call = itemView.findViewById(R.id.callButton);
            video = itemView.findViewById(R.id.videoButton);
        }
    }

    private String cap(String s) { if (s == null || s.isEmpty()) return ""; return s.substring(0,1).toUpperCase() + s.substring(1); }
}
