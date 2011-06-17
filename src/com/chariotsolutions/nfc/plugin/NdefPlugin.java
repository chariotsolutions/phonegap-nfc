package com.chariotsolutions.nfc.plugin;

import java.lang.reflect.Field;
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
import android.os.Parcelable;
import android.util.Log;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class NdefPlugin extends Plugin {
	private static Stack<Intent> queuedIntents = new Stack<Intent>();
	private static final String REGISTER = "register";
	private static String TAG = "NdefPlugin";
	private PendingIntent pendingIntent = null;
	private IntentFilter[] intentFilters = null;

	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		Log.d(TAG, "executing");
		if (action.equalsIgnoreCase(REGISTER)) {
			Intent intent = new Intent(ctx, ctx.getClass());
			intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			pendingIntent = PendingIntent.getActivity(ctx, 0, intent, 0);

			try {
				intentFilters = new IntentFilter[] { addDataTypeToNewIntentFilter(data) };
			} catch (InstantiationException e) {
				Log.e(TAG, e.toString());
				return new PluginResult(Status.ERROR);
			}

			try {
				registerPluginWithMainActivity();
			} catch (Exception e) {
				Log.e(TAG, e.toString());
				return new PluginResult(Status.ERROR);
			}

			this.ctx.runOnUiThread(new NfcRunnable(ctx,
					this.getPendingIntent(), this.getIntentFilters()));

			while(!queuedIntents.isEmpty()) {
				parseMessage(queuedIntents.pop());
			}
			return new PluginResult(Status.OK);
		}
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

	private IntentFilter[] getIntentFilters() {
		return intentFilters;
	}

	public void parseMessage(Intent intent) {
		if (intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
			Parcelable[] rawData = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			JSONArray jsonData = moveBytesToJSON(rawData);
			Log.d(TAG, jsonData.toString());
			String command = "NdefPlugin.fireNfc(" + jsonData + ")";
			this.sendJavascript(command);
		}
	}

	/**
     * Build a JSON representation of the NDEF Messages
     * There are one or more messages.
     * Each message contains one or more records.
     * Each record is a byte array.
     * [
     *   {
     *     records : [
     *       [ byte, byte, byte, byte ]
     *     ]
     *   },
     *   {
	 *     records : [
	 *       [ byte, byte, byte, byte ],
	 *       [ byte, byte, byte, byte ]
 	 *     ]
     *   }
	 * ]
	 * 
	 * @param rawMessages Parcelable[] of NdefMessages
	 * @return JSON
	 */
	private JSONArray moveBytesToJSON(Parcelable[] rawMessages) {

		JSONArray jsonData = new JSONArray();
		List<Parcelable> messages = Arrays.asList(rawMessages);
		for (Parcelable parcelable : messages) {
			NdefMessage message = (NdefMessage) parcelable;
			List<NdefRecord> records = Arrays.asList(message.getRecords());

			JSONObject jsonMessage = new JSONObject();
			JSONArray jsonRecords = new JSONArray();

			for (NdefRecord r : records) {
				for (int i = 0; i < r.getPayload().length; i++) {
					jsonRecords.put(r.getPayload()[i]);
				}
			}

			try {
				jsonMessage.put("records", jsonRecords);
			} catch (JSONException e) {
				//Not sure why this would happen, docs are unclear.
				Log.e(TAG,"Failed to add message records to JSON: "+ jsonRecords.toString());
			}
			jsonData.put(jsonMessage);
		}
		return jsonData;

	}

	public void pauseNfc() {
		this.ctx.runOnUiThread(new NfcPausable(ctx));
	}

	public void startNfc() {
		this.ctx.runOnUiThread(new NfcRunnable(ctx, this.getPendingIntent(), this.getIntentFilters()));
	}

	@SuppressWarnings("rawtypes")
	private void registerPluginWithMainActivity() throws Exception {
		// get the class of the 'main' activity (this could change per app hence the reflection)
		Class mainActivityClass = Class.forName(this.ctx.getClass().getName());

		Field ndefReaderPluginField = mainActivityClass.getField("ndefReaderPlugin");
		// cast the 'phonegapActivity' to our main activity
		Object mainActivity = mainActivityClass.cast(this.ctx);
		ndefReaderPluginField.set(mainActivity, this);
	}

	class NfcRunnable implements Runnable {
		private Activity activity = null;
		private PendingIntent pendingIntent = null;
		private IntentFilter[] intentFilters = null;

		public NfcRunnable(Activity activity, PendingIntent pendingIntent, IntentFilter[] intentFilters) {
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
					activity, pendingIntent, intentFilters, null);
		}
	}

	class NfcPausable implements Runnable {
		private Activity activity;

		public NfcPausable(Activity activity) {
			this.activity = activity;
		}

		/**
		 * pausing NFC needs to be run on the main (ui thread)
		 * http://developer.android .com/reference/android/nfc/NfcAdapter.html#
		 * disableForegroundDispatch (android.app.Activity)
		 */
		public void run() {
			NfcAdapter.getDefaultAdapter(activity).disableForegroundDispatch(activity);
		}
	}

	public static void saveIntent(Intent intent) {
		queuedIntents.push(intent);
	}
}
