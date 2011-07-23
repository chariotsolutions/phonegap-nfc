package com.chariotsolutions.nfc.plugin;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.*;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.util.Log;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NdefPlugin extends Plugin {
    private static final String REGISTER_MIME_TYPE = "registerMimeType";
    private static final String REGISTER_NDEF = "registerNdef";
    private static final String REGISTER_NDEF_FORMATTABLE = "registerNdefFormattable";
    private static final String WRITE_TAG = "writeTag";
    private static final String SHARE_TAG = "shareTag";
    private static final String UNSHARE_TAG = "unshareTag";

    private static final String NDEF = "ndef";
    private static final String NDEF_MIME = "ndef-mime";
    private static final String NDEF_UNFORMATTED = "ndef-unformatted";

    private NdefMessage p2pMessage = null;
    private Intent currentIntent = null;
    private static String TAG = "NdefPlugin";
    private PendingIntent pendingIntent = null;
    private List<IntentFilter> intentFilters = new ArrayList<IntentFilter>();
    private ArrayList<String[]> techLists = new ArrayList<String[]>();

    private boolean executeHasBeenCalled = false;
    private Intent savedIntent;

    @Override
    public PluginResult execute(String action, JSONArray data, String callbackId) {
        Log.d(TAG, "execute " + action);
        createPendingIntent();

        executeHasBeenCalled = true;
        if (savedIntent != null) {
            Log.d(TAG, "Processing saved intent " + savedIntent);
            parseMessage(savedIntent);
            savedIntent = null;
        }

        if (action.equalsIgnoreCase(REGISTER_MIME_TYPE)) {
            try {
                String mimeType = data.getString(0);
                intentFilters.add(createIntentFilter(mimeType));
            } catch (MalformedMimeTypeException e) {
                return new PluginResult(Status.ERROR, "Invalid MIME Type");
            } catch (JSONException e) {
                return new PluginResult(Status.JSON_EXCEPTION, "Invalid MIME Type");
            }
            startNfc();

            return new PluginResult(Status.OK);
        } else if (action.equalsIgnoreCase(REGISTER_NDEF)) {
            addTechList(new String[]{Ndef.class.getName()});
            startNfc();

            return new PluginResult(Status.OK);
        } else if (action.equalsIgnoreCase(REGISTER_NDEF_FORMATTABLE)) {
            addTechList(new String[]{NdefFormatable.class.getName()});
            startNfc();

            return new PluginResult(Status.OK);
        } else if (action.equalsIgnoreCase(WRITE_TAG)) {
            if (currentIntent == null) {
                return new PluginResult(Status.ERROR, "Failed to write tag, received null intent");
            }

            try {
                Tag tag = currentIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                NdefRecord[] records = Util.jsonToNdefRecords(data.getString(0));
                writeTag(new NdefMessage(records), tag);
            } catch (JSONException e) {
                return new PluginResult(Status.JSON_EXCEPTION, "Error reading NDEF message from JSON");
            } catch (Exception e) {
                return new PluginResult(Status.ERROR, e.getMessage());
            }

            return new PluginResult(Status.OK);
        } else if (action.equalsIgnoreCase(SHARE_TAG)) {

            try {
                NdefRecord[] records = Util.jsonToNdefRecords(data.getString(0));
                this.p2pMessage = new NdefMessage(records);

                startNdefPush();

            } catch (JSONException e) {
                return new PluginResult(Status.JSON_EXCEPTION, "Error reading NDEF message from JSON");
            }

            return new PluginResult(Status.OK);

        } else if (action.equalsIgnoreCase(UNSHARE_TAG)) {
            p2pMessage = null;
            stopNdefPush();
            return new PluginResult(Status.OK);
        }
        Log.d(TAG, "no result");
        return new PluginResult(Status.NO_RESULT);
    }

    private void createPendingIntent() {
        if (pendingIntent == null) {
            Intent intent = new Intent(ctx, ctx.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0);
        }
    }

    private void addTechList(String[] list) {
        this.addTechFilter();
        this.addToTechList(list);
    }

    private void addTechFilter() {
        intentFilters.add(new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED));
    }

    private void startNfc() {
        createPendingIntent(); // onResume can call startNfc before execute

        this.ctx.runOnUiThread(new Runnable() {
            public void run() {
                NfcAdapter.getDefaultAdapter(ctx).enableForegroundDispatch(
                        ctx, getPendingIntent(), getIntentFilters(), getTechLists());
                if (p2pMessage != null) {
                    NfcAdapter.getDefaultAdapter(ctx).enableForegroundNdefPush(ctx, p2pMessage);
                }
            }
        });
    }

    private void stopNfc() {
        this.ctx.runOnUiThread(new Runnable() {
            public void run() {
                NfcAdapter.getDefaultAdapter(ctx).disableForegroundDispatch(ctx);
                NfcAdapter.getDefaultAdapter(ctx).disableForegroundNdefPush(ctx);
            }
        });
    }

    private void startNdefPush() {
        this.ctx.runOnUiThread(new Runnable() {
            public void run() {
                NfcAdapter.getDefaultAdapter(ctx).enableForegroundNdefPush(ctx, p2pMessage);
            }
        });
    }

    private void stopNdefPush() {
        this.ctx.runOnUiThread(new Runnable() {
            public void run() {
                NfcAdapter.getDefaultAdapter(ctx).disableForegroundNdefPush(ctx);
            }
        });
    }

    private void addToTechList(String[] techs) {
        techLists.add(techs);
    }

    private IntentFilter createIntentFilter(String mimeType) throws MalformedMimeTypeException {
        IntentFilter intentFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        intentFilter.addDataType(mimeType);
        return intentFilter;
    }

    private PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    private IntentFilter[] getIntentFilters() {
        return intentFilters.toArray(new IntentFilter[intentFilters.size()]);
    }

    private String[][] getTechLists() {
        //noinspection ToArrayCallWithZeroLengthArrayArgument
        return techLists.toArray(new String[0][0]);
    }

    public void parseMessage(Intent intent) {
        String action = intent.getAction();
        this.currentIntent = intent;

        if (action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
            Parcelable[] rawData = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            for (Parcelable message : rawData) {
                fireNdefEvent(NDEF_MIME, message);
            }

        } else if (action.equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

            for (String tagTech : tag.getTechList()) {
                if (tagTech.equalsIgnoreCase(NdefFormatable.class.getName())) {
                    fireNdefEvent(NDEF_UNFORMATTED);
                } else if (tagTech.equalsIgnoreCase(Ndef.class.getName())) {
                    Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                    if (messages.length == 0) { // empty formatted tag
                        fireNdefEvent(NDEF);
                    } else {
                        for (Parcelable message : messages) {
                            fireNdefEvent(NDEF, message);
                        }
                    }
                }
            }
        }
    }

    private void fireNdefEvent(String type) {
        JSONArray jsonData = new JSONArray();
        fireNdefEvent(type, jsonData);
    }

    private void fireNdefEvent(String type, Parcelable parcelable) {
        JSONArray jsonData = Util.messageToJSON((NdefMessage) parcelable);
        fireNdefEvent(type, jsonData);
    }

    private void fireNdefEvent(String type, JSONArray ndefMessage) {
        String command = "navigator.nfc.fireEvent('" + type + "', " + ndefMessage + ")";
        Log.d(TAG, command);
        this.sendJavascript(command);
    }

    private void writeTag(NdefMessage message, Tag tag) throws TagWriteException, IOException, FormatException {

        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            ndef.connect();

            if (!ndef.isWritable()) {
                throw new TagWriteException("Tag is read only");
            }

            int size = message.toByteArray().length;
            if (ndef.getMaxSize() < size) {
                String errorMessage = "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size + " bytes.";
                throw new TagWriteException(errorMessage);
            }
            ndef.writeNdefMessage(message);
        } else {
            NdefFormatable formatable = NdefFormatable.get(tag);
            if (formatable != null) {
                formatable.connect();
                formatable.format(message);
            } else {
                throw new TagWriteException("Tag doesn't support NDEF");
            }
        }
    }

    private boolean shouldSaveIntent() {

        if (executeHasBeenCalled) {
            return false;
        }

        int flags = ctx.getIntent().getFlags();
        if ((flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
            Log.d(TAG, "Launched from history, ignoring recycled intent");
            return false;
        }

        return true;
    }

    @Override
    public void onPause(boolean multitasking) {
        Log.d(TAG, "onPause " + ctx.getIntent());
        super.onPause(multitasking);
        stopNfc();
    }

    @Override
    public void onResume(boolean multitasking) {
        Log.d(TAG, "onResume " + ctx.getIntent());

        super.onResume(multitasking);
        startNfc();

        if (shouldSaveIntent()) {
            Log.d(TAG, "Saving Intent until we're initialized");
            savedIntent = ctx.getIntent();
            ctx.setIntent(new Intent());
        }

    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent " + intent);
        super.onNewIntent(intent);
        parseMessage(intent);
        ctx.setIntent(new Intent());
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy " + ctx.getIntent());
        super.onDestroy();
    }
}
