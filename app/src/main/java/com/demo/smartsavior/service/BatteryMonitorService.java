package com.demo.smartsavior.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.demo.smartsavior.Sms;

public class BatteryMonitorService extends IntentService {

	SharedPreferences preference;
	
	final String ALARM_MSG_TEXT = "Help!!! Battery power on my device is below";
	
	public BatteryMonitorService() {
		super("BatteryMonitorService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
		Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		int rawlevel = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int level = -1;
        if (rawlevel >= 0 && scale > 0) {
            level = (rawlevel * 100) / scale;
        }
        
        preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int preferredBatteryLevel = preference.getInt("pref_key_battery_drainage", 0);
        
        StringBuilder sb = new StringBuilder(ALARM_MSG_TEXT);
        
        if(level < preferredBatteryLevel)
        {
        	sb.append(" ");
        	sb.append("" + level + "%");
        	
        	String alarmSms = new String(sb);
        	
        	String parent1 = preference.getString("pref_key_parent1", "");
        	String parent2 = preference.getString("pref_key_parent2", "");
        	
        	if(!TextUtils.isEmpty(parent1))
        		Sms.send(parent1, alarmSms);
        	if(!TextUtils.isEmpty(parent2))
        		Sms.send(parent2, alarmSms);
        }
        
        Log.i("level", "" + level);
	}

}
