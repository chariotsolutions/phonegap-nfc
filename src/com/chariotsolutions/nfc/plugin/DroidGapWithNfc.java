package com.chariotsolutions.nfc.plugin;

import android.content.Intent;
import android.nfc.NfcAdapter;

import com.phonegap.DroidGap;

/**
 * Base class handles the NFC intents for your application.  Extend  
 */
public class DroidGapWithNfc extends DroidGap {
	//when the 'register' call finishes NdefReaderPlugin gets initialized by the plug-in
	public NdefPlugin ndefReaderPlugin = null;

	@Override
	public void onResume() {
		super.onResume();

		// App is open and scanning.  Start NFC so we receive onNewIntent events.
		if (ndefReaderPlugin != null) {
			ndefReaderPlugin.startNfc();
		}

		// App has been opened via the intent filter
		Intent resumedIntent = getIntent();
		if(NfcAdapter.ACTION_NDEF_DISCOVERED.equalsIgnoreCase(resumedIntent.getAction())) {
			if (ndefReaderPlugin == null) {
				NdefPlugin.saveIntent(resumedIntent);
			} else {
			    ndefReaderPlugin.parseMessage(resumedIntent);
		    }
			setIntent(new Intent());
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (ndefReaderPlugin != null) ndefReaderPlugin.pauseNfc();
	}

	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (ndefReaderPlugin != null) ndefReaderPlugin.parseMessage(intent);
	}
}
