package com.demo.smartsavior.service;

import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.os.Looper;
import android.util.Log;

public class UninstallListenerThread extends Thread {

	boolean exit  = false;
	Context context;
	ActivityManager am;
	
	public UninstallListenerThread(Context context) {
		this.context = context;
		am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
	}
	
	@Override
	public void run() {
		
		Looper.prepare();
		
		while(!exit)
		{
			List<ActivityManager.RunningTaskInfo> runningsTasks = am.getRunningTasks(MAX_PRIORITY);
			
			String activityName = runningsTasks.get(0).topActivity.getClassName();
			
			Log.d("current actiivty", activityName);
		}
		
		Looper.loop();
	}
}
