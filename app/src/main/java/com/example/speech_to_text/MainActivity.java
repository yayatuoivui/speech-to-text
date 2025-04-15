package com.example.speech_to_text;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SPEECH_REQUEST_CODE = 201;

    private TextView inputText, translatedText;
    private Spinner languageSpinner;
    private Button speakButton;
    private Translator translator;
    private String targetLanguage = TranslateLanguage.VIETNAMESE; // Mặc định là tiếng Việt
    private Map<String, String> languageMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        inputText = findViewById(R.id.inputText);
        translatedText = findViewById(R.id.translatedText);
        languageSpinner = findViewById(R.id.languageSpinner);
        speakButton = findViewById(R.id.speakButton);

        // Kiểm tra quyền ghi âm
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        }

        // Thiết lập spinner cho lựa chọn ngôn ngữ
        setupLanguageSpinner();

        // Xử lý sự kiện nhấn nút nói
        speakButton.setOnClickListener(v -> startSpeechToText());
    }

    private void setupLanguageSpinner() {
        languageMap = new HashMap<>();
        languageMap.put("Vietnamese", TranslateLanguage.VIETNAMESE);
        languageMap.put("Spanish", TranslateLanguage.SPANISH);
        languageMap.put("French", TranslateLanguage.FRENCH);
        languageMap.put("German", TranslateLanguage.GERMAN);
        languageMap.put("Japanese", TranslateLanguage.JAPANESE);

        List<String> languages = new ArrayList<>(languageMap.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguage = languages.get(position);
                targetLanguage = languageMap.get(selectedLanguage);
                setupTranslator();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupTranslator() {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLanguage)
                .build();
        translator = Translation.getClient(options);

        // Tải mô hình dịch
        DownloadConditions conditions = new DownloadConditions.Builder()
                .build();
        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(v -> {})
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this,
                        "Model download failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.getLanguage());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech to text not supported",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                inputText.setText(spokenText);
                translateText(spokenText);
            }
        }
    }

    private void translateText(String text) {
        translator.translate(text)
                .addOnSuccessListener(translated -> translatedText.setText(translated))
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this,
                        "Translation failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Record audio permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (translator != null) {
            translator.close();
        }
    }
}