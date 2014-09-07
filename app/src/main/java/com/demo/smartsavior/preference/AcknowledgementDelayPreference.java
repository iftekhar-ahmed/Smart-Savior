package com.demo.smartsavior.preference;

import com.demo.smartsavior.R;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

public class AcknowledgementDelayPreference extends DialogPreference {

	NumberPicker picker;
	TextView tvMessage;
	Integer preferredValue;

	static final int MIN_VALUE = 1;
	static final int MAX_VALUE = 60;
	static final int DEFAULT_VALUE = MIN_VALUE;
	static final String MESSAGE = "Select delay in minutes";

	public AcknowledgementDelayPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.preference_number_picker);
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		picker = (NumberPicker) view.findViewById(R.id.pref_num_picker);
		picker.setMinValue(MIN_VALUE);
		picker.setMaxValue(MAX_VALUE);
		picker.setValue(getPersistedInt(MIN_VALUE));
		tvMessage = (TextView) view.findViewById(R.id.textView_message);
		tvMessage.setText(MESSAGE);
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		super.onClick(dialog, which);
		if (which == DialogInterface.BUTTON_POSITIVE) {
			this.preferredValue = picker.getValue();
			persistInt(preferredValue);
			callChangeListener(preferredValue);
		}
	}

	@Override
	protected void onSetInitialValue(boolean restorePersistedValue,
			Object defaultValue) {
		int def = (defaultValue instanceof Number) ? (Integer) defaultValue
				: (defaultValue != null) ? Integer.parseInt(defaultValue
						.toString()) : 1;
		if (restorePersistedValue) {
			this.preferredValue = getPersistedInt(def);
		} else
			this.preferredValue = (Integer) defaultValue;
	}

	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInt(index, 1);
	}
}
