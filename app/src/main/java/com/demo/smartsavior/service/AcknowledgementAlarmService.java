package com.demo.smartsavior.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.demo.smartsavior.Sms;

public class AcknowledgementAlarmService extends IntentService {

SharedPreferences preference;

	static final String ALARM_SMS = "Your child failed to acknowledge your message. S/he could be in trouble!!";
	
	public AcknowledgementAlarmService() {
		super("AcknowledgementAlarmService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		
        preference = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String smsText = ALARM_SMS;
        
        String parent1 = preference.getString("pref_key_parent1", "");
    	String parent2 = preference.getString("pref_key_parent2", "");
    	
    	if(!TextUtils.isEmpty(parent1))
    		Sms.send(parent1, smsText);
    	if(!TextUtils.isEmpty(parent2))
    		Sms.send(parent2, smsText);
        
        Log.i("alarm smsText", "" + smsText);
	}

}
