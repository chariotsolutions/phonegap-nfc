### NFC
"NFC PhoneGap integration"

### Event Types:
* NDEF Tag read

### ndef

This is an event that fires when user initiates contact with an NFC Tag with a supported mime type

<pre>document.addEventListener("ndef", yourCallbackFunction, false);</pre>

### Add Plugin
Add the phonegap-nfc.jar to your project in Eclipse. Right click on libs and select Build Path > Configure Build Path. Choose Java Build Path and select the Libraries tab. Click add Jars and select phonegap-nfc.jar. If you are building an Android project from the command line jar files found in libs are automatically compiled in.

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

## Licence ##

The MIT License

Copyright (c) 2011 Chariot Solutions

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.