package com.quanlu.mindos;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL = "reflection_reminders";
    @Override public void onReceive(Context context,Intent intent) {
        if(!ReminderScheduler.enabled(context))return;
        int i=intent.getIntExtra("index",0);
        if(i<0 || i>=ReminderScheduler.HOURS.length)return;
        ReminderScheduler.scheduleOne(context,i); // Schedule tomorrow's reminder.
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        NotificationManager manager=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager==null)return;
        if(Build.VERSION.SDK_INT>=26){
            NotificationChannel channel=new NotificationChannel(CHANNEL,"日间觉察提醒",NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("每天六次心智觉察问题");manager.createNotificationChannel(channel);
        }
        Intent open=new Intent(context,MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notification=new Notification.Builder(context,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("心智成长OS · 第"+(i+1)+"次觉察")
            .setContentText(ReminderScheduler.QUESTIONS[i])
            .setStyle(new Notification.BigTextStyle().bigText(ReminderScheduler.QUESTIONS[i]))
            .setContentIntent(pi).setAutoCancel(true).build();
        manager.notify(100+i,notification);
    }
}
