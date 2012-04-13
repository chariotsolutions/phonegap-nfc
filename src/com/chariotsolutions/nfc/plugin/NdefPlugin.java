package com.chariotsolutions.nfc.plugin;

import org.apache.cordova.api.Plugin;
import org.apache.cordova.api.PluginResult;
import org.json.JSONArray;

public class NdefPlugin extends Plugin {

    @Override
    public PluginResult execute(String action, JSONArray data, String callbackId) {
        return new PluginResult(PluginResult.Status.ERROR, "Please update your plugins.xml to use NfcPlugin");
    }
}
