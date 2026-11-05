package com.example.chachix;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseUser;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String[] EMAIL_DOMAINS = {
            "gmail.com", "yahoo.com", "yahoo.com.ph", "outlook.com",
            "hotmail.com", "icloud.com", "live.com", "proton.me"
    };

    private FirebaseAuth auth;

    private AutoCompleteTextView emailInput;
    private EditText passwordInput;
    private CheckBox rememberAccount;
    private TextView loginButton;
    private View createAccount;
    private View forgotPassword;

    private boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        auth = FirebaseAuth.getInstance();

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

        // Keep all existing view IDs from activity_main.xml.
        emailInput = findViewById(R.id.username);
        passwordInput = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);
        createAccount = findViewById(R.id.create_account);
        forgotPassword = findViewById(R.id.forgot_password);
        rememberAccount = findViewById(R.id.remember_account);

        TextView emailLabel = findViewById(R.id.username_label);
        emailLabel.setText("Email Address");

        emailInput.setHint("Enter your email address");
        emailInput.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );
        emailInput.setAutofillHints(
                View.AUTOFILL_HINT_USERNAME,
                View.AUTOFILL_HINT_EMAIL_ADDRESS
        );
        passwordInput.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);

        setupEmailSuggestions();

        // Never save a plaintext password. Firebase handles auth sessions.
        boolean remembered = ChachixSession.isRemembered(this);
        rememberAccount.setChecked(remembered);
        if (remembered) {
            emailInput.setText(ChachixSession.rememberedEmail(this));
            emailInput.setSelection(emailInput.length());
        }

        loginButton.setOnClickListener(view -> login());

        createAccount.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, Register.class))
        );

        forgotPassword.setOnClickListener(view -> resetPassword());

        passwordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login();
                return true;
            }
            return false;
        });

        FirebaseUser restoredUser = auth.getCurrentUser();
        if (restoredUser != null) {
            if (ChachixSession.isAllowed(this, restoredUser)) {
                setBusy(true);
                validateAndContinue(restoredUser, true);
            } else {
                auth.signOut();
                ChachixSession.clear(this);
            }
        }
    }

    private void setupEmailSuggestions() {
        ArrayAdapter<String> suggestionsAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                new ArrayList<>()
        );
        emailInput.setAdapter(suggestionsAdapter);
        emailInput.setThreshold(1);

        emailInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String entered = s.toString().trim();
                int at = entered.indexOf('@');
                List<String> matches = new ArrayList<>();

                if (at > 0 && at == entered.lastIndexOf('@')) {
                    String name = entered.substring(0, at);
                    String typedDomain = entered.substring(at + 1).toLowerCase(Locale.ROOT);
                    for (String domain : EMAIL_DOMAINS) {
                        if (domain.startsWith(typedDomain)
                                && !domain.equals(typedDomain)) {
                            matches.add(name + "@" + domain);
                        }
                    }
                }

                suggestionsAdapter.clear();
                suggestionsAdapter.addAll(matches);
                suggestionsAdapter.notifyDataSetChanged();

                if (!matches.isEmpty() && emailInput.hasFocus()) {
                    // Show completions beneath the email field, not from a server.
                    emailInput.post(() -> {
                        if (emailInput.hasFocus() && suggestionsAdapter.getCount() > 0) {
                            emailInput.showDropDown();
                        }
                    });
                } else {
                    emailInput.dismissDropDown();
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void login() {
        if (busy) {
            return;
        }

        String email = validEmail();
        if (email == null) {
            return;
        }

        // Do not trim or locally save passwords.
        String password = passwordInput.getText().toString();
        if (password.isEmpty()) {
            passwordInput.setError("Enter your password");
            passwordInput.requestFocus();
            return;
        }

        setBusy(true);
        loginButton.setText("LOGGING IN...");

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (!task.isSuccessful() || auth.getCurrentUser() == null) {
                        setBusy(false);
                        Toast.makeText(this,
                                "Unable to log in. Check your email, password, and connection.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    // Must refresh account status before granting access.
                    validateAndContinue(auth.getCurrentUser(), false);
                });
    }

    private void validateAndContinue(FirebaseUser user, boolean restoringSession) {
        setBusy(true);
        user.reload().addOnCompleteListener(this, task -> {
            if (!task.isSuccessful() || auth.getCurrentUser() == null
                    || !user.getUid().equals(auth.getCurrentUser().getUid())) {
                endInvalidSession("Could not verify your account status. Check your connection and sign in again.");
                return;
            }

            // Refresh the Firebase ID token before opening the dashboard.
            user.getIdToken(true).addOnCompleteListener(this, tokenTask -> {
                if (!tokenTask.isSuccessful()) {
                    endInvalidSession("Could not validate your session. Please sign in again.");
                    return;
                }
                if (restoringSession) {
                    // The remembered UID was already checked before reload().
                    if (!ChachixSession.isAllowed(this, user)) {
                        endInvalidSession("Please sign in again.");
                        return;
                    }
                } else {
                    ChachixSession.afterSuccessfulLogin(this, user, rememberAccount.isChecked());
                    // The Android password manager may offer to save credentials.
                    AutofillManager autofill = getSystemService(AutofillManager.class);
                    if (autofill != null) autofill.commit();
                }
                openDashboard();
            });
        });
    }

    private void endInvalidSession(String message) {
        auth.signOut();
        ChachixSession.clear(this);
        setBusy(false);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void openDashboard() {
        Intent intent = new Intent(MainActivity.this, Dashboard.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void resetPassword() {
        if (busy) {
            return;
        }

        String email = validEmail();
        if (email == null) {
            return;
        }

        setBusy(true);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(this, task -> {
                    setBusy(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(
                                MainActivity.this,
                                "If this email has an account, "
                                        + "check its inbox and spam folder "
                                        + "for password reset instructions.",
                                Toast.LENGTH_LONG
                        ).show();
                    } else {
                        Toast.makeText(
                                MainActivity.this,
                                "Unable to send password reset instructions. "
                                        + "Check your connection and try again.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private String validEmail() {
        String email = emailInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Enter your email address");
            emailInput.requestFocus();
            return null;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            emailInput.requestFocus();
            return null;
        }
        return email;
    }

    private void setBusy(boolean value) {
        busy = value;

        loginButton.setEnabled(!value);
        createAccount.setEnabled(!value);
        forgotPassword.setEnabled(!value);
        emailInput.setEnabled(!value);
        passwordInput.setEnabled(!value);
        rememberAccount.setEnabled(!value);

        if (!value) {
            loginButton.setText("LOGIN");
        }
    }
}
