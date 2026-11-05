package com.example.chachix;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseUser;

/** Stores non-secret preferences only. NEVER store passwords or ID tokens here. */
public final class ChachixSession {
    private static final String PREFS = "chachix_login_preferences";
    private static final String REMEMBER = "remember_account";
    private static final String EMAIL = "saved_email";
    private static final String UID = "saved_uid";

    // Process-only session for people who do NOT select Remember me.
    // The value disappears when Android terminates the app process.
    private static volatile String temporaryUid;

    private ChachixSession() { }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isRemembered(Context context) {
        return prefs(context).getBoolean(REMEMBER, false);
    }

    public static String rememberedEmail(Context context) {
        return isRemembered(context) ? prefs(context).getString(EMAIL, "") : "";
    }

    public static boolean isAllowed(Context context, FirebaseUser user) {
        if (user == null) return false;
        if (isRemembered(context)) {
            return user.getUid().equals(prefs(context).getString(UID, ""));
        }
        return user.getUid().equals(temporaryUid);
    }

    /** Call after a successful Firebase sign-in and session validation. */
    public static void afterSuccessfulLogin(Context context, FirebaseUser user, boolean remember) {
        SharedPreferences.Editor editor = prefs(context).edit();
        if (remember) {
            editor.putBoolean(REMEMBER, true);
            editor.putString(EMAIL, user.getEmail() == null ? "" : user.getEmail());
            editor.putString(UID, user.getUid());
            temporaryUid = null;
        } else {
            editor.putBoolean(REMEMBER, false);
            editor.remove(EMAIL);
            editor.remove(UID);
            temporaryUid = user.getUid();
        }
        editor.apply();
    }

    /** Call on explicit logout, account switch, and invalid session. */
    public static void clear(Context context) {
        temporaryUid = null;
        prefs(context).edit().remove(REMEMBER).remove(EMAIL).remove(UID).apply();
    }
}
