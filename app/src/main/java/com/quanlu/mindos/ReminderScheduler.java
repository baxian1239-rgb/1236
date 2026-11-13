package com.quanlu.mindos;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public final class ReminderScheduler {
    static final String PREFS = "mindos_preferences";
    static final String ENABLED = "reminders_enabled";
    static final int[] HOURS = {11, 13, 15, 17, 19, 21};
    static final int[] MINUTES = {0, 30, 15, 0, 30, 0};
    static final String[] QUESTIONS = {
        "现在的行动，是在回避更重要的事吗？",
        "过去两小时的行动，反映了什么目标？",
        "这一刻，我是在靠近目标还是重复旧轨迹？",
        "什么重要的事情，我正假装不重要？",
        "此刻的选择，是在保护旧身份吗？",
        "今天什么时候最有生命力？"
    };
    private ReminderScheduler() {}
    static boolean enabled(Context context) {
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(ENABLED,false);
    }
    static void setEnabled(Context context,boolean enabled) {
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(ENABLED,enabled).apply();
        if (enabled) scheduleAll(context); else cancelAll(context);
    }
    static PendingIntent pending(Context context,int index) {
        Intent intent = new Intent(context, ReminderReceiver.class)
            .setAction("com.quanlu.mindos.REMINDER_" + index)
            .putExtra("index",index);
        return PendingIntent.getBroadcast(context,2000+index,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void scheduleAll(Context context) {
        for(int i=0;i<HOURS.length;i++) scheduleOne(context,i);
    }
    static void scheduleOne(Context context,int index) {
        if (!enabled(context)) return;
        Calendar date=Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY,HOURS[index]);date.set(Calendar.MINUTE,MINUTES[index]);
        date.set(Calendar.SECOND,0);date.set(Calendar.MILLISECOND,0);
        if(date.getTimeInMillis()<=System.currentTimeMillis()) date.add(Calendar.DATE,1);
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(alarms!=null){
            // Inexact alarm. No special exact-alarm entitlement is requested.
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,date.getTimeInMillis(),pending(context,index));
        }
    }
    static void cancelAll(Context context){
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if(alarms!=null)for(int i=0;i<HOURS.length;i++) alarms.cancel(pending(context,i));
    }
}
