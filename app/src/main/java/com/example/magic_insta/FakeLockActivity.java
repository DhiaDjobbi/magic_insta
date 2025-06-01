package com.example.magic_insta;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public class FakeLockActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Flags to appear over lock screen and keep screen on
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        // Black full-screen view
        View blackView = new View(this);
        blackView.setBackgroundColor(0xFF000000); // Black
        setContentView(blackView);

        // Hide system UI for immersive fullscreen
        blackView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        // Hide ActionBar if exists
        if (getActionBar() != null) {
            getActionBar().hide();
        }

        // Set screen brightness to minimum (dimmest)
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = 0.0f;  // 0 = dimmest
        getWindow().setAttributes(layout);
    }

    @Override
    public void onBackPressed() {
        // Disable back button to mimic lock screen behavior
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null && intent.getBooleanExtra("exit", false)) {
            // Restore brightness to normal before finishing
            WindowManager.LayoutParams layout = getWindow().getAttributes();
            layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE; // Use system default
            getWindow().setAttributes(layout);

            finish();
        }
    }
}
