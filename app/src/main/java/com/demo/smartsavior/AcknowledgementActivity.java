package com.demo.smartsavior;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.demo.smartsavior.service.AcknowledgementAlarmService;

public class AcknowledgementActivity extends Activity implements
		OnClickListener {

	String sender;
	String message;
	String acknowledgementMessage = "";

	int btnSafePressCount, btnUnsafePressCount;
	boolean hasReplied;

	TextView tvMessage;
	TextView tvTitle;
	Button buttonSafe;
	Button buttonUnsafe;
	View progressLayout;
	View mainLayout;

	AlarmManager alarmMgr;
	PendingIntent pendingIntent;
	SharedPreferences preference;

	ChildLocator childLocator;

	static final String[] PROJECTION = { Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY
			: CommonDataKinds.Phone.DISPLAY_NAME };

	static final String SAFE_DEFAULT_MSG = "This is an acknowledgement from your child. He/She is safe.";
	static final String UNSAFE_DEFAULT_MSG = "Your child is in danger.";
	static final String LOCATION_PART = "Currently staying at:";
	static final String LOCATION_NOT_FOUND = "Location was not found";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_acknowledgement);

		childLocator = new ChildLocator(this);
		childLocator.startToGetLocation();

		mainLayout = findViewById(R.id.relativeLayout_acknowledgement);
		progressLayout = findViewById(R.id.ack_sending_status);
		buttonSafe = (Button) findViewById(R.id.btn_say_safe);
		buttonUnsafe = (Button) findViewById(R.id.btn_say_unsafe);

		buttonSafe.setOnClickListener(this);
		buttonUnsafe.setOnClickListener(this);

		tvMessage = (TextView) findViewById(R.id.tv_message);
		tvMessage.setMovementMethod(new ScrollingMovementMethod());
		tvTitle = (TextView) findViewById(R.id.tv_title);

		Bundle extras = getIntent().getExtras();

		if (extras != null) {
			sender = extras.getString("sender");
			message = extras.getString("message")
					.replace(InternalSettings.MESSAGE_TAG, "").trim();

			tvMessage.setText(message);
		}

		tvTitle.setText("Message From " + getSenderName(sender));

		preference = PreferenceManager.getDefaultSharedPreferences(this);

		setAckDelayAlarm();
	}

	private String getSenderName(String sender) {

		String name = sender;
		Uri uri = Uri.withAppendedPath(
				CommonDataKinds.Phone.CONTENT_FILTER_URI, Uri.encode(sender));

		String select = "((" + CommonDataKinds.Phone.DISPLAY_NAME
				+ " NOTNULL) AND (" + CommonDataKinds.Phone.DISPLAY_NAME
				+ " != '' ))";

		Cursor result = getContentResolver().query(uri, PROJECTION, select,
				null, CommonDataKinds.Phone.DISPLAY_NAME);

		while (result.moveToNext())
			name = result.getString(result.getColumnIndex(PROJECTION[0]));

		result.close();

		return name;
	}

	@Override
	public void onBackPressed() {

		if (hasReplied)
			super.onBackPressed();
		else
			Toast.makeText(this, "Please press a button to reply",
					Toast.LENGTH_LONG).show();
	}

	@Override
	public void onClick(View v) {

		switch (v.getId()) {
		case R.id.btn_say_safe:
			btnSafePressCount++;
			if (btnSafePressCount == 2) {
				Location loc = childLocator.getLocation();
				acknowledgementMessage = preference.getString(
						"pref_key_ack_msg", SAFE_DEFAULT_MSG);
				showProgress(true);
				(new GetAddressTask(this)).execute(loc);
				break;
			}
			break;
		case R.id.btn_say_unsafe:
			btnUnsafePressCount++;
			if (btnUnsafePressCount == 2) {
				Location loc = childLocator.getLocation();
				acknowledgementMessage = UNSAFE_DEFAULT_MSG;
				showProgress(true);
				(new GetAddressTask(this)).execute(loc);
				break;
			}
			break;
		}
	}

	private void setAckDelayAlarm() {

		int delay = preference.getInt("pref_key_ack_delay", 1);

		alarmMgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		Intent alarmSmsIntent = new Intent(this,
				AcknowledgementAlarmService.class);
		pendingIntent = PendingIntent.getService(this, 0, alarmSmsIntent, 0);
		Calendar time = Calendar.getInstance();
		time.setTimeInMillis(System.currentTimeMillis());
		time.add(Calendar.MINUTE, delay);
		alarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(),
				pendingIntent);
	}

	/**
	 * A subclass of AsyncTask that calls getFromLocation() in the background.
	 * The class definition has these generic types: Location - A Location
	 * object containing the current location. Void - indicates that progress
	 * units are not used String - An address passed to onPostExecute()
	 */
	private class GetAddressTask extends AsyncTask<Location, Void, String> {
		Context mContext;

		public GetAddressTask(Context context) {
			super();
			mContext = context;
		}

		/**
		 * Get a Geocoder instance, get the latitude and longitude look up the
		 * address, and return it
		 * 
		 * @params params One or more Location objects
		 * @return A string containing the address of the current location, or
		 *         an empty string if no address can be found, or an error
		 *         message
		 */
		@Override
		protected String doInBackground(Location... params) {
			Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
			// Get the current location from the input parameter list
			Location loc = params[0];
			// Create a list to contain the result address
			List<Address> addresses = null;
			try {
				/*
				 * Return 1 address.
				 */
				addresses = geocoder.getFromLocation(loc.getLatitude(),
						loc.getLongitude(), 1);
			} catch (IOException e1) {
				Log.e("LocationSampleActivity",
						"IO Exception in getFromLocation()");
				e1.printStackTrace();
				return ("");
			} catch (IllegalArgumentException e2) {
				// Error message to post in the log
				String errorString = "Illegal arguments "
						+ Double.toString(loc.getLatitude()) + " , "
						+ Double.toString(loc.getLongitude())
						+ " passed to address service";
				Log.e("LocationSampleActivity", errorString);
				e2.printStackTrace();
				return ("");
			}
			// If the reverse geocode returned an address
			if (addresses != null && addresses.size() > 0) {
				// Get the first address
				Address address = addresses.get(0);
				/*
				 * Format the first line of address (if available), city, and
				 * country name.
				 */
				String addressText = String.format(
						"%s, %s, %s",
						// If there's a street address, add it
						address.getMaxAddressLineIndex() > 0 ? address
								.getAddressLine(0) : "",
						// Locality is usually a city
						address.getLocality(),
						// The country of the address
						address.getCountryName());
				// Return the text
				return addressText;
			} else {
				return "No address found";
			}
		}

		/**
		 * A method that's called once doInBackground() completes. Turn off the
		 * indeterminate activity indicator and set the text of the UI element
		 * that shows the address. If the lookup failed, display the error
		 * message.
		 */
		@Override
		protected void onPostExecute(String address) {
			Log.i("address", address);
			StringBuilder msgBuilder = new StringBuilder();
			msgBuilder.append(acknowledgementMessage);
			msgBuilder.append(" ");
			String msgText = TextUtils.isEmpty(address) ? msgBuilder.append(
					LOCATION_NOT_FOUND).toString() : msgBuilder
					.append(LOCATION_PART).append(" ").append(address)
					.toString();
			Log.i("msg", msgText);
			Sms.send(sender, msgText);

			// cancel Acknowledgement delay alarm
			alarmMgr.cancel(pendingIntent);
			pendingIntent.cancel();

			hasReplied = true;
			showProgress(false);
			finish();
		}

	}

	/**
	 * Shows the progress UI and hides the login form.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			progressLayout.setVisibility(View.VISIBLE);
			progressLayout.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							progressLayout.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mainLayout.setVisibility(View.VISIBLE);
			mainLayout.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mainLayout.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			progressLayout.setVisibility(show ? View.VISIBLE : View.GONE);
			mainLayout.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
}
