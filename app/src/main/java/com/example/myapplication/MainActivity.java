package com.example.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    // Déclaration des vues
    private TextView tempTextView, humidityTextView, cnnResultTextView;
    private static final String TAG = "FetchData";
    private ImageView plantImage;
    private Button selectOrTakePhotoButton, predictButton;

    // Codes de requête pour la caméra et la galerie
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;

    private Bitmap capturedImage = null; // Variable pour stocker l'image capturée

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialisation des vues
        plantImage = findViewById(R.id.plant_image);
        selectOrTakePhotoButton = findViewById(R.id.select_or_take_photo_button);
        predictButton = findViewById(R.id.predict_button);
        cnnResultTextView = findViewById(R.id.cnnResultTextView);
        humidityTextView = findViewById(R.id.humidite);
        tempTextView = findViewById(R.id.temperature);

        // Écouteur de clic pour le bouton de sélection ou de prise de photo
        selectOrTakePhotoButton.setOnClickListener(view -> openImageSourceChooser());

        // Écouteur de clic pour le bouton de prédiction
        predictButton.setOnClickListener(view -> {
            if (capturedImage != null) {
                // Convertir l'image en Base64 et l'envoyer au serveur
                String base64Image = convertImageToBase64(capturedImage);
                sendImageToServer(base64Image);
                // Récupérer les données après l'envoi de l'image
                fetchAndUpdateData();
            } else {
                Toast.makeText(MainActivity.this, "Veuillez d'abord capturer ou sélectionner une image.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Méthode pour récupérer les données et mettre à jour l'UI
    private void fetchAndUpdateData() {
        new Thread(() -> {
            String result = fetchDataFromServer("http://192.168.152.68:5000/data");
            runOnUiThread(() -> updateUI(result));
        }).start();
    }

    // Méthode pour récupérer les données depuis le serveur
    private String fetchDataFromServer(String urlString) {
        String result = "";
        try {
            URL url = new URL(urlString);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);

            int responseCode = urlConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder stringBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }
                reader.close();
                result = stringBuilder.toString();
            } else {
                Log.e(TAG, "Erreur de réponse : " + responseCode);
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur de connexion : ", e);
        }
        return result;
    }

    // Méthode pour mettre à jour l'UI avec les données récupérées
    private void updateUI(String result) {
        if (result == null || result.isEmpty()) {
            Toast.makeText(MainActivity.this, "Erreur : Aucune donnée reçue", Toast.LENGTH_LONG).show();
            tempTextView.setText("Erreur : Aucune donnée reçue");
            humidityTextView.setText("");
            cnnResultTextView.setText("");
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(result);
            double temperature = jsonObject.getDouble("temperature");
            int humidity = jsonObject.getInt("humidity");
            String cnnResult = jsonObject.getString("predicted_class");

            // Afficher la température, l'humidité et le résultat CNN
            tempTextView.setText("Température : " + temperature + "°C");
            humidityTextView.setText("Humidité : " + humidity + "%");
            cnnResultTextView.setText("Résultat CNN : " + cnnResult);
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors du parsing des données : ", e);
            Toast.makeText(MainActivity.this, "Erreur de parsing des données", Toast.LENGTH_LONG).show();
            tempTextView.setText("Erreur de parsing");
            humidityTextView.setText("");
            cnnResultTextView.setText("");
        }
    }

    // Ouvrir un dialog pour choisir entre la caméra ou la galerie
    private void openImageSourceChooser() {
        CharSequence[] options = {"Prendre une Photo", "Choisir depuis la Galerie"};
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Sélectionner ou Prendre une Photo");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                openCamera();
            } else if (which == 1) {
                openGallery();
            }
        });
        builder.show();
    }

    // Ouvrir la caméra pour prendre une photo
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, REQUEST_CAMERA);
    }

    // Ouvrir la galerie pour choisir une image
    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    // Gérer le résultat de la caméra ou de la galerie
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                if (data != null && data.getExtras() != null) {
                    capturedImage = (Bitmap) data.getExtras().get("data");
                    plantImage.setImageBitmap(capturedImage);
                }
            } else if (requestCode == REQUEST_GALLERY) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    plantImage.setImageURI(selectedImage);
                    try {
                        capturedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Convertir l'image Bitmap en chaîne Base64
    private String convertImageToBase64(Bitmap image) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Envoyer l'image au serveur via une requête POST
    private void sendImageToServer(String base64Image) {
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.152.68:5000/upload");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("image", base64Image);

                connection.getOutputStream().write(jsonObject.toString().getBytes());

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "Image envoyée avec succès");
                } else {
                    Log.e(TAG, "Échec de l'envoi de l'image. Code de réponse : " + responseCode);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erreur lors de l'envoi de l'image au serveur : ", e);
            }
        }).start();
    }
}
