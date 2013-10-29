**Cordova 3.0 is the recommended way to use phonegap-nfc, see [Getting Started Cordova CLI](https://github.com/chariotsolutions/phonegap-nfc/blob/master/doc/GettingStartedCLI.md).**

# Installing PhoneGap NFC

* [Android](#android)
* [BlackBerry 10](#blackberry-10)
* [BlackBerry 7](#blackberry-7)
* [Windows Phone 8](#windows-phone-8)

These instructions assume you have an existing PhoneGap project. 

## Android

Your Android project should be using PhoneGap 2.8.

### Installing with Plugman (recommended)

Use [plugman](https://github.com/apache/cordova-plugman) to add phonegap-nfc to your Android project.  Plugman requires [node.js](http://nodejs.org) and is installed through npm.

Install plugman 0.7.10 or greater

    $ npm install -g plugman

Get the latest source code

    $ git clone https://github.com/chariotsolutions/phonegap-nfc.git

Install the plugin

    $ plugman --platform android --project /path/to/your/project --plugin /path/to/phonegap-nfc

### Manually Installing on Android

Get the latest source code

    $ git clone https://github.com/chariotsolutions/phonegap-nfc.git

Copy the Java source files from src/android/src/ of phonegap-nfc project into the source directory of your Android project.

    $ cp -R phonegap-nfc/src/android/src/ $YOUR_PROJECT/src
    
For windows use xcopy

    c:\> xcopy phonegap-nfc\src\android\src %YOUR_PROJECT%\src /S

#### config.xml 

Add the NfcPlugin in res/xml/config.xml

    <plugin name="NfcPlugin" value="com.chariotsolutions.nfc.plugin.NfcPlugin"/>

#### JavaScript 

Copy phonegap-nfc.js into assets/www/js/

     $ cp phonegap-nfc/www/phonegap-nfc.js $YOUR_PROJECT/assets/www/js/
     
Windows     
     
     $ copy phonegap-nfc\www\phonegap-nfc.js %YOUR_PROJECT%\assets\www\js\
    
Include phonegap-nfc.js in index.html.  Ensure that phonegap-nfc.js is *after* cordova.js and *before* index.js

    <script type="text/javascript" src="js/phonegap-nfc.js"></script>        

#### AndroidManifest.xml

Add NFC permissions

    <uses-permission android:name="android.permission.NFC" />

Ensure that the `minSdkVersion` is 14

    <uses-sdk android:minSdkVersion="14" />
    
### Requiring NFC

If you want to restrict your application to only devices with NFC hardware, set uses-feature so Google Play will restrict the listing.  If NFC is optional in your application, omit the uses-feature element.

    <uses-feature android:name="android.hardware.nfc" android:required="true" />

## BlackBerry 10

Get the latest source code

    $ git clone https://github.com/chariotsolutions/phonegap-nfc.git

### JavaScript 

The BB10 implementation is 100% JavaScript.  You must build the code from source.

    $ ant build-javascript

Copy dist/phonegap-nfc-VERSION.js and add it to your www folder

    $ cp dist/phonegap-nfc-0.4.2.js $YOUR_PROJECT/www
    
Include phonegap-nfc-VERSION.js in index.html

    <script type="text/javascript" src="phonegap-nfc-0.4.2.js"></script>        

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

## BlackBerry 7

Get the latest source code

    $ git clone https://github.com/chariotsolutions/phonegap-nfc.git

Build the code

    $ ant webworks

### JavaScript 

Copy dist/phonegap-nfc-VERSION.js and add it to your www folder
    
Include phonegap-nfc-VERSION.js in index.html

    <script type="text/javascript" src="phonegap-nfc-0.4.2.js"></script>       

### Java

The webworks jar contains source code that must be included in the Cordova jar file

Put phonegap-nfc-webworks-VERSION.jar in the root of your webworks project.

    $ mkdir build/plugin
    $ cd build/plugin/
    $ jar xf ../../phonegap-nfc-webworks-0.4.2.jar
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
	
You can delete phonegap-nfc-webworks-VERSION.jar

### plugins.xml

Configure the NfcPlugin in www/plugins.xml

    <plugin name="NfcPlugin" value="com.chariotsolutions.nfc.plugin.NfcPlugin"/>
    
## Windows Phone 8

Get the latest source code

    c:\> git clone https://github.com/chariotsolutions/phonegap-nfc.git

Copy the plugin files from phonegap-nfc\src\windows-phone-8 to the Plugins directory of your project

    c:\phonegap-nfc> copy src\windows-phone-8\*.cs %YOUR_PROJECT%\Plugins
    
Copy the javascript files from phonegap-nfc\www to the www directory of your project

    c:\phonegap-nfc> copy src\www\phonegap-nfc.js %YOUR_PROJECT%\www
    
Include phonegap-nfc-VERSION.js in index.html

    <script type="text/javascript" src="phonegap-nfc.js"></script>
    
Add NfcPlugin to config.xml

    <plugin name="NfcPlugin"/>
    
Open WMAppManifest.xml, choose the Capabilities tab, and check ID_CAP_PROXIMITY permission
