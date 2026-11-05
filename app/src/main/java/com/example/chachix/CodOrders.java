package com.example.chachix;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Creates a COD order request. Amounts are in Philippine centavos.
 * IMPORTANT: Client-side prices are estimates, not trusted merchant prices.
 * An admin must validate prices and stock before confirming fulfillment.
 */
public final class CodOrders {
    private static final int MAX_ITEMS = 30;
    private static final int MAX_QUANTITY = 99;
    private static final long MAX_UNIT_CENTAVOS = 10_000_000L;

    private CodOrders() { }

    public static final class Item {
        public final String key;
        public final String name;
        public final String size;
        public final long unitPriceCentavos;
        public final int quantity;

        public Item(String key, String name, String size,
                    long unitPriceCentavos, int quantity) {
            this.key = key;
            this.name = name;
            this.size = size;
            this.unitPriceCentavos = unitPriceCentavos;
            this.quantity = quantity;
        }
    }

    public static String newOrderId() {
        return FirebaseFirestore.getInstance()
                .collection("orders").document().getId();
    }

    public static Task<Void> submit(
            String orderId, FirebaseUser user,
            String recipient, String address,
            List<Item> items, long discountCentavos,
            long shippingCentavos, long displayedTotalCentavos
    ) {
        if (user == null || user.getUid().isEmpty()) {
            throw new IllegalArgumentException("Please sign in again.");
        }
        if (orderId == null || orderId.isEmpty() || orderId.contains("/")) {
            throw new IllegalArgumentException("Invalid order ID.");
        }
        if (recipient == null || recipient.trim().isEmpty()
                || recipient.length() > 250
                || address == null || address.trim().isEmpty()
                || address.length() > 750) {
            throw new IllegalArgumentException("Complete your delivery details.");
        }
        if (items == null || items.isEmpty() || items.size() > MAX_ITEMS) {
            throw new IllegalArgumentException("Please select 1 to 30 items.");
        }
        if (discountCentavos < 0 || shippingCentavos < 0) {
            throw new IllegalArgumentException("Invalid order charges.");
        }

        long subtotal = 0;
        ArrayList<Map<String, Object>> rows = new ArrayList<>();
        for (Item item : items) {
            if (item.key == null || item.key.isEmpty() || item.key.length() > 100
                    || item.name == null || item.name.isEmpty() || item.name.length() > 100
                    || item.size == null || item.size.isEmpty() || item.size.length() > 40
                    || item.quantity < 1 || item.quantity > MAX_QUANTITY
                    || item.unitPriceCentavos < 1
                    || item.unitPriceCentavos > MAX_UNIT_CENTAVOS) {
                throw new IllegalArgumentException("Invalid item details or quantity.");
            }

            long line = Math.multiplyExact(item.unitPriceCentavos, item.quantity);
            subtotal = Math.addExact(subtotal, line);
            Map<String, Object> row = new HashMap<>();
            row.put("key", item.key);
            row.put("name", item.name);
            row.put("size", item.size);
            row.put("unitPriceCents", item.unitPriceCentavos);
            row.put("quantity", item.quantity);
            row.put("lineTotalCents", line);
            rows.add(row);
        }

        if (discountCentavos > subtotal) {
            throw new IllegalArgumentException("Invalid discount.");
        }
        long recomputedTotal = Math.addExact(
                Math.subtractExact(subtotal, discountCentavos),
                shippingCentavos);
        if (recomputedTotal != displayedTotalCentavos) {
            throw new IllegalArgumentException(
                    "Checkout amount changed. Return to the cart and try again.");
        }

        Map<String, Object> order = new HashMap<>();
        order.put("orderId", orderId);
        order.put("userId", user.getUid());
        order.put("email", user.getEmail() == null ? "" : user.getEmail());
        order.put("recipient", recipient.trim());
        order.put("address", address.trim());
        order.put("items", rows);
        order.put("subtotalCents", subtotal);
        order.put("discountCents", discountCentavos);
        order.put("shippingCents", shippingCentavos);
        order.put("totalCents", recomputedTotal);
        order.put("currency", "PHP");
        order.put("paymentMethod", "COD");
        order.put("paymentStatus", "unpaid");
        order.put("status", "pending");
        order.put("createdAt", FieldValue.serverTimestamp());
        order.put("updatedAt", FieldValue.serverTimestamp());

        DocumentReference ref = FirebaseFirestore.getInstance()
                .collection("orders").document(orderId);
        return ref.set(order);
    }
}
