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
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
    private List<IntentFilter> intentFilters = null;
    private ArrayList<String[]> techLists = null;

    @Override
    // TODO refactor this into multiple methods
    public PluginResult execute(String action, JSONArray data, String callbackId) {
        initialize();
        
        if (action.equalsIgnoreCase(REGISTER_MIME_TYPE)) {
            try {
                String mimeType = data.getString(0);
                intentFilters.add(createIntentFilter(mimeType));
            } catch (MalformedMimeTypeException e) {
                return new PluginResult(Status.ERROR, "Invalid MIME Type");
            } catch (JSONException e) {
                return  new PluginResult(Status.JSON_EXCEPTION, "Invalid MIME Type");
            }
            startNfc();
            
            return new PluginResult(Status.OK);         
        } else if (action.equalsIgnoreCase(REGISTER_NDEF)) {    
            addTechList(new String[] { Ndef.class.getName() });
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
            	NdefRecord[] records = jsonToNdefRecords(data.getString(0));
                writeTag(new NdefMessage(records), tag);
            } catch (JSONException e) {
            	return new PluginResult(Status.JSON_EXCEPTION, "Error reading ndefMessage from JSON");
            } catch (Exception e) {
                return new PluginResult(Status.ERROR, e.getMessage());
            }

            return new PluginResult(Status.OK);
        } else if (action.equalsIgnoreCase(SHARE_TAG)) {
        	
			try {
				NdefRecord[] records = jsonToNdefRecords(data.getString(0));
	            this.p2pMessage = new NdefMessage(records);
	            	            
                startNdefPush();
	            
			} catch (JSONException e) {
                return new PluginResult(Status.JSON_EXCEPTION, "Error reading ndefMessage from JSON");
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

	private void initialize() {
		if (pendingIntent == null) {
            Intent intent = new Intent(ctx, ctx.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
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

    private void addTechFilter() {
        intentFilters.add(new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED));
    }

    private void startNfc() {
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
        for (byte aByte : bytes) {
            json.put(aByte);
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
    
    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        stopNfc();
    }
    
    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        startNfc();

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
