PhoneGap NFC Plugin
==========================

The PhoneGap NFC Plugin provides access to Near Field Communication (NFC) functionality, allowing applications to read NDEF message in NFC tags. A "tag" may actually be another device that appears as a tag.

Supported Platforms
-------------------
* Android

**Requires PhoneGap 1.0**

Configuration
=============

Assuming you have an existing PhoneGap 1.0 Android project:

### Java

[Download phonegap-nfc.jar](https://github.com/chariotsolutions/phonegap-nfc/archives/master) and add it to lib/

Configure the NdefPlugin in res/xml/plugins.xml

    <plugin name="NdefPlugin" value="com.chariotsolutions.nfc.plugin.NdefPlugin"/>

### JavaScript 

[Download phonegap-nfc.js](https://github.com/chariotsolutions/phonegap-nfc/archives/master) and add it to assets/www
    
Include phonegap-nfc.js in index.html

    <script type="text/javascript" charset="utf-8" src="phonegap-nfc.js"></script>        

### AndroidManifest.xml

Add NFC permissions

    <uses-permission android:name="android.permission.NFC" />
    <uses-feature android:name="android.hardware.nfc" android:required="true" />

Ensure that the `minSdkVersion` is 10

    <uses-sdk android:minSdkVersion="10" />


NFC
===========

> The nfc object provides access to the devices's NFC sensor.

Methods
-------

- nfc.addMimeTypeListener
- nfc.addNdefListener
- nfc.addNdefFormattableListener
- nfc.writeTag
- nfc.shareTag
- nfc.unshareTag
- nfc.eraseTag

nfc.addMimeTypeListener
==============================
Registers an event listener for NDEF tags matching a specified MIME type.

    navigator.nfc.addMimeTypeListener(mimeType, callback, [onSuccess], [onFailure]);

Parameters
----------
- __mimeType__: The MIME type to filter for messages.
- __callback__: The callback that is called when an NDEF tag matching the MIME type is read.
- __onSuccess__: (Optional) The callback that is called when the listener is added.
- __onFailure__: (Optional) The callback that is called if there was an error.

Description
-----------

Function `nfc.addMimeTypeListener` registers the callback for ndef-mime events.

A ndef-mime event occurs when a `Ndef.TNF_MIME_MEDIA` tag is read and matches the specified MIME type.

This function can be called multiple times to register different MIME types.

Supported Platforms
-------------------

- Android

nfc.addNdefListener
==============================
Registers an event listener for any NDEF tag.

    navigator.nfc.addNdefListener(callback, [onSuccess], [onFailure]);

Parameters
----------
- __callback__: The callback that is called when an NDEF tag is read.
- __onSuccess__: (Optional) The callback that is called when the listener is added.
- __onFailure__: (Optional) The callback that is called if there was an error.

Description
-----------

Function `nfc.addNdefListener` registers the callback for ndef events.

A ndef event occurs when a NDEF tag is read.

NOTE: Registered mimeTypeListeners takes precedence over the more generic NDEF listener.


Supported Platforms
-------------------

- Android

nfc.addNdefFormattableListener
==============================
Registers an event listener for formattable NDEF tags.

    navigator.nfc.addNdefFormattableListener(callback, [onSuccess], [onFailure]);

Parameters
----------
- __callback__: The callback that is called when NDEF formattable tag is read.
- __onSuccess__: (Optional) The callback that is called when the listener is added.
- __onFailure__: (Optional) The callback that is called if there was an error.

Description
-----------

Function `nfc.addNdefFormattableListener` registers the callback for ndef-formattable events.

A ndef-formattable event occurs when a tag is read that can be NDEF formatted.  This is not fired for tags that are already formatted as NDEF.  The ndef-formattable event will not contain an NdefMessage.

Supported Platforms
-------------------

- Android

nfc.writeTag
==============================
Writes data to an NDEF tag.

    navigator.nfc.writeTag(ndefMessage, [onSuccess], [onFailure]);

Parameters
----------
- __ndefMessage__: The NdefMessage that is written to the tag.
- __onSuccess__: (Optional) The callback that is called when the tag is written.
- __onFailure__: (Optional) The callback that is called if there was an error.

Description
-----------

Function `nfc.writeTag` writes an NdefMessage to a NFC tag.

This method *must* be called from within an NDEF Event Handler. 

Supported Platforms
-------------------

- Android

nfc.shareTag
==============================
Shares a NdefMessage via peer-to-peer.

    navigator.nfc.shareTag(ndefMessage, [onSuccess], [onFailure]);

Parameters
----------
- __ndefMessage__: The NdefMessage that is shared.
- __onSuccess__: (Optional) The callback that is called when the message is pushed.
- __onFailure__: (Optional) The callback that is called if there was an error.

Description
-----------

Function `nfc.shareTag` writes an NdefMessage via peer-to-peer.  This should appear as an NFC tag to another device.

Supported Platforms
-------------------

- Android

nfc.unshareTag
==============================
Stop sharing NDEF data via peer-to-peer.

    navigator.nfc.unshareTag([onSuccess], [onFailure]);

Parameters
----------
- __onSuccess__: (Optional) The callback that is called when sharing stops.
- __onFailure__: (Optional) The callback that is called if there was an error.

Description
-----------

Function `nfc.unshareTag` stops sharing data via peer-to-peer.

nfc.eraseTag
==============================
Erase a NDEF tag

    navigator.nfc.eraseTag([onSuccess], [onFailure]);

Parameters
----------
- __onSuccess__: (Optional) The callback that is called when sharing stops.
- __onFailure__: (Optional) The callback that is called if there was an error.

Description
-----------

Function `nfc.eraseTag` erases a tag by writing an empty message.  Will format unformatted tags before writing.

This method *must* be called from within an NDEF Event Handler.

Supported Platforms
-------------------

- Android

Ndef
========
> The Ndef object provides NDEF constants, functions for creating NdefRecords, and functions for converting data.
> See [android.nfc.NdefRecord](http://developer.android.com/reference/android/nfc/NdefRecord.html) for documentation about constants

NdefMessage
============
Represents an NDEF (NFC Data Exchange Format) data message that contains one or more NdefRecords.
This plugin uses an array of NdefRecords to represent an NdefMessage.

NdefRecord
============
Represents a logical (unchunked) NDEF (NFC Data Exchange Format) record.

Properties
----------
- __tnf__: 3-bit TNF (Type Name Format) - use one of the TNF_* constants
- __type__: byte array, containing zero to 255 bytes, must not be null
- __id__: byte array, containing zero to 255 bytes, must not be null
- __payload__: byte array, containing zero to (2 ** 32 - 1) bytes, must not be null

The Ndef object has a function for creating NdefRecords

    var record = Ndef.record(Ndef.TNF_ABSOLUTE_URI, Ndef.RTD_URI, [], Ndef.stringToBytes("http://chariotsolutions.com"));
    
There are also helper functions for some types of records

    var record = Ndef.uriRecord("http://chariotsolutions.com");

See `Ndef.record`, `Ndef.textRecord`, `Ndef.mimeMediaRecord`, and `Ndef.uriRecord`.

The Ndef object has functions to convert some data types to and from byte arrays.  

See the phonegap-nfc.js source for more documentation.

NDEF Events
============
NDEF Events are fired when NFC tags are read.  Listeners are added by registering callback with the `nfc` object. 

Properties
----------
- __type__: event type 
- __tagData__: NdefMessage
 
Types
---------
- ndef-mime
- ndef
- ndef-formattable

Intents
===========

  Intents can be used to launch your application when a NFC tag is read.  This is optional and configured in AndroidManifest.xml.

    <intent-filter>
      <action android:name="android.nfc.action.NDEF_DISCOVERED" />
      <data android:mimeType="text/pg" />
      <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
  
Note: `data android:mimeType="text/pg"` should match the data type you specified in JavaScript

  We have found it necessary to add `android:noHistory="true"` to the activity element so that scanning a tag launches the application after the user has pressed the home button.


Sample Projects
================

- [NFC Reader](https://github.com/don/phonegap-nfc-reader)
- [NFC Writer](https://github.com/don/phonegap-nfc-writer)
- [NFC Peer to Peer](https://github.com/don/phonegap-p2p)
- [Rock Paper Scissors](https://github.com/don/rockpaperscissors)

License
================

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
