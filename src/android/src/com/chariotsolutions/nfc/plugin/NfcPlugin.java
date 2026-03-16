package com.chariotsolutions.nfc.plugin;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

public class NfcPlugin extends CordovaPlugin {
    private static final String REGISTER_MIME_TYPE = "registerMimeType";
    private static final String REMOVE_MIME_TYPE = "removeMimeType";
    private static final String REGISTER_NDEF = "registerNdef";
    private static final String REMOVE_NDEF = "removeNdef";
    private static final String REGISTER_NDEF_FORMATABLE = "registerNdefFormatable";
    private static final String REGISTER_DEFAULT_TAG = "registerTag";
    private static final String REMOVE_DEFAULT_TAG = "removeTag";
    private static final String WRITE_TAG = "writeTag";
    private static final String MAKE_READ_ONLY = "makeReadOnly";
    private static final String ERASE_TAG = "eraseTag";
    private static final String SHARE_TAG = "shareTag";
    private static final String UNSHARE_TAG = "unshareTag";
    private static final String HANDOVER = "handover"; // Android Beam
    private static final String STOP_HANDOVER = "stopHandover";
    private static final String ENABLED = "enabled";
    private static final String INIT = "init";
    private static final String SHOW_SETTINGS = "showSettings";

    private static final String NDEF = "ndef";
    private static final String NDEF_MIME = "ndef-mime";
    private static final String NDEF_FORMATABLE = "ndef-formatable";
    private static final String TAG_DEFAULT = "tag";

    private static final String READER_MODE = "readerMode";
    private static final String DISABLE_READER_MODE = "disableReaderMode";

    private static final String CONNECT = "connect";
    private static final String CLOSE = "close";
    private static final String TRANSCEIVE = "transceive";
    private TagTechnology tagTechnology = null;
    private Class<?> tagTechnologyClass;

    private static final String CHANNEL = "channel";

    private static final String STATUS_NFC_OK = "NFC_OK";
    private static final String STATUS_NO_NFC = "NO_NFC";
    private static final String STATUS_NFC_DISABLED = "NFC_DISABLED";
    private static final String STATUS_NDEF_PUSH_DISABLED = "NDEF_PUSH_DISABLED";
    private static final String SOFT_REFRESH_DISCOVERY = "softRefreshDiscovery";
    private static final String TAG = "NfcPlugin";
    private final List<IntentFilter> intentFilters = new ArrayList<>();
    private final ArrayList<String[]> techLists = new ArrayList<>();

    private NdefMessage p2pMessage = null;
    private PendingIntent pendingIntent = null;

    private Intent savedIntent = null;

    private CallbackContext readerModeCallback;
    private CallbackContext channelCallback;

    private PostponedPluginResult postponedPluginResult = null;
    private static Boolean enableExtraLogging = false;

    class PostponedPluginResult {
        private Date moment;
        private PluginResult pluginResult;

        PostponedPluginResult(Date moment, PluginResult pluginResult) {
            this.moment = moment;
            this.pluginResult = pluginResult;
        }

