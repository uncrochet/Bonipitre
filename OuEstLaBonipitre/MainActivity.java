package com.example.ouestlabonipitre;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private DatabaseReference mDatabase;
    private Button applyFilterBtn, clearFilterBtn, zoomToLastMarkerBtn;
    private EditText dateFilterInput;
    private LatLng lastPosition;
    private Bitmap markerIcon;
    // ArrayList to store marker coordinates
    private ArrayList<LatLng> markerCoordinates = new ArrayList<>();

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
        applyFilterBtn = findViewById(R.id.applyFilter);
        clearFilterBtn = findViewById(R.id.clearFilter);
        dateFilterInput = findViewById(R.id.dateFilter);
        zoomToLastMarkerBtn = findViewById(R.id.zoomToLastMarker);

        // Add listeners for buttons (filters)
        applyFilterBtn.setOnClickListener(view -> applyDateFilter());
        clearFilterBtn.setOnClickListener(view -> clearDateFilter());

        // Inside onCreate
        zoomToLastMarkerBtn.setOnClickListener(view -> {
            if (lastPosition != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPosition, 15));
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        // Enable zoom controls
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // Set map to satellite view
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        // Customize the map (add markers, polyline, etc.)
        loadMarkersFromFirebase();
    }

    private void loadMarkersFromFirebase() {
        // Firebase listener to load markers (similar to the child_added listener in your HTML code)
        mDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                double lat = snapshot.child("latitude").getValue(Double.class);
                double lng = snapshot.child("longitude").getValue(Double.class);
                String locationId = snapshot.getKey();

                lastPosition = new LatLng(lat, lng); // Save the last position
                // Add marker on the map
                addMarker(lat, lng, locationId);
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

    private void addMarker(double lat, double lng, String locationId) {
        LatLng position = new LatLng(lat, lng);
        // Check if the marker icon is already loaded
        if (markerIcon != null) {
            // Use the cached marker icon for the marker
            mMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title("Location: " + locationId)
                    .icon(BitmapDescriptorFactory.fromBitmap(markerIcon))
            );
        } else {
            // If the icon is not yet loaded, add the marker without an icon
            mMap.addMarker(new MarkerOptions().position(position).title("Location: " + locationId));
        }
        // Store marker coordinates
        markerCoordinates.add(position);

        // Draw the polyline
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(markerCoordinates)
                .color(Color.WHITE)
                .width(5);
        mMap.addPolyline(polylineOptions);
    }

//    private void addMarker(double lat, double lng, String locationId) {
        // Add markers to Google Map, similar to your HTML's addMarker function
//        LatLng position = new LatLng(lat, lng);
//        mMap.addMarker(new MarkerOptions().position(position).title("Location: " + locationId));
//    }

    private void applyDateFilter() {
        String selectedDate = dateFilterInput.getText().toString();

        // Remove all current markers
        mMap.clear();

        // Add only the markers that match the selected date
        mDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                String locationId = snapshot.getKey();
                String markerDate = locationId.split("_")[0];  // Extract date from locationId

                if (markerDate.equals(selectedDate)) {
                    double lat = snapshot.child("latitude").getValue(Double.class);
                    double lng = snapshot.child("longitude").getValue(Double.class);
                    addMarker(lat, lng, locationId);
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle changes in the marker data (optional)
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Handle removal of markers (optional)
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Handle marker moves (optional)
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle errors
                Log.e("DateFilter", "Filter error: " + error.getMessage());
            }
        });
    }

    private void clearDateFilter() {
        // Clear the filter and show all markers again
    }
}
