package com.example.chachix;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

/** Unified, light pink and white dialogs. Does not change any business logic. */
public final class ChachixPopups {
    private static final int WINE = Color.rgb(102, 38, 64);
    private static final int PINK = Color.rgb(198, 67, 109);
    private static final int MUTED = Color.rgb(131, 94, 110);
    private static final int PALE = Color.rgb(255, 238, 244);
    private static final int BORDER = Color.rgb(239, 208, 222);

    private ChachixPopups() { }

    /** Drop-in replacement for existing AlertDialog.Builder usages. */
    public static final class Builder extends AlertDialog.Builder {
        public Builder(Context context) {
            super(context, R.style.ChachixPopupDialogTheme);
        }

        public Builder(Context context, int ignoredThemeResId) {
            super(context, R.style.ChachixPopupDialogTheme);
        }

        @Override
        public AlertDialog show() {
            AlertDialog dialog = super.show();
            decorateButtons(dialog);
            return dialog;
        }
    }

    public static final class OrderLine {
        public final String name;
        public final String amount;

        public OrderLine(String name, String amount) {
            this.name = name;
            this.amount = amount;
        }
    }

    /** Visually structured replacement for the large text-only COD confirmation. */
    public static void showCodConfirmation(
            Context context, String recipient, String address,
            List<OrderLine> lines,
            String discount, String shipping, String total,
            Runnable placeOrder) {

        LinearLayout body = baseBody(context);
        body.addView(eyebrow(context, "CASH ON DELIVERY"));
        body.addView(headline(context, "Confirm your order"));
        body.addView(description(context, "Review your details before placing the order."));
        space(context, body, 15);

        LinearLayout addressCard = card(context);
        addressCard.addView(section(context, "DELIVERY DETAILS"));
        addressCard.addView(primary(context, recipient));
        addressCard.addView(description(context, address));
        body.addView(addressCard);
        space(context, body, 16);

        body.addView(section(context, "YOUR ITEMS"));
        space(context, body, 7);
        if (lines != null) {
            for (OrderLine line : lines) {
                body.addView(amountRow(context, line.name, line.amount, false));
                space(context, body, 8);
            }
        }
        space(context, body, 4);
        View separator = new View(context);
        separator.setBackgroundColor(BORDER);
        body.addView(separator, new LinearLayout.LayoutParams(-1, dp(context, 1)));
        space(context, body, 13);

        body.addView(amountRow(context, "Discount", "-" + discount, false));
        space(context, body, 7);
        body.addView(amountRow(context, "Shipping", shipping, false));
        space(context, body, 14);

        LinearLayout totalCard = card(context);
        totalCard.setBackground(rounded(context, PALE, 14, BORDER));
        totalCard.addView(amountRow(context, "TOTAL ESTIMATE", total, true));
        body.addView(totalCard);
        space(context, body, 12);
        body.addView(description(context,
                "Pay cash upon delivery. The merchant will confirm stock and prices before shipping."));

        new Builder(context)
                .setView(scrollBody(context, body))
                .setNegativeButton("Edit order", null)
                .setPositiveButton("Place order", (dialog, which) -> placeOrder.run())
                .show();
    }

    /** Keeps the same successful-order callbacks. */
    public static void showOrderSuccess(
            Context context, String orderId, Runnable openOrders, Runnable done) {
        LinearLayout body = baseBody(context);
        TextView check = primary(context, "✓");
        check.setTextSize(36);
        check.setTextColor(PINK);
        check.setGravity(Gravity.CENTER);
        body.addView(check);
        space(context, body, 5);
        TextView title = headline(context, "Order placed!");
        title.setGravity(Gravity.CENTER);
        body.addView(title);
        space(context, body, 6);
        TextView subtitle = description(context, "Your Cash on Delivery order was received.");
        subtitle.setGravity(Gravity.CENTER);
        body.addView(subtitle);
        space(context, body, 18);
        LinearLayout idCard = card(context);
        idCard.addView(section(context, "YOUR ORDER ID"));
        TextView id = primary(context, orderId);
        id.setTextIsSelectable(true);
        id.setTextSize(16);
        idCard.addView(id);
        body.addView(idCard);
        space(context, body, 15);
        body.addView(description(context,
                "Status: Pending merchant confirmation. Track your order from My Orders."));

        new Builder(context)
                .setView(scrollBody(context, body))
                .setCancelable(false)
                .setNegativeButton("Done", (d, w) -> done.run())
                .setPositiveButton("My orders", (d, w) -> openOrders.run())
                .show();
    }

