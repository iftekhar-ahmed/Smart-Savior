package com.demo.smartsavior;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import com.demo.smartsavior.R;
import com.demo.smartsavior.receiver.BootCompleteReceiver;
import com.demo.smartsavior.receiver.InboundMessageReceiver;
import com.demo.smartsavior.receiver.PhoneCallReceiver;

public class ParentPreferenceActivity extends PreferenceActivity {

	Preference prefContact1;
	Preference prefContact2;
	CheckBoxPreference cbpAutoReply;

	SharedPreferences preference;

	String selectedPreference;

	static final int PICK_CONTACT = 100;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.parent_preference);

		Bundle extras = getIntent().getExtras();

		if (extras != null) {
			if (extras.getBoolean("stopInboundMsgReceiver"))
				switchReceiver(InboundMessageReceiver.class,
						PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
			if (extras.getBoolean("stopBootCompleteReceiver"))
				switchReceiver(BootCompleteReceiver.class,
						PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
		}

		preference = PreferenceManager.getDefaultSharedPreferences(this);

		prefContact1 = (Preference) findPreference("pref_key_contact_1");
		prefContact1.setTitle(preference.getString(prefContact1.getKey(),
				prefContact1.getTitle().toString()));
		prefContact1
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {

						selectedPreference = preference.getTitle().toString();
						inflateContactList();
						return true;
					}
				});

		prefContact2 = (Preference) findPreference("pref_key_contact_2");
		prefContact2.setTitle(preference.getString(prefContact2.getKey(),
				prefContact2.getTitle().toString()));
		prefContact2
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {

						selectedPreference = preference.getTitle().toString();
						inflateContactList();
						return true;
					}
				});

		cbpAutoReply = (CheckBoxPreference) findPreference("pref_key_auto_reply");
		cbpAutoReply
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
	}

	private void inflateContactList() {

		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
		startActivityForResult(intent, PICK_CONTACT);
	}

	private void switchReceiver(Class receiver, int state) {

		ComponentName component = new ComponentName(this, receiver);
		getPackageManager().setComponentEnabledSetting(component, state,
				PackageManager.DONT_KILL_APP);
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

								Log.d("child phone number", number);
								if (selectedPreference.equals(prefContact1
										.getTitle().toString())) {
									number = number.replaceAll("\\s+", "");
									prefContact1.setTitle(number);
									prefContact1
											.getEditor()
											.putString("pref_key_contact_1",
													number).commit();
								} else if (selectedPreference
										.equals(prefContact2.getTitle()
												.toString())) {
									number = number.replaceAll("\\s+", "");
									prefContact2.setTitle(number);
									prefContact2
											.getEditor()
											.putString("pref_key_contact_2",
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
