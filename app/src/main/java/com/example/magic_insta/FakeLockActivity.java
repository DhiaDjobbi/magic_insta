package com.example.magic_insta;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class FakeLockActivity extends Activity {

    private CameraPreview cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Lock screen flags
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        );

        // Dim the brightness
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = 0.0f;
        getWindow().setAttributes(layout);

        // Root layout
        FrameLayout rootLayout = new FrameLayout(this);

        // Add small camera preview container (bottom layer)
        FrameLayout previewContainer = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                dpToPx(200), dpToPx(200)
        );
        params.setMargins(dpToPx(16), dpToPx(16), 0, 0); // top-left
        previewContainer.setLayoutParams(params);
        previewContainer.setBackgroundColor(0xFF222222);
        rootLayout.addView(previewContainer);  // FIRST -> BEHIND

        // Add black full-screen view (top layer)
        View blackView = new View(this);
        blackView.setBackgroundColor(0xFF000000);
        blackView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        rootLayout.addView(blackView);  // LAST -> ON TOP

        setContentView(rootLayout);

        // Hide ActionBar if exists
        if (getActionBar() != null) {
            getActionBar().hide();
        }

        // Attach camera preview
        cameraPreview = new CameraPreview(this);
        previewContainer.addView(cameraPreview);
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (cameraPreview != null) {
            cameraPreview.releaseCamera();
        }
    }

    @Override
    public void onBackPressed() {
        // Disabled
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getBooleanExtra("exit", false)) {
            WindowManager.LayoutParams layout = getWindow().getAttributes();
            layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            getWindow().setAttributes(layout);
            finish();
        }
    }
}
