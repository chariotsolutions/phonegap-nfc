package com.chariotsolutions.nfc.plugin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class NfcActivity extends Activity
{
    private static Intent launchIntent = null;
    private static boolean initialized = false;

    public static final String TAG = "NfcActivity";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Log.i(TAG, "onCreate: " + getIntent().getAction());
        launchIntent = getIntent();

        if (!initialized) {
            // This looks like a cold start (Cordova has not launched) so send a launch intent
            final Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launchIntent != null) {
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            } else {
                Log.w(TAG, "onCreate: no launch intent defined!");
            }
        }

        finish();
    }

    public static void onPluginInitialize() {
        Log.i(TAG, "onPluginInitialize");
        initialized = true;
    }

    public static Intent getLaunchIntent() {
        final Intent result = launchIntent;
        launchIntent = null;
        Log.i(TAG, "getLaunchIntent: " +  result);
        return result;
    }
}
