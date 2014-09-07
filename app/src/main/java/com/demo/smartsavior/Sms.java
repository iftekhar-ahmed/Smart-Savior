package com.demo.smartsavior;

import android.telephony.SmsManager;

public class Sms {

	public static void send(String phoneNumber, String message) {
		SmsManager sms = SmsManager.getDefault();
		sms.sendTextMessage(phoneNumber, null, message, null, null);
	}
}
