package com.active911.gcmdebuglogger;

import java.util.Set;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
/*
 * Receiver for handling incoming GCM messages
 */
public class GcmBroadcastReceiver extends BroadcastReceiver {
	
	static final String TAG = "GcmBroadcastReceiver";
    private NotificationManager mNotificationManager;
    private Context mContext;
    
    @Override
    public void onReceive(Context context, Intent intent) {
    	MainActivity.cancelTimer();
    	MainActivity.appendOutput("Got a response from GCM, could be an error...");
    	Set<String> keys = intent.getExtras().keySet();
    	Log.i(TAG, "Received intent with keys:" + keys.toString());
    	for(String s:keys){
    		try{
    		Log.i(TAG, s + ": " + intent.getStringExtra(s));
    		if(s.equals("error")){
    			MainActivity.appendOutput("Yep, it's an error: " + intent.getStringExtra("error"));
    		}
    		}catch(Exception e){
    			//e.printStackTrace();
    		}
    	}
    	//Hack to support old registration
    	String regId = intent.getExtras().getString("registration_id");
    	if(regId!=null && !regId.equals("")){
    		Log.i(TAG, "Received Registration Id: " + regId);
    		GcmRegistration.setRegistrationId(context, regId);
    		MainActivity.sendLogs(true);
    	}
    	else{
    		MainActivity.sendLogs(false);
    	}
    }
}
