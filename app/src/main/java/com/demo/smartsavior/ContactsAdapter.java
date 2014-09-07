package com.demo.smartsavior;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.QuickContactBadge;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ContactsAdapter extends SimpleCursorAdapter implements OnClickListener{

	Context context;
	int layoutResourceId;
	ContactVerifyCallback contactVerifyCallback;
	SparseBooleanArray itemCheckedArray;
	Drawable drawableNoBadge;

	public ContactsAdapter(Context context, int layout, MatrixCursor c,
			String[] from, int[] to, int flags, ContactVerifyCallback callbackEvent) {
		super(context, layout, c, from, to, flags);

		this.context = context;
		layoutResourceId = layout;
		contactVerifyCallback = callbackEvent;
		itemCheckedArray = new SparseBooleanArray();
		drawableNoBadge = context.getResources().getDrawable(R.drawable.ic_action_user);
	}

	@Override
	public int getViewTypeCount() {

		if (getCount() != 0)
			return getCount();

		return 1;
	}

	@Override
	public int getItemViewType(int position) {

		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		Cursor cursor = getCursor();

		cursor.moveToPosition(position);

		long id = cursor.getLong(cursor
				.getColumnIndex(CommonDataKinds.Phone._ID));
		String lookupKey = cursor.getString(cursor
				.getColumnIndex(CommonDataKinds.Phone.LOOKUP_KEY));
		
		// returns _ID if BUILD VERSION < HONEYCOMB, returns null if no photo asset found
		String thumbnailUri = cursor.getString(cursor
				.getColumnIndex(CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI));
		String contactName = cursor.getString(cursor
				.getColumnIndex(CommonDataKinds.Phone.DISPLAY_NAME));
		String contactNo = cursor.getString(cursor
				.getColumnIndex(CommonDataKinds.Phone.NUMBER));
		String displayNumber = cursor.getString(cursor
				.getColumnIndex(AllowListDbCursorLoader.DISPLAY_NUMBER));
		boolean isTrusted = Boolean.parseBoolean(cursor.getString(cursor
				.getColumnIndex(AllowListDbCursorLoader.TRUSTED_CONTACT)));

		ViewHolder holder = null;

		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(
					layoutResourceId, parent, false);

			holder = new ViewHolder();

			holder.contactBadge = (QuickContactBadge) convertView
					.findViewById(R.id.qcb);

			holder.cbContact = (CheckBox) convertView
					.findViewById(R.id.checkBox_allow_contact);

			contactVerifyCallback.editTrustedContactList(contactNo, contactName,
					isTrusted);

			// ensures the entire list item is checkable
			holder.cbContact.setOnClickListener(this);
			convertView.setOnClickListener(this);

			holder.tvName = (TextView) convertView
					.findViewById(R.id.textView_contact_name);
			holder.tvPhone = (TextView) convertView
					.findViewById(R.id.textView_contact_phone);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		holder.cbContact.setTag(position);

		holder.tvName.setText(contactName);
		holder.tvPhone.setText(displayNumber);

		Uri contactUri = Contacts.getLookupUri(id, lookupKey);
		holder.contactBadge.assignContactUri(contactUri);

		if (thumbnailUri != null) {
			Bitmap thumbnailImage = loadContactPhotoThumbnail(thumbnailUri);
			holder.contactBadge.setImageBitmap(thumbnailImage);
		}
		else
		{
			holder.contactBadge.setImageDrawable(drawableNoBadge);
		}
		
		holder.cbContact.setEnabled(contactVerifyCallback.callControlOn());
		holder.cbContact.setChecked(itemCheckedArray.get(position, isTrusted));

		if(position % 2 == 0)
			convertView.setBackgroundResource(R.drawable.bg_contact_list_item);
		else
			convertView.setBackgroundResource(R.drawable.bg_contact_list_item_alternative);
		
		return convertView;
	}

	static class ViewHolder {
		QuickContactBadge contactBadge;
		TextView tvName;
		TextView tvPhone;
		CheckBox cbContact;
	}

	/**
	 * Load a contact photo thumbnail and return it as a Bitmap, resizing the
	 * image to the provided image dimensions as needed.
	 * 
	 * @param photoData
	 *            photo ID Prior to Honeycomb, the contact's _ID value. For
	 *            Honeycomb and later, the value of PHOTO_THUMBNAIL_URI.
	 * @return A thumbnail Bitmap, sized to the provided width and height.
	 *         Returns null if the thumbnail is not found.
	 */
	private Bitmap loadContactPhotoThumbnail(String photoData) {
		// Creates an asset file descriptor for the thumbnail file.
		AssetFileDescriptor afd = null;
		// try-catch block for file not found
		try {
			// Creates a holder for the URI.
			Uri thumbUri;
			// If Android 3.0 or later
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				// Sets the URI from the incoming PHOTO_THUMBNAIL_URI
				thumbUri = Uri.parse(photoData);
			} else {
				// Prior to Android 3.0, constructs a photo Uri using _ID
				/*
				 * Creates a contact URI from the Contacts content URI incoming
				 * photoData (_ID)
				 */
				final Uri contactUri = Uri.withAppendedPath(
						Contacts.CONTENT_URI, photoData);
				/*
				 * Creates a photo URI by appending the content URI of
				 * Contacts.Photo.
				 */
				thumbUri = Uri.withAppendedPath(contactUri,
						Contacts.Photo.CONTENT_DIRECTORY);
			}

			/*
			 * Retrieves an AssetFileDescriptor object for the thumbnail URI
			 * using ContentResolver.openAssetFileDescriptor
			 */
			afd = context.getContentResolver().openAssetFileDescriptor(
					thumbUri, "r");
			/*
			 * Gets a file descriptor from the asset file descriptor. This
			 * object can be used across processes.
			 */
			FileDescriptor fileDescriptor = afd.getFileDescriptor();
			// Decode the photo file and return the result as a Bitmap
			// If the file descriptor is valid
			if (fileDescriptor != null) {
				// Decodes the bitmap
				return BitmapFactory.decodeFileDescriptor(fileDescriptor, null,
						null);
			}
			// If the file isn't found
		} catch (FileNotFoundException e) {
			/*
			 * Handle file not found errors
			 */
		}
		// In all cases, close the asset file descriptor
		finally {
			if (afd != null) {
				try {
					afd.close();
				} catch (IOException e) {
				}
			}
		}
		return null;
	}

	@Override
	public void onClick(View v) {
		
		CheckBox cbTrustContact;
		
		if(v instanceof CheckBox)
			cbTrustContact = (CheckBox) v;
		
		else
		{
			cbTrustContact = ((ViewHolder) v.getTag()).cbContact;
			if(cbTrustContact.isEnabled()) cbTrustContact.toggle();
		}

		int pos = (Integer) cbTrustContact.getTag();

		Cursor cursor = getCursor();

		cursor.moveToPosition(pos);

		String contactNo = cursor.getString(cursor
				.getColumnIndex(CommonDataKinds.Phone.NUMBER));
		String contactName = cursor.getString(cursor
				.getColumnIndex(CommonDataKinds.Phone.DISPLAY_NAME));

		itemCheckedArray.put(pos, cbTrustContact.isChecked());

		contactVerifyCallback.editTrustedContactList(contactNo,
				contactName, cbTrustContact.isChecked());
	}
	
	@Override
	public Cursor swapCursor(Cursor c) {
		
		itemCheckedArray.clear();
		return super.swapCursor(c);
	}
}
