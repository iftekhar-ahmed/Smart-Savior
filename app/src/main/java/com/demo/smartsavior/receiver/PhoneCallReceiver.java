package com.demo.smartsavior.receiver;

import java.io.File;
import java.lang.reflect.Method;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
import com.demo.smartsavior.PhoneStateChangeCallback;
import com.demo.smartsavior.PhoneStateChangeListener;

public class PhoneCallReceiver extends BroadcastReceiver {

	ITelephony telephonyService;	
	SQLiteDatabase contactsDb;
	
	static String phoneNumber; // field is static because onReceived gets called more than once 
							   // to a single action
	
	@Override
	public void onReceive(final Context context, Intent intent) {

		if(intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL))
		{
			phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
		}
		
		final TelephonyManager telephonyManager = (TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE);
		
		PhoneStateChangeListener phoneStateListener = new PhoneStateChangeListener(
				context, telephonyManager, new PhoneStateChangeCallback() {

					@Override
					public boolean filterIncomingCall(String incomingNo) {
						
						if(incomingNo != null) phoneNumber = incomingNo;
						
						return filterCall(context, telephonyManager);
					}
				});

		telephonyManager.listen(phoneStateListener,
				PhoneStateListener.LISTEN_CALL_STATE);
	}

	private boolean filterCall(Context context, TelephonyManager telephonyManager)
	{
		try {
			Class c = Class.forName(telephonyManager.getClass()
					.getName());
			Method m = c.getDeclaredMethod("getITelephony");

			m.setAccessible(true);

			telephonyService = (ITelephony) m
					.invoke(telephonyManager);

		} catch (Exception e) {
			e.printStackTrace();
		}

		File dir = context.getDir("whitelist",
				Context.MODE_PRIVATE);
		if (dir.exists())
			dir.mkdir();
		contactsDb = SQLiteDatabase.openDatabase(
				dir + "/appDb", null, Context.MODE_PRIVATE);

		String allowed = "select phone_no from allow_list";

		Cursor allowedContacts = contactsDb.rawQuery(allowed,
				null);

		while (allowedContacts.moveToNext()) {
			if (PhoneNumberUtils.compare(phoneNumber,
					allowedContacts.getString(0))) {
				Log.i("trusted!", phoneNumber);
				return true;
			}
		}

		try {
			telephonyService.endCall();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		allowedContacts.close();
		contactsDb.close();

		return false;
	}
}
