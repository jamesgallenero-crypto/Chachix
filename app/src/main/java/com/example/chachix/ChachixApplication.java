package com.example.chachix;

import android.app.Application;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Enforces the Remember Me choice even if Android restores a non-login Activity. */
public class ChachixApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        if (user != null && !ChachixSession.isAllowed(this, user)) {
            auth.signOut();
            ChachixSession.clear(this);
        }
    }
}
