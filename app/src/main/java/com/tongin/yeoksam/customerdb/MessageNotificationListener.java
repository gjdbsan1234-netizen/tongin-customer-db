package com.tongin.yeoksam.customerdb;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class MessageNotificationListener extends NotificationListenerService {
    private static final String CHANNEL_ID = "tongin_message_channel";
    private static final Set<String> MESSAGE_APPS = new HashSet<>(Arrays.asList(
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging"
    ));
    private static final Pattern PHONE = Pattern.compile("01[016789][\\s-]?\\d{3,4}[\\s-]?\\d{4}");

    @Override public void onNotificationPosted(StatusBarNotification sbn) {
        if (!MESSAGE_APPS.contains(sbn.getPackageName())) return;
        Bundle extras = sbn.getNotification().extras;
        String title = text(extras.getCharSequence(Notification.EXTRA_TITLE));
        String body = text(extras.getCharSequence(Notification.EXTRA_BIG_TEXT));
        if (body.isEmpty()) body = text(extras.getCharSequence(Notification.EXTRA_TEXT));
        if (body.isEmpty()) return;
        String combined = title.isEmpty() ? body : title + "\n" + body;
        if (!looksLikeMovingLead(combined)) return;

        SharedPreferences prefs = getSharedPreferences("tongin_sms", Context.MODE_PRIVATE);
        int count = prefs.getInt("receive_count", 0) + 1;
        prefs.edit()
            .putString("pending_sms", combined)
            .putLong("last_received_at", System.currentTimeMillis())
            .putInt("receive_count", count)
            .commit();
        showCustomerDbNotification();
    }

    private boolean looksLikeMovingLead(String message) {
        boolean movingKeyword = message.contains("[통인CS]") || message.contains("이사일") ||
            message.contains("이사날짜") || message.contains("이사 날짜") ||
            message.contains("현주소") || message.contains("후주소") ||
            message.contains("출발지") || message.contains("도착지") ||
            message.contains("방문견적");
        boolean customerSignal = PHONE.matcher(message).find() || message.contains("고객님") ||
            message.contains("고객명") || message.contains("고객 성함");
        return movingKeyword && customerSignal;
    }

    private String text(CharSequence value) { return value == null ? "" : value.toString().trim(); }

    private void showCustomerDbNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, "이사 문자 자동등록", NotificationManager.IMPORTANCE_HIGH));
        int noticeId = (int) (System.currentTimeMillis() & 0x7fffffff);
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pending = PendingIntent.getActivity(this, noticeId, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notice = new Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_email)
            .setContentTitle("통인 고객DB")
            .setContentText("이사 관련 문자를 감지했습니다. 눌러서 확인하세요.")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build();
        manager.notify(noticeId, notice);
    }
}
