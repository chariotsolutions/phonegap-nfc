package com.chariotsolutions.nfc.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.util.Log;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class NdefPlugin extends Plugin {
    private static Stack<Intent> queuedIntents = new Stack<Intent>();
    private static final String REGISTER_MIME_TYPE = "registerMimeType";
    private static final String REGISTER_NDEF = "registerNdef"; 
    private static final String REGISTER_NDEF_FORMATTABLE = "registerNdefFormattable"; 
    private static final String WRITE_TAG = "writeTag";
    private static final String P2P = "p2p";

    private static final String NDEF = "ndef"; 
    private static final String NDEF_MIME = "ndef-mime"; 
    private static final String NDEF_UNFORMATTED = "ndef-unformatted"; 


    private NdefMessage p2pMessage = null;
    private Intent currentIntent = null;
    private static String TAG = "NdefPlugin";
    private PendingIntent pendingIntent = null;
    private List<IntentFilter> intentFilters = null;
    private ArrayList<String[]> techLists = null;

    @Override
    // TODO refactor this into multiple methods
    public PluginResult execute(String action, JSONArray data, String callbackId) {
        initialize();
        
        if (action.equalsIgnoreCase(REGISTER_MIME_TYPE)) {
            try {
                intentFilters.add(addDataTypeToNewIntentFilter(data));
            } catch (InstantiationException e) {
                Log.e(TAG, e.toString());
                return new PluginResult(Status.ERROR);
            }

            enableNfc();            
            parseQueuedMessages();
            
            return new PluginResult(Status.OK);         
        } else if (action.equalsIgnoreCase(REGISTER_NDEF)) {    
            addTechList(new String[] { Ndef.class.getName() });
            enableNfc(); 
            parseQueuedMessages();
            
            return new PluginResult(Status.OK);
        } else if (action.equalsIgnoreCase(REGISTER_NDEF_FORMATTABLE)) {
            addTechList(new String[] { NdefFormatable.class.getName()});
            enableNfc();
            parseQueuedMessages();
            
            return new PluginResult(Status.OK);
        } else if (action.equalsIgnoreCase(WRITE_TAG)) {    
            Tag tag = null;
            if (currentIntent == null) {
                Log.e(TAG, "Failed to write tag, recieved null intent");
                return new PluginResult(Status.ERROR);
            } else {
                tag = currentIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            }
             
            try {
            	NdefRecord[] records = jsonToNdefRecords(data.getString(0));
                writeTag(new NdefMessage(records), tag);            

            } catch (JSONException e) {
            	Log.e(TAG, "error reading ndefMessage from JSON");
            	return new PluginResult(Status.ERROR);			
            }
            
            return new PluginResult(Status.OK);
            
        } else if (action.equalsIgnoreCase(P2P)) {
        	
			try {
				NdefRecord[] records = jsonToNdefRecords(data.getString(0));
	            this.p2pMessage = new NdefMessage(records);
	            	            
	            this.ctx.runOnUiThread(new Runnable() {
	            	public void run() {
	                    NfcAdapter.getDefaultAdapter(NdefPlugin.this.ctx).enableForegroundNdefPush(NdefPlugin.this.ctx, p2pMessage);
	            	}
	            });	            
	            
			} catch (JSONException e) {
				Log.e(TAG, "error reading ndefMessage from JSON");
	            return new PluginResult(Status.ERROR);			
	        }
			
            return new PluginResult(Status.OK);

        }
        Log.d(TAG, "no result");
        return new PluginResult(Status.NO_RESULT);
    }

	private void initialize() {
		if (pendingIntent == null) {
            Intent intent = new Intent(ctx, ctx.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0);
        }
        
        if (techLists == null) { 
        	techLists = new ArrayList<String[]>();
        }
        
        if (intentFilters == null) {
            intentFilters = new ArrayList<IntentFilter>();
        }
	}

	private NdefRecord[] jsonToNdefRecords(String ndefMessageAsJSON) throws JSONException {
		JSONArray jsonRecords = new JSONArray(ndefMessageAsJSON);
		NdefRecord[] records = new NdefRecord[jsonRecords.length()];
		for (int i = 0; i < jsonRecords.length(); i++) {                
			JSONObject record = jsonRecords.getJSONObject(i);
		    byte tnf = (byte) record.getInt("tnf");
		    byte[] type = jsonToByteArray(record.getJSONArray("type"));
		    byte[] id = jsonToByteArray(record.getJSONArray("id"));
		    byte[] payload = jsonToByteArray(record.getJSONArray("payload"));
		    records[i] = new NdefRecord(tnf, type, id, payload);
		}
		return records;
	}

    private void addTechList(String[] list) {
        this.addTechFilter();
        this.addToTechList(list);       
    }

    private void parseQueuedMessages() {
        while(!queuedIntents.isEmpty()) {
            parseMessage(queuedIntents.pop());
        }
    }

    private void addTechFilter() {
        intentFilters.add(new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED));
    }

    private void addToTechList(String[] techs) {
        techLists.add(techs);
    }   
    
    private void enableNfc() {
        try {
            this.ctx.runOnUiThread(new NfcRunnable(ctx,
                this.getPendingIntent(), this.getIntentFilters().toArray(new IntentFilter[this.getIntentFilters().size()]), 
                techLists.toArray(new String[0][0])));
        } catch ( Exception e ) {
            Log.e(TAG, e.toString());
        }
    }

    private IntentFilter addDataTypeToNewIntentFilter(JSONArray data)
            throws InstantiationException {
        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        String mimeType = null;
        try {
            mimeType = (String) data.getString(0);
        } catch (JSONException jsone) {
            Log.e(TAG, "No data type supplied");
            throw new InstantiationException();
        }

        // TODO make this less crap
        if (mimeType != null) {
            try {
                ndef.addDataType(mimeType.toString());
            } catch (MalformedMimeTypeException e) {
                Log.e(TAG, e.toString());
                throw new InstantiationException();
            }
        } else {
            Log.e(TAG, "Data Type was null");
            throw new InstantiationException();
        }
        return ndef;
    }

    private PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    private List<IntentFilter> getIntentFilters() {
        return intentFilters;
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
                    return;
                } else if (tagTech.equalsIgnoreCase(Ndef.class.getName())) {
                    for (Parcelable message : intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)) {
                        fireNdefEvent(NDEF, message);
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
        JSONArray jsonData = messageToJSON((NdefMessage)parcelable);
        fireNdefEvent(type, jsonData);
    }
    
    private void fireNdefEvent(String type, JSONArray ndefMessage) {
        String command = "navigator.nfc.fireEvent('" + type + "', " + ndefMessage + ")";
        this.sendJavascript(command);
    }

    private JSONArray messageToJSON(NdefMessage message) {
        List<JSONObject> list = new ArrayList<JSONObject>(); 
        List<NdefRecord> records = Arrays.asList(message.getRecords());

        for (NdefRecord r : records) {
            list.add(recordToJSON(r));
        }
        return new JSONArray(list);
    }
    
    private JSONObject recordToJSON(NdefRecord record) {
        JSONObject json = new JSONObject();
        try {                   
            json.put("tnf", record.getTnf());
            json.put("type", byteArrayToJSON(record.getType()));
            json.put("id", byteArrayToJSON(record.getId()));
            json.put("payload", byteArrayToJSON(record.getPayload()));
        } catch (JSONException e) {
            //Not sure why this would happen, documentation is unclear.
            Log.e(TAG,"Failed to convert ndef record into json: " + record.toString(), e);
        }
        return json;
    }
    
    private JSONArray byteArrayToJSON(byte[] bytes) {
        JSONArray json = new JSONArray();
        for (int i = 0; i < bytes.length; i++) {
            json.put(bytes[i]);
        }
        return json;
    }
    
    private byte[] jsonToByteArray(JSONArray json) throws JSONException {
        byte[] b = new byte[json.length()];
        for (int i = 0; i < json.length(); i++) {
            b[i] = (byte)json.getInt(i);
        }
        return b;
    }

    private PluginResult writeTag(NdefMessage message, Tag tag) {
        Log.d(TAG, "writeTag");
        int size = message.toByteArray().length;

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    Log.e(TAG, "Failed to write tag - read only");
                    return new PluginResult(Status.ERROR);
                }
                if (ndef.getMaxSize() < size) {
                    Log.e(TAG, "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size + " bytes.");
                    return new PluginResult(Status.ERROR);
                }

                ndef.writeNdefMessage(message);
                return new PluginResult(Status.OK);
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        Log.e(TAG, "Formatted tag and wrote message");
                        return new PluginResult(Status.OK);
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to format tag.");
                        return new PluginResult(Status.ERROR);
                    }
                } else {
                    Log.e(TAG, "Tag doesn't support NDEF.");
                    return new PluginResult(Status.ERROR);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to write tag");
        }
        return new PluginResult(Status.ERROR);
    }

    class NfcRunnable implements Runnable {
        private Activity activity = null;
        private PendingIntent pendingIntent = null;
        private IntentFilter[] intentFilters = null;
        private String[][] techLists = null;
        
        public NfcRunnable(Activity activity, PendingIntent pendingIntent, IntentFilter[] intentFilters, String[][] techLists) {
            this.techLists = techLists;
            this.activity = activity;
            this.pendingIntent = pendingIntent;
            this.intentFilters = intentFilters;
        }

        /**
         * resuming NFC needs to be run on the main (ui thread)
         * http://developer.android.com/reference/android/nfc/NfcAdapter.html#
         * enableForegroundDispatch
         * (android.app.Activity,%20android.app.PendingIntent
         * ,%20android.content.IntentFilter[],%20java.lang.String[][])
         */
        public void run() {
            Log.d(TAG, "starting NFC!");
            NfcAdapter.getDefaultAdapter(activity).enableForegroundDispatch(
                    activity, pendingIntent, intentFilters, techLists);
        }
    }

    class NfcPausable implements Runnable {
        private Activity activity;

        public NfcPausable(Activity activity) {
            this.activity = activity;
        }

        /**
         * pausing NFC needs to be run on the main (ui thread)
         * http://developer.android.com/reference/android/nfc/NfcAdapter.html#
         * disableForegroundDispatch (android.app.Activity)
         */
        public void run() {
            Log.d(TAG, "Pausing NFC");
            NfcAdapter.getDefaultAdapter(activity).disableForegroundDispatch(activity);
            NfcAdapter.getDefaultAdapter(activity).disableForegroundNdefPush(activity);
        }
    }

    public static void saveIntent(Intent intent) {
        queuedIntents.push(intent);
    }
    
    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        this.ctx.runOnUiThread(new NfcPausable(ctx));
    }
    
    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        this.ctx.runOnUiThread(new NfcRunnable(ctx, this.getPendingIntent(), this.getIntentFilters().toArray(new IntentFilter[this.getIntentFilters().size()]), 
                this.techLists.toArray(new String[0][0])));
        
        Intent resumedIntent = ctx.getIntent();
        if(NfcAdapter.ACTION_NDEF_DISCOVERED.equalsIgnoreCase(resumedIntent.getAction())) {
            parseMessage(resumedIntent);
            ctx.setIntent(new Intent());
        }
    }
    
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        parseMessage(intent);
        Log.d(TAG, "new intent");
    }
}
