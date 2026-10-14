package com.example.chachix;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;

    private EditText emailInput;
    private EditText passwordInput;
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

        // Keep your existing XML IDs.
        emailInput = findViewById(R.id.username);
        passwordInput = findViewById(R.id.password);
        loginButton = findViewById(R.id.login_button);
        createAccount = findViewById(R.id.create_account);
        forgotPassword = findViewById(R.id.forgot_password);

        TextView emailLabel = findViewById(R.id.username_label);
        emailLabel.setText("Email Address");

        emailInput.setHint("Enter your email address");
        emailInput.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT
                        | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        );
        emailInput.setAutofillHints(View.AUTOFILL_HINT_EMAIL_ADDRESS);

        loginButton.setOnClickListener(view -> login());

        createAccount.setOnClickListener(view ->
                startActivity(
                        new Intent(MainActivity.this, Register.class)
                )
        );

        forgotPassword.setOnClickListener(view -> resetPassword());

        passwordInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                login();
                return true;
            }

            return false;
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

        // Do not trim passwords.
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
                    setBusy(false);

                    if (task.isSuccessful()) {
                        Intent intent =
                                new Intent(MainActivity.this, Dashboard.class);

                        intent.addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
                        );

                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(
                                MainActivity.this,
                                "Unable to log in. Check your email, "
                                        + "password, and internet connection.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
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

        if (!value) {
            loginButton.setText("LOGIN");
        }
    }
}