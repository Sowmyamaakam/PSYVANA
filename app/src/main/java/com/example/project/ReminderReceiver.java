package com.example.project;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "appointments_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String apptId = intent.getStringExtra("appointment_id");
        if (apptId == null) apptId = "";

        createChannel(context);

        Intent open = new Intent(context, BookAppointmentActivity.class);
        open.putExtra("appointment_id", apptId);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                apptId.hashCode(),
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Appointment Reminder")
                .setContentText("Your session starts in about 4 hours.")
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(Math.abs(apptId.hashCode()), builder.build());
    }

    private void createChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Appointment Reminders", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Reminders for upcoming appointments");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