        boolean isValid() {
            return this.moment.after(new Date(new Date().getTime() - 30000));
        }
    }
    
    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "execute " + action);
        if(enableExtraLogging == true) sendLogEvent("execute", action, 0);
        if (action.equalsIgnoreCase(SHOW_SETTINGS)) {
            showSettings(callbackContext);
            return true;
        }

        if (action.equalsIgnoreCase(CHANNEL)) {
            channelCallback = callbackContext;
            if (postponedPluginResult != null) {
                Log.i(TAG, "Postponed plugin result available");

                if (postponedPluginResult.isValid()) {
                    Log.i(TAG, "Postponed plugin result is valid, resending it now");
                    sendLogEvent("execute channel", "Postponed plugin result is valid, resending it now", 0);
                    channelCallback.sendPluginResult(postponedPluginResult.pluginResult);
                } else {
                    Log.i(TAG, "Postponed plugin result not valid anymore, so ignoring it");
                    sendLogEvent("execute channel", "Postponed plugin result not valid anymore, so ignoring it", 2);
                }

                postponedPluginResult = null;
            }
            return true;
        }

        if (action.equalsIgnoreCase(DISABLE_READER_MODE)) {
            disableReaderMode(callbackContext);
            return true;
        }

        if (!getNfcStatus().equals(STATUS_NFC_OK)) {
            callbackContext.error(getNfcStatus());
            return true;
        }

        createPendingIntent();

        switch (action) {
            case SOFT_REFRESH_DISCOVERY:
                softRefreshDiscovery(data.getString(0), callbackContext);
                break;
            case READER_MODE:
                int flags = data.getInt(0);
                readerMode(flags, callbackContext);
                break;

            case REGISTER_MIME_TYPE:
                registerMimeType(data, callbackContext);
                break;

            case REMOVE_MIME_TYPE:
                removeMimeType(data, callbackContext);
                break;

            case REGISTER_NDEF:
                registerNdef(callbackContext);
                break;

            case REMOVE_NDEF:
                removeNdef(callbackContext);
                break;

            case REGISTER_NDEF_FORMATABLE:
                registerNdefFormatable(callbackContext);
                break;

            case REGISTER_DEFAULT_TAG:
                registerDefaultTag(callbackContext);
                break;

            case REMOVE_DEFAULT_TAG:
                removeDefaultTag(callbackContext);
                break;

            case WRITE_TAG:
                writeTag(data, callbackContext);
                break;

            case MAKE_READ_ONLY:
                makeReadOnly(callbackContext);
                break;

            case ERASE_TAG:
                eraseTag(callbackContext);
                break;

            case SHARE_TAG:
                shareTag(data, callbackContext);
                break;

            case UNSHARE_TAG:
                unshareTag(callbackContext);
                break;

            case HANDOVER:
                handover(data, callbackContext);
                break;

            case STOP_HANDOVER:
                stopHandover(callbackContext);
                break;

            case INIT:
                init(callbackContext);
                break;

            case ENABLED:
                callbackContext.success(STATUS_NFC_OK);
                break;

            case CONNECT:
                String tech = data.getString(0);
                int timeout = data.optInt(1, -1);
                connect(tech, timeout, callbackContext);
                break;

            case TRANSCEIVE:
                CordovaArgs args = new CordovaArgs(data);
                byte[] command = args.getArrayBuffer(0);
                transceive(command, callbackContext);
                break;

            case CLOSE:
                close(callbackContext);
                break;

            default:
                return false;
        }

        return true;
    }
