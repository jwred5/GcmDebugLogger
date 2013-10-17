package com.active911.gcmdebuglogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

import android.os.Bundle;
import android.provider.Settings;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener{
	
	private static final String TAG = "MainActivity";
	private static MainActivity _instance;
	private static boolean logsSent = false;
	
	private static final String API_LOCATION = "";
	private static final String KEYSTORE_PASSWORD = "";
	private static final boolean SELF_SIGNED_DESTINATION = true;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		_instance = this;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public void onClick(View v){
		//Let's try GCM
		appendOutput("Calling GCM...");
		logsSent = false;
		GcmRegistration.registerBackground(this);
	}
	@Override
	public void onSaveInstanceState(Bundle savedData){
		savedData.putCharSequence("output", ((TextView) findViewById(R.id.output)).getText());
	}
	@Override
	public void onRestoreInstanceState(Bundle savedData){
		if(savedData.containsKey("output")){
			((TextView) findViewById(R.id.output)).setText(savedData.getCharSequence("output"));
		}
	}
	
	
	public static synchronized void sendLogs(boolean success){
		if(logsSent ){
			return;
		}
		logsSent = true;
		appendOutput("Test Complete.  Sending Logs");
		final HttpClient client;
		
		if(SELF_SIGNED_DESTINATION){
			//Trust our self-signed cert
			KeyStore localTrustStore;
			SchemeRegistry schemeRegistry = new SchemeRegistry();
			try {
				localTrustStore = KeyStore.getInstance("BKS");
				InputStream in = _instance.getResources().openRawResource(R.raw.cacerts);
				localTrustStore.load(in, KEYSTORE_PASSWORD.toCharArray());
	
				schemeRegistry.register(new Scheme("http", PlainSocketFactory
				                .getSocketFactory(), 80));
				SSLSocketFactory sslSocketFactory = new SSLSocketFactory(localTrustStore);
				schemeRegistry.register(new Scheme("https", sslSocketFactory, 443));
			} catch (KeyStoreException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (Exception e) {
				appendOutput("Could not connect to active911 server. Connection not secure.");
				e.printStackTrace();
			} 
			HttpParams p = new BasicHttpParams();
			ClientConnectionManager cm = 
			    new ThreadSafeClientConnManager(p, schemeRegistry);
	
			client = new DefaultHttpClient(cm, p); 
		}
		else{
			client = new DefaultHttpClient();
		}
		final HttpPost post = new HttpPost(API_LOCATION);
		String log = grabLog();
		if(log == null){
			appendOutput("Error trying to send logs: Could not grab logs.  Sending error message instead");
			log = "Error: Could not grab logs";
		}
		

		final List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("build", String.valueOf(android.os.Build.VERSION.SDK_INT)));
		List<String> emails = new ArrayList<String>();
		Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
		Account[] accounts = AccountManager.get(_instance).getAccounts();
		for (Account account : accounts) {
		    if (emailPattern.matcher(account.name).matches()) {
		        emails.add(account.name);
		    }
		}
		String android_id =  Settings.Secure.getString(_instance.getContentResolver(),
				 Settings.Secure.ANDROID_ID); 
		params.add(new BasicNameValuePair("identifier",android_id));
		params.add(new BasicNameValuePair("email", TextUtils.join(";", emails)));
		params.add(new BasicNameValuePair("success", String.valueOf(success)));
		params.add(new BasicNameValuePair("log", log));
		new Thread(new Runnable(){
		
			@Override
			public void run() {
				try {
					
					
					
					Log.d(TAG, "Sending Post Request");
					post.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
					
					HttpResponse response = client.execute(post);
					BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "UTF-8"));
					String r = reader.readLine();
					Log.d(TAG, "Parsing response");
					appendOutput("Response from Server: " + r);
				} catch (Exception e) {
					Log.e(TAG, "Error trying to send request: " + e.toString());
					appendOutput("Error trying to send logs: " + e.toString());
				}
				
			}
		}).start();
	}
	private static String grabLog(){
		try {
			Process process = Runtime.getRuntime().exec("logcat -d -v time");
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			StringBuilder log=new StringBuilder();
			String line = "";
			while ((line = bufferedReader.readLine()) != null) {
				log.append(line + "\n");
			}
			return log.toString();
		} 
		catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	public static void appendOutput(final String string){
		_instance.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				TextView output = (TextView) _instance.findViewById(R.id.output);
				output.append("\n" + string);
			}
		});
	}
	
	private static Timer mTimer;
	
	public static void startTimer(){
		if(mTimer != null){
			cancelTimer();
		}
			
		mTimer = new Timer();
		mTimer.schedule(new TimerTask(){

			@Override
			public void run() {
				Log.e(TAG, "Got no response from sending the old GCM Intent");
				appendOutput("No activity for 30 seconds.  Looks like a GCM failure.");
				sendLogs(false);
			}
			
		}, 30 * 1000);
	}
	
	public static void cancelTimer(){
		Log.i(TAG, "Canceling timer");
		if(mTimer != null){
			mTimer.cancel();
			mTimer.purge();
			mTimer = null;
		}
	}
}
