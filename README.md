### NFC
"NFC PhoneGap integration"

### Event Types:
* NDEF Tag read

### ndef

This is an event that fires when user initiates contact with an NFC Tag with a supported mime type

<pre>document.addEventListener("ndef", yourCallbackFunction, false);</pre>

### Add Plugin
Something to do with jar?

### Extend DroidGapWithNfc
In your main application activity you need to do the following. 
Change the <code>extends DroidGap</code> to <code>extends DroidGapWithNfc</code> and add the import <code>import com.chariotsolutions.nfc.plugin.DroidGapWithNfc;</code>

### Import plugin
<pre><code>
&lt;script type="text/javascript" charset="utf-8" src="phonegap.nfc.js"&gt;&lt;/script&gt;
</code>
</pre>

### Details
To listen for NFC tag detection on Android you can register an event listener for the 'ndef' event. You will also need to 'register' a mime type with the plugin which matches the mime type you used to write your tags

Typically, you will want to attach an event listener with document.addEventListener once you receive the PhoneGap 'deviceready' event.

### Supported Platforms

* Android

### Quick Example

<pre> window.plugins.NdefReaderPlugin.register("text/pg", win, fail); </pre>

<pre> document.addEventListener("ndef", myNfcListener, false); </pre>

### Full Example - AndroidManifest.xml

Will also need to amend your AndroidManifest.xml with the following:
Allow use of NFC:
<pre>
&lt;uses-permission android:name="android.permission.NFC" /&gt;
&lt;uses-feature android:name="android.hardware.nfc" android:required="true" /&gt;
</pre>

Update your activity to include the following intent filter
<pre>
&lt;intent-filter&gt;
	&lt;action android:name="android.nfc.action.NDEF_DISCOVERED" /&gt;
	&lt;data android:mimeType="text/pg" /&gt;
	&lt;category android:name="android.intent.category.DEFAULT" /&gt;
&lt;/intent-filter&gt;
</pre>
### Note: <code>data android:mimeType="text/pg"</code> this mimeType should match the mimeType you specified in JavaScript

Lastly update the <code>minSdkVersion</code> to '10' if you don't have that in your AndroidManifest.xml just add the whole tag in:
<pre>
&lt;uses-sdk android:minSdkVersion="10" /&gt;	
</pre>

### Full Example - index.html

See [index.html](https://github.com/chariotsolutions/phonegap-nfc/blob/master/assets/www/index.html)

### Known Issues:
After hitting the home button (pausing the application), and scanning a tag the application resumes, but fails to read the tag.
I've reproduced this in native Android only and I am working on the issue.