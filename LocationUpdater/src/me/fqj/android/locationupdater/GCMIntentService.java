package me.fqj.android.locationupdater;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
//import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.util.concurrent.Semaphore;

/**
 * Created by fqj1994 on 13-8-20.
 */
public class GCMIntentService extends IntentService {

	private Semaphore sem;
	public GCMIntentService() {
        super("GCMIntentService");
    }

    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);
        if (!extras.isEmpty()) {
            if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            	sem = new Semaphore(0);
                updateLocation(intent);
                
            }
        }
        //sendNotification("Received 1 Positioning Request");
        GCMBroadcastReceiver.completeWakefulIntent(intent);
    }

    private LocationManager locationm;

    public void updateLocation(final Intent intent) {
        
      //  Looper.prepare();
        new AsyncTask() {

			@Override
			protected Object doInBackground(Object... arg0) {
				// TODO Auto-generated method stub
				Looper.prepare();
				String provider = LocationManager.NETWORK_PROVIDER;
		        locationm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		        Log.d("LocationUpdater", "req location");
		        locationm.requestLocationUpdates(provider, 100, 0, new LocationListener() {
		            @Override
		            public void onLocationChanged(Location location) {
		                try {
		                    Log.d("LocationUpdater", "got location");
		                    SharedPreferences pref = getSharedPreferences(MainActivity.class.getSimpleName(), MODE_PRIVATE);
		                    String url = pref.getString("me.fqj.android.locationupdater.locreport", "");
		                    if (url != "") {
		                        Log.d("LocationUpdater", "report location");
		                        new DefaultHttpClient().execute(new HttpGet(url + Double.toString(location.getLatitude()) + "," +  Double.toString(location.getLongitude()) + "," + Double.toString(location.getAccuracy())));
		                        Log.d("LocationUpdater", "reported location");
		                    }
		                } catch (IOException e) {
		                    //
		                }
		                locationm.removeUpdates(this);
		                
		                Looper.myLooper().quit();
		                //Looper.myLooper().quit();
		                //Looper.myLooper().quit();
		            }

		            @Override
		            public void onStatusChanged(String s, int i, Bundle bundle) {

		            }

		            @Override
		            public void onProviderEnabled(String s) {
		            }

		            @Override
		            public void onProviderDisabled(String s) {

		            }
		        }, null);
		        Looper.loop();
		        sem.release();
		        return null;
			}
        }.execute(null, null, null);
        
        //Looper.loop();
        //Looper.prepare();
        try {
        	sem.acquire();
        } catch (Exception e) {}
    }


    private void sendNotification(String s) {
        NotificationManager mNotificationManager = (NotificationManager) (this.getSystemService(NOTIFICATION_SERVICE));
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this).setContentTitle(s).setContentText(s).setSmallIcon(R.drawable.ic_launcher);
        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(1, mBuilder.build());
    }
}
