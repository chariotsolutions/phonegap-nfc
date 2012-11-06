package com.chariotsolutions.nfc.plugin;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.*;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.TagTechnology;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Parcelable;
import android.util.Log;
import org.apache.cordova.api.CallbackContext;
import org.apache.cordova.api.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class NfcPlugin extends CordovaPlugin {
    private static final String REGISTER_MIME_TYPE = "registerMimeType";
    private static final String REGISTER_NDEF = "registerNdef";
    private static final String REGISTER_NDEF_FORMATABLE = "registerNdefFormatable";
    private static final String REGISTER_DEFAULT_TAG = "registerTag";
    private static final String CONNECT = "connect";
    private static final String WRITE_NDEF = "writeNdef";
    private static final String CLOSE = "close";
    private static final String WRITE_TAG = "writeTag";
    private static final String SHARE_TAG = "shareTag";
    private static final String UNSHARE_TAG = "unshareTag";
    private static final String INIT = "init";

    private static final String NDEF = "ndef";
    private static final String NDEF_MIME = "ndef-mime";
    private static final String NDEF_FORMATABLE = "ndef-formatable";
    private static final String TAG_DEFAULT = "tag";


    private static final String TAG = "NfcPlugin";

    private static class TagWorker {
        private TagTechnology tech;
        private Handler asyncHandler;

        TagWorker(TagTechnology tech) {
            this.tech = tech;
            HandlerThread asyncThread = new HandlerThread("PhoneGap NFC worker");
            asyncThread.start();
            this.asyncHandler = new Handler(asyncThread.getLooper());
        }

        public void close(CallbackContext ctx) {
            try {
                tech.close();
                ctx.success();
            } catch (IOException e) {
                ctx.error("Failed to close NFC tag connection: " + e.getMessage());
            }
            asyncHandler.getLooper().quit();
        }

        void connect(final CallbackContext ctx) {
            asyncHandler.post(new Runnable() {
                public void run() {
                    try {
                        tech.connect();  // blocking I/O
                        ctx.success();
                    } catch (IOException e) {
                        ctx.error("NFC tag connect failed: " + e.getMessage());
                    }
                }
            });
        }

        // Support for deprecated stateless usage of writeTag
        void connectSync() throws IOException {
            Log.d(TAG, "Blocking on NFC tag connect; consider using nfc.connect");
            tech.connect();
        }
        void closeWhenDone() {
            asyncHandler.post(new Runnable() {
                public void run() {
                    Log.d(TAG, "Closing the one-shot NFC tag connection; consider using nfc.connect");
                    try {
                        tech.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to close NFC tag connection", e);
                    }
                    asyncHandler.getLooper().quit();
                }
            });
        }

        void writeNdef(final NdefMessage message, final CallbackContext ctx) {
            asyncHandler.post(new Runnable() {
                public void run() {
                    try {
                        if (tech instanceof Ndef) {
                            Ndef ndef = (Ndef)tech;
                            if (!ndef.isWritable()) {
                                throw new TagWriteException("Tag is read only");
                            }

                            int size = message.toByteArray().length;  // Could use NdefMessage.getByteArrayLength() with API level 16
                            if (ndef.getMaxSize() < size) {
                                String errorMessage = "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size + " bytes.";
                                throw new TagWriteException(errorMessage);
                            }

                            ndef.writeNdefMessage(message);  // blocking I/O
                        } else if (tech instanceof NdefFormatable) {
                            NdefFormatable formatable = (NdefFormatable)tech;
                            formatable.format(message);  // blocking I/O
                        } else {
                            Log.wtf(TAG, "Unexpected tag technology " + tech.getClass().getName());
                            throw new TagWriteException("Internal error");
                        }
                        ctx.success();
                    } catch (Exception e) {
                        ctx.error("NDEF write error: " + e.getMessage());
                    }
                }
            });
        }
    }

    private final List<IntentFilter> intentFilters = new ArrayList<IntentFilter>();
    private final ArrayList<String[]> techLists = new ArrayList<String[]>();

    private NdefMessage p2pMessage = null;
    private PendingIntent pendingIntent = null;

    private Intent savedIntent = null;

    private TagWorker tagWorker = null;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "execute " + action);
        createPendingIntent();

        if (action.equalsIgnoreCase(REGISTER_MIME_TYPE)) {
            String mimeType = "";
            try {
                mimeType = data.getString(0);
                intentFilters.add(createIntentFilter(mimeType));
            } catch (MalformedMimeTypeException e) {
                callbackContext.error("Invalid MIME Type " + mimeType);
                return false;
            }
            startNfc();
            callbackContext.success();
            return true;

        } else if (action.equalsIgnoreCase(REGISTER_NDEF)) {
            addTechList(new String[]{Ndef.class.getName()});
            startNfc();
            callbackContext.success();
            return true;

        } else if (action.equalsIgnoreCase(REGISTER_NDEF_FORMATABLE)) {
            addTechList(new String[]{NdefFormatable.class.getName()});
            startNfc();
            callbackContext.success();
            return true;

        }  else if (action.equals(REGISTER_DEFAULT_TAG)) {
            addTagFilter();
            startNfc();
            callbackContext.success();
            return true;

        } else if (action.equalsIgnoreCase(CONNECT)) {
            if (tagWorker != null) {
                callbackContext.error("Already connected");
                return true;
            }
            if (savedIntent == null) {
                callbackContext.error("No tag is detected");
                return true;
            }
            Tag tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            TagTechnology tech = Util.ndefTechForTag(tag);
            if (tech == null) {
                callbackContext.error("Tag doesn't support NDEF");
                return true;
            }

            tagWorker = new TagWorker(tech);
            tagWorker.connect(callbackContext);
            return true;

        } else if (action.equalsIgnoreCase(CLOSE)) {
            if (tagWorker == null) {
                callbackContext.error("Tag is not connected");
                return true;
            }
            tagWorker.close(callbackContext);
            tagWorker = null;
            return true;

        } else if (action.equalsIgnoreCase(WRITE_NDEF)) {
            if (tagWorker == null) {
                callbackContext.error("Tag is not connected");
                return true;
            }
            NdefRecord[] records = Util.jsonToNdefRecords(data.getString(0));
            tagWorker.writeNdef(new NdefMessage(records), callbackContext);
            return true;

        } else if (action.equalsIgnoreCase(WRITE_TAG)) {
            if (getIntent() == null) {  // TODO remove this and handle LostTag
                callbackContext.error("Failed to write tag, received null intent");
            }

            final Tag tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            NdefRecord[] records = Util.jsonToNdefRecords(data.getString(0));
            writeTag(new NdefMessage(records), tag, callbackContext);
            return true;

        } else if (action.equalsIgnoreCase(SHARE_TAG)) {

            NdefRecord[] records = Util.jsonToNdefRecords(data.getString(0));
            this.p2pMessage = new NdefMessage(records);

            startNdefPush();

            callbackContext.success();
            return true;

        } else if (action.equalsIgnoreCase(UNSHARE_TAG)) {
            p2pMessage = null;
            stopNdefPush();
            callbackContext.success();
            return true;

        } else if (action.equalsIgnoreCase(INIT)) {
            Log.d(TAG, "Enabling plugin " + getIntent());

            startNfc();
            if (!recycledIntent()) {
                parseMessage();
            }
            callbackContext.success();
            return true;

        }
        Log.d(TAG, "no result");
        return false;
    }

    private void createPendingIntent() {
        if (pendingIntent == null) {
            Activity activity = getActivity();
            Intent intent = new Intent(activity, activity.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
        }
    }

    private void addTechList(String[] list) {
        this.addTechFilter();
        this.addToTechList(list);
    }

    private void addTechFilter() {
        intentFilters.add(new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED));
    }

    private void addTagFilter() {
        intentFilters.add(new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED));
    }

    private void startNfc() {
        createPendingIntent(); // onResume can call startNfc before execute

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null) {
                    nfcAdapter.enableForegroundDispatch(getActivity(), getPendingIntent(), getIntentFilters(), getTechLists());

                    if (p2pMessage != null) {
                        nfcAdapter.enableForegroundNdefPush(getActivity(), p2pMessage);
                    }

                }
            }
        });
    }

    private void stopNfc() {
        Log.d(TAG, "stopNfc");
        getActivity().runOnUiThread(new Runnable() {
            public void run() {

                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null) {
                    nfcAdapter.disableForegroundDispatch(getActivity());
                    nfcAdapter.disableForegroundNdefPush(getActivity());
                }
            }
        });
    }

    private void startNdefPush() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {

                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null) {
                    nfcAdapter.enableForegroundNdefPush(getActivity(), p2pMessage);
                }
            }
        });
    }

    private void stopNdefPush() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {

                NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                if (nfcAdapter != null) {
                    nfcAdapter.disableForegroundNdefPush(getActivity());
                }

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

    void parseMessage() {
        Intent intent = getIntent();
        Log.d(TAG, "parseMessage " + intent);
        String action = intent.getAction();
        Log.d(TAG, "action " + action);
        if (action == null) { return; }

        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Parcelable[] messages = intent.getParcelableArrayExtra((NfcAdapter.EXTRA_NDEF_MESSAGES));

        if (action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
            Ndef ndef = Ndef.get(tag);
            fireNdefEvent(NDEF_MIME, ndef, messages);

        } else if (action.equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
            for (String tagTech : tag.getTechList()) {
                Log.d(TAG, tagTech);
                if (tagTech.equals(NdefFormatable.class.getName())) {
                    fireNdefEvent(NDEF_FORMATABLE, null, null);
                } else if (tagTech.equals(Ndef.class.getName())) { //
                    Ndef ndef = Ndef.get(tag);
                    fireNdefEvent(NDEF, ndef, messages);
                }
            }
        }

        if (action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            fireTagEvent(tag);
        }

        setIntent(new Intent());
    }

    private void fireNdefEvent(String type, Ndef ndef, Parcelable[] messages) {

        String javascriptTemplate =
            "var e = document.createEvent(''Events'');\n" +
            "e.initEvent(''{0}'');\n" +
            "e.tag = {1};\n" +
            "document.dispatchEvent(e);";

        JSONObject jsonObject = buildNdefJSON(ndef, messages);
        String tag = jsonObject.toString();

        String command = MessageFormat.format(javascriptTemplate, type, tag);
        Log.v(TAG, command);
        this.webView.sendJavascript(command);

    }

    private void fireTagEvent (Tag tag) {
        String javascriptTemplate =
            "var e = document.createEvent(''Events'');\n" +
            "e.initEvent(''{0}'');\n" +
            "e.tag = {1};\n" +
            "document.dispatchEvent(e);";

        String command = MessageFormat.format(javascriptTemplate, TAG_DEFAULT, Util.tagToJSON(tag));
        Log.v(TAG, command);
        this.webView.sendJavascript(command);
    }

    JSONObject buildNdefJSON(Ndef ndef, Parcelable[] messages) {

        JSONObject json = Util.ndefToJSON(ndef);

        // ndef is null for peer-to-peer
        // ndef and messages are null for ndef format-able
        if (ndef == null && messages != null) {

            try {

                if (messages.length > 0) {
                    NdefMessage message = (NdefMessage) messages[0];
                    json.put("ndefMessage", Util.messageToJSON(message));
                    // guessing type, would prefer a more definitive way to determine type
                    json.put("type", "NDEF Push Protocol");
                }

                if (messages.length > 1) {
                    Log.wtf(TAG, "Expected one ndefMessage but found " + messages.length);
                }

            } catch (JSONException e) {
                // shouldn't happen
                Log.e(Util.TAG, "Failed to convert ndefMessage into json", e);
            }
        }
        return json;
    }

    private void writeTag(NdefMessage message, Tag tag, CallbackContext ctx) {
        boolean oneShotWorker = false;
        if (tagWorker == null) {
            // Deprecated stateless usage without prior connect()

            TagTechnology tech = Util.ndefTechForTag(tag);
            if (tech == null) {
                ctx.error("Tag doesn't support NDEF");
                return;
            }

            tagWorker = new TagWorker(tech);
            try {
                tagWorker.connectSync();
            } catch (IOException e) {
                ctx.error("NFC tag connect failed: " + e.getMessage());
                tagWorker = null;
                return;
            }
            oneShotWorker = true;
        }

        tagWorker.writeNdef(message, ctx);

        if (oneShotWorker) {
            tagWorker.closeWhenDone();
            tagWorker = null;
        }
    }

    private boolean recycledIntent() { // TODO this is a kludge, find real solution

        int flags = getIntent().getFlags();
        if ((flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
            Log.i(TAG, "Launched from history, killing recycled intent");
            setIntent(new Intent());
            return true;
        }
        return false;
    }

    @Override
    public void onPause(boolean multitasking) {
        Log.d(TAG, "onPause " + getIntent());
        super.onPause(multitasking);
        stopNfc();
    }

    @Override
    public void onResume(boolean multitasking) {
        Log.d(TAG, "onResume " + getIntent());
        super.onResume(multitasking);
        startNfc();
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent " + intent);
        super.onNewIntent(intent);
        setIntent(intent);
        savedIntent = intent;
        parseMessage();
    }

    private Activity getActivity() {
        return this.cordova.getActivity();
    }

    private Intent getIntent() {
        return getActivity().getIntent();
    }

    private void setIntent(Intent intent) {
        getActivity().setIntent(intent);
    }

}