private void softRefreshDiscovery(String action, CallbackContext callbackContext) {
    getActivity().runOnUiThread(() -> {
        try{
            String message = "softRefreshDiscovery ," + action + ", ";
            if(action.equalsIgnoreCase("enableExtraLogging")){
                enableExtraLogging = true;
            }
            Log.d(TAG, "softRefreshDiscovery " + action);
            message = message + " enableExtraLogging = (" + (enableExtraLogging.toString()) + "),";
            NfcAdapter ad = NfcAdapter.getDefaultAdapter(getActivity());
            if(ad == null){
                Log.d(TAG, "softRefreshDiscovery nfcAdapter is null");
            } else {
                if(action.equalsIgnoreCase("resetDiscoveryTechnology")){
                    message = message + "rdt ";
                    ad.resetDiscoveryTechnology(getActivity());
                }   
                if(action.equalsIgnoreCase("setDiscoveryTechnologyKeep")){
                    message = message + "sdtk ";
                    ad.setDiscoveryTechnology(getActivity(),ad.FLAG_READER_DISABLE, ad.FLAG_LISTEN_KEEP);
                }    
                if(action.equalsIgnoreCase("setDiscoveryTechnologyDisable")){
                    message = message + "sdtd ";
                    ad.setDiscoveryTechnology(getActivity(),ad.FLAG_READER_DISABLE, ad.FLAG_LISTEN_DISABLE);
                }    
                message = message + " NfcAdapter.isEnabled (" + (ad.isEnabled()) + "),";
                // does not work on android 9
                //message = message + " isSecureNfcSupported (" + (ad.isSecureNfcSupported()) + "),";
            }
            if(action.equalsIgnoreCase("resetNfcOnAndroid")){
                message = message + "rnoa ";
                Bundle extras = new Bundle();
                readerModeCallback = callbackContext;
                getActivity().runOnUiThread(() -> {
                    NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
                    if (nfcAdapter != null) {
                        int flags = NfcAdapter.FLAG_READER_NFC_A
                                    | NfcAdapter.FLAG_READER_NFC_B
                                    | NfcAdapter.FLAG_READER_NFC_F
                                    | NfcAdapter.FLAG_READER_NFC_V
                                    | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK;                            
                        nfcAdapter.enableReaderMode(getActivity(), callback, flags, null);
                        nfcAdapter.disableReaderMode(getActivity());
                    } else {
                        callbackContext.error("NFC Adapter not available");
                    }
                });                
            }
            IntentFilter[] intentFilters = getIntentFilters();
            message = message + " intentFilters count (" + (intentFilters.length) + "),";
            
            String[][] techLists = getTechLists();
            message = message + " techLists count (" + (techLists.length) + "),";
            message = message + " enableExtraLogging = (" + (enableExtraLogging) + ")";
            //message = message + " isTagIntentAllowed is Null(" + (ad.isTagIntentAllowed()) + ")";
            message = message + " NfcAdapter (" + (ad == null ? "Is Null" : "Is Not Null") + "),";
            message = message + " readerModeCallback (" + (readerModeCallback == null ? "Is Null" : "Is Not Null") + "),";
            message = message + " channelCallback (" + (channelCallback == null ? "Is Null" : "Is Not Null") + "),";
            message = message + " savedIntent (" + (savedIntent == null ? "Is Null" : "Is Not Null") + "),";
            message = message + " pendingIntent (" + (pendingIntent == null ? "Is Null" : "Is Not Null") + "),";
            message = message + " getActivity (" + (getActivity() == null ? "Is Null" : "Is Not Null") + "),";
            Intent intent = getIntent();
            message = message + " getIntent (" + (intent == null ? "Is Null" : "Is Not Null") + "),";
            if(intent != null){
                String action2 = intent.getAction();
                message = message + " intent action (" + (action2 == null ? "Is Null" : action2) + "),";
            }
            if(enableExtraLogging == true) sendLogEvent("softRefreshDiscovery " + action + " ", message, 1);
            callbackContext.success(message);
        } catch (Exception e) {
               sendLogEvent("softRefreshDiscovery " + action + " ", "Exception Error: " + e.getMessage(), 2);    
               callbackContext.error(e.getMessage());
        }        
    });
}
 
    private String getNfcStatus() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (nfcAdapter == null) {
            return STATUS_NO_NFC;
        } else if (!nfcAdapter.isEnabled()) {
            return STATUS_NFC_DISABLED;
        } else {
            return STATUS_NFC_OK;
        }
    }

    private void readerMode(int flags, CallbackContext callbackContext) {
        Log.d(TAG, "readerMode ");
        Bundle extras = new Bundle();
        readerModeCallback = callbackContext;
        getActivity().runOnUiThread(() -> {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
            if (nfcAdapter != null) {
                nfcAdapter.enableReaderMode(getActivity(), callback, flags, extras);
            } else {
                callbackContext.error("NFC Adapter not available");
            }
        });
    }
    

    private void disableReaderMode(CallbackContext callbackContext) {
        Log.d(TAG, "disableReaderMode ");
        getActivity().runOnUiThread(() -> {
            readerModeCallback = null;
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
            if (nfcAdapter != null) {
                nfcAdapter.disableReaderMode(getActivity());
            }
            callbackContext.success();
        });
    }

    private final NfcAdapter.ReaderCallback callback = tag -> {
        Log.d(TAG, "ReaderCallback 1 ");
        JSONObject json;
        List<String> techList = Arrays.asList(tag.getTechList());
        if (techList.contains(Ndef.class.getName())) {
            Ndef ndef = Ndef.get(tag);
            json = Util.ndefToJSON(ndef);
        } else {
            json = Util.tagToJSON(tag);
        }

        Intent tagIntent = new Intent();
        tagIntent.putExtra(NfcAdapter.EXTRA_TAG, tag);
        setIntent(tagIntent);

        PluginResult result = new PluginResult(PluginResult.Status.OK, json);
        result.setKeepCallback(true);
        if (readerModeCallback != null) {
            readerModeCallback.sendPluginResult(result);
        } else {
            Log.i(TAG, "readerModeCallback is null - reader mode probably disabled in the meantime");
            sendLogEvent("ReaderCallback", "readerModeCallback is null - reader mode probably disabled in the meantime", 2);
        }
    };

    private void registerDefaultTag(CallbackContext callbackContext) {
        Log.d(TAG, "registerDefaultTag ");
        addTagFilter();
        restartNfc();
        callbackContext.success();
    }

    private void removeDefaultTag(CallbackContext callbackContext) {
        Log.d(TAG, "removeDefaultTag ");
        removeTagFilter();
        restartNfc();
        callbackContext.success();
    }

    private void registerNdefFormatable(CallbackContext callbackContext) {
        Log.d(TAG, "registerNdefFormatable ");
        addTechList(new String[]{NdefFormatable.class.getName()});
        restartNfc();
        callbackContext.success();
    }

    private void registerNdef(CallbackContext callbackContext) {
        Log.d(TAG, "registerNdef ");
        addTechList(new String[]{Ndef.class.getName()});
        restartNfc();
        callbackContext.success();
    }

    private void removeNdef(CallbackContext callbackContext) {
        Log.d(TAG, "removeNdef ");
        removeTechList(new String[]{Ndef.class.getName()});
        restartNfc();
        callbackContext.success();
    }

    private void unshareTag(CallbackContext callbackContext) {
        callbackContext.success();
    }

    private void init(CallbackContext callbackContext) {
        Log.d(TAG, "Enabling plugin " + getIntent());
        startNfc();
        if (!recycledIntent()) {
            parseMessage();
        }
        callbackContext.success();
    }

    private void removeMimeType(JSONArray data, CallbackContext callbackContext) throws JSONException {
        String mimeType = data.getString(0);
        Log.d(TAG, "removeMimeType mimeType=" + mimeType);
        removeIntentFilter(mimeType);
        restartNfc();
        callbackContext.success();
    }

    private void registerMimeType(JSONArray data, CallbackContext callbackContext) throws JSONException {
        String mimeType = "";
        try {
            mimeType = data.getString(0);
            Log.d(TAG, "registerMimeType mimeType=" + mimeType);
            intentFilters.add(createIntentFilter(mimeType));
            restartNfc();
            callbackContext.success();
        } catch (MalformedMimeTypeException e) {
            sendLogEvent("registerMimeType", "Error " + e.getMessage(), 2);            
            callbackContext.error("Invalid MIME Type " + mimeType);
        }
    }

    private void eraseTag(CallbackContext callbackContext) {
        Log.d(TAG, "eraseTag ");
        Tag tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        NdefRecord[] records = {
            new NdefRecord(NdefRecord.TNF_EMPTY, new byte[0], new byte[0], new byte[0])
        };
        writeNdefMessage(new NdefMessage(records), tag, callbackContext);
    }

    private void writeTag(JSONArray data, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "writeTag ");
        if (getIntent() == null) {
            sendLogEvent("writeTag", "Failed to write tag, received null intent ", 2);   
            callbackContext.error("Failed to write tag, received null intent");
        }

        Tag tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        NdefRecord[] records = Util.jsonToNdefRecords(data.getString(0));
        writeNdefMessage(new NdefMessage(records), tag, callbackContext);
    }

   private void writeNdefMessage(final NdefMessage message, final Tag tag, final CallbackContext callbackContext) {
        Log.d(TAG, "writeNdefMessage ");
       cordova.getThreadPool().execute(() -> {
           try {
               Ndef ndef = Ndef.get(tag);
               if (ndef != null) {
                   ndef.connect();
                   if (ndef.isWritable()) {
                       int size = message.toByteArray().length;
                       if (ndef.getMaxSize() < size) {
                        sendLogEvent("writeNdefMessage", "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size + " bytes.", 2);  
                           callbackContext.error("Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size + " bytes.");
                       } else {
                           ndef.writeNdefMessage(message);
                           callbackContext.success();
                       }
                   } else {
                       callbackContext.error("Tag is read only");
                       sendLogEvent("writeNdefMessage", "Tag is read only", 2);  
                   }
                   ndef.close();
               } else {
                   NdefFormatable formatable = NdefFormatable.get(tag);
                   if (formatable != null) {
                       formatable.connect();
                       formatable.format(message);
                       callbackContext.success();
                       formatable.close();
                   } else {
                       callbackContext.error("Tag doesn't support NDEF");
                       sendLogEvent("writeNdefMessage", "Tag doesn't support NDEF", 2);  
                   }
               }
           } catch (FormatException e) {
               callbackContext.error(e.getMessage());
               sendLogEvent("writeNdefMessage", "FormatException Error: " + e.getMessage(), 2);    
           } catch (TagLostException e) {
                sendLogEvent("writeNdefMessage", "TagLostException Error: " + e.getMessage(), 2);    
               callbackContext.error(e.getMessage());
           } catch (IOException e) {
               sendLogEvent("writeNdefMessage", "IOException Error: " + e.getMessage(), 2);    
               callbackContext.error(e.getMessage());
           }
       });
   }

    private void makeReadOnly(final CallbackContext callbackContext) {
        Log.d(TAG, "makeReadOnly ");
        if (getIntent() == null) {
            sendLogEvent("makeReadOnly", "Failed to make tag read only, received null intent", 2);  
            callbackContext.error("Failed to make tag read only, received null intent");
            return;
        }

        final Tag tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            sendLogEvent("makeReadOnly", "Failed to make tag read only, tag is null", 2);  
            callbackContext.error("Failed to make tag read only, tag is null");
            return;
        }

        cordova.getThreadPool().execute(() -> {
            boolean success = false;
            String message = "Could not make tag read only";

            Ndef ndef = Ndef.get(tag);

            try {
                if (ndef != null) {
                    ndef.connect();

                    if (!ndef.isWritable()) {
                        message = "Tag is not writable";
                    } else if (ndef.canMakeReadOnly()) {
                        success = ndef.makeReadOnly();
                    } else {
                        message = "Tag cannot be made read only";
                    }
                } else {
                    message = "Tag is not NDEF";
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to make tag read only", e);
                if (e.getMessage() != null) {
                    message = e.getMessage();
                } else {
                    message = e.toString();
                }
                sendLogEvent("makeReadOnly", "IOException Error: " + message, 2);    
            }

            if (success) {
                callbackContext.success();
            } else {
                callbackContext.error(message);
            }
        });
    }

    private void shareTag(JSONArray data, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "shareTag ");
        callbackContext.success();
    }

    private void handover(JSONArray data, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "handover ");
        callbackContext.success();
    }

    private void stopHandover(CallbackContext callbackContext) {
        Log.d(TAG, "stopHandover ");
        callbackContext.success();
    }

    private void showSettings(CallbackContext callbackContext) {
        Log.d(TAG, "showSettings ");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NFC_SETTINGS);
            getActivity().startActivity(intent);
        } else {
            Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
            getActivity().startActivity(intent);
        }
        callbackContext.success();
    }

    private void createPendingIntent() {
        if (pendingIntent == null) {
            Log.d(TAG, "createPendingIntent: Does not exist, so create one.");
            Activity activity = getActivity();
            Intent intent = new Intent(activity, activity.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_MUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
            }
        } else {
            Log.d(TAG, "createPendingIntent: Already exists");
        }
    }

    private void addTechList(String[] list) {
        Log.d(TAG, "addTechList ");
        addTechFilter();
        addToTechList(list);
    }

    private void removeTechList(String[] list) {
        Log.d(TAG, "removeTechList ");
        removeTechFilter();
        removeFromTechList(list);
    }

    private void addTechFilter() {
        Log.d(TAG, "addTechFilter ");
        intentFilters.add(new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED));
    }

    private void removeTechFilter() {
        Log.d(TAG, "removeTechFilter ");
        Iterator<IntentFilter> iterator = intentFilters.iterator();
        while (iterator.hasNext()) {
            IntentFilter intentFilter = iterator.next();
            if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intentFilter.getAction(0))) {
                iterator.remove();
            }
        }
    }

    private void addTagFilter() {
        Log.d(TAG, "addTagFilter ");
        intentFilters.add(new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED));
    }

    private void removeTagFilter() {
        Log.d(TAG, "removeTagFilter ");
        Iterator<IntentFilter> iterator = intentFilters.iterator();
        while (iterator.hasNext()) {
            IntentFilter intentFilter = iterator.next();
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intentFilter.getAction(0))) {
                iterator.remove();
            }
        }
    }

    private void restartNfc() {
        Log.d(TAG, "restartNfc Begin");
        stopNfc();
        startNfc();
        Log.d(TAG, "restartNfc End");
    }

    private void startNfc() {
        Log.d(TAG, "startNfc Begin");
        createPendingIntent();

        getActivity().runOnUiThread(() -> {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                try{
                    if(nfcAdapter == null){
                        sendLogEvent("startNfc", "nfcAdapter is null, waiting 5 seconds" , 2); 
                        // wait 5 seconds
                        Thread.sleep(5000);
                        nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
                    }
                    if (getActivity().isFinishing() == true){
                        sendLogEvent("startNfc", "nfcAdapter is not null, but activity is finishing, waiting 5 seconds" , 2); 
                        Thread.sleep(5000);
                    } 
                    
                } catch( Exception ex){
                    sendLogEvent("startNfc", "Error trying to wait for 5 seconds. Error: " + ex.getMessage(), 2);  
                }
            if(nfcAdapter == null){
                sendLogEvent("startNfc", "nfcAdapter is still null after 5 seconds." , 2); 
            } else if (getActivity().isFinishing() == true){
                sendLogEvent("startNfc", "nfcAdapter is not null, but activity is still finishing after 5 seconds." , 2); 
            } else if (nfcAdapter != null && !getActivity().isFinishing()) {
                try {
                    IntentFilter[] intentFilters = getIntentFilters();
                    String[][] techLists = getTechLists();
                    if (intentFilters.length > 0 || techLists.length > 0) {
                        nfcAdapter.enableForegroundDispatch(getActivity(), getPendingIntent(), intentFilters, techLists);
                    } else {
                        sendLogEvent("startNfc", "intentFilters and techLists lengths are zero" , 2); 
                    }
                } catch (IllegalStateException e) {
                    Log.w(TAG, "Illegal State Exception starting NFC. Assuming application is terminating.");
                    sendLogEvent("startNfc", "Illegal State Exception starting NFC. Assuming application is terminating. Error: " + e.getMessage(), 2);    
                }
            } else {
                sendLogEvent("startNfc", "This can never happen." , 2); 
            }
            Log.d(TAG, "startNfc End");
        });
    }

    private void stopNfc() {
        getActivity().runOnUiThread(() -> {
            Log.d(TAG, "stopNfc Begin");
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

            if (nfcAdapter != null) {
                try {
                    nfcAdapter.disableForegroundDispatch(getActivity());
                } catch (IllegalStateException e) {
                    Log.w(TAG, "Illegal State Exception stopping NFC. Assuming application is terminating.");
                    sendLogEvent("stopNfc", "Illegal State Exception starting NFC. Assuming application is terminating. Error: " + e.getMessage(), 2);    
                }
            } else {
                Log.d(TAG, "stopNfc nfcAdapter is null");
            }
            Log.d(TAG, "stopNfc End");
        });
    }

    private void addToTechList(String[] techs) {
        Log.d(TAG, "addToTechList ");
        techLists.add(techs);
    }

    private void removeFromTechList(String[] techs) {
        Log.d(TAG, "removeFromTechList ");
        Iterator<String[]> iterator = techLists.iterator();
        while (iterator.hasNext()) {
            String[] list = iterator.next();
            if (Arrays.equals(list, techs)) {
                iterator.remove();
            }
        }
    }

    private void removeIntentFilter(String mimeType) {
        Log.d(TAG, "removeIntentFilter ");
        Iterator<IntentFilter> iterator = intentFilters.iterator();
        while (iterator.hasNext()) {
            IntentFilter intentFilter = iterator.next();
            String mt = intentFilter.getDataType(0);
            if (mimeType.equals(mt)) {
                iterator.remove();
            }
        }
    }

    private IntentFilter createIntentFilter(String mimeType) throws MalformedMimeTypeException {
        Log.d(TAG, "createIntentFilter ");
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
        return techLists.toArray(new String[0][0]);
    }

    private void parseMessage() {
        cordova.getThreadPool().execute(() -> {
            Log.d(TAG, "parseMessage Begin intent=" + getIntent());
            Intent intent = getIntent();
            String action = intent.getAction();
            Log.d(TAG, "parseMessage action=" + action);
            if (action == null) {
                return;
            }

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if (action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
                Ndef ndef = Ndef.get(tag);
                fireNdefEvent(NDEF_MIME, ndef, messages);
            } else if (action.equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
                for (String tagTech : tag.getTechList()) {
                    Log.d(TAG, tagTech);
                    if (tagTech.equals(NdefFormatable.class.getName())) {
                        fireNdefFormatableEvent(tag);
                    } else if (tagTech.equals(Ndef.class.getName())) {
                        Ndef ndef = Ndef.get(tag);
                        fireNdefEvent(NDEF, ndef, messages);
                    }
                }
            } else if (action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
                fireTagEvent(tag, messages);
            } else {
                Log.d(TAG, "parseMessage Unhandled if else");
            }

            setIntent(new Intent());
            Log.d(TAG, "parseMessage End");
        });
    }

