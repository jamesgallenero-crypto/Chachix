package com.example.chachix;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Pink/white programmatic UI: no existing XML layouts or resource IDs are changed. */
public class OrderHistoryActivity extends AppCompatActivity {
    private static final int BG = Color.rgb(255, 248, 251);
    private static final int INK = Color.rgb(104, 36, 63);
    private static final int MUTED = Color.rgb(128, 106, 118);
    private static final int PINK = Color.rgb(211, 71, 118);
    private static final int BORDER = Color.rgb(236, 217, 225);

    private static final String[] FILTERS = {
            "All", "To Pay", "To Ship", "To Receive", "To Rate", "Cancelled"
    };
    private static final String[] FILTER_KEYS = {
            "all", "to_pay", "to_ship", "to_receive", "to_rate", "cancelled"
    };

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private ListenerRegistration listener;
    private LinearLayout content;
    private LinearLayout filterBar;
    private TextView statusText;
    private String filterKey = "all";
    private final List<DocumentSnapshot> orders = new ArrayList<>();
    private boolean fromCache;

    public static Intent forFilter(Context context, String filter) {
        Intent intent = new Intent(context, OrderHistoryActivity.class);
        intent.putExtra("orders_filter", filter);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        filterKey = getIntent().getStringExtra("orders_filter");
        if (filterKey == null) filterKey = "all";
        createScreen();
        renderFilters();
        showInfo("Loading orders...");
    }

    private void createScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(false);
        root.setPadding(dp(14), dp(12), dp(14), dp(12));
        setContentView(root);

