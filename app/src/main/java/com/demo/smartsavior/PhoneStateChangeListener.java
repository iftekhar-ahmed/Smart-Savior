package com.demo.smartsavior;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class PhoneStateChangeListener extends PhoneStateListener {

	TelephonyManager telephonyManager;
	PhoneStateChangeCallback phoneStateChangeCallback;
	SharedPreferences preferences;

	String incomingNumber;
	String childPhone1;
	String childPhone2;

	int prev_state;
	int pref_num_missed_calls;
	boolean flagCallCounted;
	String appUserMode;

	public PhoneStateChangeListener(Context context,
			TelephonyManager telephonyManager, PhoneStateChangeCallback callback) {
		this.telephonyManager = telephonyManager;
		phoneStateChangeCallback = callback;

		preferences = PreferenceManager.getDefaultSharedPreferences(context);

		appUserMode = preferences.getString("userMode", "");
		pref_num_missed_calls = preferences.getInt("pref_key_num_miss_call", 1);
		childPhone1 = preferences.getString("pref_key_contact_1", "");
		childPhone2 = preferences.getString("pref_key_contact_2", "");
	}

	@Override
	public void onCallStateChanged(int state, String incomingNo) {
		super.onCallStateChanged(state, incomingNumber);

		if (incomingNo != null && !incomingNo.equals(""))
			this.incomingNumber = incomingNo;

		switch (state) {
		case TelephonyManager.CALL_STATE_RINGING:
			prev_state = state;
			if (appUserMode.equals(InternalSettings.UserMode.MODE_CHILD))
			{
				phoneStateChangeCallback.filterIncomingCall(incomingNumber);
			}
			break;

		case TelephonyManager.CALL_STATE_OFFHOOK:
			prev_state = state;
			// safety filtering after call is active/on-hold/dialing
			if (appUserMode.equals(InternalSettings.UserMode.MODE_CHILD)) 
			{
				phoneStateChangeCallback.filterIncomingCall(incomingNumber);
			}
			// resets missed call counter
			if(appUserMode.equals(InternalSettings.UserMode.MODE_PARENT))
			{
				if (PhoneNumberUtils.compare(incomingNumber, childPhone1)) {
					preferences.edit().putInt("phone1MissedCallCount", 0).commit();
				} else if (PhoneNumberUtils.compare(incomingNumber, childPhone2)) {
					preferences.edit().putInt("phone2MissedCallCount", 0).commit();
				}
			}
			break;

		case TelephonyManager.CALL_STATE_IDLE:

			if (appUserMode.equals(InternalSettings.UserMode.MODE_PARENT)
					&& prev_state == TelephonyManager.CALL_STATE_IDLE
					&& !flagCallCounted) {

				if (PhoneNumberUtils.compare(incomingNumber, childPhone1)) {
					if (isReplyNeeded("phone1MissedCallCount"))
						Sms.send(
								childPhone1,
								InternalSettings.MESSAGE_TAG
										+ "\n"
										+ preferences.getString(
												"pref_key_sms_text", "").trim());
				} else if (PhoneNumberUtils
						.compare(incomingNumber, childPhone2)) {
					if (isReplyNeeded("phone2MissedCallCount"))
						Sms.send(
								childPhone2,
								InternalSettings.MESSAGE_TAG
										+ "\n"
										+ preferences.getString(
												"pref_key_sms_text", "").trim());
				}

				flagCallCounted = true;
			}

			// Very important to place this line after the missed call check
			// above.
			// This ensures that we ignore the first time that phone state
			// changes to idle
			// We work with the second idle state which is triggered by either
			// user terminating the call or call terminating after ringing for a
			// while
			prev_state = state;

			break;

		default:
			break;
		}

		telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
	}

	private boolean isReplyNeeded(String sharedPreferenceKey) {

		int missCallCount = preferences.getInt(sharedPreferenceKey, 0);
		missCallCount += 1;

		Log.i(sharedPreferenceKey, "" + missCallCount);

		if (missCallCount == pref_num_missed_calls) {
			preferences.edit().putInt(sharedPreferenceKey, 0).commit();
			return true;
		}

		preferences.edit().putInt(sharedPreferenceKey, missCallCount).commit();
		return false;
	}
}
