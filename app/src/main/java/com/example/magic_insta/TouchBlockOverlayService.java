package com.example.magic_insta;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class TouchBlockOverlayService extends Service {
    private static final String TAG = "TouchBlockOverlay";
    
    private WindowManager windowManager;
    private View overlayView;
    
    public static final String ACTION_SHOW_OVERLAY = "SHOW_OVERLAY";
    public static final String ACTION_HIDE_OVERLAY = "HIDE_OVERLAY";
    
    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        createOverlayView();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_SHOW_OVERLAY.equals(action)) {
                showOverlay();
            } else if (ACTION_HIDE_OVERLAY.equals(action)) {
                hideOverlay();
            }
        }
        return START_STICKY;
    }
      private void createOverlayView() {
        // Create a FrameLayout that will serve as the overlay
        FrameLayout overlayLayout = new FrameLayout(this);
        
        // Set 90% black
        overlayLayout.setBackgroundColor(Color.parseColor("#E6000000"));

        // Set up touch interceptor to block all touches
        overlayLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Consume all touch events to prevent them from reaching the underlying apps
                Log.d(TAG, "Touch event blocked during automation");
                return true; // Return true to consume the event
            }
        });
        
        overlayView = overlayLayout;
    }
    
    private void showOverlay() {
        if (overlayView == null || overlayView.getParent() != null) {
            Log.d(TAG, "Overlay already showing or view is null");
            return;
        }
        
        try {            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                    WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );
            
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 0;
            params.y = 0;
            
            windowManager.addView(overlayView, params);
            Log.d(TAG, "Touch blocking overlay shown");
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing overlay: " + e.getMessage());
        }
    }
    
    private void hideOverlay() {
        if (overlayView != null && overlayView.getParent() != null) {
            try {
                windowManager.removeView(overlayView);
                Log.d(TAG, "Touch blocking overlay hidden");
            } catch (Exception e) {
                Log.e(TAG, "Error hiding overlay: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        hideOverlay();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
