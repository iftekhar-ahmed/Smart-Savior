package com.demo.smartsavior;

import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract.CommonDataKinds;

public class AllowListDbCursorLoader extends SimpleCursorLoader {

	Context context;
	SQLiteDatabase contactsDb;
	String filter;
	Uri baseUri;

	static final String[] PROJECTION = {
			CommonDataKinds.Phone._ID,
			CommonDataKinds.Phone.LOOKUP_KEY,
			CommonDataKinds.Phone.NUMBER,
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
					: CommonDataKinds.Phone._ID,
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY
					: CommonDataKinds.Phone.DISPLAY_NAME };

	static final String SELECT = "(" + CommonDataKinds.Phone.DISPLAY_NAME
			+ " NOTNULL) AND (" + CommonDataKinds.Phone.DISPLAY_NAME
			+ " != '' ) AND (" + CommonDataKinds.Phone.IN_VISIBLE_GROUP
			+ "=1) AND (" + CommonDataKinds.Phone.HAS_PHONE_NUMBER + "=1)";

	public static final String TRUSTED_CONTACT = "_allowed";
	public static final String DISPLAY_NUMBER = "display_number";

	public AllowListDbCursorLoader(Context context, SQLiteDatabase db,
			String queryConstraint) {
		super(context);

		this.context = context;
		contactsDb = db;
		filter = queryConstraint;
	}

	@Override
	public Cursor loadInBackground() {

		if (filter != null) {
			baseUri = Uri.withAppendedPath(
					CommonDataKinds.Phone.CONTENT_FILTER_URI,
					Uri.encode(filter));
		} else {
			baseUri = CommonDataKinds.Phone.CONTENT_URI;
		}

		Cursor allContacts = context.getContentResolver().query(baseUri,
				PROJECTION, SELECT, null,
				CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC");

		String allowed = "select phone_no, name from allow_list order by name ASC";

		Cursor allowedContacts = contactsDb.rawQuery(allowed, null);

		MatrixCursor mCursor = new MatrixCursor(new String[] {
				CommonDataKinds.Phone._ID, CommonDataKinds.Phone.LOOKUP_KEY,
				CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
				CommonDataKinds.Phone.DISPLAY_NAME,
				CommonDataKinds.Phone.NUMBER, DISPLAY_NUMBER, TRUSTED_CONTACT });

		CursorJoiner cj = new CursorJoiner(allContacts, new String[] {
				CommonDataKinds.Phone.DISPLAY_NAME,
				CommonDataKinds.Phone.NUMBER }, allowedContacts, new String[] {
				"name", "phone_no" });

		ArrayList<String> phones = new ArrayList<String>();
		String storage_number;
		String display_number;

		for (CursorJoiner.Result result : cj) {

			switch (result) {
			case BOTH:
				storage_number = allContacts.getString(allContacts
						.getColumnIndex(CommonDataKinds.Phone.NUMBER));
				display_number = storage_number.replaceAll("\\s+", "");
				display_number = display_number.startsWith("+") == true ? display_number
						.substring(3) : display_number;

				if (phones.contains(display_number)) // Check for possible
														// duplicate entry due to cursor joining
					break;
				mCursor.addRow(new String[] {
						allContacts.getString(allContacts
								.getColumnIndex(CommonDataKinds.Phone._ID)),
						allContacts.getString(allContacts
								.getColumnIndex(CommonDataKinds.Phone.LOOKUP_KEY)),
						allContacts.getString(allContacts
								.getColumnIndex(CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)),
						allContacts.getString(allContacts
								.getColumnIndex(CommonDataKinds.Phone.DISPLAY_NAME)),
						storage_number, display_number, Boolean.toString(true) });
				break;
			case LEFT:
				storage_number = allContacts.getString(allContacts
						.getColumnIndex(CommonDataKinds.Phone.NUMBER));

				display_number = storage_number.replaceAll("\\s+", "");
				display_number = display_number.startsWith("+") == true ? display_number
						.substring(3) : display_number;

				if (phones.contains(display_number)) // Check for possible
														// duplicate entry due to cursor joining
					break;
				mCursor.addRow(new String[] {
						allContacts.getString(allContacts
								.getColumnIndex(CommonDataKinds.Phone._ID)),
						allContacts.getString(allContacts
								.getColumnIndex(CommonDataKinds.Phone.LOOKUP_KEY)),
						allContacts.getString(allContacts
								.getColumnIndex(CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)),
						allContacts.getString(allContacts
								.getColumnIndex(CommonDataKinds.Phone.DISPLAY_NAME)),
						storage_number, display_number, Boolean.toString(false) });
				break;
			default:
				storage_number = allowedContacts.getString(allowedContacts
						.getColumnIndex("phone_no"));

				display_number = storage_number.replaceAll("\\s+", "");
				display_number = display_number.startsWith("+") == true ? display_number
						.substring(3) : display_number;
			}

			phones.add(display_number);
		}

		allContacts.close();
		allowedContacts.close();

		return mCursor;
	}
}
