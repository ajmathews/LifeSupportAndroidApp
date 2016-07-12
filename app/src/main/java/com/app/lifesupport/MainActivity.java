package com.app.lifesupport;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";
    private static final String STATE_HOME = "HOME";
    private static final String STATE_ROAD = "ROAD";
    private static final String WIFI_HOME = "\"Loading...\"";
    private static final int UPDATE_INTERVAL = 1000;
    private static final int FASTEST_INTERVAL = 1000; // in ms
    private static final int SMALLEST_DISPLACEMENT = 5; // in meters
    private static final String HOME_ADDRESS_LINE = "12303 Coral Reef Drive";

    private static String currentState = "";
    private GoogleApiClient googleApiClient;
    LocationRequest locationRequest;
    LocationListener locationListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Set initial state
        if (wifiManager.getConnectionInfo().getSSID().equals(WIFI_HOME))
            atHome();
        else
            onTheRoad();

        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        locationRequest = new LocationRequest();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_LOW_POWER);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setSmallestDisplacement(SMALLEST_DISPLACEMENT);



        // Get via location manager
//        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                boolean isHome = false;
                logInfo(location.toString());

                Geocoder geocoder;
                List<Address> addresses = new ArrayList<>();
                geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                try {
                    addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                Log.d(TAG, "Wifi ====" + wifiManager.getConnectionInfo().getSSID());

                if ( (wifiManager.getConnectionInfo().getSSID().equals(WIFI_HOME) || addresses.get(0).getAddressLine(0).equals(HOME_ADDRESS_LINE))
                        && currentState.equals(STATE_ROAD) ) {
                    // When you were on the road and you just came back and connected to the Wifi
                    logInfo("Sweet home Alabama");
                    atHome();
                } else if (!wifiManager.getConnectionInfo().getSSID().equals(WIFI_HOME) && currentState.equals(STATE_HOME)) {
                    // When you were at home and left the Wifi
                    logInfo("Riders on the storm");
                    onTheRoad();
                }
            }

        };

        googleApiClient.connect();
    }

    @Override
    protected void onStart() {
        super.onStart();
        logInfo("onStart");

        TextView textView = (TextView) (findViewById(R.id.logView));
        textView.setMovementMethod(new ScrollingMovementMethod());
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // Display the connection status
        // Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            logInfo("You dont have permission");
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, locationListener);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Disconnected. Please re-connect.",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    private void onTheRoad() {
        currentState = STATE_ROAD;
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
            logInfo("Bluetooth has been switched on");
        }
        generateNotification("On the Road!", "Activated");
    }

    private void atHome() {
        currentState = STATE_HOME;
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            logInfo("Bluetooth has been switched off");
        }
        generateNotification("At Home!", "Activated");
    }

    private void generateNotification(String title, String text) {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        // FLAG_CANCEL_CURRENT makes sure the activity is on re-created
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentIntent(intent)
                        .setContentText(text);
//                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, mBuilder.build());

    }

    private void logInfo(String msg) {
        Log.i(TAG, msg);
        TextView textView = (TextView) findViewById(R.id.logView);
        Calendar cal = new GregorianCalendar(TimeZone.getDefault());
        StringBuilder stringBuilder = new StringBuilder();

        Date date = cal.getTime();
        SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        stringBuilder.append(textView.getText());
        stringBuilder.append("\n");
        stringBuilder.append(format1.format(date));
        stringBuilder.append(" : ");
        stringBuilder.append(msg);
        textView.setText(stringBuilder.toString());
    }

}
