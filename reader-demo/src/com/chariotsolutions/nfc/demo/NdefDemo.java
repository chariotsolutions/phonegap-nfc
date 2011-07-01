package com.chariotsolutions.nfc.demo;

import android.os.Bundle;

import com.phonegap.DroidGap;

public class NdefDemo extends DroidGap {    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.loadUrl("file:///android_asset/www/index.html");
    }
}
