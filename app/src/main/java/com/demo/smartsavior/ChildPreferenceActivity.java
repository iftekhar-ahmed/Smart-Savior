package com.demo.smartsavior;

import java.util.Calendar;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.widget.Toast;

import com.demo.smartsavior.R;
import com.demo.smartsavior.receiver.PhoneCallReceiver;
import com.demo.smartsavior.service.BatteryMonitorService;

public class ChildPreferenceActivity extends PreferenceActivity {

	SharedPreferences preference;
	DialogPreference dpBatteryPower;
	CheckBoxPreference cbpCallControl;
	Preference prefParent1;
	Preference prefParent2;

	String selectedPreference;
	boolean credentialsAllRight;

	static final int PICK_CONTACT = 100;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.child_preference);

		preference = PreferenceManager.getDefaultSharedPreferences(this);

		ActionBar actionBar = getActionBar();

		if (preference.getBoolean("credentialsAllRight", false))
			actionBar.setDisplayHomeAsUpEnabled(true);
		else
			actionBar.setDisplayHomeAsUpEnabled(false);

		prefParent1 = (Preference) findPreference("pref_key_parent1");
		prefParent1.setTitle(preference.getString(prefParent1.getKey(),
				prefParent1.getTitle().toString()));
		prefParent1
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {

						selectedPreference = preference.getTitle().toString();
						inflateContactList();
						return true;
					}
				});

		prefParent2 = (Preference) findPreference("pref_key_parent2");
		prefParent2.setTitle(preference.getString(prefParent2.getKey(),
				prefParent2.getTitle().toString()));
		prefParent2
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {

						selectedPreference = preference.getTitle().toString();
						inflateContactList();
						return true;
					}
				});

		cbpCallControl = (CheckBoxPreference) findPreference("pref_key_call_control");
		cbpCallControl
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {

						if ((Boolean) newValue)
							switchReceiver(
									PhoneCallReceiver.class,
									PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
						else
							switchReceiver(
									PhoneCallReceiver.class,
									PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
						return true;
					}
				});

		OnPreferenceChangeListener powerPrefChangeListener = new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(Preference preference,
					Object newValue) {

				boolean alermAlreadySet = (PendingIntent.getService(
						ChildPreferenceActivity.this, 0, new Intent(
								ChildPreferenceActivity.this,
								BatteryMonitorService.class),
						PendingIntent.FLAG_NO_CREATE) != null);

				if (!alermAlreadySet)
					setBatteryAlarm();

				return true;
			}
		};

		dpBatteryPower = (DialogPreference) findPreference("pref_key_battery_drainage");
		dpBatteryPower.setOnPreferenceChangeListener(powerPrefChangeListener);
	}

	private void switchReceiver(Class receiver, int state) {

		ComponentName component = new ComponentName(this, receiver);
		getPackageManager().setComponentEnabledSetting(component, state,
				PackageManager.DONT_KILL_APP);
	}

	private void inflateContactList() {

		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
		startActivityForResult(intent, PICK_CONTACT);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch (item.getItemId()) {
		case android.R.id.home:
			onDestroy();
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public void onBackPressed() {

		credentialsAllRight = true;

		if (TextUtils.isEmpty(preference.getString("pref_key_username", ""))
				|| TextUtils.isEmpty(preference.getString("pref_key_password",
						""))) {
			showToast("Please provide Username and Password", 50);
			credentialsAllRight = false;
		}

		if (credentialsAllRight)
		{
			onDestroy();
		}

		preference.edit()
				.putBoolean("credentialsAllRight", credentialsAllRight)
				.commit();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		getIntent().putExtra("callControlOn", preference.getBoolean("pref_key_call_control", false));
		setResult(RESULT_OK, getIntent());
		finish();
	}

	private void showToast(String text, int yOffset) {
		Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.BOTTOM, 0, yOffset);
		toast.show();
	}

	private void setBatteryAlarm() {
		AlarmManager alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent batteryIntent = new Intent(this, BatteryMonitorService.class);
		PendingIntent pendingIntent = PendingIntent.getService(this, 0,
				batteryIntent, 0);
		Calendar time = Calendar.getInstance();
		time.setTimeInMillis(System.currentTimeMillis());
		time.add(Calendar.MINUTE,
				InternalSettings.BATTERY_CHECK_INTERVAL_MINUTES);
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(),
				InternalSettings.BATTERY_CHECK_INTERVAL_MINUTES * 60 * 1000,
				pendingIntent);
		Log.i("battery", "alarm set");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case (PICK_CONTACT):
			if (resultCode == Activity.RESULT_OK) {
				if (data != null) {
					Uri uri = data.getData();

					Log.d("uri", uri.toString());
					if (uri != null) {
						Cursor c = null;
						try {
							c = getContentResolver()
									.query(uri,
											new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER },
											null, null, null);

							if (c != null && c.moveToFirst()) {
								String number = c.getString(0);

								Log.d("parent phone number", number);
								if (selectedPreference.equals(prefParent1
										.getTitle().toString())) {
									prefParent1.setTitle(number);
									prefParent1
											.getEditor()
											.putString("pref_key_parent1",
													number).commit();
								} else if (selectedPreference
										.equals(prefParent2.getTitle()
												.toString())) {
									prefParent2.setTitle(number);
									prefParent2
											.getEditor()
											.putString("pref_key_parent2",
													number).commit();
								}
							}
						} finally {
							if (c != null) {
								c.close();
							}
						}
					}
				}
			}
		}
	}
}