    private static void decorateButtons(AlertDialog dialog) {
        Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positive != null) {
            positive.setAllCaps(false);
            positive.setTextColor(Color.WHITE);
            positive.setBackgroundResource(R.drawable.chachix_popup_primary);
            positive.setBackgroundTintList(null);
            positive.setPadding(dp(dialog.getContext(), 14), 0,
                    dp(dialog.getContext(), 14), 0);
            positive.setMinHeight(dp(dialog.getContext(), 42));
        }
        for (int which : new int[]{AlertDialog.BUTTON_NEGATIVE, AlertDialog.BUTTON_NEUTRAL}) {
            Button button = dialog.getButton(which);
            if (button != null) {
                button.setAllCaps(false);
                button.setTextColor(PINK);
                button.setBackgroundTintList(null);
            }
        }
    }

    private static LinearLayout baseBody(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(context, 22), dp(context, 20),
                dp(context, 22), dp(context, 14));
        return layout;
    }

    private static ScrollView scrollBody(Context context, LinearLayout layout) {
        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.addView(layout);
        return scroll;
    }

    private static LinearLayout card(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(context, 14), dp(context, 12),
                dp(context, 14), dp(context, 12));
        layout.setBackground(rounded(context, Color.rgb(255, 250, 252), 14, BORDER));
        return layout;
    }

    private static GradientDrawable rounded(Context context, int color, int radius, int stroke) {
        GradientDrawable result = new GradientDrawable();
        result.setColor(color);
        result.setCornerRadius(dp(context, radius));
        result.setStroke(dp(context, 1), stroke);
        return result;
    }

    private static TextView primary(Context context, String text) {
        TextView view = text(context, text, 15, WINE);
        view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private static TextView headline(Context context, String text) {
        TextView view = text(context, text, 24, WINE);
        view.setTypeface(Typeface.create("serif", Typeface.BOLD));
        return view;
    }

    private static TextView eyebrow(Context context, String text) {
        TextView view = text(context, text, 11, PINK);
        view.setTypeface(null, Typeface.BOLD);
        view.setLetterSpacing(0.13f);
        return view;
    }

    private static TextView section(Context context, String text) {
        TextView view = text(context, text, 11, MUTED);
        view.setTypeface(null, Typeface.BOLD);
        view.setLetterSpacing(0.08f);
        view.setPadding(0, 0, 0, dp(context, 6));
        return view;
    }

    private static TextView description(Context context, String text) {
        TextView view = text(context, text, 13, MUTED);
        view.setLineSpacing(dp(context, 3), 1f);
        return view;
    }

    private static TextView text(Context context, String text, int size, int color) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private static LinearLayout amountRow(
            Context context, String label, String amount, boolean highlight) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView left = text(context, label, highlight ? 13 : 14, highlight ? WINE : MUTED);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        if (highlight) left.setTypeface(null, Typeface.BOLD);
        row.addView(left);
        TextView right = text(context, amount, highlight ? 19 : 14, highlight ? PINK : WINE);
        right.setTypeface(null, Typeface.BOLD);
        right.setGravity(Gravity.END);
        row.addView(right);
        return row;
    }

    private static void space(Context context, LinearLayout body, int heightDp) {
        View spacer = new View(context);
        body.addView(spacer, new LinearLayout.LayoutParams(1, dp(context, heightDp)));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
