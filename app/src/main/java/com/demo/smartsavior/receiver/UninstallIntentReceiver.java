package com.demo.smartsavior.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.demo.smartsavior.service.UninstallListenerThread;

public class UninstallIntentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent extras) {
		
		String[] packageNames = extras.getStringArrayExtra("android.intent.extra.PACKAGES");
		
		if(packageNames != null)
		{
			for(String packageName : packageNames)
			{
				if(packageName != null && packageName.equals("com.demo.smartsavior"))
					new UninstallListenerThread(context).start();
			}
		}
	}

}
