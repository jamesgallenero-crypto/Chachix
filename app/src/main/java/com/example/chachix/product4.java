package com.example.chachix;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Locale;

public class product4 extends AppCompatActivity {

    private int selectedSizeIndex = 0;

    private final String[] sizes = {
            "100ML", "50ML", "30ML", "15ML"
    };

    private final int[] prices = {
            399, 252, 200, 136
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product4);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        if (savedInstanceState != null) {
            selectedSizeIndex =
                    savedInstanceState.getInt("selected_size", 0);
        }

        if (selectedSizeIndex < 0
                || selectedSizeIndex >= sizes.length) {
            selectedSizeIndex = 0;
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

            view.setPadding(
                    left + bars.left,
                    top + bars.top,
                    right + bars.right,
                    bottom + bars.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(main);

        findViewById(R.id.detail_back).setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        findViewById(R.id.detail_buy_now).setOnClickListener(view ->
                showSizePopup(false)
        );

        findViewById(R.id.detail_add_to_cart).setOnClickListener(view ->
                showSizePopup(true)
        );
    }

    private void showSizePopup(boolean addToCartMode) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(
                addToCartMode
                        ? R.layout.activity_product4_cart
                        : R.layout.activity_product4_buy
        );

        dialog.setCanceledOnTouchOutside(true);

        TextView priceView;
        TextView selectedSizeView;
        TextView actionButton;
        TextView[] sizeButtons;

        if (addToCartMode) {
            priceView =
                    dialog.findViewById(R.id.cart_size_price);

            selectedSizeView =
                    dialog.findViewById(R.id.cart_size_selected);

            actionButton =
                    dialog.findViewById(R.id.product_cart_add);

            sizeButtons = new TextView[]{
                    dialog.findViewById(R.id.cart_size_100ml),
                    dialog.findViewById(R.id.cart_size_50ml),
                    dialog.findViewById(R.id.cart_size_30ml),
                    dialog.findViewById(R.id.cart_size_15ml)
            };

        } else {
            priceView =
                    dialog.findViewById(R.id.size_product_price);

            selectedSizeView =
                    dialog.findViewById(R.id.size_selected_label);

            actionButton =
                    dialog.findViewById(R.id.size_buy_now);

            sizeButtons = new TextView[]{
                    dialog.findViewById(R.id.size_100ml),
                    dialog.findViewById(R.id.size_50ml),
                    dialog.findViewById(R.id.size_30ml),
                    dialog.findViewById(R.id.size_15ml)
            };
        }

        for (int i = 0; i < sizeButtons.length; i++) {
            final int index = i;

            sizeButtons[i].setOnClickListener(view -> {
                selectedSizeIndex = index;

                updateSizeSelection(
                        sizeButtons,
                        priceView,
                        selectedSizeView,
                        actionButton,
                        addToCartMode
                );
            });
        }

        updateSizeSelection(
                sizeButtons,
                priceView,
                selectedSizeView,
                actionButton,
                addToCartMode
        );

        actionButton.setOnClickListener(view -> {
            actionButton.setEnabled(false);

            if (addToCartMode) {
                addToCart();
                dialog.dismiss();
            } else {
                dialog.dismiss();
                openPayment();
            }
        });

        showBottomDialog(dialog);
    }

    private void openPayment() {
        Intent intent = new Intent(product4.this, payment.class);

        intent.putExtra("buy_now", true);
        intent.putExtra(
                "buy_key",
                "kelsey_" + sizes[selectedSizeIndex]
        );
        intent.putExtra("buy_name", "Kelsey");
        intent.putExtra("buy_size", sizes[selectedSizeIndex]);
        intent.putExtra("buy_price", prices[selectedSizeIndex]);
        intent.putExtra("buy_quantity", 1);

        startActivity(intent);
    }

    private void updateSizeSelection(
            TextView[] sizeButtons,
            TextView priceView,
            TextView selectedSizeView,
            TextView actionButton,
            boolean addToCartMode
    ) {
        for (int i = 0; i < sizeButtons.length; i++) {
            sizeButtons[i].setSelected(i == selectedSizeIndex);
        }

        int selectedPrice = prices[selectedSizeIndex];

        priceView.setText(
                String.format(Locale.US, "₱%d.00", selectedPrice)
        );

        selectedSizeView.setText(sizes[selectedSizeIndex]);

        if (addToCartMode) {
            actionButton.setText("Add to cart");
        } else {
            actionButton.setText(
                    "BUY NOW\nFlash sale ₱" + selectedPrice
            );
        }
    }

    private void addToCart() {
        SharedPreferences preferences =
                getSharedPreferences("chachix_cart", MODE_PRIVATE);

        String selectedSize = sizes[selectedSizeIndex];
        int selectedPrice = prices[selectedSizeIndex];
        String itemKey = "kelsey_" + selectedSize;

        int quantity =
                preferences.getInt(itemKey + "_quantity", 0);

        int newQuantity = quantity == Integer.MAX_VALUE
                ? quantity
                : quantity + 1;

        preferences.edit()
                .putString(itemKey + "_name", "Kelsey")
                .putString(itemKey + "_size", selectedSize)
                .putInt(itemKey + "_price", selectedPrice)
                .putInt(itemKey + "_quantity", newQuantity)
                .apply();

        Toast.makeText(
                this,
                "Kelsey " + selectedSize + " added to cart",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void showBottomDialog(Dialog dialog) {
        dialog.show();

        Window window = dialog.getWindow();

        if (window == null) {
            return;
        }

        window.setBackgroundDrawable(
                new ColorDrawable(Color.TRANSPARENT)
        );

        window.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
        );

        window.setGravity(Gravity.BOTTOM);
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        WindowManager.LayoutParams params = window.getAttributes();
        params.dimAmount = 0.45f;
        window.setAttributes(params);

        View content = window.getDecorView()
                .findViewById(android.R.id.content);

        final int left = content.getPaddingLeft();
        final int top = content.getPaddingTop();
        final int right = content.getPaddingRight();
        final int bottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
            );

            view.setPadding(
                    left + bars.left,
                    top,
                    right + bars.right,
                    bottom + bars.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(content);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("selected_size", selectedSizeIndex);
        super.onSaveInstanceState(outState);
    }
}