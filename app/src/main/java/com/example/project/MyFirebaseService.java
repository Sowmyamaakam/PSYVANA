package com.example.project;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.app.PendingIntent;
import android.content.Intent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "reports_channel";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Save token to Firestore under users/{uid}.fcmToken when logged in
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("fcmToken", token);
            FirebaseFirestore.getInstance().collection("users").document(uid).update(updates);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        createChannelIfNeeded();
        String title = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getTitle() : "Notification";
        String body = remoteMessage.getNotification() != null ? remoteMessage.getNotification().getBody() : "";

        // Deep link based on data payload
        String type = remoteMessage.getData() != null ? remoteMessage.getData().get("type") : null;
        String apptId = remoteMessage.getData() != null ? remoteMessage.getData().get("appointmentId") : null;
        String reportUrl = remoteMessage.getData() != null ? remoteMessage.getData().get("reportUrl") : null;
        Intent tapIntent;
        if ("session_started".equals(type)) {
            tapIntent = new Intent(this, UpcomingAppointmentsActivity.class);
            tapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if ("chat_started".equals(type) && apptId != null) {
            tapIntent = new Intent(this, ChatActivity.class);
            tapIntent.putExtra(ChatActivity.EXTRA_APPOINTMENT_ID, apptId);
            tapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else if ("report_ready".equals(type) && apptId != null) {
            tapIntent = new Intent(this, ChatActivity.class);
            tapIntent.putExtra(ChatActivity.EXTRA_APPOINTMENT_ID, apptId);
            tapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else {
            tapIntent = new Intent(this, HomeActivity.class);
            tapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        }
        PendingIntent contentIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), tapIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Reports", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for reports and chat updates");
            nm.createNotificationChannel(channel);
        }
    }
}
