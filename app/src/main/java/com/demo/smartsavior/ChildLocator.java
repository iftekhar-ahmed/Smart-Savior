package com.demo.smartsavior;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.Bundle;

public class ChildLocator {

	Context context;
	LocationManager locationManager;
	Location oldLocation;

	static final int TWO_MINUTES = 1000 * 60 * 2;

	public ChildLocator(Context context) {

		this.context = context;
	}

	public void startToGetLocation() {

		//setMobileDataEnabled(context, true);
		
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);

		oldLocation = getLastKnowLocation();

		// Define a listener that responds to location updates
		LocationListener locationListener = new LocationListener() {
			public void onLocationChanged(Location location) {

				if (isBetterLocation(location, oldLocation))
					oldLocation = location;
			}

			public void onStatusChanged(String provider, int status,
					Bundle extras) {
			}

			public void onProviderEnabled(String provider) {
			}

			public void onProviderDisabled(String provider) {
			}
		};

		// Register the listener with the Location Manager to receive location
		// updates
		locationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
	}

	public Location getLocation() {
		
		//setMobileDataEnabled(context, false);
		return oldLocation;
	}

	public Location getLastKnowLocation() {
		String locationProvider = LocationManager.NETWORK_PROVIDER;
		Location lastKnownLocation = locationManager
				.getLastKnownLocation(locationProvider);

		return lastKnownLocation;
	}

	/**
	 * Determines whether one Location reading is better than the current
	 * Location fix
	 * 
	 * @param location
	 *            The new Location that you want to evaluate
	 * @param currentBestLocation
	 *            The current Location fix, to which you want to compare the new
	 *            one
	 */
	protected boolean isBetterLocation(Location location,
			Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is newer or older
		long timeDelta = location.getTime() - currentBestLocation.getTime();
		boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
		boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
		boolean isNewer = timeDelta > 0;

		// If it's been more than two minutes since the current location, use
		// the new location
		// because the user has likely moved
		if (isSignificantlyNewer) {
			return true;
			// If the new location is more than two minutes older, it must be
			// worse
		} else if (isSignificantlyOlder) {
			return false;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
				.getAccuracy());
		boolean isLessAccurate = accuracyDelta > 0;
		boolean isMoreAccurate = accuracyDelta < 0;
		boolean isSignificantlyLessAccurate = accuracyDelta > 200;

		// Check if the old and new location are from the same provider
		boolean isFromSameProvider = isSameProvider(location.getProvider(),
				currentBestLocation.getProvider());

		// Determine location quality using a combination of timeliness and
		// accuracy
		if (isMoreAccurate) {
			return true;
		} else if (isNewer && !isLessAccurate) {
			return true;
		} else if (isNewer && !isSignificantlyLessAccurate
				&& isFromSameProvider) {
			return true;
		}
		return false;
	}

	/** Checks whether two providers are the same */
	private boolean isSameProvider(String provider1, String provider2) {
		if (provider1 == null) {
			return provider2 == null;
		}
		return provider1.equals(provider2);
	}

	@Deprecated
	// Requires android.permission.CHANGE_NETWORK_STATE. Doesn't always work. Unsafe to use
	private void setMobileDataEnabled(Context context, boolean enabled) {
		final ConnectivityManager conman = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		Class conmanClass;
		final Field iConnectivityManagerField;
		final Object iConnectivityManager;
		final Class iConnectivityManagerClass;
		final Method setMobileDataEnabledMethod;
		try {
			conmanClass = Class.forName(conman.getClass().getName());
			iConnectivityManagerField = conmanClass
					.getDeclaredField("mService");
			iConnectivityManagerField.setAccessible(true);
			iConnectivityManager = iConnectivityManagerField.get(conman);
			iConnectivityManagerClass = Class.forName(iConnectivityManager
					.getClass().getName());
			setMobileDataEnabledMethod = iConnectivityManagerClass
					.getDeclaredMethod("setMobileDataEnabled", Boolean.TYPE);

			setMobileDataEnabledMethod.setAccessible(true);

			setMobileDataEnabledMethod.invoke(iConnectivityManager, enabled);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
