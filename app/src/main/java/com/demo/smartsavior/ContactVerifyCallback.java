package com.demo.smartsavior;


public interface ContactVerifyCallback {

	void editTrustedContactList(String contactNo, String contactName, boolean trusted);
	boolean callControlOn();
}
