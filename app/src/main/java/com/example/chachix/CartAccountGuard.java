package com.example.chachix;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseUser;

/**
 * The current app uses one device-wide SharedPreferences cart. This guard
 * prevents an old account's cart from being shown or ordered by a new account.
 * It clears pre-upgrade unowned carts once and clears on account switches.
 * A later phase should migrate cart items to per-user Firestore documents.
 */
public final class CartAccountGuard {
    private static final String CART_PREFS = "chachix_cart";
    private static final String OWNER_PREFS = "chachix_cart_owner";
    private static final String OWNER_KEY = "uid";

    private CartAccountGuard() { }

    public static void ensureFor(Context context, FirebaseUser user) {
        if (user == null) return;
        SharedPreferences owner = context.getSharedPreferences(
                OWNER_PREFS, Context.MODE_PRIVATE);
        String previous = owner.getString(OWNER_KEY, "");
        if (!user.getUid().equals(previous)) {
            // Never display a cart from an unknown or different account.
            context.getSharedPreferences(CART_PREFS, Context.MODE_PRIVATE)
                    .edit().clear().commit();
            owner.edit().putString(OWNER_KEY, user.getUid()).commit();
        }
    }
}
