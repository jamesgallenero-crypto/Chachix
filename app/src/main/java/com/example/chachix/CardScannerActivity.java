package com.example.chachix;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.CameraController;
import androidx.camera.view.LifecycleCameraController;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CardScannerActivity extends AppCompatActivity {

    public static final String EXTRA_CARD_NUMBER = "card_number";
    public static final String EXTRA_EXPIRY_DATE = "expiry_date";

    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "(?<!\\d)(?:\\d[ -]?){12,18}\\d(?!\\d)"
    );

    private static final Pattern EXPIRY_PATTERN = Pattern.compile(
            "(?<!\\d)(0[1-9]|1[0-2])\\s*[/\\-]\\s*"
                    + "(20\\d{2}|\\d{2})(?!\\d)"
    );

    private PreviewView preview;
    private TextView status;
    private TextView numberLabel;
    private TextView expiryLabel;
    private Button useButton;

    private LifecycleCameraController controller;
    private TextRecognizer recognizer;

    private String cardNumber = "";
    private String expiryDate = "";
    private boolean closing;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) {
                            startCamera();
                        } else {
                            status.setText(
                                    "Camera permission denied. Enable camera "
                                            + "access in app settings, then reopen."
                            );
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CANCELED);
        buildScreen();

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS
        );

        useButton.setOnClickListener(view -> {
            if (closing || cardNumber.isEmpty()) {
                return;
            }

            closing = true;

            Intent data = new Intent();
            data.putExtra(EXTRA_CARD_NUMBER, cardNumber);
            data.putExtra(EXTRA_EXPIRY_DATE, expiryDate);

            setResult(RESULT_OK, data);
            finish();
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        int padding = dp(16);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    padding + bars.left,
                    padding + bars.top,
                    padding + bars.right,
                    padding + bars.bottom
            );

            return insets;
        });

        TextView title = new TextView(this);
        title.setText("Scan your card");
        title.setTextSize(24);
        root.addView(title);

        status = new TextView(this);
        status.setText(
                "Show one card. Keep its number and expiry date clear."
        );
        status.setPadding(0, dp(12), 0, dp(12));
        root.addView(status);

        preview = new PreviewView(this);
        preview.setImplementationMode(
                PreviewView.ImplementationMode.COMPATIBLE
        );
        preview.setScaleType(PreviewView.ScaleType.FIT_CENTER);

        root.addView(
                preview,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f
                )
        );

        numberLabel = new TextView(this);
        numberLabel.setText("Card number: waiting...");
        numberLabel.setTextSize(18);
        numberLabel.setPadding(0, dp(12), 0, dp(8));
        root.addView(numberLabel);

        expiryLabel = new TextView(this);
        expiryLabel.setText("Expiry date: waiting...");
        expiryLabel.setTextSize(18);
        root.addView(expiryLabel);

        useButton = new Button(this);
        useButton.setText("Use scanned card");
        useButton.setEnabled(false);
        root.addView(useButton);

        Button cancel = new Button(this);
        cancel.setText("Cancel");
        cancel.setOnClickListener(view -> finish());
        root.addView(cancel);

        setContentView(root);
        ViewCompat.requestApplyInsets(root);
    }

    private void startCamera() {
        if (closing || controller != null) {
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        controller = new LifecycleCameraController(this);

        LifecycleCameraController current = controller;

        try {
            current.setCameraSelector(
                    CameraSelector.DEFAULT_BACK_CAMERA
            );

            current.setEnabledUseCases(
                    CameraController.IMAGE_ANALYSIS
            );

            current.setImageAnalysisBackpressureStrategy(
                    ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
            );

            MlKitAnalyzer analyzer = new MlKitAnalyzer(
                    Collections.singletonList(recognizer),
                    ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
                    executor,
                    result -> {
                        if (closing || isFinishing() || isDestroyed()) {
                            return;
                        }

                        if (result.getThrowable(recognizer) != null) {
                            status.setText(
                                    "Hold the card steady in good lighting."
                            );
                            return;
                        }

                        Text text = result.getValue(recognizer);

                        if (text != null) {
                            readCard(text);
                        }
                    }
            );

            current.setImageAnalysisAnalyzer(executor, analyzer);

            preview.setController(current);
            current.bindToLifecycle(this);

            current.getInitializationFuture().addListener(() -> {
                if (closing || isFinishing() || isDestroyed()) {
                    return;
                }

                try {
                    current.getInitializationFuture().get();
                } catch (Exception e) {
                    status.setText(
                            "Camera could not start. Reopen the scanner."
                    );
                }
            }, executor);

        } catch (Exception e) {
            status.setText(
                    "Camera could not start. Check camera access."
            );
        }
    }

    private void readCard(Text text) {
        String foundNumber = "";

        outer:
        for (Text.TextBlock block : text.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Matcher matcher = NUMBER_PATTERN.matcher(line.getText());

                while (matcher.find()) {
                    String candidate = matcher.group()
                            .replaceAll("[ -]", "");

                    if (isValidCardNumber(candidate)) {
                        foundNumber = candidate;
                        break outer;
                    }
                }
            }
        }

        if (foundNumber.isEmpty()) {
            return;
        }

        if (!foundNumber.equals(cardNumber)) {
            expiryDate = "";
        }

        cardNumber = foundNumber;

        Matcher expiry = EXPIRY_PATTERN.matcher(text.getText());

        if (expiry.find()) {
            String year = expiry.group(2);

            expiryDate = expiry.group(1)
                    + "/"
                    + year.substring(year.length() - 2);
        }

        numberLabel.setText("Card number: " + cardNumber);

        expiryLabel.setText(
                expiryDate.isEmpty()
                        ? "Expiry: enter it on the next screen"
                        : "Expiry: " + expiryDate
        );

        status.setText(
                "Check the details, then tap Use scanned card."
        );

        useButton.setEnabled(true);
    }

    public static boolean isValidCardNumber(String raw) {
        if (raw == null || !raw.matches("[0-9 -]+")) {
            return false;
        }

        String number = raw.replaceAll("[ -]", "");

        if (number.length() < 13
                || number.length() > 19
                || number.matches("0+")) {
            return false;
        }

        int sum = 0;
        boolean doubleDigit = false;

        for (int i = number.length() - 1; i >= 0; i--) {
            int digit = number.charAt(i) - '0';

            if (doubleDigit) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return sum % 10 == 0;
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    @Override
    protected void onDestroy() {
        closing = true;

        if (controller != null) {
            controller.clearImageAnalysisAnalyzer();
            controller.unbind();
        }

        if (recognizer != null) {
            recognizer.close();
        }

        super.onDestroy();
    }
}