package com.chariotsolutions.nfc.plugin;

import android.app.Activity;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.Toast;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.util.Log;

import com.chariotsolutions.nfc.plugin.NfcPlugin;

import java.util.HashMap;
import java.util.Map;
import android.os.Parcelable;
import android.nfc.NdefMessage;

public class NfcActivity extends Activity {
    private static String TAG = "NFCLogs";
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.sendPushPayload();
        finish();
        forceMainActivityReload();
    }
    private void sendPushPayload() {
        Log.d(TAG, "==> USER TAPPED NFCtag");
        Bundle intentExtras = getIntent().getExtras();
        if(intentExtras == null) {
            return;
        }
        Log.d(TAG, "==> USER TAPPED NFC");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("wasTapped", true);
        for (String key : intentExtras.keySet()) {
            Object value = intentExtras.get(key);
            Log.d(TAG, "\tKey: " + key + " Value: " + value);
            data.put(key, value);
        }
        Parcelable[] rawMessages = (Parcelable[]) intentExtras.get("android.nfc.extra.NDEF_MESSAGES");
        if (rawMessages != null) {
            NdefMessage[] messages = new NdefMessage[rawMessages.length];
            for (int i = 0; i < rawMessages.length; i++) {
                messages[i] = (NdefMessage) rawMessages[i];
            }
            NfcPlugin.setInitialPushPayload(messages);
        }
    }
    private void forceMainActivityReload() {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
        startActivity(launchIntent);
    }
}

