package com.example.magic_insta;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class CardDetectionUploader {
    private static final String TAG = "CardDetection";
    private static final String API_URL = "https://serverless.roboflow.com/playing-cards-ow27d/4";
    private static final String API_KEY = "0QhSmtqbPLqHHK4bd6BN";
    private static final double CONFIDENCE_THRESHOLD = 0.5;
    
    public static class DetectionResult {
        public boolean hasCard;
        public String cardClass;
        public double confidence;
        
        public DetectionResult(boolean hasCard, String cardClass, double confidence) {
            this.hasCard = hasCard;
            this.cardClass = cardClass;
            this.confidence = confidence;
        }
    }
    
    public static DetectionResult detectCard(Bitmap bitmap) throws Exception {
        String fullUrl = API_URL + "?api_key=" + API_KEY;
        URL url = new URL(fullUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);

        // Convert bitmap to base64 (without data URI prefix)
        String base64Image = encodeToBase64(bitmap);

        // Send the base64 image directly as body
        OutputStream os = conn.getOutputStream();
        os.write(base64Image.getBytes());
        os.flush();
        os.close();

        // Read response
        Scanner in = new Scanner(conn.getInputStream());
        StringBuilder result = new StringBuilder();
        while (in.hasNextLine()) {
            result.append(in.nextLine());
        }
        in.close();
        
        String response = result.toString();
        Log.d(TAG, "API Response: " + response);
        
        return parseResponse(response);
    }
    
    private static DetectionResult parseResponse(String jsonResponse) throws Exception {
        JSONObject jsonObject = new JSONObject(jsonResponse);
        JSONArray predictions = jsonObject.getJSONArray("predictions");
        
        if (predictions.length() == 0) {
            Log.d(TAG, "No cards detected in image");
            return new DetectionResult(false, null, 0.0);
        }
        
        // Get the first prediction with confidence above threshold
        for (int i = 0; i < predictions.length(); i++) {
            JSONObject prediction = predictions.getJSONObject(i);
            double confidence = prediction.getDouble("confidence");
            String cardClass = prediction.getString("class");

            if (confidence >= CONFIDENCE_THRESHOLD) {
                Log.d(TAG, "Card detected - Class: " + cardClass + ", Confidence: " + confidence);
                return new DetectionResult(true, cardClass, confidence);
            }
        }

        Log.d(TAG, "Cards detected but confidence below threshold");
        return new DetectionResult(false, null, 0.0);
    }

    private static String encodeToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }
}