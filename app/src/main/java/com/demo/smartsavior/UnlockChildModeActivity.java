package com.demo.smartsavior;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class UnlockChildModeActivity extends Activity {
	
	private String mUsername;
	private String mPassword;
	private SharedPreferences loginPreference;

	// UI references.
	private EditText mUsernameView;
	private EditText mPasswordView;
	private View mUnlockFormView;
	private View mUnlockStatusView;
	private TextView mUnlockStatusMessageView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_unlock_child_mode);
		
		mPasswordView = (EditText) findViewById(R.id.password);
		mPasswordView
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					@Override
					public boolean onEditorAction(TextView textView, int id,
							KeyEvent keyEvent) {
						if (id == R.id.unlock || id == EditorInfo.IME_NULL) {
							attemptLogin();
							return true;
						}
						return false;
					}
				});

		mUsernameView = (EditText) findViewById(R.id.username);
		mUnlockFormView = findViewById(R.id.unlock_form);
		mUnlockStatusView = findViewById(R.id.unlock_status);
		mUnlockStatusMessageView = (TextView) findViewById(R.id.unlock_status_message);

		findViewById(R.id.unlock_button).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						attemptLogin();
					}
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.activity_login_child_mode, menu);
		return true;
	}

	/**
	 * Attempts to sign in or register the account specified by the login form.
	 * If there are form errors (invalid email, missing fields, etc.), the
	 * errors are presented and no actual login attempt is made.
	 */
	public void attemptLogin() {

		// Reset errors.
		mUsernameView.setError(null);
		mPasswordView.setError(null);

		// Store values at the time of the login attempt.
		mUsername = mUsernameView.getText().toString();
		mPassword = mPasswordView.getText().toString();

		boolean cancel = false;
		View focusView = null;
		loginPreference = PreferenceManager.getDefaultSharedPreferences(this);

		// Check for a valid password.
		if (TextUtils.isEmpty(mPassword)) {
			mPasswordView.setError(getString(R.string.error_field_required));
			focusView = mPasswordView;
			cancel = true;
		} else if (!mPassword.equals(loginPreference.getString("pref_key_password", ""))) {
			mPasswordView.setError(getString(R.string.error_incorrect_password));
			focusView = mPasswordView;
			cancel = true;
		}

		// Check for a valid username.
		if (TextUtils.isEmpty(mUsername)) {
			mUsernameView.setError(getString(R.string.error_field_required));
			focusView = mUsernameView;
			cancel = true;
		} else if (!mUsername.equals(loginPreference.getString("pref_key_username", ""))) {
			mUsernameView.setError(getString(R.string.error_incorrect_username));
			focusView = mUsernameView;
			cancel = true;
		}

		if (cancel) {
			// There was an error; don't attempt login and focus the first
			// form field with an error.
			focusView.requestFocus();
		} else {
			// Show a progress spinner, and kick off a background task to
			// perform the user login attempt.
			mUnlockStatusMessageView.setText(R.string.unlock_progress);
			showProgress(true);
			startActivity(new Intent(UnlockChildModeActivity.this, ContactsActivity.class));
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

			mUnlockStatusView.setVisibility(View.VISIBLE);
			mUnlockStatusView.animate().setDuration(shortAnimTime)
					.alpha(show ? 1 : 0)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mUnlockStatusView.setVisibility(show ? View.VISIBLE
									: View.GONE);
						}
					});

			mUnlockFormView.setVisibility(View.VISIBLE);
			mUnlockFormView.animate().setDuration(shortAnimTime)
					.alpha(show ? 0 : 1)
					.setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							mUnlockFormView.setVisibility(show ? View.GONE
									: View.VISIBLE);
						}
					});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mUnlockStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
			mUnlockFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}
	
}