private void sendLogEvent(String source, String message, Integer errorLevel){
    try {
        if(errorLevel > 1){
            Log.e(TAG, "sendLogEvent source=" + source + " ErrorLevel=" + errorLevel.toString() +  " message=" + message);
        } else {
            Log.d(TAG, "sendLogEvent source=" + source + " ErrorLevel=" + errorLevel.toString() +  " message=" + message);
        }
        JSONObject event = new JSONObject();
        event.put("type", "sendLogEvent");
        event.put("errorLevel", errorLevel);
        event.put("source", source);
        event.put("message", message);
        event.put("timestamp", new Date());
        event.put("nfcStatus", getNfcStatus());
        sendEvent("ndef", event);
    } catch (JSONException e) {
        Log.e(TAG, "Error sendLogEvent", e);
    }
}
    private void sendEvent(String type, JSONObject tag) {
        try {
            Log.d(TAG, "sendEvent type=" + type);
            JSONObject event = new JSONObject();
            event.put("type", type);
            event.put("tag", tag);

            PluginResult result = new PluginResult(PluginResult.Status.OK, event);
            result.setKeepCallback(true);

            if (channelCallback != null) {
                channelCallback.sendPluginResult(result);
            } else {
                Log.d(TAG, "sendEvent Postponed type=" + type);
                postponedPluginResult = new PostponedPluginResult(new Date(), result);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error sending NFC event through the channel", e);
        }
    }

    private void fireNdefEvent(String type, Ndef ndef, Parcelable[] messages) {
        Log.d(TAG, "fireNdefEvent type=" + type);
        JSONObject json = buildNdefJSON(ndef, messages);
        sendEvent(type, json);
    }

    private void fireNdefFormatableEvent(Tag tag) {
        Log.d(TAG, "fireNdefFormatableEvent " + tag);
        sendEvent(NDEF_FORMATABLE, Util.tagToJSON(tag));
    }

    private void fireTagEvent(Tag tag, Parcelable[] messages) {
        Log.d(TAG, "fireTagEvent tag=" + tag);
        sendEvent(TAG_DEFAULT, Util.tagToJSON(tag));
    }

    private JSONObject buildNdefJSON(Ndef ndef, Parcelable[] messages) {
        JSONObject json = Util.ndefToJSON(ndef);
        Log.d(TAG, "buildNdefJSON Begin");
        if (ndef == null && messages != null) {
            try {
                if (messages.length > 0) {
                    NdefMessage message = (NdefMessage) messages[0];
                    json.put("ndefMessage", Util.messageToJSON(message));
                    json.put("type", "NDEF Push Protocol");
                }

                if (messages.length > 1) {
                    sendLogEvent("buildNdefJSON", "Expected one ndefMessage but found " + messages.length, 2);
                    Log.wtf(TAG, "Expected one ndefMessage but found " + messages.length);
                }
            } catch (JSONException e) {
                Log.e(Util.TAG, "Failed to convert ndefMessage into json", e);
                sendLogEvent("buildNdefJSON", "Failed to convert ndefMessage into json " + e.getMessage(), 2);
            }
        }
        Log.d(TAG, "buildNdefJSON End");
        return json;
    }

    private boolean recycledIntent() {
        Log.d(TAG, "recycledIntent ");
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
        Log.d(TAG, "onPause Begin multitasking=" + multitasking + " Intent=" + getIntent());
        super.onPause(multitasking);
        if (multitasking) {
            stopNfc();
        }
        Log.d(TAG, "onPause End multitasking=" + multitasking + " Intent=" + getIntent());
    }

    @Override
    public void onResume(boolean multitasking) {
        Log.d(TAG, "onResume Begin multitasking=" + multitasking + " Intent=" + getIntent());
        super.onResume(multitasking);
        startNfc();
        Log.d(TAG, "onResume End multitasking=" + multitasking + " Intent=" + getIntent());
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent Begin " + intent);
        super.onNewIntent(intent);
        setIntent(intent);
        savedIntent = intent;
        parseMessage();
        Log.d(TAG, "onNewIntent End " + intent);
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

    private void connect(final String tech, final int timeout, final CallbackContext callbackContext) {
        Log.d(TAG, "connect ");
        this.cordova.getThreadPool().execute(() -> {
            try {
                Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if (tag == null && savedIntent != null) {
                    tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                }

                if (tag == null) {
                    Log.e(TAG, "No Tag");
                    sendLogEvent("connect", "Error No Tag", 2);
                    callbackContext.error("No Tag");
                    return;
                }

                JSONObject resultObject = new JSONObject();

                List<String> techList = Arrays.asList(tag.getTechList());
                if (techList.contains(tech)) {
                    tagTechnologyClass = Class.forName(tech);
                    Method method = tagTechnologyClass.getMethod("get", Tag.class);
                    tagTechnology = (TagTechnology) method.invoke(null, tag);

                    try {
                        Method maxTransceiveLengthMethod = tagTechnologyClass.getMethod("getMaxTransceiveLength");
                        resultObject.put("maxTransceiveLength", maxTransceiveLengthMethod.invoke(tagTechnology));
                    } catch (NoSuchMethodException e) {
                        // Some technologies do not support this, so just ignore.
                    } catch (JSONException e) {
                        Log.e(TAG, "Error serializing JSON", e);
                        sendLogEvent("connect", "Error serializing JSON" + e.getMessage(), 2);
                    }
                }

                if (tagTechnology == null) {
                    callbackContext.error("Tag does not support " + tech);
                    sendLogEvent("connect", "Tag does not support " + tech, 2);
                    return;
                }

                tagTechnology.connect();
                setTimeout(timeout);
                callbackContext.success(resultObject);

            } catch (IOException ex) {
                Log.e(TAG, "Tag connection failed", ex);
                sendLogEvent("connect", "Tag connection failed: " + ex.getMessage(), 2);
                callbackContext.error("Tag connection failed");

            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                Log.e(TAG, e.getMessage(), e);
                sendLogEvent("connect", "Error" + e.getMessage(), 2);
                callbackContext.error(e.getMessage());
            }
        });
    }

    private void setTimeout(int timeout) {
        if (timeout < 0) {
            return;
        }
        try {
            Method setTimeout = tagTechnologyClass.getMethod("setTimeout", int.class);
            setTimeout.invoke(tagTechnology, timeout);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            // ignore
        }
    }

    private void close(CallbackContext callbackContext) {
        Log.d(TAG, "close ");
        cordova.getThreadPool().execute(() -> {
            try {
                if (tagTechnology != null && tagTechnology.isConnected()) {
                    tagTechnology.close();
                    tagTechnology = null;
                    callbackContext.success();
                } else {
                    callbackContext.success();
                }
            } catch (IOException ex) {
                Log.e(TAG, "Error closing nfc connection", ex);
                sendLogEvent("close", "Error closing nfc connection " + ex.getMessage(), 2);
                callbackContext.error("Error closing nfc connection " + ex.getLocalizedMessage());
            }
        });
    }

    private void transceive(final byte[] data, final CallbackContext callbackContext) {
        Log.d(TAG, "transceive ");
        cordova.getThreadPool().execute(() -> {
            try {
                if (tagTechnology == null) {
                    Log.e(TAG, "No Tech");
                    callbackContext.error("No Tech");
                    return;
                }
                if (!tagTechnology.isConnected()) {
                    Log.e(TAG, "Not connected");
                    callbackContext.error("Not connected");
                    return;
                }

                Method transceiveMethod = tagTechnologyClass.getMethod("transceive", byte[].class);
                byte[] response = (byte[]) transceiveMethod.invoke(tagTechnology, data);

                callbackContext.success(response);

            } catch (NoSuchMethodException e) {
                String error = "TagTechnology " + tagTechnologyClass.getName() + " does not have a transceive function";
                Log.e(TAG, error, e);
                sendLogEvent("transceive", error + " " + e.getMessage(), 2);
                callbackContext.error(error);
            } catch (NullPointerException | IllegalAccessException | InvocationTargetException e) {
                Log.e(TAG, e.getMessage(), e);
                sendLogEvent("transceive", "Error " + e.getMessage(), 2);
                callbackContext.error(e.getMessage());
            }
        });
    }



}
