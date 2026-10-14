package com.example.chachix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore database;

    private EditText fullNameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private EditText phoneInput;
    private EditText streetInput;
    private EditText provinceInput;
    private EditText cityInput;
    private EditText zipInput;

    private CheckBox termsCheckbox;
    private TextView signupButton;
    private View loginLink;

    private boolean busy = false;

    private String pendingUid;
    private Map<String, Object> pendingProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        View main = findViewById(R.id.main);

        final int left = main.getPaddingLeft();
        final int top = main.getPaddingTop();
        final int right = main.getPaddingRight();
        final int bottom = main.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(main, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            Insets keyboard = insets.getInsets(
                    WindowInsetsCompat.Type.ime()
            );

            view.setPadding(
                    left + bars.left,
                    top + bars.top,
                    right + bars.right,
                    bottom + Math.max(bars.bottom, keyboard.bottom)
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(main);

        fullNameInput = findViewById(R.id.full_name);
        emailInput = findViewById(R.id.email);
        passwordInput = findViewById(R.id.new_password);
        confirmPasswordInput = findViewById(R.id.confirm_password);
        phoneInput = findViewById(R.id.phone);
        streetInput = findViewById(R.id.street);
        provinceInput = findViewById(R.id.province);
        cityInput = findViewById(R.id.city);
        zipInput = findViewById(R.id.zip_code);

        termsCheckbox = findViewById(R.id.terms_checkbox);
        signupButton = findViewById(R.id.signup_button);
        loginLink = findViewById(R.id.login_link);

        // Restore a pending profile save after screen rotation.
        if (savedInstanceState != null) {
            pendingUid = savedInstanceState.getString("pending_uid");

            Bundle savedProfile =
                    savedInstanceState.getBundle("pending_profile");

            if (pendingUid != null && savedProfile != null) {
                Map<String, Object> address = new HashMap<>();
                address.put("street", savedProfile.getString("street", ""));
                address.put("province", savedProfile.getString("province", ""));
                address.put("city", savedProfile.getString("city", ""));
                address.put("zipCode", savedProfile.getString("zipCode", ""));

                pendingProfile = new HashMap<>();
                pendingProfile.put(
                        "fullName",
                        savedProfile.getString("fullName", "")
                );
                pendingProfile.put("email", savedProfile.getString("email", ""));
                pendingProfile.put("phone", savedProfile.getString("phone", ""));
                pendingProfile.put("address", address);
                pendingProfile.put("termsAccepted", true);
                pendingProfile.put("createdAt", FieldValue.serverTimestamp());
            }
        }

        setBusy(false);

        signupButton.setOnClickListener(view -> registerAccount());

        loginLink.setOnClickListener(view -> openLogin());
    }

    private void registerAccount() {
        if (busy) {
            return;
        }

        // Retry saving details without creating a second account.
        if (pendingUid != null && pendingProfile != null) {
            saveProfile();
            return;
        }

        EditText[] requiredFields = {
                fullNameInput,
                emailInput,
                passwordInput,
                confirmPasswordInput,
                phoneInput,
                streetInput,
                provinceInput,
                cityInput,
                zipInput
        };

        for (EditText input : requiredFields) {
            if (value(input).isEmpty()) {
                input.setError("This field is required");
                input.requestFocus();
                return;
            }
        }

        String email = value(emailInput);

        // Preserve spaces in passwords.
        String password = passwordInput.getText().toString();
        String confirmation = confirmPasswordInput.getText().toString();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordInput.setError("Use at least 6 characters");
            passwordInput.requestFocus();
            return;
        }

        if (!password.equals(confirmation)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return;
        }

        if (!value(zipInput).matches("\\d{4}")) {
            zipInput.setError("Enter a 4-digit ZIP code");
            zipInput.requestFocus();
            return;
        }

        if (!termsCheckbox.isChecked()) {
            showMessage("Please agree to the terms and conditions");
            return;
        }

        Map<String, Object> address = new HashMap<>();
        address.put("street", value(streetInput));
        address.put("province", value(provinceInput));
        address.put("city", value(cityInput));
        address.put("zipCode", value(zipInput));

        Map<String, Object> profile = new HashMap<>();
        profile.put("fullName", value(fullNameInput));
        profile.put("email", email);
        profile.put("phone", value(phoneInput));
        profile.put("address", address);
        profile.put("termsAccepted", true);
        profile.put("createdAt", FieldValue.serverTimestamp());

        setBusy(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful()) {
                        setBusy(false);

                        Exception error = task.getException();

                        showMessage(
                                error != null
                                        ? error.getLocalizedMessage()
                                        : "Unable to create account"
                        );

                        return;
                    }

                    FirebaseUser user = auth.getCurrentUser();

                    if (user == null) {
                        setBusy(false);
                        showMessage("Unable to access the created account");
                        return;
                    }

                    pendingUid = user.getUid();
                    pendingProfile = profile;

                    saveProfile();
                });
    }

    private void saveProfile() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null || !user.getUid().equals(pendingUid)) {
            showMessage("Please log in to your account before saving details");
            return;
        }

        setBusy(true);
        signupButton.setText("SAVING DETAILS...");

        database.collection("users")
                .document(pendingUid)
                .set(pendingProfile)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        pendingUid = null;
                        pendingProfile = null;

                        showMessage("Account created. Please log in.");

                        auth.signOut();
                        openLogin();
                    } else {
                        setBusy(false);

                        Exception error = task.getException();

                        showMessage(
                                "Account created, but details were not saved. "
                                        + "Tap RETRY SAVE."
                                        + (error == null
                                        ? ""
                                        : "\n" + error.getLocalizedMessage())
                        );
                    }
                });
    }

    private String value(EditText input) {
        return input.getText().toString().trim();
    }

    private void setBusy(boolean value) {
        busy = value;

        signupButton.setEnabled(!value);
        loginLink.setEnabled(!value);

        signupButton.setText(
                value
                        ? "CREATING ACCOUNT..."
                        : pendingUid != null ? "RETRY SAVE" : "SIGN UP"
        );

        boolean editable = !value && pendingUid == null;

        EditText[] fields = {
                fullNameInput,
                emailInput,
                passwordInput,
                confirmPasswordInput,
                phoneInput,
                streetInput,
                provinceInput,
                cityInput,
                zipInput
        };

        for (EditText input : fields) {
            input.setEnabled(editable);
        }

        termsCheckbox.setEnabled(editable);
    }

    private void showMessage(String message) {
        Toast.makeText(
                this,
                message == null ? "Something went wrong" : message,
                Toast.LENGTH_LONG
        ).show();
    }

    private void openLogin() {
        Intent intent = new Intent(Register.this, MainActivity.class);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (pendingUid == null || pendingProfile == null) {
            return;
        }

        outState.putString("pending_uid", pendingUid);

        Bundle profile = new Bundle();

        profile.putString(
                "fullName",
                (String) pendingProfile.get("fullName")
        );
        profile.putString("email", (String) pendingProfile.get("email"));
        profile.putString("phone", (String) pendingProfile.get("phone"));

        Object addressObject = pendingProfile.get("address");

        if (addressObject instanceof Map) {
            Map<?, ?> address = (Map<?, ?>) addressObject;

            profile.putString("street", (String) address.get("street"));
            profile.putString("province", (String) address.get("province"));
            profile.putString("city", (String) address.get("city"));
            profile.putString("zipCode", (String) address.get("zipCode"));
        }

        outState.putBundle("pending_profile", profile);
    }
}