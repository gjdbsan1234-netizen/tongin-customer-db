package com.tongin.yeoksam.customerdb;

import android.app.NotificationChannel;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "tongin_sms_channel";

    @Override public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) return;
        StringBuilder body = new StringBuilder();
        for (SmsMessage message : Telephony.Sms.Intents.getMessagesFromIntent(intent)) body.append(message.getMessageBody());
        if (body.length() == 0) return;
        SharedPreferences prefs = context.getSharedPreferences("tongin_sms", Context.MODE_PRIVATE);
        int count = prefs.getInt("receive_count", 0) + 1;
        prefs.edit()
            .putString("pending_sms", body.toString())
            .putLong("last_received_at", System.currentTimeMillis())
            .putInt("receive_count", count)
            .commit();
        showNotification(context);
    }

    private void showNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "고객 문자 자동등록", NotificationManager.IMPORTANCE_HIGH));
        }
        Intent open = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int noticeId = (int) (System.currentTimeMillis() & 0x7fffffff);
        PendingIntent pending = PendingIntent.getActivity(context, noticeId, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder notice = new Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_email)
            .setContentTitle("통인 고객DB")
            .setContentText("새 문자를 감지했습니다. 눌러서 고객정보를 확인하세요.")
            .setAutoCancel(true)
            .setContentIntent(pending);
        manager.notify(noticeId, notice.build());
    }
}
