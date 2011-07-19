package com.chariotsolutions.nfc.plugin;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Util {

    static NdefRecord[] jsonToNdefRecords(String ndefMessageAsJSON) throws JSONException {
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

    static JSONArray byteArrayToJSON(byte[] bytes) {
        JSONArray json = new JSONArray();
        for (byte aByte : bytes) {
            json.put(aByte);
        }
        return json;
    }

    static byte[] jsonToByteArray(JSONArray json) throws JSONException {
        byte[] b = new byte[json.length()];
        for (int i = 0; i < json.length(); i++) {
            b[i] = (byte) json.getInt(i);
        }
        return b;
    }

    static JSONArray messageToJSON(NdefMessage message) {
        List<JSONObject> list = new ArrayList<JSONObject>();
        List<NdefRecord> records = Arrays.asList(message.getRecords());

        for (NdefRecord r : records) {
            list.add(recordToJSON(r));
        }
        return new JSONArray(list);
    }

    static JSONObject recordToJSON(NdefRecord record) {
        JSONObject json = new JSONObject();
        try {
            json.put("tnf", record.getTnf());
            json.put("type", byteArrayToJSON(record.getType()));
            json.put("id", byteArrayToJSON(record.getId()));
            json.put("payload", byteArrayToJSON(record.getPayload()));
        } catch (JSONException e) {
            //Not sure why this would happen, documentation is unclear.
            Log.e("NdefPlugin", "Failed to convert ndef record into json: " + record.toString(), e);
        }
        return json;
    }

}
