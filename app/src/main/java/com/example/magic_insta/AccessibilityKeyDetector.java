package com.example.magic_insta;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class AccessibilityKeyDetector extends AccessibilityService {

    private static final String TAG = "AccessKeyDetector";

    @Override
    protected void onServiceConnected() {
        Log.i(TAG, "Accessibility Service Connected");
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {
        // Only process key down events.
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int keyCode = event.getKeyCode();

            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                handleVolumeUp();
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                handleVolumeDown();
            }
        }
        return true;
    }

    private void handleVolumeUp() {
        vibrate();
        Log.d(TAG, "Volume Up pressed - vibration triggered");

        // Close the FakeLockActivity if it’s open
        Intent intent = new Intent(this, FakeLockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("exit", true);
        startActivity(intent);
    }


    private void handleVolumeDown() {
        vibrate();
        Log.d(TAG, "Volume Down pressed - vibration triggered");

        Intent intent = new Intent(this, FakeLockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(100);
            }
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not required for key detection.
    }

    @Override
    public void onInterrupt() {
        // Handle interruption if needed.
    }
}
