package com.demo.smartsavior;

import java.io.File;

import com.demo.smartsavior.receiver.PhoneCallReceiver;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

public class HomeActivity extends Activity {

	SharedPreferences preferences;
	RadioGroup btnRadioGroup;
	Button btnGo;
	SQLiteDatabase contactsDb;
	boolean callControlDefault;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		
		if(preferences.getString("userMode", "").equals(InternalSettings.UserMode.MODE_CHILD))
		{
			if(preferences.getBoolean("credentialsAllRight", false))
				startActivity(new Intent(HomeActivity.this, UnlockChildModeActivity.class));
			else
				startActivity(new Intent(HomeActivity.this, ChildPreferenceActivity.class));
			finish();
		}
		else if(preferences.getString("userMode", "").equals(InternalSettings.UserMode.MODE_PARENT))
		{
			startActivity(new Intent(HomeActivity.this, ParentPreferenceActivity.class));
			finish();
		}
		
		callControlDefault = getResources().getBoolean(R.bool.call_control_default);
		if(!preferences.getBoolean("pref_key_call_control", callControlDefault))
			switchReceiver(PhoneCallReceiver.class, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);

		File dir = getDir("whitelist", MODE_PRIVATE);
		if (!dir.exists())
			dir.mkdir();
		contactsDb = SQLiteDatabase.openOrCreateDatabase(dir + "/appDb", null,
				null);

		if (!preferences.getBoolean("isDbCreated", false))
			createTables();

		btnRadioGroup = (RadioGroup) findViewById(R.id.radio_group);

		btnGo = (Button) findViewById(R.id.btn_go);

		btnGo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				int selectedButtonId = btnRadioGroup.getCheckedRadioButtonId();

				if (selectedButtonId == R.id.rbtn_child) {
					goToChildActivity();
				} else if (selectedButtonId == R.id.rbtn_parent) {
					goToParentActivity();
				}
			}

			private void goToParentActivity() {
				preferences.edit().putString("userMode", InternalSettings.UserMode.MODE_PARENT).commit();
				Intent intent = new Intent(HomeActivity.this, ParentPreferenceActivity.class);
				intent.putExtra("stopInboundMsgReceiver", true);
				intent.putExtra("stopBootCompleteReceiver", true);
				startActivity(intent);
				finish();
			}

			private void goToChildActivity() {
				preferences.edit().putString("userMode", InternalSettings.UserMode.MODE_CHILD).commit();
				startActivity(new Intent(HomeActivity.this,
						ChildPreferenceActivity.class));
				finish();
			}
		});
	}
	
	private void switchReceiver(Class receiver, int state) {

		ComponentName component = new ComponentName(this, receiver);
		getPackageManager().setComponentEnabledSetting(component, state,
				PackageManager.DONT_KILL_APP);
	}

	private void createTables() {

		String createAllowTable = "create TABLE allow_list "
				+ "(id INT NOT NULL, " + "phone_no varchar NOT NULL," + "name varchar NOT NULL)";

		contactsDb.execSQL(createAllowTable);

		preferences.edit().putBoolean("isDbCreated", true).commit();
	}

}
