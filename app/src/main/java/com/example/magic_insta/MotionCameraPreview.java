package com.example.magic_insta;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class MotionCameraPreview extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private static final String TAG = "MotionPreview";
    private Camera camera;
    private SurfaceHolder holder;
    private byte[] lastFrame;
    private boolean isWaitingToCapture = false;
    private Context context;
    private final Handler handler = new Handler();

    public MotionCameraPreview(Context context) {
        super(context);
        this.context = context;
        holder = getHolder();
        holder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open(findFrontCamera());
        try {
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(this);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int findFrontCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return i;
            }
        }
        return 0; // fallback
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (data == null || isWaitingToCapture) return;

        Camera.Size size = camera.getParameters().getPreviewSize();
        if (lastFrame == null) {
            lastFrame = Arrays.copyOf(data, data.length);
            return;
        }

        int diff = computeFrameDifference(lastFrame, data);
        if (diff > 100000) { // Adjust threshold as needed
            isWaitingToCapture = true;
            Log.d(TAG, "Motion detected! Waiting for stabilization...");

            handler.postDelayed(() -> captureImage(), 400);
        }

        lastFrame = Arrays.copyOf(data, data.length);
    }

    private int computeFrameDifference(byte[] frame1, byte[] frame2) {
        int diff = 0;
        for (int i = 0; i < frame1.length; i += 10) {
            diff += Math.abs((frame1[i] & 0xFF) - (frame2[i] & 0xFF));
        }
        return diff;
    }

    private void captureImage() {
        Log.d(TAG, "Capturing image...");
        camera.takePicture(null, null, (data, camera) -> {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            uploadImage(bitmap);
            camera.startPreview();
            isWaitingToCapture = false;
        });
    }

    private void uploadImage(Bitmap bitmap) {
        Log.d(TAG, "Uploading image to OCR API...");
        new Thread(() -> {
            try {
                String response = OcrUploader.upload(bitmap);
                Log.d(TAG, "OCR Result: " + response);
            } catch (Exception e) {
                Log.e(TAG, "OCR upload failed", e);
            }
        }).start();
    }

    public void releaseCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {}
    @Override public void surfaceDestroyed(SurfaceHolder holder) { releaseCamera(); }
}
