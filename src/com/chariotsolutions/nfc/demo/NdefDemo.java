package com.chariotsolutions.nfc.demo;

import android.os.Bundle;

import com.chariotsolutions.nfc.plugin.DroidGapWithNfc;

public class NdefDemo extends DroidGapWithNfc {	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.loadUrl("file:///android_asset/www/index.html");
	}
}
