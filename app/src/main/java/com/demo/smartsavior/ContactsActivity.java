package com.demo.smartsavior;

import java.io.File;
import java.lang.reflect.Field;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.CommonDataKinds;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;

public class ContactsActivity extends FragmentActivity implements
		OnQueryTextListener, LoaderManager.LoaderCallbacks<Cursor> {

	ListView lvContacts;
	View contactLoadProgressView;
	SQLiteDatabase contactsDb;
	ContactsAdapter mCursorAdapter;
	SharedPreferences preference;

	boolean callControlOn;
	String queryFilter;
	
	static final int CHANGE_SETTINGS = 999;

	static final String[] FROM_COLUMNS = {
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? CommonDataKinds.Phone.DISPLAY_NAME
					: CommonDataKinds.Phone.DISPLAY_NAME,
			CommonDataKinds.Phone.NUMBER };

	static final int[] TO_IDS = { R.id.textView_contact_name,
			R.id.textView_contact_phone };

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contacts);

		File dir = getDir("whitelist", MODE_PRIVATE);
		if (!dir.exists())
			dir.mkdir();
		contactsDb = SQLiteDatabase.openOrCreateDatabase(dir + "/appDb", null,
				null);
		
		preference = PreferenceManager.getDefaultSharedPreferences(this);
		callControlOn = preference.getBoolean("pref_key_call_control", false);

		contactLoadProgressView = findViewById(R.id.contact_load_status);
		lvContacts = (ListView) findViewById(R.id.list_contacts);

		mCursorAdapter = new ContactsAdapter(this, R.layout.list_item_contacts,
				null, FROM_COLUMNS, TO_IDS, 0, new ContactVerifyCallback() {

					@Override
					public void editTrustedContactList(String contactNo,
							String contactName, boolean trusted) {

						String query = "";

						if (trusted) {
							query = "insert into allow_list values('"
									+ System.currentTimeMillis() + "','"
									+ contactNo + "','" + contactName + "')";
						} else {
							query = "delete from allow_list where phone_no='"
									+ contactNo + "'";
						}

						contactsDb.execSQL(query);
					}

					@Override
					public boolean callControlOn() {
						
						return callControlOn;
					}
				});

		lvContacts.setAdapter(mCursorAdapter);

		getSupportLoaderManager().initLoader(0, null, this);

		getOverflowMenu();
	}

	private void getOverflowMenu() {

		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class
					.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(
					android.R.integer.config_shortAnimTime);

			contactLoadProgressView.setVisibility(View.VISIBLE);
			contactLoadProgressView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							contactLoadProgressView
									.setVisibility(show ? View.VISIBLE
											: View.GONE);
						}
					});

			lvContacts.setVisibility(View.VISIBLE);
			lvContacts.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							lvContacts.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			contactLoadProgressView.setVisibility(show ? View.VISIBLE
					: View.GONE);
			lvContacts.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);
		
		if(requestCode == CHANGE_SETTINGS && resultCode == RESULT_OK)
		{
			boolean callControlPref = data.getExtras().getBoolean("callControlOn");
			if(callControlOn != callControlPref)
			{
				callControlOn = callControlPref;
				getSupportLoaderManager().restartLoader(0, null, this);		
			}
		}
	}

	@Override
	public void onBackPressed() {

		if (contactsDb.isOpen())
			contactsDb.close();

		super.onBackPressed();
	}

	@Override
	protected void onNewIntent(Intent intent) {

		setIntent(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_contacts_menu, menu);

		SearchView searchView = (SearchView) menu.findItem(R.id.action_search)
				.getActionView();

		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

		searchView.setSearchableInfo(searchManager
				.getSearchableInfo(getComponentName()));

		searchView.setOnQueryTextListener(this);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.action_settings:

			Intent intent = new Intent(ContactsActivity.this,
					ChildPreferenceActivity.class);
			startActivityForResult(intent, CHANGE_SETTINGS);
			break;

		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (contactsDb.isOpen())
			contactsDb.close();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle data) {

		showProgress(true);
		return new AllowListDbCursorLoader(this, contactsDb, queryFilter);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

		mCursorAdapter.swapCursor(data);
		showProgress(false);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

		mCursorAdapter.swapCursor(null);
	}

	@Override
	public boolean onQueryTextChange(String newText) {

		queryFilter = !TextUtils.isEmpty(newText) ? newText : null;
		getSupportLoaderManager().restartLoader(0, null, this);
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {

		queryFilter = !TextUtils.isEmpty(query) ? query : null;
		getSupportLoaderManager().restartLoader(0, null, this);
		return true;
	}

}
