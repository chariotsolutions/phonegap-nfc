package com.chariotsolutions.nfc.plugin;

import android.app.Activity;
import android.app.NotificationManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Vibrator;
import android.widget.Toast;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.util.Log;

import com.chariotsolutions.nfc.plugin.NfcPlugin;

import java.util.HashMap;
import java.util.Map;
import android.os.Parcelable;
import android.nfc.NdefMessage;

import org.json.JSONException;

public class NfcActivity extends Activity {
    private static String TAG = "NFCLogs";

    // @Override
    // protected void onStart() {
    //     super.onStart();
    //     boolean shouldMainActivityReload = true;
    //     try {
    //         shouldMainActivityReload = this.sendPushPayload();
    //     } catch (JSONException e) {
    //         e.printStackTrace();
    //     }
    //     finish();
    //     if (shouldMainActivityReload) {
    //         forceMainActivityReload();
    //     }

    // }

   @Override
   public void onCreate(Bundle savedInstanceState)
   {
       super.onCreate(savedInstanceState);
       boolean shouldMainActivityReload = true;
       try {
           shouldMainActivityReload = this.sendPushPayload();
       } catch (JSONException e) {
           e.printStackTrace();
       }
       finish();
       if (shouldMainActivityReload) {
        forceMainActivityReload();
       }
   }
    private boolean sendPushPayload() throws JSONException {
        Log.d(TAG, "==> USER TAPPED NFCtag");
        Bundle intentExtras = getIntent().getExtras();
        if(intentExtras == null) {
            return false;
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
            return true;
        }
        try {
            //noinspection DataFlowIssue
            int notification_id = (Integer) data.get("notification_id");
            if (notification_id > -1) {
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                manager.cancel(notification_id);
            }
            String raw_data = data.get("data").toString();
            if (raw_data.length() > 0) {
                NfcPlugin.setInitialPushPayloadRaw(raw_data);
                getIntent().removeExtra("data");
                return false;
            }
        } catch (Exception e) {
            //handle this later
            Log.e(TAG, e.toString());
        }
        return false;
    }
    private void forceMainActivityReload() {
        PackageManager pm = getPackageManager();
        Intent launchIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
        startActivity(launchIntent);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }
}

