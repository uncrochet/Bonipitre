package com.example.positionapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    public static final String ACTION_UPDATE_LOCATION = "com.example.positionapp.ACTION_UPDATE_LOCATION";
    public static final String EXTRA_LOCATION = "location";
    public static final String EXTRA_ALTITUDE = "altitude";
    public static final String EXTRA_PRESSURE = "pressure";
    public static final String EXTRA_STATUS = "status";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    public static final String ACTION_REQUEST_STATUS = "com.example.positionapp.ACTION_REQUEST_STATUS";
    public static final String ACTION_UPDATE_INTERVAL = "com.example.positionapp.ACTION_UPDATE_INTERVAL";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private int locationUpdateInterval = 1800; // Default interval
    private double lastTime = 0, currentTime = 0, lastLatitude = 0, lastLongitude = 0, lastAltitude=0;
    private float lastPressure=0;
    private String lastHour;
    private Handler handler = new Handler(Looper.getMainLooper());
    private DatabaseReference database;

    private SensorManager sensorManager;
    private Sensor pressureSensor;
    private float pressureValue = 0.0f;  // Store the current pressure value

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private BroadcastReceiver intervalUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
 //           Log.d(TAG, "onReceive "+action);
            if (action != null) {
                if (action.equals(ACTION_UPDATE_INTERVAL)) {
                    int newInterval = intent.getIntExtra("locationUpdateInterval", locationUpdateInterval);
                    updateLocationRequestInterval(newInterval);
                } else if (action.equals(ACTION_REQUEST_STATUS)) {
                    // Send the current location and status
                    sendCurrentStatus();
                }
            }
        }
    };

    private void sendCurrentStatus() {
        // Assume you have a variable holding the latest location, e.g., lastLocation
//        Log.d(TAG, "sendCurrentStatus");
        if (lastLatitude != 0) {
            Intent intent = new Intent(ACTION_UPDATE_LOCATION);
            intent.putExtra(EXTRA_LOCATION, lastLatitude + ", " + lastLongitude);
            intent.putExtra(EXTRA_STATUS, "Firebase successful " + lastHour); // Or any other status
            intent.putExtra(EXTRA_ALTITUDE, lastAltitude + " m" ); // Or any other status
            intent.putExtra(EXTRA_PRESSURE, lastPressure + " hPa"); // Or any other status
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }
    public void onCreate() {
        super.onCreate();
        // Register the BroadcastReceiver for interval updates and status requests
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_INTERVAL);
        filter.addAction(ACTION_REQUEST_STATUS);
        LocalBroadcastManager.getInstance(this).registerReceiver(intervalUpdateReceiver, filter);

        // Initialize Sensor Manager and register pressure sensor listener
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);

        if (pressureSensor != null) {
            sensorManager.registerListener(pressureListener, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Start location updates with default interval
        startLocationUpdates(locationUpdateInterval);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        startForeground(1, buildNotification());

        // Initialize Firebase Realtime Database
        database = FirebaseDatabase.getInstance().getReference("locations");

        Log.d(TAG, "Firebase initialized and ready to use");

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        int interval = intent.getIntExtra("locationUpdateInterval", locationUpdateInterval); // Default is 10 seconds
        startLocationUpdates(interval);
        return START_STICKY;
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE // Ajoutez ce flag pour Android 12+
        );
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service de localisation")
                .setContentText("Récupération de la position GPS")
                .setSmallIcon(R.drawable.ic_place)
                .setContentIntent(pendingIntent)
                .build();
    }

    // Pressure sensor listener to update the pressure value in real time
    private final SensorEventListener pressureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
                pressureValue = event.values[0];  // Capture the current pressure value
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Handle accuracy changes if necessary
        }
    };

    private void startLocationUpdates(int interval) {
        // Use LocationRequest.Builder for the new API
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval*1000L)
                .setMinUpdateIntervalMillis(interval * 1000L) // Minimum time interval between location updates
                .build();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateLocationRequestInterval(int newInterval) {
        locationUpdateInterval = newInterval;
        Log.d(TAG, "Updated interval to: " + newInterval);
        // Re-start location updates with the new interval
        fusedLocationClient.removeLocationUpdates(locationCallback);
        startLocationUpdates(locationUpdateInterval);
    }

    // Method to calculate sea-level pressure based on altitude
    private float calculateSeaLevelPressure(float measuredPressure, double altitude) {
        return (float) (Math.floor((measuredPressure * Math.pow(1 - (altitude / 44330.0), -5.255)) * 10) / 10.0);
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult == null) {
                return;
            }
            for (Location location : locationResult.getLocations()) {
                if (location != null) {
                    // Here you capture the current pressure value from the sensor
                    float currentPressure = pressureValue;
                    double altitude = location.getAltitude();  // Get the altitude in meters
                    // Calculate sea-level pressure using the measured pressure and altitude
                    float seaLevelPressure = calculateSeaLevelPressure(currentPressure, altitude);

                    sendInfoBroadcast(location.getLatitude() + ", " + location.getLongitude(),  Math.floor(altitude), seaLevelPressure);
                    sendInfoToFirebase(location.getLatitude(), location.getLongitude(), altitude, seaLevelPressure);
                }
            }
        }
    };

    private String getMetaDataValue(String name) {
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            return appInfo.metaData.getString(name);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Location data model class
    public static class LocationData {
        public double latitude;
        public double longitude;
        public double altitude;
        public double timestamp;
        public double speed;
        public double pressure;

        // Empty constructor required for Firebase
        public LocationData() {
        }

        public LocationData(double latitude, double longitude, double altitude, double timestamp, double speed, float pressureValue) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.altitude = altitude;
            this.timestamp = timestamp;
            this.speed = speed;
            this.pressure = pressureValue;  // Initialize pressure field
        }
    }
    // Function to send location data to Firebase
    private void sendInfoToFirebase(double latitude, double longitude, double altitude, float pressure) {
        // Create a unique ID for the location update (e.g., using a timestamp)
        currentTime = System.currentTimeMillis();
        String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String currentHour = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        double speed = 0;
        float[] results = new float[1];
        // Calculate the distance in meters
        Location.distanceBetween(lastLatitude, lastLongitude, latitude, longitude, results);
        float distanceInMeters = results[0]; // Distance in meters
        if( currentTime > lastTime) {
            speed = Math.floor(3.6 * distanceInMeters * 1000 /(currentTime - lastTime) * 10) / 10.0;
        }
        String locationId = currentDate + "_" + currentHour ;

        // Create a map to store the location data
        LocationData locationData = new LocationData(latitude, longitude, altitude, currentTime, speed, pressure);

        Log.d(TAG, "sendLocationToFirebase:"+locationId + ", pressure:"+pressure);

        // Store the location data under a unique node in Firebase
        database.child(locationId).setValue(locationData).addOnSuccessListener(aVoid -> {
                    // Successfully sent location
                    Log.d(TAG, "Location sent to Firebase successfully! "+currentHour);
                    sendStatusBroadcast("Firebase successful "+currentHour);
                }).addOnFailureListener(e -> {
                    // Failed to send location
                    Log.e(TAG, "Failed to send location to Firebase! "+currentHour);
                    sendStatusBroadcast("Firebase failed "+currentHour);
                });
        lastTime = currentTime;
        lastLatitude = latitude;
        lastLongitude = longitude;
        lastAltitude = altitude;
        lastPressure = pressure;
        lastHour = currentHour;

    }

    private void sendInfoBroadcast(String coordinates, double altitude, float pressure) {
        Intent intent = new Intent(ACTION_UPDATE_LOCATION);
        intent.putExtra(EXTRA_LOCATION, coordinates);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        intent.putExtra(EXTRA_ALTITUDE, String.valueOf(altitude));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        intent.putExtra(EXTRA_PRESSURE, String.valueOf(pressure));
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendStatusBroadcast(String status) {
        Intent intent = new Intent(ACTION_UPDATE_LOCATION);
        intent.putExtra(EXTRA_STATUS, status);
//        Log.d(TAG, "sendStatusBroadcast");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
        handler.removeCallbacksAndMessages(null);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(intervalUpdateReceiver);
        if (sensorManager != null) {
            sensorManager.unregisterListener(pressureListener);
        }
    }

    private void createNotificationChannel() {
        String channelId = "LocationServiceChannel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Location Service")
                .setContentText("Location updates are running in the background.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification); // Start the service in the foreground
    }
}
