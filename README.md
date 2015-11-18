PhoneGap NFC Plugin
==========================

The NFC plugin allows you to read and write  NFC tags. You can also beam to, and receive from, other NFC enabled devices.

Use to
* read data from NFC tags
* write data to NFC tags
* send data to other NFC enabled devices
* receive data from NFC devices

This plugin uses NDEF (NFC Data Exchange Format) for maximum compatibilty between NFC devices, tag types, and operating systems.

Supported Platforms
-------------------
* Android
* Windows (includes Windows Phone 8.1, Windows 8.1, Windows 10)
* BlackBerry 10
* Windows Phone 8
* BlackBerry 7

## Contents

* [Installing](#installing)
* [NFC](#nfc)
* [NDEF](#ndef)
  - [NdefMessage](#ndefmessage)
  - [NdefRecord](#ndefrecord)
* [Events](#events)
* [Platform Differences](#platform-differences)
* [BlackBerry 10 Invoke Target](#blackberry-10-invoke-target)
* [Launching Application when Scanning a Tag](#launching-your-android-application-when-scanning-a-tag)
* [Testing](#testing)
* [Sample Projects](#sample-projects)
* [Book](#book)
* [License](#license)

# Installing

### Cordova

    $ cordova plugin add phonegap-nfc

### PhoneGap

    $ phonegap plugin add phonegap-nfc

### PhoneGap Build

Edit config.xml to install the plugin for [PhoneGap Build](http://build.phonegap.com).

    <gap:plugin name="phonegap-nfc" source="npm" />


Windows Phone 8.1 should use the **windows** platform. The Silverlight based Windows Phone 8 code is no longer being maintained.

BlackBerry 7 support is only available for Cordova 2.x. For applications targeting BlackBerry 7, you may need to use an older version of phonegap-nfc.

See [Getting Started](https://github.com/chariotsolutions/phonegap-nfc/blob/master/doc/GettingStartedCLI.md) and [Getting Started BlackBerry 10](https://github.com/chariotsolutions/phonegap-nfc/blob/master/doc/GettingStartedBlackberry10.md)for more details.

# NFC

> The nfc object provides access to the device's NFC sensor.

## Methods

- [nfc.addNdefListener](#nfcaddndeflistener)
- [nfc.removeNdefListener](#nfcremovendeflistener)
- [nfc.addTagDiscoveredListener](#nfcaddtagdiscoveredlistener)
- [nfc.removeTagDiscoveredListener](#nfcremovetagdiscoveredlistener)
- [nfc.addMimeTypeListener](#nfcaddmimetypelistener)
- [nfc.removeMimeTypeListener](#nfcremovemimetypelistener)
- [nfc.addNdefFormatableListener](#nfcaddndefformatablelistener)
- [nfc.write](#nfcwrite)
- [nfc.makeReadOnly](#nfcmakereadonly)
- [nfc.share](#nfcshare)
- [nfc.unshare](#nfcunshare)
- [nfc.erase](#nfcerase)
- [nfc.handover](#nfchandover)
- [nfc.stopHandover](#nfcstophandover)
- [nfc.enabled](#nfcenabled)
- [nfc.showSettings](#nfcshowsettings)

## nfc.addNdefListener

Registers an event listener for any NDEF tag.

    nfc.addNdefListener(callback, [onSuccess], [onFailure]);

### Parameters

- __callback__: The callback that is called when an NDEF tag is read.
- __onSuccess__: (Optional) The callback that is called when the listener is added.
- __onFailure__: (Optional) The callback that is called if there was an error.

### Description

Function `nfc.addNdefListener` registers the callback for ndef events.

A ndef event is fired when a NDEF tag is read.

For BlackBerry 10, you must configure the type of tags your application will read with an [invoke-target in config.xml](#blackberry-10-invoke-target).

On Android registered [mimeTypeListeners](#nfcaddmimetypelistener) takes precedence over this more generic NDEF listener.

### Supported Platforms

- Android
- Windows
- BlackBerry 7
- BlackBerry 10
- Windows Phone 8

## nfc.removeNdefListener

Removes the previously registered event listener for NDEF tags added via `nfc.addNdefListener`.

    nfc.removeNdefListener(callback, [onSuccess], [onFailure]);

### Parameters

- __callback__: The previously registered callback.
- __onSuccess__: (Optional) The callback that is called when the listener is successfully removed.
- __onFailure__: (Optional) The callback that is called if there was an error during removal.

### Supported Platforms

- Android
- Windows
- BlackBerry 7

## nfc.addTagDiscoveredListener

Registers an event listener for tags matching any tag type.

    nfc.addTagDiscoveredListener(callback, [onSuccess], [onFailure]);

### Parameters

- __callback__: The callback that is called when a tag is detected.
- __onSuccess__: (Optional) The callback that is called when the listener is added.
- __onFailure__: (Optional) The callback that is called if there was an error.

### Description

Function `nfc.addTagDiscoveredListener` registers the callback for tag events.

This event occurs when any tag is detected by the phone.

### Supported Platforms

- Android
- Windows
- BlackBerry 7

## nfc.removeTagDiscoveredListener

Removes the previously registered event listener added via `nfc.addTagDiscoveredListener`.

    nfc.removeTagDiscoveredListener(callback, [onSuccess], [onFailure]);

### Parameters

- __callback__: The previously registered callback.
- __onSuccess__: (Optional) The callback that is called when the listener is successfully removed.
- __onFailure__: (Optional) The callback that is called if there was an error during removal.

### Supported Platforms

- Android
- Windows
- BlackBerry 7

## nfc.addMimeTypeListener

Registers an event listener for NDEF tags matching a specified MIME type.

    nfc.addMimeTypeListener(mimeType, callback, [onSuccess], [onFailure]);

### Parameters

- __mimeType__: The MIME type to filter for messages.
- __callback__: The callback that is called when an NDEF tag matching the MIME type is read.
- __onSuccess__: (Optional) The callback that is called when the listener is added.
- __onFailure__: (Optional) The callback that is called if there was an error.

### Description

Function `nfc.addMimeTypeListener` registers the callback for ndef-mime events.

A ndef-mime event occurs when a `Ndef.TNF_MIME_MEDIA` tag is read and matches the specified MIME type.

This function can be called multiple times to register different MIME types. You should use the *same* handler for all MIME messages.

    nfc.addMimeTypeListener("text/json", *onNfc*, success, failure);
    nfc.addMimeTypeListener("text/demo", *onNfc*, success, failure);

On Android, MIME types for filtering should always be lower case. (See [IntentFilter.addDataType()](http://developer.android.com/reference/android/content/IntentFilter.html#addDataType\(java.lang.String\)))

### Supported Platforms

- Android
- Windows
- BlackBerry 7

## nfc.removeMimeTypeListener

Removes the previously registered event listener added via `nfc.addMimeTypeListener`.

    nfc.removeMimeTypeListener(mimeType, callback, [onSuccess], [onFailure]);

### Parameters

- __mimeType__: The MIME type to filter for messages.
- __callback__: The previously registered callback.
- __onSuccess__: (Optional) The callback that is called when the listener is successfully removed.
- __onFailure__: (Optional) The callback that is called if there was an error during removal.

### Supported Platforms

- Android
- BlackBerry 7

## nfc.addNdefFormatableListener

Registers an event listener for formatable NDEF tags.

    nfc.addNdefFormatableListener(callback, [onSuccess], [onFailure]);

### Parameters

- __callback__: The callback that is called when NDEF formatable tag is read.
- __onSuccess__: (Optional) The callback that is called when the listener is added.
- __onFailure__: (Optional) The callback that is called if there was an error.

### Description

Function `nfc.addNdefFormatableListener` registers the callback for ndef-formatable events.

A ndef-formatable event occurs when a tag is read that can be NDEF formatted.  This is not fired for tags that are already formatted as NDEF.  The ndef-formatable event will not contain an NdefMessage.

### Supported Platforms

- Android

## nfc.write

Writes an NDEF Message to a NFC tag.

A NDEF Message is an array of one or more NDEF Records

    var message = [
        ndef.textRecord("hello, world"),
        ndef.uriRecord("http://github.com/chariotsolutions/phonegap-nfc")
    ];

    nfc.write(message, [onSuccess], [onFailure]);

### Parameters

- __ndefMessage__: An array of NDEF Records.
- __onSuccess__: (Optional) The callback that is called when the tag is written.
- __onFailure__: (Optional) The callback that is called if there was an error.

### Description

Function `nfc.write` writes an NdefMessage to a NFC tag.

On **Android** this method *must* be called from within an NDEF Event Handler.
On **Windows** this method *may* be called from within the NDEF Event Handler.

On **Windows Phone 8.1** this method should be called outside the NDEF Event Handler, otherwise Windows tries to read the tag contents as you are writing to the tag.

### Supported Platforms

- Android
- Windows
- BlackBerry 7
- Windows Phone 8

## nfc.makeReadOnly

Makes a NFC tag read only.  **Warning this is permanent.**

    nfc.makeReadOnly([onSuccess], [onFailure]);

### Parameters

- __onSuccess__: (Optional) The callback that is called when the tag is locked.
- __onFailure__: (Optional) The callback that is called if there was an error.

### Description

Function `nfc.makeReadOnly` make a NFC tag read only. **Warning this is permanent** and can not be undone.

On **Android** this method *must* be called from within an NDEF Event Handler.

Example usage

    onNfc: function(nfcEvent) {

        var record = [
            ndef.textRecord("hello, world")
        ];

        var failure = function(reason) {
            alert("ERROR: " + reason);
        };

        var lockSuccess = function() {
            alert("Tag is now read only.");
        };

        var lock = function() {
            nfc.makeReadOnly(lockSuccess, failure);
        };

        nfc.write(record, lock, failure);

    },

### Supported Platforms

- Android

## nfc.share

Shares an NDEF Message via peer-to-peer.

A NDEF Message is an array of one or more NDEF Records

    var message = [
        ndef.textRecord("hello, world")
    ];

    nfc.share(message, [onSuccess], [onFailure]);

### Parameters

- __ndefMessage__: An array of NDEF Records.
- __onSuccess__: (Optional) The callback that is called when the message is pushed.
- __onFailure__: (Optional) The callback that is called if there was an error.

### Description

Function `nfc.share` writes an NdefMessage via peer-to-peer.  This should appear as an NFC tag to another device.

### Supported Platforms

- Android
- Windows
- BlackBerry 7
- BlackBerry 10
- Windows Phone 8

### Platform differences

    Android - shares message until unshare is called
    Blackberry 10 - shares the message one time or until unshare is called
    Windows Phone 8 - must be called from within a NFC event handler like nfc.write

## nfc.unshare

Stop sharing NDEF data via peer-to-peer.

    nfc.unshare([onSuccess], [onFailure]);

### Parameters

- __onSuccess__: (Optional) The callback that is called when sharing stops.
- __onFailure__: (Optional) The callback that is called if there was an error.

### Description

Function `nfc.unshare` stops sharing data via peer-to-peer.

### Supported Platforms

- Android
- Windows
- BlackBerry 7
- BlackBerry 10

## nfc.erase

Erase a NDEF tag

    nfc.erase([onSuccess], [onFailure]);

### Parameters

- __onSuccess__: (Optional) The callback that is called when sharing stops.
- __onFailure__: (Optional) The callback that is called if there was an error.

### Description

Function `nfc.erase` erases a tag by writing an empty message.  Will format unformatted tags before writing.

This method *must* be called from within an NDEF Event Handler.

### Supported Platforms

- Android
- BlackBerry 7

## nfc.handover

Send a file to another device via NFC handover.

    var uri = "content://media/external/audio/media/175";
    nfc.handover(uri, [onSuccess], [onFailure]);


    var uris = [
        "content://media/external/audio/media/175",
        "content://media/external/audio/media/176",
        "content://media/external/audio/media/348"
    ];
    nfc.handover(uris, [onSuccess], [onFailure]);


### Parameters

- __uri__: A URI as a String, or an *array* of URIs.
- __onSuccess__: (Optional) The callback that is called when the message is pushed.
- __onFailure__: (Optional) The callback that is called if there was an error.

### Description

Function `nfc.handover` shares files to a NFC peer using handover. Files are sent by specifying a file:// or context:// URI or a list of URIs. The file transfer is initiated with NFC but the transfer is completed with over Bluetooth or WiFi which is handled by a NFC handover request. The Android code is responsible for building the handover NFC Message.

This is Android only, but it should be possible to add implementations for other platforms.

### Supported Platforms

- Android

## nfc.stopHandover

Stop sharing NDEF data via NFC handover.

    nfc.stopHandover([onSuccess], [onFailure]);

### Parameters

- __onSuccess__: (Optional) The callback that is called when sharing stops.
- __onFailure__: (Optional) The callback that is called if there was an error.

### Description

Function `nfc.stopHandover` stops sharing data via peer-to-peer.

### Supported Platforms

- Android

## nfc.showSettings

Show the NFC settings on the device.

    nfc.showSettings(success, failure);

### Description

Function `showSettings` opens the NFC settings for the operating system.

### Parameters

- __success__: Success callback function [optional]
- __failure__: Error callback function, invoked when error occurs. [optional]

### Quick Example

    nfc.showSettings();

### Supported Platforms

- Android
- Windows
- BlackBerry 10

## nfc.enabled

Check if NFC is available and enabled on this device.

nfc.enabled(onSuccess, onFailure);

### Parameters

- __onSuccess__: The callback that is called when NFC is enabled.
- __onFailure__: The callback that is called when NFC is disabled or missing.

### Description

Function `nfc.enabled` explicitly checks to see if the phone has NFC and if NFC is enabled. If
everything is OK, the success callback is called. If there is a problem, the failure callback
will be called with a reason code.

The reason will be **NO_NFC** if the device doesn't support NFC and **NFC_DISABLED** if the user has disabled NFC.

Note: that on Android the NFC status is checked before every API call **NO_NFC** or **NFC_DISABLED** can be returned in **any** failure function.

Windows will return **NO_NFC_OR_NFC_DISABLED** when NFC is not present or disabled. If the user disabled NFC after the application started, Windows may return **NFC_DISABLED**. Windows checks the NFC status before most API calls, but there are some cases when the NFC state can not be determined.

### Supported Platforms

- Android
- Windows

# NDEF

> The `ndef` object provides NDEF constants, functions for creating NdefRecords, and functions for converting data.
> See [android.nfc.NdefRecord](http://developer.android.com/reference/android/nfc/NdefRecord.html) for documentation about constants

## NdefMessage

Represents an NDEF (NFC Data Exchange Format) data message that contains one or more NdefRecords.
This plugin uses an array of NdefRecords to represent an NdefMessage.

## NdefRecord

Represents a logical (unchunked) NDEF (NFC Data Exchange Format) record.

### Properties

- __tnf__: 3-bit TNF (Type Name Format) - use one of the TNF_* constants
- __type__: byte array, containing zero to 255 bytes, must not be null
- __id__: byte array, containing zero to 255 bytes, must not be null
- __payload__: byte array, containing zero to (2 ** 32 - 1) bytes, must not be null

The `ndef` object has a function for creating NdefRecords

    var type = "text/pg",
        id = [],
        payload = nfc.stringToBytes("Hello World"),
        record = ndef.record(ndef.TNF_MIME_MEDIA, type, id, payload);

There are also helper functions for some types of records

Create a URI record

    var record = ndef.uriRecord("http://chariotsolutions.com");

Create a plain text record

    var record = ndef.textRecord("Plain text message");

Create a mime type record

    var mimeType = "text/pg",
        payload = "Hello Phongap",
        record = ndef.mimeMediaRecord(mimeType, nfc.stringToBytes(payload));

Create an Empty record

    var record = ndef.emptyRecord();

Create an Android Application Record (AAR)

    var record = ndef.androidApplicationRecord('com.example');

See `ndef.record`, `ndef.textRecord`, `ndef.mimeMediaRecord`, and `ndef.uriRecord`.

The Ndef object has functions to convert some data types to and from byte arrays.

See the [phonegap-nfc.js](https://github.com/chariotsolutions/phonegap-nfc/blob/master/www/phonegap-nfc.js) source for more documentation.

# Events

Events are fired when NFC tags are read.  Listeners are added by registering callback functions with the `nfc` object.  For example ` nfc.addNdefListener(myNfcListener, win, fail);`

## NfcEvent

### Properties

- __type__: event type
- __tag__: Ndef tag

### Types

- tag
- ndef-mime
- ndef
- ndef-formatable

The tag contents are platform dependent.

`id` and `techTypes` may be included when scanning a tag on Android.  `serialNumber` may be included on BlackBerry 7.

`id` and `serialNumber` are different names for the same value.  `id` is typically displayed as a hex string `ndef.bytesToHexString(tag.id)`.

Windows, Windows Phone 8, and BlackBerry 10 read the NDEF information from a tag, but do not have access to the tag id or other meta data like capacity, read-only status or tag technologies.

Assuming the following NDEF message is written to a tag, it will produce the following events when read.

    var ndefMessage = [
        ndef.createMimeRecord('text/pg', 'Hello PhoneGap')
    ];

#### Sample Event on Android

    {
        type: 'ndef',
        tag: {
            "isWritable": true,
            "id": [4, 96, 117, 74, -17, 34, -128],
            "techTypes": ["android.nfc.tech.IsoDep", "android.nfc.tech.NfcA", "android.nfc.tech.Ndef"],
            "type": "NFC Forum Type 4",
            "canMakeReadOnly": false,
            "maxSize": 2046,
            "ndefMessage": [{
                "id": [],
                "type": [116, 101, 120, 116, 47, 112, 103],
                "payload": [72, 101, 108, 108, 111, 32, 80, 104, 111, 110, 101, 71, 97, 112],
                "tnf": 2
            }]
        }
    }

#### Sample Event on BlackBerry 7

    {
        type: 'ndef',
        tag: {
            "tagType": "4",
            "isLocked": false,
            "isLockable": false,
            "freeSpaceSize": "2022",
            "serialNumberLength": "7",
            "serialNumber": [4, 96, 117, 74, -17, 34, -128],
            "name": "Desfire EV1 2K",
            "ndefMessage": [{
                "tnf": 2,
                "type": [116, 101, 120, 116, 47, 112, 103],
                "id": [],
                "payload": [72, 101, 108, 108, 111, 32, 80, 104, 111, 110, 101, 71, 97, 112]
            }]
        }
    }

#### Sample Event on Windows, BlackBerry 10, or Windows Phone 8

    {
        type: 'ndef',
        tag: {
            "ndefMessage": [{
                "tnf": 2,
                "type": [116, 101, 120, 116, 47, 112, 103],
                "id": [],
                "payload": [72, 101, 108, 108, 111, 32, 80, 104, 111, 110, 101, 71, 97, 112]
            }]
        }
    }

## Getting Details about Events

The raw contents of the scanned tags are written to the log before the event is fired.  Use `adb logcat` on Android and Event Log (hold alt + lglg) on BlackBerry.

You can also log the tag contents in your event handlers.  `console.log(JSON.stringify(nfcEvent.tag))`  Note that you want to stringify the tag not the event to avoid a circular reference.

# Platform Differences

## Non-NDEF Tags

Only Android and BlackBerry 7 can read data from non-NDEF NFC tags.

## Mifare Classic Tags

BlackBerry 7, BlackBerry 10 and many newer Android phones will not read Mifare Classic tags.  Mifare Ultralight tags will work since they are NFC Forum Type 2 tags. Newer Windows 8.1 phones (Lumia 640) can read Mifare Classic tags.

## Tag Id and Meta Data

Windows Phone 8, BlackBerry 10, and Windows read the NDEF information from a tag, but do not have access to the tag id or other meta data like capacity, read-only status or tag technologies.

## Multiple Listeners

Multiple listeners can be registered in JavaScript. e.g. addNdefListener, addTagDiscoveredListener, addMimeTypeListener.

On Android, only the most specific event will fire.  If a Mime Media Tag is scanned, only the addMimeTypeListener callback is called and not the callback defined in addNdefListener. You can use the same event handler for multiple listeners.

For Windows, this plugin mimics the Android behavior. If an ndef event is fired, a tag event will not be fired. You should receive one event per tag.

On BlackBerry 7, all the events fire if a Mime Media Tag is scanned.

## addTagDiscoveredListener

On Android, addTagDiscoveredListener scans non-NDEF tags and NDEF tags. The tag event does NOT contain an ndefMessage even if there are NDEF messages on the tag.  Use addNdefListener or addMimeTypeListener to get the NDEF information.

Windows can scan non-NDEF (unformatted) tags using addTagDiscoveredListener. The tag event will not include any data.

On BlackBerry 7, addTagDiscoveredListener does NOT scan non-NDEF tags.  Webworks returns the ndefMessage in the event.

### Non-NDEF tag scanned with addTagDiscoveredListener on *Android*

    {
        type: 'tag',
        tag: {
            "id": [-81, 105, -4, 64],
            "techTypes": ["android.nfc.tech.MifareClassic", "android.nfc.tech.NfcA", "android.nfc.tech.NdefFormatable"]
        }
    }


### NDEF tag scanned with addTagDiscoveredListener on *Android*

    {
        type: 'tag',
        tag: {
            "id": [4, 96, 117, 74, -17, 34, -128],
            "techTypes": ["android.nfc.tech.IsoDep", "android.nfc.tech.NfcA", "android.nfc.tech.Ndef"]
        }
    }

### Non-NDEF tag scanned with addTagDiscoveredListener on *Windows*

    {
        type: 'tag',
        tag: {
        }
    }

# BlackBerry 10 Invoke Target

This plugin uses the [BlackBerry Invocation Framework](http://developer.blackberry.com/native/documentation/cascades/device_platform/invocation/receiving_invocation.html) to read NFC tags on BlackBerry 10. This means that you need to register an invoke target in the config.xml.

If your project supports multiple platforms, copy www/config.xml to merges/config.xml and add a `rim:invoke-target` tag. The invoke-target determines which tags your app will scan when it is running. If your application is not running, BlackBerry will launch it when a matching tag is scanned.

This sample configuration attempts to open any NDEF tag.

    <rim:invoke-target id="your.unique.id.here">
        <type>APPLICATION</type>
        <filter>
            <action>bb.action.OPEN</action>
            <mime-type>application/vnd.rim.nfc.ndef</mime-type>
            <!-- any TNF Empty(0), Well Known(1), MIME Media(2), Absolute URI(3), External(4) -->
            <property var="uris" value="ndef://0,ndef://1,ndef://2,ndef://3,ndef://4" />
        </filter>
    </rim:invoke-target>

You can configure you application to handle only certain tags.

For example to scan only MIME Media tags of type "text/pg" use

    <rim:invoke-target id="your.unique.id.here">
        <type>APPLICATION</type>
        <filter>
            <action>bb.action.OPEN</action>
            <mime-type>application/vnd.rim.nfc.ndef</mime-type>
            <!-- TNF MIME Media(2) with type "text/pg" -->
            <property var="uris" value="ndef://2/text/pg" />
        </filter>
    </rim:invoke-target>

Or to scan only Plain Text tags use

    <rim:invoke-target id="your.unique.id.here">
        <type>APPLICATION</type>
        <filter>
            <action>bb.action.OPEN</action>
            <mime-type>application/vnd.rim.nfc.ndef</mime-type>
            <!-- TNF Well Known(1), RTD T -->
            <property var="uris" value="ndef://1/T" />
        </filter>
    </rim:invoke-target>

See the [BlackBerry documentation](http://developer.blackberry.com/native/documentation/cascades/device_comm/nfc/receiving_content.html) for more info.

# Launching your Android Application when Scanning a Tag

On Android, intents can be used to launch your application when a NFC tag is read.  This is optional and configured in AndroidManifest.xml.

    <intent-filter>
      <action android:name="android.nfc.action.NDEF_DISCOVERED" />
      <data android:mimeType="text/pg" />
      <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>

Note: `data android:mimeType="text/pg"` should match the data type you specified in JavaScript

We have found it necessary to add `android:noHistory="true"` to the activity element so that scanning a tag launches the application after the user has pressed the home button.

See the Android documentation for more information about [filtering for NFC intents](http://developer.android.com/guide/topics/connectivity/nfc/nfc.html#ndef-disc).

Testing
=======

Tests require the [Cordova Plugin Test Framework](https://github.com/apache/cordova-plugin-test-framework)

Create a new project

    git clone https://github.com/chariotsolutions/phonegap-nfc
    cordova create nfc-test com.example.nfc.test NfcTest
    cd nfc-test
    cordova platform add android
    cordova plugin add ../phonegap-nfc
    cordova plugin add ../phonegap-nfc/tests
    cordova plugin add http://git-wip-us.apache.org/repos/asf/cordova-plugin-test-framework.git

Change the start page in `config.xml`

    <content src="cdvtests/index.html" />

Run the app on your phone

    cordova run


Sample Projects
================

- [Ionic NFC Reader](https://github.com/don/ionic-nfc-reader)
- [NFC Reader](https://github.com/don/phonegap-nfc-reader)
- [NFC Writer](https://github.com/don/phonegap-nfc-writer)
- [NFC Peer to Peer](https://github.com/don/phonegap-p2p)
- [ApacheCon 2014 Demos](https://github.com/don/apachecon-nfc-demos)
- [Rock Paper Scissors](https://github.com/don/rockpaperscissors) *Android 2.x only*

Book
=======
Need more info? Check out my book <a href="http://www.tkqlhce.com/click-7835726-11260198-1430755877000?url=http%3A%2F%2Fshop.oreilly.com%2Fproduct%2F0636920021193.do%3Fcmp%3Daf-prog-books-videos-product_cj_9781449372064_%2525zp&cjsku=0636920021193" target="_top">
Beginning NFC: Near Field Communication with Arduino, Android, and PhoneGap</a><img src="http://www.lduhtrp.net/image-7835726-11260198-1430755877000" width="1" height="1" border="0"/>

<a href="http://www.kqzyfj.com/click-7835726-11260198-1430755877000?url=http%3A%2F%2Fshop.oreilly.com%2Fproduct%2F0636920021193.do%3Fcmp%3Daf-prog-books-videos-product_cj_9781449372064_%2525zp&cjsku=0636920021193" target="_top"><img src="http://akamaicovers.oreilly.com/images/0636920021193/cat.gif" border="0" alt="Beginning NFC"/></a><img src="http://www.ftjcfx.com/image-7835726-11260198-1430755877000" width="1" height="1" border="0"/>

License
================

The MIT License

Copyright (c) 2011-2015 Chariot Solutions

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
