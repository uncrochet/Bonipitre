package com.example.positionapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private EditText intervalInput;
    private Button setIntervalButton;
    private int locationUpdateInterval = 1800; // Default interval in seconds

    private TextView gpsCoordinatesTextView;
    private TextView gpsAltitudeTextView;
    private TextView pressureTextView;
    private TextView sendStatusTextView;

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Log.d(TAG, "Broadcast received");
            if (LocationService.ACTION_UPDATE_LOCATION.equals(intent.getAction())) {
                if (intent.hasExtra(LocationService.EXTRA_LOCATION)) {
                    String coordinates = intent.getStringExtra(LocationService.EXTRA_LOCATION);
//                    Log.d("LocationReceiver", "Coordinates received: " + coordinates);
                    updateGPSCoordinates(coordinates);
                }
                if (intent.hasExtra(LocationService.EXTRA_STATUS)) {
                    String status = intent.getStringExtra(LocationService.EXTRA_STATUS);
//                    Log.d("LocationReceiver", "Status received: " + status);
                    updateSendStatus(status);
                }
                if (intent.hasExtra(LocationService.EXTRA_ALTITUDE)) {
                    String altitude = intent.getStringExtra(LocationService.EXTRA_ALTITUDE);
//                    Log.d("LocationReceiver", "Altitude received: " + altitude);
                    updateAltitude(altitude);
                }
                if (intent.hasExtra(LocationService.EXTRA_PRESSURE)) {
                    String pressure = intent.getStringExtra(LocationService.EXTRA_PRESSURE);
//                    Log.d("LocationReceiver", "pressure received: " + pressure);
                    updatePressure(pressure);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        gpsCoordinatesTextView = findViewById(R.id.gps_coordinates);
        gpsAltitudeTextView = findViewById(R.id.gps_altitude);
        pressureTextView = findViewById(R.id.pressure);
        sendStatusTextView = findViewById(R.id.send_status);

        // Initialize the UI elements
        intervalInput = findViewById(R.id.intervalInput);
        intervalInput.setText(String.valueOf(locationUpdateInterval));
        setIntervalButton = findViewById(R.id.setIntervalButton);
// Handle setting the interval when the button is clicked
        setIntervalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String intervalText = intervalInput.getText().toString();
                if (!TextUtils.isEmpty(intervalText)) {
                    try {
                        int newInterval = Integer.parseInt(intervalText);
                        if (newInterval > 0) {
                            locationUpdateInterval = newInterval;
                            setLocationUpdateInterval(locationUpdateInterval);
                            Toast.makeText(MainActivity.this, "Interval set to " + newInterval + " s", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Please enter a valid number greater than 0", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Invalid input. Please enter a number.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Initialize the "Hide App" button
        Button hideAppButton = findViewById(R.id.hideAppButton);
        hideAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveAppToBackground(); // Method to move the app to the background
            }
        });

        Button stopServiceButton = findViewById(R.id.stopServiceButton);
        stopServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationService(); // Method to stop the location service
                finish(); // Closes the MainActivity
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.FOREGROUND_SERVICE_LOCATION
                    }, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startLocationService();
        }
    }

    private void moveAppToBackground() {
        moveTaskToBack(true); // This moves the app to the background
    }

    // Method to pass the new interval to the LocationService
    private void setLocationUpdateInterval(int interval) {
        Log.d(TAG, "Setting new interval: " + interval);
        Intent intent = new Intent(LocationService.ACTION_UPDATE_INTERVAL);
        intent.putExtra("locationUpdateInterval", interval);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void startLocationService() {
        Log.d(TAG, "startLocationService");
        Intent intent = new Intent(this, LocationService.class);
        ContextCompat.startForegroundService(this, intent);
    }

    private void stopLocationService() {
        Log.d(TAG, "Stopping LocationService");
        Intent intent = new Intent(this, LocationService.class);
        stopService(intent);
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "Registering BroadcastReceiver");
        IntentFilter filter = new IntentFilter(LocationService.ACTION_UPDATE_LOCATION);
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, filter);

        // Request the current status from LocationService
        requestCurrentStatus();

    }

    private void requestCurrentStatus() {
        // Send a broadcast to LocationService asking for the current location and status
        Intent intent = new Intent(LocationService.ACTION_REQUEST_STATUS);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("MainActivity", "Unregistering BroadcastReceiver");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver);
    }

    public void updateGPSCoordinates(String coordinates) {
//        Log.d(TAG, "updateGPSCoordinates");
        gpsCoordinatesTextView.setText("Position GPS : " + coordinates);
    }

    public void updateSendStatus(String status) {
//        Log.d(TAG, "updateSendStatus");
        sendStatusTextView.setText("Statut de l'envoi : " + status);
    }

    public void updateAltitude(String altitude) {
        gpsAltitudeTextView.setText("Altitude GPS : " + altitude);
    }
    public void updatePressure(String pressure) {
        pressureTextView.setText("Pressure : " + pressure);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                Log.d("onRequestPermissionsResult", "Permission de localisation refusée.");
                updateGPSCoordinates("Permission de localisation refusée.");
            }
        }
    }
}
