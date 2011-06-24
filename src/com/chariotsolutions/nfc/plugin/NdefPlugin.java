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
import android.content.Context;
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
import android.os.Vibrator;
import android.util.Log;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class NdefPlugin extends Plugin {
	private static Stack<Intent> queuedIntents = new Stack<Intent>();
	private static final String REGISTER = "register";
	private static final String WRITE_TAG = "writeTag";
	private static final String REGISTER_FOR_WRITE = "registerForWrite"; // TODO rename, it's not write
	private Intent currentIntent = null;
	private static String TAG = "NdefPlugin";
	private PendingIntent pendingIntent = null;
	private List<IntentFilter> intentFilters = null;
	private String[][] techLists = null;

	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		Log.d(TAG, "ARSE");
		if (action.equalsIgnoreCase(REGISTER)) {

			Intent intent = new Intent(ctx, ctx.getClass());
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0);

			try {
				if (intentFilters == null) {
					intentFilters = new ArrayList<IntentFilter>();
				}
				intentFilters.add(addDataTypeToNewIntentFilter(data));
			} catch (InstantiationException e) {
				Log.e(TAG, e.toString());
				return new PluginResult(Status.ERROR);
			}

			try {
				this.ctx.runOnUiThread(new NfcRunnable(ctx,
					this.getPendingIntent(), this.getIntentFilters().toArray(new IntentFilter[this.getIntentFilters().size()]), null));
			} catch ( Exception e ) {
				Log.e(TAG, e.toString());
			} 

			while(!queuedIntents.isEmpty()) {
				parseMessage(queuedIntents.pop());
			}
			
			return new PluginResult(Status.OK);
		} else if (action.equalsIgnoreCase(REGISTER_FOR_WRITE)) { 	
			if (intentFilters == null) {
				intentFilters = new ArrayList<IntentFilter>();
			}
			intentFilters.add(new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED));
	        techLists = new String[][] { 
	        		{ Ndef.class.getName() },
					{ NdefFormatable.class.getName() } };
	        
			return new PluginResult(Status.OK);
		} else if (action.equalsIgnoreCase(WRITE_TAG)) {	
			Vibrator v = (Vibrator) this.ctx.getSystemService(Context.VIBRATOR_SERVICE);
	    	v.vibrate(100);

	    	Tag tag = null;
	    	if (currentIntent == null) {
	    		Log.e(TAG, "Failed to write tag, recieved null intent");
	    		return new PluginResult(Status.ERROR);
	    	} else {
	    		tag = currentIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	    	}
	    
	    	JSONArray ndefMessageJSON;
	    	
	        try {
	        	Log.e(TAG, data.getString(0));
	        	ndefMessageJSON = new JSONArray(data.getString(0));
			} catch (JSONException e) {
				Log.e(TAG, "error reading ndefMessage from JSON");
	    		return new PluginResult(Status.ERROR);
			}
	    	
			NdefRecord[] records = new NdefRecord[ndefMessageJSON.length()];
			for (int i = 0; i < ndefMessageJSON.length(); i++) {				
				try {
					// This seems kludgy am I using JSON wrong? (Or is it just Java?)
					JSONObject record = ndefMessageJSON.getJSONObject(i);
					byte tnf = (byte) record.getInt("tnf");
					byte[] type = jsonToByteArray(record.getJSONArray("type"));
					byte[] id = jsonToByteArray(record.getJSONArray("id"));
					byte[] payload = jsonToByteArray(record.getJSONArray("payload"));
					records[i] = new NdefRecord(tnf, type, id, payload);
				} catch (JSONException e) {
					Log.e(TAG, "error reading record from JSON", e);
		    		return new PluginResult(Status.ERROR);
				}
			}
				    	
	    	writeTag(new NdefMessage(records), tag);    		
	    	return new PluginResult(Status.OK);
			
		}
		Log.d(TAG, "no result");
		return new PluginResult(Status.NO_RESULT);
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
		String command = "";
		if (intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
			this.currentIntent = intent;
			Parcelable[] rawData = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			for (Parcelable parcelable : Arrays.asList(rawData)) {
				JSONArray jsonData = messageToJSON((NdefMessage)parcelable);
				command = "NdefPlugin.fireNfc(" + jsonData + ")";
				this.sendJavascript(command);
			}
			
		} else if (intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			
			String[] tagTechs = tag.getTechList();
			for (String s : tagTechs) {
				if (s.equalsIgnoreCase(NdefFormatable.class.getName())) {
					command = "alert('Fire NdefFormatable event');";
				} else if (s.equalsIgnoreCase(Ndef.class.getName())) {
					command = "alert('Fire Ndef event');";
				} else {
					command = "alert('Scanned a tag I don't understand ' + "+s+");";
				}
				this.sendJavascript(command);
			}			
		}
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
		this.ctx.runOnUiThread(new NfcRunnable(ctx, this.getPendingIntent(), this.getIntentFilters().toArray(new IntentFilter[this.getIntentFilters().size()]), this.techLists));
		
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
