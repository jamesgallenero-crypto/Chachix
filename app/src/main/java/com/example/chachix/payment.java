package com.example.chachix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

public class payment extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore database;
    private ListenerRegistration profileListener;
    private SharedPreferences preferences;

    private RelativeLayout itemsContainer;
    private TextView recipientView;
    private TextView addressView;
    private TextView emptyView;
    private TextView placeOrderButton;

    private RadioButton codButton;
    private RadioButton cardButton;
    private RadioButton gcashButton;

    private final ArrayList<CartItem> items = new ArrayList<>();

    private boolean buyNowMode;
    private CartItem buyNowItem;

    private String selectedMethod = "Cash on delivery";
    private boolean deliveryReady;

    private String cardNumber = "";
    private String cardExpiry = "";
    private boolean cardDetailsConfirmed;
    private AlertDialog cardDialog;

    private double originalTotal;
    private double discount;
    private double productSubtotal;
    private double shipping;
    private double total;

    private static final double DISCOUNT_RATE = 0.10;
    private static final int SHIPPING_FEE = 100;

    private final ActivityResultLauncher<Intent> cardScannerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK
                                && result.getData() != null) {

                            Intent data = result.getData();

                            String number = data.getStringExtra(
                                    CardScannerActivity.EXTRA_CARD_NUMBER
                            );

                            String expiry = data.getStringExtra(
                                    CardScannerActivity.EXTRA_EXPIRY_DATE
                            );

                            cardNumber = number == null ? "" : number;
                            cardExpiry = expiry == null ? "" : expiry;
                            cardDetailsConfirmed = false;

                            updateCardLabel();
                        }

                        if (!isFinishing()
                                && !isDestroyed()
                                && "Credit/debit card".equals(selectedMethod)) {
                            showCardDetailsDialog();
                        }
                    }
            );

    private static class CartItem {

        final String key;
        final String name;
        final String size;
        final int price;
        final int quantity;

        CartItem(String key, String name, String size,
                 int price, int quantity) {
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
        setContentView(R.layout.activity_payment);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        auth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
        preferences = getSharedPreferences("chachix_cart", MODE_PRIVATE);

        itemsContainer = findViewById(R.id.payment_items_container);
        recipientView = findViewById(R.id.payment_recipient);
        addressView = findViewById(R.id.payment_address);
        emptyView = findViewById(R.id.payment_empty);
        placeOrderButton = findViewById(R.id.payment_place_order);

        codButton = findViewById(R.id.payment_cod);
        cardButton = findViewById(R.id.payment_card);
        gcashButton = findViewById(R.id.payment_gcash);

        recipientView.setText("Loading delivery information...");
        addressView.setText("");

        setupInsets();
        setupButtons();
        restoreCheckoutItems(savedInstanceState);

        if (savedInstanceState != null) {
            selectedMethod = savedInstanceState.getString(
                    "payment_method",
                    "Cash on delivery"
            );
        }

        selectPaymentMethod(selectedMethod);
        updateCardLabel();
        updateOrderButton();
    }

    private void setupInsets() {
        View main = findViewById(R.id.main);

        int left = main.getPaddingLeft();
        int top = main.getPaddingTop();
        int right = main.getPaddingRight();
        int bottom = main.getPaddingBottom();

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
        findViewById(R.id.payment_back).setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        View.OnClickListener selectCod = view ->
                selectPaymentMethod("Cash on delivery");

        View.OnClickListener selectCard = view -> {
            selectPaymentMethod("Credit/debit card");
            showCardDetailsDialog();
        };

        View.OnClickListener selectGcash = view ->
                selectPaymentMethod("GCash");

        codButton.setOnClickListener(selectCod);
        findViewById(R.id.payment_cod_row).setOnClickListener(selectCod);

        cardButton.setOnClickListener(selectCard);
        findViewById(R.id.payment_card_row).setOnClickListener(selectCard);

        gcashButton.setOnClickListener(selectGcash);
        findViewById(R.id.payment_gcash_row).setOnClickListener(selectGcash);

        findViewById(R.id.payment_delivery_card)
                .setOnClickListener(view -> showDeliveryDetails());

        placeOrderButton.setOnClickListener(view -> reviewOrder());
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            Intent intent = new Intent(this, MainActivity.class);

            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TASK
            );

            startActivity(intent);
            finish();
            return;
        }

        loadDeliveryInformation(user);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCart();
    }

    private void loadDeliveryInformation(FirebaseUser user) {
        if (profileListener != null) {
            profileListener.remove();
        }

        deliveryReady = false;
        updateOrderButton();

        profileListener = database.collection("users")
                .document(user.getUid())
                .addSnapshotListener((document, error) -> {
                    if (error != null) {
                        deliveryReady = false;
                        recipientView.setText("Unable to load delivery details");
                        addressView.setText(
                                "Check your connection. Tap here to retry."
                        );
                        updateOrderButton();
                        return;
                    }

                    if (document == null || !document.exists()) {
                        deliveryReady = false;
                        recipientView.setText("Profile details unavailable");
                        addressView.setText(
                                "Your account needs saved delivery details."
                        );
                        updateOrderButton();
                        return;
                    }

                    String fullName = getText(document.get("fullName"));
                    String phone = getText(document.get("phone"));

                    String street = "";
                    String city = "";
                    String province = "";
                    String zipCode = "";

                    Object addressObject = document.get("address");

                    if (addressObject instanceof Map) {
                        Map<?, ?> address = (Map<?, ?>) addressObject;

                        street = getText(address.get("street"));
                        city = getText(address.get("city"));
                        province = getText(address.get("province"));
                        zipCode = getText(address.get("zipCode"));
                    }

                    recipientView.setText(
                            (fullName.isEmpty() ? "Name not provided" : fullName)
                                    + (phone.isEmpty() ? "" : "  " + phone)
                    );

                    String region = city;

                    if (!province.isEmpty()
                            && !province.equalsIgnoreCase(city)) {
                        region += (region.isEmpty() ? "" : ", ") + province;
                    }

                    if (!zipCode.isEmpty()) {
                        region += (region.isEmpty() ? "" : " ") + zipCode;
                    }

                    String addressText = street;

                    if (!region.isEmpty()) {
                        addressText +=
                                (addressText.isEmpty() ? "" : "\n") + region;
                    }

                    addressView.setText(
                            addressText.isEmpty()
                                    ? "Address not provided"
                                    : addressText
                    );

                    deliveryReady = !fullName.isEmpty()
                            && !phone.isEmpty()
                            && !street.isEmpty()
                            && !city.isEmpty()
                            && !province.isEmpty()
                            && !zipCode.isEmpty();

                    updateOrderButton();
                });
    }

    private void showDeliveryDetails() {
        if (!deliveryReady) {
            FirebaseUser user = auth.getCurrentUser();

            if (user != null) {
                recipientView.setText("Loading delivery information...");
                addressView.setText("");
                loadDeliveryInformation(user);
            }

            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Delivery information")
                .setMessage(
                        recipientView.getText()
                                + "\n\n" + addressView.getText()
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void restoreCheckoutItems(Bundle state) {
        Intent intent = getIntent();

        buyNowMode = state != null
                ? state.getBoolean("checkout_buy_now", false)
                : intent.getBooleanExtra("buy_now", false);

        buyNowItem = null;

        if (!buyNowMode) {
            return;
        }

        // Preserve removal and quantity changes after screen rotation.
        if (state != null
                && !state.getBoolean("checkout_has_item", false)) {
            return;
        }

        String key;
        String name;
        String size;
        int price;
        int quantity;

        if (state != null) {
            key = state.getString("checkout_key");
            name = state.getString("checkout_name");
            size = state.getString("checkout_size");
            price = state.getInt("checkout_price", -1);
            quantity = state.getInt("checkout_quantity", 1);
        } else {
            key = intent.getStringExtra("buy_key");
            name = intent.getStringExtra("buy_name");
            size = intent.getStringExtra("buy_size");
            price = intent.getIntExtra("buy_price", -1);
            quantity = intent.getIntExtra("buy_quantity", 1);
        }

        if (key == null || key.trim().isEmpty()
                || name == null || name.trim().isEmpty()
                || size == null || size.trim().isEmpty()
                || price < 0 || quantity <= 0) {
            Toast.makeText(
                    this,
                    "Product details are missing. Select the product again.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        buyNowItem = new CartItem(key, name, size, price, quantity);
    }

    private void loadCart() {
        items.clear();

        // Buy Now displays the product received from the product screen.
        if (buyNowMode) {
            if (buyNowItem != null) {
                items.add(buyNowItem);
            }

            displayProducts();
            updateSummary();
            return;
        }

        // Cart checkout displays products saved in SharedPreferences.
        ArrayList<String> keys = new ArrayList<>();

        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (entry.getKey().endsWith("_quantity")
                    && entry.getValue() instanceof Integer
                    && (Integer) entry.getValue() > 0) {

                keys.add(entry.getKey().substring(
                        0,
                        entry.getKey().length() - "_quantity".length()
                ));
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

            items.add(new CartItem(key, name, size, price, quantity));
        }

        displayProducts();
        updateSummary();
    }

    private void displayProducts() {
        itemsContainer.removeAllViews();

        int previousId = View.NO_ID;

        for (CartItem item : items) {
            if (previousId != View.NO_ID) {
                View divider = new View(this);
                divider.setId(View.generateViewId());
                divider.setBackgroundColor(Color.parseColor("#FFB3C2"));

                RelativeLayout.LayoutParams dividerParams =
                        new RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                dp(1)
                        );

                dividerParams.addRule(RelativeLayout.BELOW, previousId);
                itemsContainer.addView(divider, dividerParams);
                previousId = divider.getId();
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

            if (previousId != View.NO_ID) {
                rowParams.addRule(RelativeLayout.BELOW, previousId);
            }

            itemsContainer.addView(row, rowParams);
            previousId = row.getId();

            bindProduct(row, item);
        }

        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void bindProduct(View row, CartItem item) {
        setRowText(
                row,
                R.id.cart_item_name,
                item.name.toUpperCase(Locale.ROOT)
        );

        setRowText(
                row,
                R.id.cart_item_description,
                "Eau de Parfum " + item.size
        );

        setRowText(row, R.id.cart_item_unit_price, money(item.price));

        setRowText(
                row,
                R.id.cart_item_quantity,
                String.valueOf(item.quantity)
        );

        setRowText(
                row,
                R.id.cart_item_total,
                money((double) item.price * item.quantity)
        );

        ImageView photo = row.findViewById(R.id.cart_item_image);
        photo.setImageResource(getProductImage(item.key));
        photo.setContentDescription(item.name + " perfume");

        row.findViewById(R.id.cart_item_plus)
                .setOnClickListener(view -> {
                    if (item.quantity < Integer.MAX_VALUE) {
                        changeQuantity(item, item.quantity + 1);
                    }
                });

        View minus = row.findViewById(R.id.cart_item_minus);
        minus.setEnabled(item.quantity > 1);
        minus.setAlpha(item.quantity > 1 ? 1f : 0.4f);

        minus.setOnClickListener(view -> {
            if (item.quantity > 1) {
                changeQuantity(item, item.quantity - 1);
            }
        });

        row.findViewById(R.id.cart_item_remove)
                .setOnClickListener(view ->
                        new AlertDialog.Builder(this)
                                .setTitle("Remove product?")
                                .setMessage(item.name + " " + item.size)
                                .setPositiveButton(
                                        "Remove",
                                        (dialog, which) -> removeItem(item)
                                )
                                .setNegativeButton("Cancel", null)
                                .show()
                );
    }

    private void changeQuantity(CartItem item, int quantity) {
        if (quantity <= 0) {
            return;
        }

        if (buyNowMode) {
            buyNowItem = new CartItem(
                    item.key,
                    item.name,
                    item.size,
                    item.price,
                    quantity
            );
        } else {
            preferences.edit()
                    .putInt(item.key + "_quantity", quantity)
                    .apply();
        }

        loadCart();
    }

    private void removeItem(CartItem item) {
        if (buyNowMode) {
            buyNowItem = null;
            loadCart();
            return;
        }

        preferences.edit()
                .remove(item.key + "_name")
                .remove(item.key + "_size")
                .remove(item.key + "_price")
                .remove(item.key + "_quantity")
                .apply();

        loadCart();
    }

    private void updateSummary() {
        originalTotal = 0;
        long itemCount = 0;

        for (CartItem item : items) {
            originalTotal += (double) item.price * item.quantity;
            itemCount += item.quantity;
        }

        discount = Math.round(originalTotal * DISCOUNT_RATE * 100) / 100.0;
        productSubtotal = originalTotal - discount;
        shipping = items.isEmpty() ? 0 : SHIPPING_FEE;
        total = productSubtotal + shipping;

        setText(R.id.payment_original, money(originalTotal));
        setText(R.id.payment_discount, "-" + money(discount));
        setText(R.id.payment_subtotal, money(productSubtotal));
        setText(R.id.payment_shipping, money(shipping));
        setText(R.id.payment_shipping_subtotal, money(shipping));
        setText(R.id.payment_shipping_discount, "-₱0.00");
        setText(R.id.payment_summary_total, money(total));
        setText(R.id.payment_bottom_total, money(total));

        setText(
                R.id.payment_item_count,
                "Total (" + itemCount
                        + (itemCount == 1 ? " item)" : " items)")
        );

        updateOrderButton();
    }

    private void updateOrderButton() {
        boolean enabled = deliveryReady && !items.isEmpty();

        placeOrderButton.setEnabled(enabled);
        placeOrderButton.setAlpha(enabled ? 1f : 0.5f);
    }

    private void selectPaymentMethod(String method) {
        if (!"Cash on delivery".equals(method)
                && !"Credit/debit card".equals(method)
                && !"GCash".equals(method)) {
            method = "Cash on delivery";
        }

        selectedMethod = method;

        codButton.setChecked("Cash on delivery".equals(method));
        cardButton.setChecked("Credit/debit card".equals(method));
        gcashButton.setChecked("GCash".equals(method));
    }

    private void updateCardLabel() {
        if (cardButton == null) {
            return;
        }

        if (cardDetailsConfirmed && cardNumber.length() >= 4) {
            cardButton.setText(
                    "Card ending in "
                            + cardNumber.substring(cardNumber.length() - 4)
            );
        } else {
            cardButton.setText("Add credit/debit card");
        }
    }

    private void showCardDetailsDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (cardDialog != null && cardDialog.isShowing()) {
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), dp(8));

        TextView explanation = new TextView(this);
        explanation.setText(
                "Scan your card or enter the details. "
                        + "Check both fields before saving."
        );
        form.addView(explanation);

        EditText numberInput = new EditText(this);
        numberInput.setHint("Card number");
        numberInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        numberInput.setSingleLine(true);
        numberInput.setText(cardNumber);
        numberInput.setSaveEnabled(false);
        form.addView(numberInput);

        EditText expiryInput = new EditText(this);
        expiryInput.setHint("Expiry date (MM/YY)");
        expiryInput.setInputType(
                InputType.TYPE_CLASS_DATETIME
                        | InputType.TYPE_DATETIME_VARIATION_DATE
        );
        expiryInput.setSingleLine(true);
        expiryInput.setText(cardExpiry);
        expiryInput.setSaveEnabled(false);
        form.addView(expiryInput);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Credit/debit card")
                .setView(form)
                .setPositiveButton("Save details", null)
                .setNeutralButton("Scan card", null)
                .setNegativeButton("Cancel", null)
                .create();

        cardDialog = dialog;

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(view -> {
                        String number = numberInput.getText()
                                .toString().trim();

                        String expiry = expiryInput.getText()
                                .toString().replaceAll("\\s", "");

                        if (!CardScannerActivity.isValidCardNumber(number)) {
                            numberInput.setError("Check the card number");
                            numberInput.requestFocus();
                            return;
                        }

                        if (!isValidCardExpiry(expiry)) {
                            expiryInput.setError(
                                    "Enter a current or future date as MM/YY"
                            );
                            expiryInput.requestFocus();
                            return;
                        }

                        cardNumber = number.replaceAll("[ -]", "");
                        cardExpiry = expiry;
                        cardDetailsConfirmed = true;

                        updateCardLabel();
                        dialog.dismiss();
                    });

            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setOnClickListener(view -> {
                        cardNumber = numberInput.getText()
                                .toString().trim();

                        cardExpiry = expiryInput.getText()
                                .toString().trim();

                        cardDetailsConfirmed = false;

                        updateCardLabel();
                        dialog.dismiss();

                        try {
                            cardScannerLauncher.launch(
                                    new Intent(
                                            payment.this,
                                            CardScannerActivity.class
                                    )
                            );
                        } catch (android.content.ActivityNotFoundException e) {
                            Toast.makeText(
                                    payment.this,
                                    "Register CardScannerActivity in the manifest.",
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    });
        });

        dialog.setOnDismissListener(ignored -> {
            if (cardDialog == dialog) {
                cardDialog = null;
            }
        });

        dialog.show();
    }

    private boolean isValidCardExpiry(String expiry) {
        if (!expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return false;
        }

        int month = Integer.parseInt(expiry.substring(0, 2));
        int year = 2000 + Integer.parseInt(expiry.substring(3, 5));

        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);
        int currentMonth = now.get(Calendar.MONTH) + 1;

        return year > currentYear
                || (year == currentYear && month >= currentMonth);
    }

    private void reviewOrder() {
        if (!deliveryReady || items.isEmpty()) {
            Toast.makeText(
                    this,
                    "Load complete delivery details and add a product first.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        if ("Credit/debit card".equals(selectedMethod)
                && !cardDetailsConfirmed) {
            showCardDetailsDialog();
            return;
        }

        StringBuilder review = new StringBuilder();

        review.append(recipientView.getText())
                .append("\n")
                .append(addressView.getText())
                .append("\n\nProducts:\n");

        for (CartItem item : items) {
            review.append(item.name)
                    .append(" ")
                    .append(item.size)
                    .append(" × ")
                    .append(item.quantity)
                    .append(" — ")
                    .append(money((double) item.price * item.quantity))
                    .append("\n");
        }

        review.append("\nProduct discount: -")
                .append(money(discount))
                .append("\nShipping: ")
                .append(money(shipping))
                .append("\nTotal: ")
                .append(money(total))
                .append("\nPayment method: ")
                .append(selectedMethod);

        if ("Credit/debit card".equals(selectedMethod)) {
            review.append("\nCard ending in ")
                    .append(cardNumber.substring(cardNumber.length() - 4))
                    .append("\nExpiry: ")
                    .append(cardExpiry);
        }

        review.append(
                "\n\nPreview only. No order has been submitted "
                        + "and no payment has been charged."
        );

        new AlertDialog.Builder(this)
                .setTitle("Review order")
                .setMessage(review.toString())
                .setPositiveButton("OK", null)
                .show();
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

    private void setRowText(View row, int id, String value) {
        TextView textView = row.findViewById(id);
        textView.setText(value);
    }

    private void setText(int id, String value) {
        TextView textView = findViewById(id);
        textView.setText(value);
    }

    private String getText(Object value) {
        return value instanceof String ? ((String) value).trim() : "";
    }

    private String money(double amount) {
        return String.format(Locale.US, "₱%.2f", amount);
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density
        );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("checkout_buy_now", buyNowMode);
        outState.putBoolean("checkout_has_item", buyNowItem != null);

        if (buyNowItem != null) {
            outState.putString("checkout_key", buyNowItem.key);
            outState.putString("checkout_name", buyNowItem.name);
            outState.putString("checkout_size", buyNowItem.size);
            outState.putInt("checkout_price", buyNowItem.price);
            outState.putInt("checkout_quantity", buyNowItem.quantity);
        }

        outState.putString("payment_method", selectedMethod);

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        if (cardDialog != null) {
            cardDialog.dismiss();
            cardDialog = null;
        }

        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }

        super.onStop();
    }
}