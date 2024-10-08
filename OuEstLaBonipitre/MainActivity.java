package com.bonipitre.ouestlabonipitre;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.DatePicker;
import android.view.View;   // Import the View class
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference mDatabase;
    private ArrayList<MarkerData> allMarkerData = new ArrayList<>();  // List to store all marker data
    private Button btnFilterDate, clearFilterBtn, zoomToLastMarkerBtn, btnCloseApp;
    private LatLng lastPosition;
    private Bitmap markerIcon;
    // ArrayList to store marker coordinates
    private ArrayList<LatLng> markerCoordinates = new ArrayList<>();
    private Polyline polyline;  // Store the reference to the polyline

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Load the custom marker icon in a background thread
        new Thread(() -> {
            try {
                URL url = new URL("https://bonipitre.fr/wp-content/uploads/2024/09/markerboat-1.png");
                Bitmap markerIconOrg = BitmapFactory.decodeStream(url.openStream());

                // Resize the bitmap (e.g., to 80x80 pixels)
                markerIcon = Bitmap.createScaledBitmap(markerIconOrg, 120, 120, false);
            } catch (IOException e) {
                Log.e("MainActivity", "URL icon error", e);
            }
        }).start();

        // Manually initialize Firebase
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }
            Log.d("MainActivity", "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e("MainActivity", "Firebase initialization failed", e);
        }

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference("locations");

        // Initialize the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e("MainActivity", "SupportMapFragment is null");
        }
        // Set up UI elements
        btnFilterDate = findViewById(R.id.btnFilterDate);
        clearFilterBtn = findViewById(R.id.clearFilter);
        zoomToLastMarkerBtn = findViewById(R.id.zoomToLastMarker);
        btnCloseApp = findViewById(R.id.btnCloseApp);

        // Add listeners for buttons (filters)
        clearFilterBtn.setOnClickListener(view -> clearDateFilter());
        btnFilterDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });

        // Inside onCreate
        zoomToLastMarkerBtn.setOnClickListener(view -> {
            if (lastPosition != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, 15));
            }
        });

        // Set up an onClick listener for the Close App button
        btnCloseApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showExitConfirmationDialog();  // Show confirmation dialog before exiting
            }
        });
    }

    // Show a confirmation dialog before closing the application
    private void showExitConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Etes vous sûr de vouloir quitter l'application?");
        builder.setCancelable(true);

        builder.setPositiveButton("Oui", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                finish();  // Close the current activity
                System.exit(0);  // Exit the application
            }
        });

        builder.setNegativeButton("Non", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();  // Cancel the dialog
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // Set map to satellite view
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        // Customize the map (add markers, polyline, etc.)
        listenToFirebaseUpdates();

        // Example of setting initial marker
        LatLng initialPosition = new LatLng(48.8566, 2.3522);  // Example LatLng (Paris)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialPosition, 5));
    }

    // Method to show DatePickerDialog
    private void showDatePickerDialog() {
        // Get the current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Create a new DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                // Get the selected date and apply the filter
                String selectedDate = String.format("%04d/%02d/%02d", year, month + 1, dayOfMonth);
                // Set the button text to the selected date
                btnFilterDate.setText(selectedDate);
                filterMarkersByDate(selectedDate);
            }
        }, year, month, day);

        // Show the DatePickerDialog
        datePickerDialog.show();
    }

    // Filter the markers based on the selected date
    private void filterMarkersByDate(String selectedDate) {
        mMap.clear();  // Clear the map before applying the filter
        ArrayList<LatLng> filteredCoordinates = new ArrayList<>();

        for (MarkerData markerData : allMarkerData) {
//            Log.d("filterMarkersByDate", markerData.getDate() +" choice: "+selectedDate);
            if (markerData.getDate().equals(selectedDate)) {
                LatLng newPosition = new LatLng(markerData.getLatitude(), markerData.getLongitude());
                addMarker(newPosition, markerData.getDate(), markerData.getTime(), markerData.getSpeed(), markerData.getAltitude(), markerData.getPressure());
                filteredCoordinates.add(newPosition);  // Store filtered coordinates
            }
        }
        // Draw polyline for filtered markers
        drawPolyline(filteredCoordinates);
    }

    private void listenToFirebaseUpdates() {
        // Firebase listener to load markers (similar to the child_added listener in your HTML code)
        mDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String previousChildName) {
                MarkerData markerData = dataSnapshot.getValue(MarkerData.class);
                if (markerData != null) {
                    LatLng newPosition = new LatLng(markerData.getLatitude(), markerData.getLongitude());
                    addMarker(newPosition, markerData.getDate(), markerData.getTime(), markerData.getSpeed(), markerData.getAltitude(), markerData.getPressure());
                    markerCoordinates.add(newPosition);
                    drawPolyline(markerCoordinates);  // Draw polyline for all markers
                    allMarkerData.add(markerData);  // Store the data for future filtering
                    lastPosition = newPosition; // Save the last position
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle changes in child nodes if needed
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Handle removal of markers if needed
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle moving of child nodes if needed
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle any errors
                Log.e("Firebase", "Database error: " + error.getMessage());
            }
        });
    }

    private void addMarker(LatLng latLng, String date, String time, double speed, double altitude, double pressure) {
        // Check if the marker icon is already loaded
        String snippetDetails = String.format("Vmoy: %.1f km/h | Alt: %.0f m | Pmer: %.1f hPa", speed, altitude, pressure);
        if (markerIcon != null) {
            // Use the cached marker icon for the marker
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(date + " "+ time)
                    .snippet(snippetDetails)
                    .icon(BitmapDescriptorFactory.fromBitmap(markerIcon))
            );
        } else {
            // If the icon is not yet loaded, add the marker without an icon
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(date + " "+ time)
                    .snippet(snippetDetails)
            );
        }
    }

    private void drawPolyline(ArrayList<LatLng> coordinates) {
        if (polyline != null) {
            polyline.remove();  // Remove the old polyline before drawing a new one
        }
        if (!coordinates.isEmpty()) {
            PolylineOptions polylineOptions = new PolylineOptions().addAll(coordinates).color(0xFFFFFFFF).width(5);
            polyline = mMap.addPolyline(polylineOptions);
        }
    }

    private void clearDateFilter() {
        // Reset the button text to the default value
        btnFilterDate.setText("Date");

        mMap.clear();
        markerCoordinates.clear();
        for (MarkerData markerData : allMarkerData) {
            LatLng latLng = new LatLng(markerData.getLatitude(), markerData.getLongitude());
            addMarker(latLng, markerData.getDate(), markerData.getTime(), markerData.getSpeed(), markerData.getAltitude(), markerData.getPressure());
            markerCoordinates.add(latLng);
        }
        drawPolyline(markerCoordinates);  // Draw polyline for all markers again
    }

}
