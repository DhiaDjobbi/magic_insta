package com.example.magic_insta;

import android.graphics.Bitmap;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class OcrUploader {

    public static String upload(Bitmap bitmap) throws Exception {
        URL url = new URL("https://b8c7-2c0f-f698-c503-9980-50e9-f463-1820-3bab.ngrok-free.app/ocr"); // Replace with your API
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String base64Image = encodeToBase64(bitmap);
        String payload = "{\"image\":\"" + base64Image + "\"}";

        OutputStream os = conn.getOutputStream();
        os.write(payload.getBytes());
        os.flush();
        os.close();

        Scanner in = new Scanner(conn.getInputStream());
        StringBuilder result = new StringBuilder();
        while (in.hasNextLine()) {
            result.append(in.nextLine());
        }
        in.close();
        return result.toString();
    }

    private static String encodeToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    }
}
