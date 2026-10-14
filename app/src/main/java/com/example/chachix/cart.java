package com.example.chachix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class cart extends AppCompatActivity {

    private SharedPreferences preferences;

    private RelativeLayout itemsContainer;
    private TextView emptyView;
    private TextView checkoutButton;
    private View clearButton;

    private final ArrayList<CartItem> items = new ArrayList<>();

    private static final double DISCOUNT_RATE = 0.10;
    private static final int SHIPPING_FEE = 100;

    private double total;

    private static class CartItem {
        String key;
        String name;
        String size;
        int price;
        int quantity;

        CartItem(
                String key,
                String name,
                String size,
                int price,
                int quantity
        ) {
            this.key = key;
            this.name = name;
            this.size = size;
            this.price = price;
            this.quantity = quantity;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        preferences = getSharedPreferences(
                "chachix_cart",
                MODE_PRIVATE
        );

        itemsContainer = findViewById(R.id.cart_items_container);
        emptyView = findViewById(R.id.cart_empty);
        checkoutButton = findViewById(R.id.cart_checkout);
        clearButton = findViewById(R.id.cart_clear);

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

        setupNavigation();

        clearButton.setOnClickListener(view -> confirmClearCart());

        checkoutButton.setOnClickListener(view -> showCheckoutReview());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart();
    }

    private void setupNavigation() {
        // Back to the previous screen.
        findViewById(R.id.cart_back).setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        // Home.
        findViewById(R.id.cart_nav_home).setOnClickListener(view ->
                openScreen(Dashboard.class)
        );

        // Perfume catalog.
        findViewById(R.id.cart_nav_perfumes).setOnClickListener(view ->
                openScreen(activity_perfumes.class)
        );

        // Already on Cart: return to the top.
        findViewById(R.id.cart_nav_cart).setOnClickListener(view -> {
            ScrollView scroll = findViewById(R.id.cart_scroll);
            scroll.smoothScrollTo(0, 0);
        });

        // Profile.
        findViewById(R.id.cart_nav_profile).setOnClickListener(view ->
                openScreen(Profile.class)
        );
    }

    private void loadCart() {
        items.clear();

        Map<String, ?> savedData = preferences.getAll();
        ArrayList<String> keys = new ArrayList<>();

        for (Map.Entry<String, ?> entry : savedData.entrySet()) {
            if (entry.getKey().endsWith("_quantity")
                    && entry.getValue() instanceof Integer
                    && (Integer) entry.getValue() > 0) {

                String key = entry.getKey().substring(
                        0,
                        entry.getKey().length() - "_quantity".length()
                );

                keys.add(key);
            }
        }

        Collections.sort(keys);

        for (String key : keys) {
            String name = preferences.getString(key + "_name", "");
            String size = preferences.getString(key + "_size", "");

            int price = preferences.getInt(key + "_price", 0);
            int quantity = preferences.getInt(key + "_quantity", 0);

            if (name == null || name.isEmpty()
                    || size == null || size.isEmpty()
                    || price < 0 || quantity <= 0) {
                continue;
            }

            items.add(
                    new CartItem(key, name, size, price, quantity)
            );
        }

        displayCart();
        updateSummary();
    }

    private void displayCart() {
        itemsContainer.removeAllViews();

        int previousViewId = View.NO_ID;

        for (CartItem item : items) {
            // Divider between products.
            if (previousViewId != View.NO_ID) {
                View divider = new View(this);

                divider.setId(View.generateViewId());
                divider.setBackgroundColor(
                        Color.parseColor("#FFB3C2")
                );

                RelativeLayout.LayoutParams dividerParams =
                        new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                dp(1)
                        );

                dividerParams.addRule(
                        RelativeLayout.BELOW,
                        previousViewId
                );

                itemsContainer.addView(divider, dividerParams);

                previousViewId = divider.getId();
            }

            View row = getLayoutInflater().inflate(
                    R.layout.cart_product_row,
                    itemsContainer,
                    false
            );

            row.setId(View.generateViewId());

            RelativeLayout.LayoutParams rowParams =
                    new RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            dp(100)
                    );

            if (previousViewId != View.NO_ID) {
                rowParams.addRule(
                        RelativeLayout.BELOW,
                        previousViewId
                );
            }

            itemsContainer.addView(row, rowParams);

            previousViewId = row.getId();

            bindRow(row, item);
        }

        emptyView.setVisibility(
                items.isEmpty() ? View.VISIBLE : View.GONE
        );
    }

    private void bindRow(View row, CartItem item) {
        // Find children inside this specific product row.
        ImageView photo =
                row.findViewById(R.id.cart_item_image);

        TextView name =
                row.findViewById(R.id.cart_item_name);

        TextView description =
                row.findViewById(R.id.cart_item_description);

        TextView unitPrice =
                row.findViewById(R.id.cart_item_unit_price);

        TextView quantity =
                row.findViewById(R.id.cart_item_quantity);

        TextView lineTotal =
                row.findViewById(R.id.cart_item_total);

        name.setText(item.name.toUpperCase(Locale.ROOT));
        description.setText("Eau de Parfum " + item.size);
        unitPrice.setText(money(item.price));
        quantity.setText(String.valueOf(item.quantity));

        lineTotal.setText(
                money((double) item.price * item.quantity)
        );

        photo.setImageResource(getProductImage(item.key));
        photo.setContentDescription(item.name + " perfume");

        // Increase quantity.
        row.findViewById(R.id.cart_item_plus)
                .setOnClickListener(view -> {
                    if (item.quantity < Integer.MAX_VALUE) {
                        changeQuantity(item, item.quantity + 1);
                    }
                });

        // Decrease quantity, keeping at least one.
        View minus = row.findViewById(R.id.cart_item_minus);

        minus.setEnabled(item.quantity > 1);
        minus.setAlpha(item.quantity > 1 ? 1f : 0.4f);

        minus.setOnClickListener(view -> {
            if (item.quantity > 1) {
                changeQuantity(item, item.quantity - 1);
            }
        });

        // Remove this product and size.
        row.findViewById(R.id.cart_item_remove)
                .setOnClickListener(view ->
                        new AlertDialog.Builder(cart.this)
                                .setTitle("Remove product?")
                                .setMessage(
                                        item.name + " " + item.size
                                )
                                .setPositiveButton(
                                        "Remove",
                                        (dialog, which) -> removeItem(item)
                                )
                                .setNegativeButton("Cancel", null)
                                .show()
                );
    }

    private int getProductImage(String key) {
        if (key.startsWith("richy_")) {
            return R.drawable.product2;
        }

        if (key.startsWith("scentricity_")) {
            return R.drawable.product1;
        }

        if (key.startsWith("luminous_")) {
            return R.drawable.product3;
        }

        if (key.startsWith("kelsey_")) {
            return R.drawable.product4;
        }

        if (key.startsWith("candice_")) {
            return R.drawable.product5;
        }

        return android.R.drawable.ic_menu_gallery;
    }

    private void changeQuantity(CartItem item, int quantity) {
        preferences.edit()
                .putInt(item.key + "_quantity", quantity)
                .apply();

        loadCart();
    }

    private void removeItem(CartItem item) {
        SharedPreferences.Editor editor = preferences.edit();

        removeSavedItem(editor, item.key);
        editor.apply();

        loadCart();
    }

    private void removeSavedItem(
            SharedPreferences.Editor editor,
            String key
    ) {
        editor.remove(key + "_name");
        editor.remove(key + "_size");
        editor.remove(key + "_price");
        editor.remove(key + "_quantity");
    }

    private void confirmClearCart() {
        if (items.isEmpty()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Clear cart?")
                .setMessage("Remove all products from your cart?")
                .setPositiveButton("Clear", (dialog, which) -> {
                    SharedPreferences.Editor editor =
                            preferences.edit();

                    for (CartItem item : items) {
                        removeSavedItem(editor, item.key);
                    }

                    editor.apply();
                    loadCart();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSummary() {
        double originalTotal = 0;

        for (CartItem item : items) {
            originalTotal +=
                    (double) item.price * item.quantity;
        }

        double discount = Math.round(
                originalTotal * DISCOUNT_RATE * 100
        ) / 100.0;

        double productSubtotal = originalTotal - discount;

        double shipping =
                items.isEmpty() ? 0 : SHIPPING_FEE;

        total = productSubtotal + shipping;

        setAmount(R.id.cart_original, money(originalTotal));

        setAmount(
                R.id.cart_discount,
                "-" + money(discount)
        );

        setAmount(
                R.id.cart_subtotal,
                money(productSubtotal)
        );

        setAmount(R.id.cart_shipping, money(shipping));
        setAmount(R.id.cart_shipping_discount, "-₱0.00");
        setAmount(R.id.cart_total, money(total));

        checkoutButton.setEnabled(!items.isEmpty());
        checkoutButton.setAlpha(
                items.isEmpty() ? 0.5f : 1f
        );

        clearButton.setEnabled(!items.isEmpty());
    }

    private void showCheckoutReview() {
        if (items.isEmpty()) {
            return;
        }

        StringBuilder review = new StringBuilder();

        for (CartItem item : items) {
            review.append(item.name)
                    .append(" ")
                    .append(item.size)
                    .append(" × ")
                    .append(item.quantity)
                    .append(" — ")
                    .append(
                            money(
                                    (double) item.price
                                            * item.quantity
                            )
                    )
                    .append("\n");
        }

        review.append("\nTotal: ")
                .append(money(total))
                .append(
                        "\n\nPayment and order submission "
                                + "are not connected yet."
                );

        new AlertDialog.Builder(this)
                .setTitle("Review your cart")
                .setMessage(review.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void setAmount(int id, String amount) {
        TextView view = findViewById(id);
        view.setText(amount);
    }

    private String money(double amount) {
        return String.format(
                Locale.US,
                "₱%.2f",
                amount
        );
    }

    private int dp(int value) {
        return Math.round(
                value * getResources()
                        .getDisplayMetrics()
                        .density
        );
    }

    private void openScreen(Class<?> screen) {
        Intent intent = new Intent(cart.this, screen);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
    }
}