Installing the Plugin (Android)
=============

Assuming you have an existing PhoneGap 2.3.0 Android project:

### Installing with Plugman (recommended)

Use [plugman](https://github.com/imhotep/plugman) to add phonegap-nfc to your Android project.  Plugman requires [node.js](http://nodejs.org) and is installed through npm.

Install plugman

    $ npm install -g plugman

Install the plugin

	$ plugman --platform android --project /path/to/your/project --plugin NFC

Modify your HTML to include phonegap-nfc.js

	<script type="text/javascript" src="js/phonegap-nfc.js"></script>  

### Manually Installing the Plugin (Android)

Note: GitHub downloads are going away soon. These instructions install an older version of the plugin. Installing with plugman is recommended.

### Java

[Download phonegap-nfc-android.jar](https://github.com/chariotsolutions/phonegap-nfc/downloads) and add it to libs/

### plugins.xml 

Configure the NfcPlugin in res/xml/plugins.xml

    <plugin name="NfcPlugin" value="com.chariotsolutions.nfc.plugin.NfcPlugin"/>

### JavaScript 

[Download phonegap-nfc.js](https://github.com/chariotsolutions/phonegap-nfc/downloads) and add it to assets/www
    
Include phonegap-nfc.js in index.html

    <script type="text/javascript" charset="utf-8" src="phonegap-nfc.js"></script>        

### AndroidManifest.xml

Add NFC permissions

    <uses-permission android:name="android.permission.NFC" />

Ensure that the `minSdkVersion` is 10

    <uses-sdk android:minSdkVersion="10" />

If you want to restrict your application to only devices with NFC hardware, set uses-feature so Google Play will restrict the listing.  If NFC is optional in your application, omit the uses-feature element.

    <uses-feature android:name="android.hardware.nfc" android:required="true" />


Installing the Plugin (Blackberry Webworks)
=============

## BlackBerry 10
Assuming you have an existing PhoneGap 2.3.0 BlackBerry Project:

### config.xml

Create a filter in config.xml to read NDEF tags via the BlackBerry 10 Invocation Framework.

    <rim:invoke-target id="<A unique ID for your project>">
        <type>APPLICATION</type>
        <filter>
            <action>bb.action.OPEN</action>
            <mime-type>application/vnd.rim.nfc.ndef</mime-type>
            <property var="uris" value="ndef://0,ndef://1,ndef://2,def://3,ndef://4" /> 
        </filter>
    </rim:invoke-target>

The example filter above filters for all NDEF tags with TNF_EMPTY (0), TNF_WELL_KNOWN (1), TNF_MIME_MEDIA (2), TNF_ABSOLUTE_URI (3), TNF_EXTERNAL_TYPE (4).

The filter can also be more restrictive.  For example we could only handle TNF_MIME_MEDIA tags with a mime type of 'text/pg'

	 <property var="uris" value="ndef://2/text/pg" /> 

### JavaScript 

The BB10 implementation is 100% JavaScript.  You must build the code from source.

	ant build-javascript

Copy dist/phonegap-nfc-VERSION.js and add it to your www folder
    
Include phonegap-nfc-VERSION.js in index.html

    <script type="text/javascript" charset="utf-8" src="phonegap-nfc-0.4.2.js"></script>        

## BlackBerry 7

Assuming you have an existing PhoneGap 2.3.0 Blackberry Webworks project:

### Build

Note: it's probably better to build from source code, rather than relying on downloads of the js and jar

    ant dist

### JavaScript 

[Download phonegap-nfc.js](https://github.com/chariotsolutions/phonegap-nfc/downloads) and add it to the www folder
    
Include phonegap-nfc.js in index.html

    <script type="text/javascript" charset="utf-8" src="phonegap-nfc.js"></script>        

### Java

[Download phonegap-nfc-webworks.jar](https://github.com/chariotsolutions/phonegap-nfc/downloads)

The webworks jar contains source code that must be included in the cordova jar file

Put phonegap-nfc-webworks.jar in the root of your webworks project.

	$ mkdir build/plugin
	$ cd build/plugin/
	$ jar xf ../../phonegap-nfc-webworks.jar
	$ jar uf ../../www/ext/cordova.2.3.0.jar .
	$ jar tf ../../www/ext/cordova.2.3.0.jar
	
Ensure that you see the NfcPlugin classes listed during the last step

	$ jar tf ../..www/ext/cordova.2.3.0.jar
	library.xml
	org/
	org/apache/
	org/apache/cordova/
	...
	org/apache/cordova/util/StringUtils.java
	com/
	com/chariotsolutions/
	com/chariotsolutions/nfc/
	com/chariotsolutions/nfc/plugin/
	com/chariotsolutions/nfc/plugin/NfcPlugin.java
	com/chariotsolutions/nfc/plugin/Util.java
	
You can delete phonegap-nfc-webworks.jar

### plugins.xml

Configure the NfcPlugin in res/xml/plugins.xml

    <plugin name="NfcPlugin" value="com.chariotsolutions.nfc.plugin.NfcPlugin"/>

### JavaScript 

[Download phonegap-nfc.js](https://github.com/chariotsolutions/phonegap-nfc/downloads) and add it to the www folder
    
Include phonegap-nfc.js in index.html

    <script type="text/javascript" charset="utf-8" src="phonegap-nfc.js"></script>        


