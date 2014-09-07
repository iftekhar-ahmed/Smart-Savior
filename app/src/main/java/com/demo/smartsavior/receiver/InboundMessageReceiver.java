package com.demo.smartsavior.receiver;

import com.demo.smartsavior.InternalSettings;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.SmsMessage;
import android.util.Log;

public class InboundMessageReceiver extends BroadcastReceiver {

	SharedPreferences preference;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		final Bundle extras = intent.getExtras();
		
		try
		{
			if(extras != null)
			{
				preference = PreferenceManager.getDefaultSharedPreferences(context);
				final Object[] pduObjs = (Object[])extras.get("pdus");
				
				String preferredParent1 = preference.getString("pref_key_parent1", "");
				String preferredParent2 = preference.getString("pref_key_parent2", "");
				
				for(int j = 0; j < pduObjs.length; j++)
				{
					SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pduObjs[j]);
					
					String sender = currentMessage.getOriginatingAddress();
					String messageBody = currentMessage.getDisplayMessageBody();
					
					if(!messageBody.startsWith(InternalSettings.MESSAGE_TAG))
						continue;
					
					if(PhoneNumberUtils.compare(sender, preferredParent1) || 
							PhoneNumberUtils.compare(sender, preferredParent2))
					{
						Intent i = new Intent();
						i.setClassName("com.demo.smartsavior", "com.demo.smartsavior.AcknowledgementActivity");
						i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						i.putExtra("sender", sender);
						i.putExtra("message", messageBody);
						context.startActivity(i);
						break;
					}
				}
			}
		}
		catch(Exception e)
		{
			Log.e("SmsReceiver", e.getMessage());
		}
	}

}
