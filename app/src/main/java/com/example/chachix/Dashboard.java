package com.example.chachix;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Locale;

public class Dashboard extends AppCompatActivity {

    private EditText searchInput;
    private TextView customerName;

    private FirebaseAuth auth;
    private FirebaseFirestore database;
    private ListenerRegistration profileListener;

    private final String[] productNames = {
            "Richy",
            "Scentricity",
            "Luminous",
            "Kelsey",
            "Candice"
    };

    private final Class<?>[] productScreens = {
            product1.class,
            product2.class,
            product3.class,
            product4.class,
            product5.class
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

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

        searchInput = findViewById(R.id.search_input);
        customerName = findViewById(R.id.customer_name);

        // Replace the fixed name immediately.
        customerName.setText("Loading...");

        setupNavigation();
        setupProducts();
        setupSearch();
        setupHeader();
        setupOffers();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Intent intent = new Intent(Dashboard.this, MainActivity.class);

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
            return;
        }

        listenToUserProfile(user);
    }

    private void listenToUserProfile(FirebaseUser user) {
        if (profileListener != null) {
            profileListener.remove();
        }

        profileListener = database.collection("users")
                .document(user.getUid())
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        if ("Loading...".contentEquals(customerName.getText())) {
                            customerName.setText("Customer");
                        }
                        return;
                    }

                    if (document != null && document.exists()) {
                        String fullName = document.getString("fullName");

                        if (fullName != null && !fullName.trim().isEmpty()) {
                            customerName.setText(fullName.trim());
                            return;
                        }
                    }

                    customerName.setText("Customer");
                });
    }

    private void setupNavigation() {
        findViewById(R.id.nav_home).setOnClickListener(view -> {
            hideKeyboard();
            searchInput.clearFocus();

            ScrollView scrollView = findParentScrollView(
                    findViewById(R.id.profile_header)
            );

            if (scrollView != null) {
                scrollView.smoothScrollTo(0, 0);
            }
        });

        findViewById(R.id.nav_offers).setOnClickListener(view -> {
            hideKeyboard();
            searchInput.clearFocus();
            scrollToSection(findViewById(R.id.offers_heading));
        });

        findViewById(R.id.nav_cart).setOnClickListener(view ->
                openScreen(cart.class)
        );

        findViewById(R.id.nav_profile).setOnClickListener(view ->
                openScreen(Profile.class)
        );
    }

    private void setupProducts() {
        findViewById(R.id.product_sentricity).setOnClickListener(view ->
                openScreen(product2.class)
        );

        findViewById(R.id.product_richy).setOnClickListener(view ->
                openScreen(product1.class)
        );

        findViewById(R.id.product_luminous).setOnClickListener(view ->
                openScreen(product3.class)
        );

        findViewById(R.id.see_all).setOnClickListener(view ->
                openScreen(activity_perfumes.class)
        );
    }

    private void setupSearch() {
        searchInput.setOnEditorActionListener((textView, actionId, event) -> {
            boolean searchPressed =
                    actionId == EditorInfo.IME_ACTION_SEARCH;

            boolean enterPressed =
                    event != null
                            && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                            && event.getAction() == KeyEvent.ACTION_DOWN;

            if (searchPressed || enterPressed) {
                searchProducts();
                return true;
            }

            return false;
        });
    }

    private void searchProducts() {
        String query = searchInput.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        hideKeyboard();
        searchInput.clearFocus();

        if (query.isEmpty()) {
            openScreen(activity_perfumes.class);
            return;
        }

        ArrayList<String> matches = new ArrayList<>();
        ArrayList<Class<?>> screens = new ArrayList<>();

        for (int i = 0; i < productNames.length; i++) {
            String name = productNames[i].toLowerCase(Locale.ROOT);

            boolean sentricityMatch =
                    i == 1 && "sentricity".contains(query);

            if (name.contains(query) || sentricityMatch) {
                matches.add(productNames[i]);
                screens.add(productScreens[i]);
            }
        }

        if (matches.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("No products found")
                    .setMessage(
                            "Try Richy, Scentricity, Luminous, Kelsey, or Candice."
                    )
                    .setPositiveButton("OK", null)
                    .show();

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Search results")
                .setItems(
                        matches.toArray(new String[0]),
                        (dialog, which) -> openScreen(screens.get(which))
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupHeader() {
        findViewById(R.id.avatar).setOnClickListener(view ->
                openScreen(Profile.class)
        );

        customerName.setOnClickListener(view ->
                openScreen(Profile.class)
        );

        findViewById(R.id.notifications_button).setOnClickListener(view ->
                new AlertDialog.Builder(this)
                        .setTitle("Notifications")
                        .setMessage("Notifications are not connected yet.")
                        .setPositiveButton("OK", null)
                        .show()
        );
    }

    private void setupOffers() {
        findViewById(R.id.offer_banner).setOnClickListener(view ->
                new AlertDialog.Builder(this)
                        .setTitle("ChaChix Scents Perfume Bar")
                        .setMessage(
                                "Bring ChaChix Scents Perfume Bar to "
                                        + "your next corporate gathering!"
                                        + "\n\nLet your guests create "
                                        + "their own scent."
                        )
                        .setPositiveButton(
                                "Browse perfumes",
                                (dialog, which) ->
                                        openScreen(activity_perfumes.class)
                        )
                        .setNegativeButton("Close", null)
                        .show()
        );
    }

    private void openScreen(Class<?> screen) {
        hideKeyboard();
        startActivity(new Intent(Dashboard.this, screen));
    }

    private ScrollView findParentScrollView(View view) {
        ViewParent parent = view.getParent();

        while (parent != null) {
            if (parent instanceof ScrollView) {
                return (ScrollView) parent;
            }

            parent = parent.getParent();
        }

        return null;
    }

    private void scrollToSection(View section) {
        ScrollView scrollView = findParentScrollView(section);

        if (scrollView != null) {
            Rect rectangle = new Rect();
            section.getDrawingRect(rectangle);

            scrollView.offsetDescendantRectToMyCoords(
                    section,
                    rectangle
            );

            scrollView.smoothScrollTo(0, rectangle.top);
        }
    }

    private void hideKeyboard() {
        View focusedView = getCurrentFocus();

        if (focusedView != null) {
            InputMethodManager keyboard =
                    (InputMethodManager) getSystemService(
                            INPUT_METHOD_SERVICE
                    );

            if (keyboard != null) {
                keyboard.hideSoftInputFromWindow(
                        focusedView.getWindowToken(),
                        0
                );
            }
        }
    }

    @Override
    protected void onStop() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }

        super.onStop();
    }
}