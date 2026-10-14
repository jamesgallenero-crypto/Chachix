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
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.Locale;
import java.util.Map;

public class Profile extends AppCompatActivity {

    private TextView nameView;
    private TextView locationView;
    private TextView initialsView;

    private FirebaseAuth auth;
    private FirebaseFirestore database;
    private ListenerRegistration profileListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

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

            view.setPadding(
                    left + bars.left,
                    top + bars.top,
                    right + bars.right,
                    bottom + bars.bottom
            );

            return insets;
        });

        ViewCompat.requestApplyInsets(main);

        nameView = findViewById(R.id.profile_user_name);
        locationView = findViewById(R.id.profile_user_location);
        initialsView = findViewById(R.id.profile_user_initials);

        nameView.setText("Loading...");
        locationView.setText("");
        initialsView.setText("?");

        setupNavigation();
        setupHeader();
        setupPurchases();
        setupWishlist();
    }

    private void setupNavigation() {
        findViewById(R.id.profile_nav_home).setOnClickListener(view ->
                openScreen(Dashboard.class)
        );

        findViewById(R.id.profile_nav_offers).setOnClickListener(view ->
                openScreen(activity_perfumes.class)
        );

        findViewById(R.id.profile_nav_cart).setOnClickListener(view ->
                openScreen(cart.class)
        );

        findViewById(R.id.profile_nav_profile).setOnClickListener(view -> {
            ScrollView scroll = findViewById(R.id.profile_scroll);
            scroll.smoothScrollTo(0, 0);
        });
    }

    private void setupHeader() {
        findViewById(R.id.profile_settings).setOnClickListener(view ->
                openScreen(setting.class)
        );

        findViewById(R.id.profile_favorites).setOnClickListener(view ->
                openScreen(wishlist_product.class)
        );

        View.OnClickListener showDetails = view ->
                new AlertDialog.Builder(Profile.this)
                        .setTitle("Your profile")
                        .setMessage(
                                nameView.getText()
                                        + "\n" + locationView.getText()
                        )
                        .setPositiveButton("OK", null)
                        .show();

        nameView.setOnClickListener(showDetails);
        initialsView.setOnClickListener(showDetails);
    }

    private void setupPurchases() {
        findViewById(R.id.purchase_history).setOnClickListener(view ->
                showPurchaseInformation("Purchase history")
        );

        findViewById(R.id.to_pay).setOnClickListener(view ->
                showPurchaseInformation("To pay")
        );

        findViewById(R.id.to_ship).setOnClickListener(view ->
                showPurchaseInformation("To ship")
        );

        findViewById(R.id.to_receive).setOnClickListener(view ->
                showPurchaseInformation("To receive")
        );

        findViewById(R.id.to_rate).setOnClickListener(view ->
                showPurchaseInformation("To rate")
        );
    }

    private void showPurchaseInformation(String title) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(
                        "Order history and delivery status are not "
                                + "connected yet. Adding an item to the "
                                + "cart does not create an order."
                )
                .setPositiveButton(
                        "Browse perfumes",
                        (dialog, which) ->
                                openScreen(activity_perfumes.class)
                )
                .setNegativeButton("Close", null)
                .show();
    }

    private void setupWishlist() {
        findViewById(R.id.view_wishlist).setOnClickListener(view ->
                openScreen(wishlist_product.class)
        );

        int[] cardIds = {
                R.id.wishlist_one,
                R.id.wishlist_two,
                R.id.wishlist_three,
                R.id.wishlist_four
        };

        for (int cardId : cardIds) {
            View card = findViewById(cardId);

            // Search inside each included card because its child IDs repeat.
            TextView title = card.findViewById(R.id.product_name);
            View image = card.findViewById(R.id.product_image);
            View favorite = card.findViewById(R.id.product_favorite);
            View addButton = card.findViewById(R.id.add_to_cart);

            View.OnClickListener openProduct = view -> {
                int product = getProductNumber(title.getText().toString());

                if (product == 0) {
                    openScreen(activity_perfumes.class);
                } else {
                    openScreen(getProductScreen(product));
                }
            };

            card.setOnClickListener(openProduct);
            title.setOnClickListener(openProduct);
            image.setOnClickListener(openProduct);

            addButton.setOnClickListener(view -> {
                int product = getProductNumber(title.getText().toString());

                if (product == 0) {
                    Toast.makeText(
                            Profile.this,
                            "Open the catalog to select this product",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                showCartPopup(product);
            });

            favorite.setOnClickListener(view ->
                    new AlertDialog.Builder(Profile.this)
                            .setTitle("Remove from wishlist?")
                            .setMessage(title.getText())
                            .setPositiveButton("Remove", (dialog, which) ->
                                    card.setVisibility(View.GONE)
                            )
                            .setNegativeButton("Cancel", null)
                            .show()
            );
        }
    }

    private int getProductNumber(String title) {
        String name = title.toLowerCase(Locale.ROOT);

        if (name.contains("richy")) return 1;

        if (name.contains("scentricity") || name.contains("sentricity")) {
            return 2;
        }

        if (name.contains("luminous")) return 3;
        if (name.contains("kelsey")) return 4;
        if (name.contains("candice")) return 5;

        return 0;
    }

    private Class<?> getProductScreen(int product) {
        switch (product) {
            case 1:
                return product1.class;
            case 2:
                return product2.class;
            case 3:
                return product3.class;
            case 4:
                return product4.class;
            default:
                return product5.class;
        }
    }

    private void showCartPopup(int product) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        int layout;
        String productName;
        String productKey;

        switch (product) {
            case 1:
                layout = R.layout.activity_product1_cart;
                productName = "Richy Scent";
                productKey = "richy";
                break;

            case 2:
                layout = R.layout.activity_product2_cart;
                productName = "Scentricity";
                productKey = "scentricity";
                break;

            case 3:
                layout = R.layout.activity_product3_cart;
                productName = "Luminous";
                productKey = "luminous";
                break;

            case 4:
                layout = R.layout.activity_product4_cart;
                productName = "Kelsey";
                productKey = "kelsey";
                break;

            default:
                layout = R.layout.activity_product5_cart;
                productName = "Candice";
                productKey = "candice";
                break;
        }

        dialog.setContentView(layout);
        dialog.setCanceledOnTouchOutside(true);

        String[] sizes = product == 1
                ? new String[]{"100ML", "50ML", "30ML", "10ML"}
                : new String[]{"100ML", "50ML", "30ML", "15ML"};

        int[] prices = product == 2
                ? new int[]{399, 224, 200, 136}
                : new int[]{399, 252, 200, 136};

        TextView priceView;
        TextView selectedView;
        TextView actionButton;
        TextView[] options;

        if (product == 1) {
            priceView = dialog.findViewById(R.id.size_product_price);
            selectedView = dialog.findViewById(R.id.size_selected_label);
            actionButton = dialog.findViewById(R.id.size_buy_now);

            options = new TextView[]{
                    dialog.findViewById(R.id.size_100ml),
                    dialog.findViewById(R.id.size_50ml),
                    dialog.findViewById(R.id.size_30ml),
                    dialog.findViewById(R.id.size_10ml)
            };
        } else {
            priceView = dialog.findViewById(R.id.cart_size_price);
            selectedView = dialog.findViewById(R.id.cart_size_selected);
            actionButton = dialog.findViewById(R.id.product_cart_add);

            options = new TextView[]{
                    dialog.findViewById(R.id.cart_size_100ml),
                    dialog.findViewById(R.id.cart_size_50ml),
                    dialog.findViewById(R.id.cart_size_30ml),
                    dialog.findViewById(R.id.cart_size_15ml)
            };
        }

        final int[] selected = {0};

        Runnable updateSelection = () -> {
            for (int i = 0; i < options.length; i++) {
                options[i].setSelected(i == selected[0]);
            }

            priceView.setText(
                    String.format(Locale.US, "₱%d.00", prices[selected[0]])
            );

            selectedView.setText(sizes[selected[0]]);
        };

        for (int i = 0; i < options.length; i++) {
            final int index = i;

            options[i].setOnClickListener(view -> {
                selected[0] = index;
                updateSelection.run();
            });
        }

        updateSelection.run();
        actionButton.setText("Add to cart");

        actionButton.setOnClickListener(view -> {
            SharedPreferences preferences =
                    getSharedPreferences("chachix_cart", MODE_PRIVATE);

            String size = sizes[selected[0]];
            String itemKey = productKey + "_" + size;

            int quantity = preferences.getInt(itemKey + "_quantity", 0);

            preferences.edit()
                    .putString(itemKey + "_name", productName)
                    .putString(itemKey + "_size", size)
                    .putInt(itemKey + "_price", prices[selected[0]])
                    .putInt(itemKey + "_quantity", quantity + 1)
                    .apply();

            Toast.makeText(
                    Profile.this,
                    productName + " " + size + " added to cart",
                    Toast.LENGTH_SHORT
            ).show();

            dialog.dismiss();
        });

        dialog.show();

        Window window = dialog.getWindow();

        if (window != null) {
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
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Intent intent = new Intent(Profile.this, MainActivity.class);

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
            return;
        }

        profileListener = database.collection("users")
                .document(user.getUid())
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        if ("Loading...".contentEquals(nameView.getText())) {
                            showDefaultProfile();
                        }
                        return;
                    }

                    if (document == null || !document.exists()) {
                        showDefaultProfile();
                        return;
                    }

                    String fullName = document.getString("fullName");

                    if (fullName == null || fullName.trim().isEmpty()) {
                        fullName = "Customer";
                    }

                    fullName = fullName.trim();

                    nameView.setText(fullName);
                    initialsView.setText(getInitials(fullName));

                    Object addressObject = document.get("address");

                    if (addressObject instanceof Map) {
                        Map<?, ?> address = (Map<?, ?>) addressObject;

                        String city = getAddressText(address, "city");
                        String province = getAddressText(address, "province");

                        String location;

                        if (!city.isEmpty() && !province.isEmpty()) {
                            location = city.equalsIgnoreCase(province)
                                    ? city
                                    : city + ", " + province;
                        } else {
                            location = city.isEmpty() ? province : city;
                        }

                        locationView.setText(
                                location.isEmpty()
                                        ? "Address not provided"
                                        : location
                        );
                    } else {
                        locationView.setText("Address not provided");
                    }
                });
    }

    private String getAddressText(Map<?, ?> address, String key) {
        Object value = address.get(key);

        return value instanceof String
                ? ((String) value).trim()
                : "";
    }

    private String getInitials(String fullName) {
        String name = fullName.trim();

        if (name.isEmpty()) {
            return "?";
        }

        String[] words = name.split("\\s+");
        String initials = words[0].substring(0, 1);

        if (words.length > 1) {
            initials += words[words.length - 1].substring(0, 1);
        }

        return initials.toUpperCase(Locale.ROOT);
    }

    private void showDefaultProfile() {
        nameView.setText("Customer");
        locationView.setText("Profile details unavailable");
        initialsView.setText("C");
    }

    private void openScreen(Class<?> screen) {
        Intent intent = new Intent(Profile.this, screen);

        intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP
        );

        startActivity(intent);
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