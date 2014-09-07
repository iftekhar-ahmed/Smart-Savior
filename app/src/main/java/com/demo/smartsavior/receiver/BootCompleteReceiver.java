package com.demo.smartsavior.receiver;

import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.demo.smartsavior.InternalSettings;
import com.demo.smartsavior.service.BatteryMonitorService;

public class BootCompleteReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		
		AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		Intent batteryIntent = new Intent(context, BatteryMonitorService.class);
		PendingIntent pendingIntent = PendingIntent.getService(context, 0, batteryIntent, 0);
		Calendar time = Calendar.getInstance();
		time.setTimeInMillis(System.currentTimeMillis());
		time.add(Calendar.MINUTE,
				InternalSettings.BATTERY_CHECK_INTERVAL_MINUTES);
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(),
				InternalSettings.BATTERY_CHECK_INTERVAL_MINUTES * 60 * 1000,
				pendingIntent);
		Log.i("battery", "alarm set");
	}

}
