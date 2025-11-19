package com.example.project;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<Map<String, Object>> items = new ArrayList<>();
    private final String myUid = FirebaseAuth.getInstance().getCurrentUser() != null ?
            FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
    private final SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.getDefault());

    public void setItems(List<Map<String, Object>> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    public void addOrUpdateAll(List<Map<String, Object>> list) {
        setItems(list);
    }

    @Override public int getItemViewType(int position) {
        Map<String, Object> m = items.get(position);
        String sender = m.get("senderId") != null ? m.get("senderId").toString() : "";
        return sender.equals(myUid) ? TYPE_SENT : TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
            return new SentVH(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
            return new ReceivedVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Map<String, Object> m = items.get(position);
        String text = m.get("text") != null ? m.get("text").toString() : "";
        long ts = 0L;
        Object createdAt = m.get("createdAt");
        if (createdAt instanceof Number) ts = ((Number) createdAt).longValue();
        String time = ts > 0 ? timeFmt.format(new Date(ts)) : "";
        if (holder instanceof SentVH) {
            ((SentVH) holder).messageText.setText(text);
            ((SentVH) holder).timeText.setText(time);
        } else if (holder instanceof ReceivedVH) {
            ((ReceivedVH) holder).messageText.setText(text);
            ((ReceivedVH) holder).timeText.setText(time);
        }
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class SentVH extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        SentVH(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
        }
    }

    static class ReceivedVH extends RecyclerView.ViewHolder {
        TextView messageText, timeText;
        ReceivedVH(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            timeText = itemView.findViewById(R.id.timeText);
        }
    }
}
