package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.gson.Gson;
import java.util.List;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.Manifest;
import android.content.pm.PackageManager;
import org.json.JSONArray;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CameraActivity extends AppCompatActivity {
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    public static final String SUPABASE_URL = "https://seuhclncvgbqbxmsspdd.supabase.co"; // ✅ Replace with your Supabase Project URL
    public static final String SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InNldWhjbG5jdmdicWJ4bXNzcGRkIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDMwNTk1NDQsImV4cCI6MjA1ODYzNTU0NH0.tkQm0uNhvpN1cxA-N_V_gbgigYoUesSKNacb9woZh7s"; // ✅ Replace with your Anon Key

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private boolean isScanned = false; // Prevent multiple scans

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                androidx.camera.core.Preview preview = new androidx.camera.core.Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
            } catch (Exception e) {
                Log.e("CameraX", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(ImageProxy imageProxy) {
        if (isScanned) {
            imageProxy.close();
            return;
        }

        @SuppressWarnings("UnsafeOptInUsageError")
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        BarcodeScanner scanner = BarcodeScanning.getClient();

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String qrCodeValue = barcode.getRawValue();
                        if (qrCodeValue != null) {
                            Log.d("QR Scan Debug", "Scanned QR Code: " + qrCodeValue);
                            isScanned = true;
                            checkQRInDatabase(qrCodeValue);  // ✅ Check if QR exists in Supabase
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("QR Scan", "QR code scanning failed", e))
                .addOnCompleteListener(task -> imageProxy.close());
    }

    private void checkQRInDatabase(String scannedLink) {
        Log.d("Supabase Debug", "checkQRInDatabase called with: " + scannedLink);
        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                String tableName = "qrcodes";
                String columnName = "link";

                // Encode scannedLink to be safe
//                String encodedLink = java.net.URLEncoder.encode(scannedLink, "UTF-8");

                Request request = new Request.Builder()
                        .url(SUPABASE_URL + "/rest/v1/" + tableName + "?select=*&" + columnName + "=eq." + scannedLink)
                        .addHeader("apikey", SUPABASE_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_KEY)
                        .addHeader("Accept", "application/json")
                        .build();

                Response response = client.newCall(request).execute();

                Log.d("Supabase Debug", "Response code: " + response.code());  // <-- ADD THIS

                String responseData = response.body().string();

                Log.d("Supabase Debug", "API Response: " + responseData);  // <-- ADD THIS


                // Add these two debug logs here:
                Log.d("Supabase Debug", "Response code: " + response.code());


                Gson gson = new Gson();
                List<QRRecord> qrRecords = gson.fromJson(responseData, new TypeToken<List<QRRecord>>(){}.getType());

                runOnUiThread(() -> {
                    if (qrRecords != null && !qrRecords.isEmpty()) {
                        openWebView(scannedLink);
                    } else {
                        showInvalidQRPopup();
                    }
                });

            } catch (Exception e) {
                Log.e("Supabase Error", "API Request Failed", e);
            }
        }).start();
    }

    private void showInvalidQRPopup() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Student Card Not Valid", Toast.LENGTH_LONG).show();
            isScanned = false;  // Reset scanning
        });
    }

    private void openWebView(String url) {
        Log.d("WebViewDebug", "Opening URL: " + url); // ✅ Log the URL
        Intent intent = new Intent(this, WebViewActivity.class);
        intent.putExtra("url", url);
        startActivity(intent);
        finish(); // Close camera activity
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
