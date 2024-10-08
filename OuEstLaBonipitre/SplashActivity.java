package com.bonipitre.ouestlabonipitre;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);  // Set the splash screen layout

        // Set the disclaimer text
        TextView disclaimerText = findViewById(R.id.disclaimerText);
        disclaimerText.setText("Disclaimer: Si les positions ne sont plus à jour, ne paniquez pas, le bateau n'a pas coulé! :)\n\n" +
                "Les positions sont envoyées via internet depuis le téléphone de David toutes les 30 minutes. Le téléphone peut tomber à l'eau, en panne, ne pas avoir accès à internet...\n\n" +
                "En cas de pépin, nous avons un téléphone Satellite Iridium pour contacter les secours.");

        // Delay for 5 seconds (3000 milliseconds)
        new Handler().postDelayed(() -> {
            // Start the main activity
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();  // Close the SplashActivity so it cannot be returned to
        }, 5000);  // Delay in milliseconds (3000ms = 3 seconds)
    }
}
