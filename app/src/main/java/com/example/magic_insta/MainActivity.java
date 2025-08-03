package com.example.magic_insta;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "MagicInstaPrefs";
    private static final String FAKE_LOCKSCREEN_KEY = "fake_lockscreen_enabled";
    private static final String INSTAGRAM_MANAGER_KEY = "instagram_manager_enabled";
    
    private CheckBox checkboxFakeLockscreen;
    private CheckBox checkboxInstagramManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (!isMyAccessibilityServiceEnabled()) {
            openAccessibilitySettings(null);
        }
        
        // Check for overlay permission (required for touch blocking)
        if (!canDrawOverlays()) {
            requestOverlayPermission();
        }
        
        checkboxFakeLockscreen = findViewById(R.id.checkbox_fake_lockscreen);
        checkboxInstagramManager = findViewById(R.id.checkbox_instagram_manager);
        
        // Load the saved states
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFakeLockscreenEnabled = prefs.getBoolean(FAKE_LOCKSCREEN_KEY, false);
        boolean isInstagramManagerEnabled = prefs.getBoolean(INSTAGRAM_MANAGER_KEY, false);
        
        checkboxFakeLockscreen.setChecked(isFakeLockscreenEnabled);
        checkboxInstagramManager.setChecked(isInstagramManagerEnabled);
        
        // Set up checkbox listeners
        checkboxFakeLockscreen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(FAKE_LOCKSCREEN_KEY, isChecked);
                editor.apply();
            }
        });
        
        checkboxInstagramManager.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(INSTAGRAM_MANAGER_KEY, isChecked);
                editor.apply();
            }
        });
    }
    
    private boolean isMyAccessibilityServiceEnabled() {
        String service = getPackageName() + "/" + AccessibilityKeyDetector.class.getName();
        String enabledServices = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.contains(service);
    }
    
    public void openAccessibilitySettings(View view) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    
    private boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true; // Pre-Marshmallow devices don't need this permission
    }
    
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
    
    // Static method to check if fake lockscreen is enabled
    public static boolean isFakeLockscreenEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        return prefs.getBoolean(FAKE_LOCKSCREEN_KEY, false);
    }
    
    // Static method to check if instagram manager is enabled
    public static boolean isInstagramManagerEnabled(android.content.Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE);
        return prefs.getBoolean(INSTAGRAM_MANAGER_KEY, false);
    }
}