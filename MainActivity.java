package uz.doriscan.ai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class MainActivity extends ComponentActivity {
    private boolean russian = false;
    private TextView title, subtitle, result, safety;
    private EditText medicineName;
    private Button scanButton, galleryButton, searchButton, languageButton;
    private ActivityResultLauncher<Void> cameraLauncher;
    private ActivityResultLauncher<String> galleryLauncher;
    private final TextRecognizer latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    private final TextRecognizer cyrillicRecognizer = TextRecognition.getClient(
        new com.google.mlkit.vision.text.cyrillic.CyrillicTextRecognizerOptions.Builder().build());

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
            if (bitmap != null) recognizeImage(InputImage.fromBitmap(bitmap, 0));
        });
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null) return;
            try {
                recognizeImage(InputImage.fromFilePath(this, uri));
            } catch (Exception e) {
                result.setText(russian ? "Не удалось открыть изображение. Попробуйте другое фото." : "Rasmni ochib bo‘lmadi. Boshqa suratni tanlang.");
            }
        });
        applyLanguage();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(42, 48, 42, 42);
        root.setBackgroundColor(0xFFF4F8FC);
        scroll.addView(root);

        languageButton = button("O‘zbekcha / Русский");
        root.addView(languageButton);
        languageButton.setOnClickListener(v -> { russian = !russian; applyLanguage(); });

        title = new TextView(this); title.setTextSize(30); title.setTextColor(0xFF1267A5); title.setGravity(Gravity.CENTER); title.setPadding(0, 24, 0, 4); root.addView(title);
        subtitle = new TextView(this); subtitle.setTextSize(16); subtitle.setTextColor(0xFF526575); subtitle.setGravity(Gravity.CENTER); subtitle.setPadding(0, 0, 0, 24); root.addView(subtitle);

        scanButton = button(""); root.addView(scanButton);
        scanButton.setOnClickListener(v -> cameraLauncher.launch(null));
        galleryButton = button("");
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2);
        gp.topMargin = 8;
        root.addView(galleryButton, gp);
        galleryButton.setOnClickListener(v -> galleryLauncher.launch("image/*"));

        TextView or = new TextView(this); or.setTextSize(15); or.setTextColor(0xFF526575); or.setPadding(0, 20, 0, 8); root.addView(or);
        medicineName = new EditText(this); medicineName.setSingleLine(true); medicineName.setTextSize(17); medicineName.setPadding(24, 16, 24, 16); medicineName.setBackgroundColor(0xFFFFFFFF); root.addView(medicineName);
        searchButton = button(""); LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2); bp.topMargin = 14; root.addView(searchButton, bp);
        searchButton.setOnClickListener(v -> searchMedicine());

        result = new TextView(this); result.setTextSize(15); result.setTextColor(0xFF243746); result.setPadding(20, 22, 20, 22); result.setBackgroundColor(0xFFFFFFFF); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2); rp.topMargin = 22; root.addView(result, rp);
        safety = new TextView(this); safety.setTextSize(13); safety.setTextColor(0xFF8A4B00); safety.setPadding(4, 22, 4, 8); root.addView(safety);
        setContentView(scroll);
    }

    private Button button(String text) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(0xFFFFFFFF); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF1677B8));
        return b;
    }

    private void applyLanguage() {
        title.setText("DoriScan AI");
        subtitle.setText(russian ? "Ваш помощник по информации о лекарствах" : "Dorilar haqida ma’lumot beruvchi aqlli yordamchi");
        scanButton.setText(russian ? "📷  Сфотографировать лекарство" : "📷  Dorini suratga olish");
        galleryButton.setText(russian ? "🖼  Выбрать фото из галереи" : "🖼  Galereyadan surat tanlash");
        medicineName.setHint(russian ? "Введите название лекарства" : "Dori nomini kiriting");
        searchButton.setText(russian ? "Найти информацию в интернете" : "Internetdan ma’lumot izlash");
        result.setText(russian ? "Здесь появится распознанный текст с упаковки." : "Bu yerda dori qutisidan aniqlangan matn ko‘rinadi.");
        safety.setText(russian ? "Важно: распознавание по фото может ошибаться. Не принимайте лекарство только на основании результата приложения. Проверяйте инструкцию и консультируйтесь с врачом или фармацевтом." : "Muhim: surat orqali aniqlash xato qilishi mumkin. Faqat ilova natijasiga qarab dori ichmang. Rasmiy yo‘riqnomani tekshiring, shifokor yoki farmatsevt bilan maslahatlashing.");
    }

    private void recognizeImage(InputImage image) {
        result.setText(russian ? "Распознаём надписи на упаковке…" : "Qutidagi yozuvlar aniqlanmoqda…");
        latinRecognizer.process(image).addOnSuccessListener(latin -> {
            cyrillicRecognizer.process(image).addOnSuccessListener(cyrillic -> {
                String found = (latin.getText() + "\n" + cyrillic.getText()).trim();
                if (found.replace("\n", "").trim().isEmpty()) {
                    result.setText(russian ? "Текст не найден. Попробуйте фото чётче или введите название вручную."
                        : "Matn topilmadi. Ravshanroq suratga oling yoki dori nomini qo‘lda kiriting.");
                } else {
                    result.setText((russian ? "Распознанный текст (проверьте название):\n" : "Aniqlangan matn (nomini tekshiring):\n") + found);
                    String firstLine = found.split("\n")[0].trim();
                    if (!firstLine.isEmpty()) medicineName.setText(firstLine);
                }
            }).addOnFailureListener(e -> result.setText(latin.getText()));
        }).addOnFailureListener(e -> result.setText(russian ? "Не удалось распознать текст. Попробуйте ещё раз."
            : "Matnni aniqlab bo‘lmadi. Qayta urinib ko‘ring."));
    }

    private void searchMedicine() {
        String name = medicineName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, russian ? "Введите название лекарства" : "Dori nomini kiriting", Toast.LENGTH_SHORT).show();
            return;
        }
        String query = name + (russian ? " инструкция состав показания противопоказания побочные эффекты" : " yo‘riqnoma tarkibi qo‘llanilishi qarshi ko‘rsatmalar nojo‘ya ta’sirlari");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(query)));
        startActivity(intent);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        latinRecognizer.close();
        cyrillicRecognizer.close();
    }
}
