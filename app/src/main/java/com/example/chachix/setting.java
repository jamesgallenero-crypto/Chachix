package com.example.chachix;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Map;

public class setting extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore database;
    private ListenerRegistration profileListener;

    private TextView nameView;
    private TextView locationView;
    private TextView changePasswordButton;

    private boolean passwordResetInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setting);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        nameView = findViewById(R.id.settings_name);
        locationView = findViewById(R.id.settings_location);

        changePasswordButton =
                findViewById(R.id.settings_change_password);

        setupInsets();
        setupButtons();
    }

    private void setupInsets() {
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

            view.setPadding(
                    left + bars.left,
                    top + bars.top,
                    right + bars.right,
                    bottom + bars.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(main);
    }

    private void setupButtons() {
        findViewById(R.id.settings_back).setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        findViewById(R.id.settings_edit_profile)
                .setOnClickListener(view -> {
                    if (auth.getCurrentUser() == null) {
                        openLogin();
                        return;
                    }

                    Intent intent = new Intent(
                            setting.this,
                            Profile.class
                    );

                    startActivity(intent);
                });

        changePasswordButton.setOnClickListener(view ->
                showPasswordResetDialog()
        );

        findViewById(R.id.settings_notifications)
                .setOnClickListener(view ->
                        openNotificationSettings()
                );

        findViewById(R.id.settings_help)
                .setOnClickListener(view -> showHelp());

        findViewById(R.id.settings_about)
                .setOnClickListener(view -> showAbout());

        findViewById(R.id.settings_logout)
                .setOnClickListener(view -> confirmLogout());

        nameView.setOnClickListener(view -> {
            FirebaseUser user = auth.getCurrentUser();

            if (user != null) {
                loadProfile(user);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            openLogin();
            return;
        }

        loadProfile(user);
    }

    private void loadProfile(FirebaseUser user) {
        stopProfileListener();

        nameView.setText("Loading profile...");
        locationView.setText("");

        profileListener = database.collection("users")
                .document(user.getUid())
                .addSnapshotListener((document, error) -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    if (error != null) {
                        nameView.setText("Unable to load profile");

                        locationView.setText(
                                "Check your connection. Tap the name to retry."
                        );

                        return;
                    }

                    if (document == null || !document.exists()) {
                        String displayName = text(user.getDisplayName());

                        nameView.setText(
                                displayName.isEmpty()
                                        ? "My account"
                                        : displayName
                        );

                        locationView.setText(
                                "Add your address in Edit Profile."
                        );

                        return;
                    }

                    String fullName = text(document.get("fullName"));

                    if (fullName.isEmpty()) {
                        fullName = text(user.getDisplayName());
                    }

                    nameView.setText(
                            fullName.isEmpty()
                                    ? "My account"
                                    : fullName
                    );

                    String street = "";
                    String city = "";
                    String province = "";

                    Object addressObject = document.get("address");

                    if (addressObject instanceof Map) {
                        Map<?, ?> address = (Map<?, ?>) addressObject;

                        street = text(address.get("street"));
                        city = text(address.get("city"));
                        province = text(address.get("province"));
                    }

                    ArrayList<String> parts = new ArrayList<>();

                    if (!street.isEmpty()) {
                        parts.add(street);
                    }

                    if (!city.isEmpty()) {
                        parts.add(city);
                    }

                    if (!province.isEmpty()
                            && !province.equalsIgnoreCase(city)) {
                        parts.add(province);
                    }

                    StringBuilder location = new StringBuilder();

                    for (String part : parts) {
                        if (location.length() > 0) {
                            location.append(", ");
                        }

                        location.append(part);
                    }

                    locationView.setText(
                            location.length() == 0
                                    ? "Add your address in Edit Profile."
                                    : location.toString()
                    );
                });
    }

    private void showPasswordResetDialog() {
        if (passwordResetInProgress) {
            return;
        }

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            openLogin();
            return;
        }

        boolean usesPassword = false;

        for (UserInfo provider : user.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(
                    provider.getProviderId()
            )) {
                usesPassword = true;
                break;
            }
        }

        if (!usesPassword) {
            new AlertDialog.Builder(this)
                    .setTitle("Change Password")
                    .setMessage(
                            "This account uses another sign-in provider. "
                                    + "Manage your password through that provider."
                    )
                    .setPositiveButton("OK", null)
                    .show();

            return;
        }

        String email = text(user.getEmail());

        if (email.isEmpty()) {
            Toast.makeText(
                    this,
                    "Your account has no email address.",
                    Toast.LENGTH_LONG
            ).show();

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setMessage(
                        "Send a password reset link to:\n\n"
                                + email
                                + "\n\nOpen the link in your email "
                                + "to choose a new password."
                )
                .setPositiveButton(
                        "Send link",
                        (dialog, which) -> sendPasswordReset(email)
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendPasswordReset(String email) {
        if (passwordResetInProgress) {
            return;
        }

        passwordResetInProgress = true;
        changePasswordButton.setEnabled(false);
        changePasswordButton.setAlpha(0.5f);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    passwordResetInProgress = false;

                    if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    changePasswordButton.setEnabled(true);
                    changePasswordButton.setAlpha(1f);

                    if (task.isSuccessful()) {
                        new AlertDialog.Builder(setting.this)
                                .setTitle("Reset email sent")
                                .setMessage(
                                        "Check " + email
                                                + " for the password reset link. "
                                                + "Check your spam folder too."
                                )
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        Toast.makeText(
                                setting.this,
                                "Unable to send the reset email. "
                                        + "Please try again.",
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void openNotificationSettings() {
        Intent intent;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent = new Intent(
                    Settings.ACTION_APP_NOTIFICATION_SETTINGS
            );

            intent.putExtra(
                    Settings.EXTRA_APP_PACKAGE,
                    getPackageName()
            );
        } else {
            intent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            );

            intent.setData(
                    Uri.parse("package:" + getPackageName())
            );
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(
                    this,
                    "Open your phone's Settings, then Apps, "
                            + "Chachix, and Notifications.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle("Help & Support")
                .setMessage(
                        "Edit Profile\n"
                                + "Update your account and delivery information.\n\n"
                                + "Buy Now\n"
                                + "Choose a perfume size, then tap Buy Now "
                                + "to open the payment screen.\n\n"
                                + "Add to Cart\n"
                                + "Choose a size and add the perfume "
                                + "to your saved cart.\n\n"
                                + "Change Password\n"
                                + "Request a password reset link "
                                + "for your email/password account.\n\n"
                                + "Profile not loading?\n"
                                + "Check your internet connection, "
                                + "then tap your name to retry."
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("About Chachix")
                .setMessage(
                        "Chachix is a perfume shopping app.\n\n"
                                + "Browse perfumes, select sizes, "
                                + "manage your cart, and review "
                                + "your products and delivery information."
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Do you want to log out of your account?")
                .setPositiveButton(
                        "Logout",
                        (dialog, which) -> {
                            stopProfileListener();
                            auth.signOut();
                            openLogin();
                        }
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openLogin() {
        stopProfileListener();

        Intent intent = new Intent(
                setting.this,
                MainActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }

    private String text(Object value) {
        return value instanceof String
                ? ((String) value).trim()
                : "";
    }

    private void stopProfileListener() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }

    @Override
    protected void onStop() {
        stopProfileListener();
        super.onStop();
    }
}