        final int left = root.getPaddingLeft(), top = root.getPaddingTop();
        final int right = root.getPaddingRight(), bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(left + bars.left, top + bars.top,
                    right + bars.right, bottom + bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(62)));

        TextView back = text("‹", 32, INK, true);
        back.setGravity(Gravity.CENTER);
        header.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        back.setContentDescription("Back");
        back.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        header.addView(heading, new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(text("My Orders", 23, INK, true));
        heading.addView(text("Cash on delivery & order tracking", 12, MUTED, false));

        statusText = text("", 12, MUTED, false);
        statusText.setPadding(dp(6), dp(8), dp(6), dp(8));
        root.addView(statusText, new LinearLayout.LayoutParams(-1, -2));

        HorizontalScrollView filtersScroller = new HorizontalScrollView(this);
        filtersScroller.setHorizontalScrollBarEnabled(false);
        root.addView(filtersScroller, new LinearLayout.LayoutParams(-1, dp(58)));
        filterBar = new LinearLayout(this);
        filterBar.setGravity(Gravity.CENTER_VERTICAL);
        filterBar.setOrientation(LinearLayout.HORIZONTAL);
        filtersScroller.addView(filterBar);

        ScrollView scroller = new ScrollView(this);
        scroller.setFillViewport(true);
        scroller.setVerticalScrollBarEnabled(false);
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(2), dp(10), dp(2), dp(20));
        scroller.addView(content);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || !ChachixSession.isAllowed(this, user)) {
            goToLogin();
            return;
        }
        if (listener != null) listener.remove();
        listener = firestore.collection("orders")
                .whereEqualTo("userId", user.getUid())
                .limit(100)
                .addSnapshotListener((snap, error) -> {
                    if (error != null) {
                        showInfo("Unable to load orders. Check your connection and Firestore rules.");
                        return;
                    }
                    if (snap == null) return;
                    orders.clear();
                    orders.addAll(snap.getDocuments());
                    fromCache = snap.getMetadata().isFromCache();
                    Collections.sort(orders, (a, b) -> {
                        Timestamp ta = a.getTimestamp("createdAt");
                        Timestamp tb = b.getTimestamp("createdAt");
                        if (ta == null && tb == null) return 0;
                        if (ta == null) return 1;
                        if (tb == null) return -1;
                        return tb.compareTo(ta);
                    });
                    renderOrders();
                });
    }

    private void goToLogin() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void renderFilters() {
        filterBar.removeAllViews();
        for (int i = 0; i < FILTERS.length; i++) {
            final String key = FILTER_KEYS[i];
            boolean active = key.equals(filterKey);
            TextView chip = text(FILTERS[i], 13, active ? Color.WHITE : INK, active);
            chip.setGravity(Gravity.CENTER);
            chip.setMinHeight(dp(42));
            chip.setPadding(dp(16), dp(8), dp(16), dp(8));
            chip.setBackground(background(active ? PINK : Color.WHITE, 22,
                    active ? PINK : BORDER));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, -2);
            params.setMargins(dp(3), dp(3), dp(6), dp(3));
            filterBar.addView(chip, params);
            chip.setOnClickListener(view -> {
                filterKey = key;
                renderFilters();
                renderOrders();
            });
        }
    }

    private void showInfo(String message) {
        content.removeAllViews();
        TextView empty = text(message, 14, MUTED, false);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(24), dp(50), dp(24), dp(50));
        content.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        statusText.setText(" ");
    }

    private void renderOrders() {
        content.removeAllViews();
        int count = 0;
        for (DocumentSnapshot order : orders) {
            if (!matches(order.getString("status"))) continue;
            count++;
            content.addView(orderCard(order));
        }
        statusText.setText(count + (count == 1 ? " order" : " orders")
                + (fromCache ? " · Cached data; may be outdated" : ""));
        if (count == 0) {
            TextView empty = text("No orders in this category yet.", 14, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(14), dp(56), dp(14), dp(56));
            content.addView(empty);
        }
    }

    private boolean matches(String status) {
        if (status == null) return "all".equals(filterKey);
        switch (filterKey) {
            case "to_pay": return "pending".equals(status);
            case "to_ship": return "confirmed".equals(status) || "packing".equals(status);
            case "to_receive": return "shipped".equals(status)
                    || "out_for_delivery".equals(status);
            case "to_rate": return "delivered".equals(status);
            case "cancelled": return "cancelled".equals(status);
            default: return true;
        }
    }

    private View orderCard(DocumentSnapshot order) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(background(Color.WHITE, 16, BORDER));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(dp(2), dp(4), dp(2), dp(14));
        card.setLayoutParams(params);

        String id = order.getId();
        card.addView(text("Order #" + id.substring(0, Math.min(10, id.length()))
                .toUpperCase(Locale.ROOT), 16, INK, true));
        card.addView(text(statusLabel(order.getString("status")), 13, PINK, true));
        Timestamp created = order.getTimestamp("createdAt");
        if (created != null) {
            card.addView(text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                    DateFormat.SHORT).format(created.toDate()), 12, MUTED, false));
        }
        Object rawItems = order.get("items");
        int itemCount = rawItems instanceof List ? ((List<?>) rawItems).size() : 0;
        card.addView(text(itemCount + (itemCount == 1 ? " item" : " items")
                + " · Cash on delivery", 13, MUTED, false));
        card.addView(text("Estimated total: " + money(asLong(order.get("totalCents"))),
                16, INK, true));
        TextView details = text("View details ›", 13, PINK, true);
        details.setPadding(0, dp(10), 0, 0);
        card.addView(details);
        card.setOnClickListener(view -> showDetails(order));
        return card;
    }

    private void showDetails(DocumentSnapshot order) {
        StringBuilder description = new StringBuilder();
        description.append("Status: ")
                .append(statusLabel(order.getString("status")))
                .append("\nPayment: Cash on delivery\n")
                .append("Payment status: ").append(str(order.get("paymentStatus")))
                .append("\n\nRecipient: ").append(str(order.get("recipient")))
                .append("\nDelivery address:\n").append(str(order.get("address")))
                .append("\n\nITEMS\n");
        Object raw = order.get("items");
        if (raw instanceof List) {
            for (Object entry : (List<?>) raw) {
                if (!(entry instanceof Map)) continue;
                Map<?, ?> row = (Map<?, ?>) entry;
                description.append(str(row.get("name"))).append(" · ")
                        .append(str(row.get("size"))).append(" × ")
                        .append(asLong(row.get("quantity"))).append("\n")
                        .append(money(asLong(row.get("lineTotalCents"))))
                        .append("\n");
            }
        }
        description.append("\nSubtotal: ").append(money(asLong(order.get("subtotalCents"))))
                .append("\nDiscount: -").append(money(asLong(order.get("discountCents"))))
                .append("\nShipping: ").append(money(asLong(order.get("shippingCents"))))
                .append("\nTotal estimate: ").append(money(asLong(order.get("totalCents"))))
                .append("\n\nOrder prices and stock await merchant confirmation.");

        AlertDialog.Builder builder = new ChachixPopups.Builder(this)
                .setTitle("Order " + order.getId())
                .setMessage(description.toString())
                .setPositiveButton("Close", null);
        if ("pending".equals(order.getString("status"))) {
            builder.setNegativeButton("Cancel order", (dialog, which) ->
                    confirmCancellation(order.getId()));
        }
        builder.show();
    }

    private void confirmCancellation(String orderId) {
        new ChachixPopups.Builder(this)
                .setTitle("Cancel pending order?")
                .setMessage("This cannot be undone after cancellation.")
                .setNegativeButton("Keep order", null)
                .setPositiveButton("Cancel order", (dialog, which) -> {
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null || !ChachixSession.isAllowed(this, user)) {
                        goToLogin();
                        return;
                    }
                    Map<String, Object> changes = new HashMap<>();
                    changes.put("status", "cancelled");
                    changes.put("updatedAt", FieldValue.serverTimestamp());
                    firestore.collection("orders").document(orderId)
                            .update(changes)
                            .addOnSuccessListener(unused -> Toast.makeText(this,
                                    "Order cancelled", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(error -> Toast.makeText(this,
                                    "Could not cancel this order. It may already be confirmed.",
                                    Toast.LENGTH_LONG).show());
                }).show();
    }

    private static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static long asLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

    private static String statusLabel(String status) {
        if (status == null) return "Unknown";
        switch (status) {
            case "pending": return "Pending merchant confirmation";
            case "confirmed": return "Order confirmed";
            case "packing": return "Preparing shipment";
            case "shipped": return "Shipped";
            case "out_for_delivery": return "Out for delivery";
            case "delivered": return "Delivered";
            case "cancelled": return "Cancelled";
            default: return status;
        }
    }

    private static String money(long centavos) {
        return String.format(Locale.US, "₱%,.2f", centavos / 100.0);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(dp(2), 1f);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private GradientDrawable background(int fill, int radiusDp, int stroke) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(radiusDp));
        bg.setStroke(dp(1), stroke);
        return bg;
    }

    private int dp(float size) {
        return Math.round(size * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onStop() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
        super.onStop();
    }
}
