package com.active911.gcmdebuglogger;

import java.io.IOException;
import java.util.Date;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmRegistration {

	private static final String TAG = "GcmRegistration";
	
	private static final String SENDER_ID = "";
	private static final long REGISTRATION_EXPIRY_TIME_MS = 365 * 24 * 3600 * 1000;
	private static final String PROPERTY_REG_ID = "registration_id";

	private static final String PROPERTY_APP_VERSION = "app_version";

	private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "server_expire";
	
	/**
	 * Gets the current registration id for application on GCM service.
	 * <p>
	 * If result is empty, the registration has failed.
	 *
	 * @return registration id, or empty string if the registration is not
	 *         complete.
	 */
	public static String getRegistrationId(SharedPreferences prefs, Context context) {
		String regId = prefs.getString(PROPERTY_REG_ID, "");
	    if (regId.length() == 0) {
	        Log.v(TAG, "Registration not found.");
	        return "";
	    }
	    // check if app was updated; if so, it must clear registration id to
	    // avoid a race condition if GCM sends a message
	    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int currentVersion = getAppVersion(context);
	    if (registeredVersion != currentVersion || isRegistrationExpired(prefs)) {
	        Log.v(TAG, "App version changed or registration expired.");
	        return "";
	    }
	    return regId;
	}
	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
	    try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}
	/**
	 * Checks if the registration has expired.
	 *
	 * <p>To avoid the scenario where the device sends the registration to the
	 * server but the server loses it, the app developer may choose to re-register
	 * after REGISTRATION_EXPIRY_TIME_MS.
	 *
	 * @return true if the registration has expired.
	 */
	private static boolean isRegistrationExpired(SharedPreferences prefs) {
	    // checks if the information is not stale
	    long expirationTime =
	    		prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
	    return System.currentTimeMillis() > expirationTime;
	}
	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration id, app versionCode, and expiration time in the 
	 * application's shared preferences.
	 * @param registrationReceiver 
	 */
	public static void registerBackground(final Context context) {
	    new Thread(new Runnable(){

	        private GoogleCloudMessaging mGcm;
	    	private static final String TAG = "Active911Service-GCMThread";
	    	
			@Override
			public void run() {
			    Log.v(TAG, "Starting Background Task");
                if (mGcm == null) {
                	Log.v(TAG, "Getting GCM Instance");
                    mGcm = GoogleCloudMessaging.getInstance(context);
                }
                else
                	Log.v(TAG, "Have a GCM Instance");
                	
            	Log.v(TAG, "Registering...");
            	try{
            		//Try the standard GCM registration.  Requires Google Play 3.1 or above
	                String regid = mGcm.register(SENDER_ID);
	                Log.v(TAG, "Device registered, registration id=" + regid);
	                
	                // Save the regid - no need to register again.
	                setRegistrationId(context, regid);
	                
	                MainActivity.appendOutput("Successfully set registration id to " + regid);

	                MainActivity.sendLogs(true);
            	} catch (IOException ex) {
	                Log.w(TAG, "GCM registration failed.  Trying manual intent for older version support");
	                MainActivity.appendOutput("GCM registration failed.  Trying manual intent for older version support. Please wait up to 30 seconds.");
	                
	                //Registration failed.
	                //Try old school registration.  Let the Receiver handle the response

	                try{
		                Intent registrationIntent = new Intent("com.google.android.c2dm.intent.REGISTER");
		                registrationIntent.putExtra("app", PendingIntent.getBroadcast(context, 0, new Intent(), 0));
		                registrationIntent.putExtra("sender", SENDER_ID);
		                context.startService(registrationIntent);
		                MainActivity.startTimer();
	                }catch(Exception e){
		                MainActivity.appendOutput("Could not use old method.  GCM Failed.");
		                MainActivity.sendLogs(false);
	                }
	            }
			}
	    }).start();
	}

	/**
	 * Stores the registration id, app versionCode, and expiration time in the
	 * application's {@code SharedPreferences}.
	 *
	 * @param context application's context.
	 * @param regId registration id
	 */
	public static void setRegistrationId(Context context, String regId){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.v(TAG, "Saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regId);
	    Log.v(TAG, "Setting registration id to " +
	    		regId);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    long expirationTime = System.currentTimeMillis() + REGISTRATION_EXPIRY_TIME_MS;

	    Log.v(TAG, "Setting registration expiry time to " +
	            new Date(expirationTime));
	    editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
	    editor.commit();
	    
	}

}
