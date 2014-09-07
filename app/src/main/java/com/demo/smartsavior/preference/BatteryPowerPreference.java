package com.demo.smartsavior.preference;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.demo.smartsavior.R;

public class BatteryPowerPreference extends DialogPreference implements OnSeekBarChangeListener {

	SeekBar seekbarBatteryPower;
	TextView textViewSeekbarProgress;
	Integer preferredValue;
	
	static final int MAX_VALUE = 100;
	
	public BatteryPowerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.preference_seekbar);
	}

	@Override
	protected void onBindDialogView(View view) {
		super.onBindDialogView(view);
		this.seekbarBatteryPower = (SeekBar)view.findViewById(R.id.seekbar_battery_drainage);
		this.textViewSeekbarProgress = (TextView)view.findViewById(R.id.textView_battery_power_value);
		
		seekbarBatteryPower.setMax(MAX_VALUE);
		seekbarBatteryPower.setProgress(getPersistedInt(0));
		seekbarBatteryPower.setOnSeekBarChangeListener(this);
		
		textViewSeekbarProgress.setText("" + seekbarBatteryPower.getProgress() + "%");
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which) {
		super.onClick(dialog, which);
		if ( which == DialogInterface.BUTTON_POSITIVE ) {
			this.preferredValue = seekbarBatteryPower.getProgress();
			persistInt( preferredValue );
			callChangeListener( preferredValue );
		}
	}
	
	@Override
	protected void onSetInitialValue(boolean restorePersistedValue,
			Object defaultValue) {
		int def = ( defaultValue instanceof Number ) ? (Integer)defaultValue
				: ( defaultValue != null ) ? Integer.parseInt(defaultValue.toString()) : 1;
		if ( restorePersistedValue ) {
			this.preferredValue = getPersistedInt(def);
		}
		else this.preferredValue = (Integer)defaultValue;
	}
		
	@Override
	protected Object onGetDefaultValue(TypedArray a, int index) {
		return a.getInt(index, 1);
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		
		textViewSeekbarProgress.setText("" + progress + "%");
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
}
