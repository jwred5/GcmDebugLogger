GcmDebugLogger
==============

Debugging the GCM issue on Gingerbread

This is a generic implementation of an app which will try to connect to GCM, then send logs to a URL which accepts the following POST parameters:
* int build - An integer build id
* String email - A String with all the email addresses on the device, seperated by semicolons
* String success - true/false whether the device successfully connected to GCM
* String identifier - The 64-bit Android identifier of the device
* log - The logcat grabbed from the device after the call to GCM completed/failed

The project omits a couple of key components:
* cacerts - A BKS keystore containing the trusted Self-Signed certificate.  We pointed this at one of our development servers using a self-signed certificate.  
		If you don't need to use https or have a real certficate, you don't need to provide this.  Just set the SELF_SIGNED_DESTINATION flag in MainActivity to false
		For more information on how to put your certificate into a Java keystore, see here: http://stackoverflow.com/questions/6515314/generating-a-bks-keystore-and-storing-app-key
		You will need the BouncyCastleKeystore.jar from http://www.bouncycastle.org/example.html
		We used http://www.bouncycastle.org/download/bcprov-jdk15on-146.jar (For Java 1.5-1.7)
		Newer versions may work but were not tested
* Sender Id - The GCM Sender Id for your project.  You must set up your own Google API project and get a GCM sender id.   See http://developer.android.com/google/gcm/gs.html for instructions.
* url - URL where you want to send the logs to.  We recommend that you use a secured HTTPS connection since you will be sending user data over the internet.
* keystore password - If you are using a self-signed certificate implementation serverside, put the password for the "cacerts" keystore

Setup:

- Put your Sender ID in the GcmRegistration.SENDER_ID constant
- If you are using a self-signed certificate, put it in a BKS keystore and place the keystore in the res/raw folder overwriting 'cacerts'.  Otherwise, set MainActivity.SELF_SIGNED_DESTINATION to false.
- Put the password for the keystore in MainActivity.KEYSTORE_PASSWORD
- Put the URL of where to send the POST request in MainActivity.API_LOCATION