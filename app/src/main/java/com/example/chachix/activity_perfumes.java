package com.example.chachix;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Locale;

public class activity_perfumes extends AppCompatActivity {

    private EditText searchInput;
    private ScrollView productScroll;

    private final String[] productNames = {
            "Scentricity",
            "Richy",
            "Luminous",
            "Kelsey",
            "Candice"
    };

    private final Class<?>[] productScreens = {
            product2.class,
            product1.class,
            product3.class,
            product4.class,
            product5.class
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_perfumes);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

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

        searchInput = findViewById(R.id.perfumes_search);

        // Find the ScrollView containing the search field.
        View currentView = searchInput;

        while (currentView.getParent() instanceof View) {
            currentView = (View) currentView.getParent();

            if (currentView instanceof ScrollView) {
                productScroll = (ScrollView) currentView;
                break;
            }
        }

        // Back button
        findViewById(R.id.perfumes_back).setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        // Product cards
        findViewById(R.id.scentricity_card).setOnClickListener(view ->
                openScreen(product2.class)
        );

        findViewById(R.id.richy_card).setOnClickListener(view ->
                openScreen(product1.class)
        );

        findViewById(R.id.luminous_card).setOnClickListener(view ->
                openScreen(product3.class)
        );

        findViewById(R.id.kelsey_card).setOnClickListener(view ->
                openScreen(product4.class)
        );

        findViewById(R.id.candice_card).setOnClickListener(view ->
                openScreen(product5.class)
        );

        // Bottom navigation
        findViewById(R.id.perfumes_nav_home).setOnClickListener(view ->
                openScreen(Dashboard.class)
        );

        findViewById(R.id.perfumes_nav_cart).setOnClickListener(view ->
                openScreen(cart.class)
        );

        findViewById(R.id.perfumes_nav_profile).setOnClickListener(view ->
                openScreen(Profile.class)
        );

        // Already on the catalog: clear search and return to the top.
        findViewById(R.id.perfumes_nav_catalog).setOnClickListener(view -> {
            searchInput.setText("");
            hideKeyboard();

            if (productScroll != null) {
                productScroll.smoothScrollTo(0, 0);
            }
        });

        // Keyboard Search button or physical Enter key
        searchInput.setOnEditorActionListener((view, actionId, event) -> {
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

    private void openScreen(Class<?> screen) {
        hideKeyboard();

        Intent intent = new Intent(activity_perfumes.this, screen);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
    }

    private void searchProducts() {
        String query = searchInput.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        hideKeyboard();

        if (query.isEmpty()) {
            if (productScroll != null) {
                productScroll.smoothScrollTo(0, 0);
            }
            return;
        }

        // Accept the alternate spelling used on the dashboard.
        if (query.equals("sentricity")) {
            query = "scentricity";
        }

        ArrayList<String> matchingNames = new ArrayList<>();
        ArrayList<Class<?>> matchingScreens = new ArrayList<>();

        for (int i = 0; i < productNames.length; i++) {
            String searchableText =
                    productNames[i].toLowerCase(Locale.ROOT)
                            + " scent unisex perfume fragrance";

            if (searchableText.contains(query)) {
                matchingNames.add(productNames[i]);
                matchingScreens.add(productScreens[i]);
            }
        }

        if (matchingNames.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("No products found")
                    .setMessage(
                            "No product matches \""
                                    + searchInput.getText().toString().trim()
                                    + "\"."
                    )
                    .setPositiveButton("OK", null)
                    .show();

            return;
        }

        if (matchingNames.size() == 1) {
            openScreen(matchingScreens.get(0));
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Search results")
                .setItems(
                        matchingNames.toArray(new String[0]),
                        (dialog, index) ->
                                openScreen(matchingScreens.get(index))
                )
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void hideKeyboard() {
        InputMethodManager keyboard =
                (InputMethodManager) getSystemService(
                        INPUT_METHOD_SERVICE
                );

        if (keyboard != null && searchInput != null) {
            keyboard.hideSoftInputFromWindow(
                    searchInput.getWindowToken(),
                    0
            );

            searchInput.clearFocus();
        }
    }
}