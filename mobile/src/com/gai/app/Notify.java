package com.gai.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** 通知工具：前台常驻通知 + 事件通知（任务栏可见） */
public class Notify {
    public static final int ID_MAIN = 1;
    public static final int ID_EVENT = 2;

    public static void ensureChannels(Context c) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel main = new NotificationChannel("gai_main", "G-ai 运行状态", NotificationManager.IMPORTANCE_LOW);
            main.setDescription("G-ai 悬浮窗与后台服务状态");
            main.setShowBadge(false);
            nm.createNotificationChannel(main);
            NotificationChannel ev = new NotificationChannel("gai_events", "G-ai 事件", NotificationManager.IMPORTANCE_HIGH);
            ev.setDescription("G-ai 连接与任务事件");
            nm.createNotificationChannel(ev);
        }
    }

    public static Notification running(Context c, String content) {
        ensureChannels(c);
        Intent i = new Intent(c, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(c, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent stopI = new Intent(c, FloatingWindowService.class).setAction("STOP");
        PendingIntent spi = PendingIntent.getService(c, 1, stopI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, "gai_main")
                : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat_g)
                .setContentTitle("G-ai 正在运行")
                .setContentText(content)
                .setContentIntent(pi)
                .setOngoing(true)
                .addAction(0, "停止", spi);
        return b.build();
    }

    public static void event(Context c, String title, String text) {
        ensureChannels(c);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(c, "gai_events")
                : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat_g)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true);
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(ID_EVENT, b.build());
    }

    public static void updateMain(Context c, String content) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(ID_MAIN, running(c, content));
    }
}
