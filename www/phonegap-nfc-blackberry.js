/*jslint browser: true, unused: vars, quotmark: double */
/*global cordova, nfc, ndef, blackberry */

// blackberry requires the com.blackberry.invoke plugin installed

// you need to edit config.xml for your app and add an invoke-target
// <rim:invoke-target id="com.chariotsolutions.nfc.demo.reader.target">
//     <type>APPLICATION</type>
//     <filter>
//         <action>bb.action.OPEN</action>
//         <mime-type>application/vnd.rim.nfc.ndef</mime-type>
//         <property value="ndef://0,ndef://1,ndef://2,ndef://3,ndef://4" var="uris" />
//     </filter>
// </rim:invoke-target>

// clobber existing share function
nfc.share = function(ndefMessage, success, failure) {
    "use strict";
    var byteArray = ndef.encodeMessage(ndefMessage),
        data = "",
        query;

    for (var i=0; i< byteArray.length; ++i) {
        data += String.fromCharCode(byteArray[i]);
    }

    query = {
        "action": "bb.action.SHARE",
        "type": "application/vnd.rim.nfc.ndef",
        "data": data
    };

    blackberry.invoke.invoke(query, success, failure);
};

// clobber existing unshare function
nfc.unshare = function(success, failure) {
    "use strict";
    blackberry.invoke.closeChildCard();
    if (success) { // no idea if it worked. assume success.
        success();
    }
};

// takes an ndefMessage from the success callback and fires a javascript event
var proxy = function(ndefMessageAsString) {
    "use strict";
    var ndefMessage = JSON.parse(ndefMessageAsString);
    cordova.fireDocumentEvent("ndef", {
        type: "ndef",
        tag: {
            ndefMessage: ndefMessage
        }
    });
};

// clobber existing addNdefListener function
nfc.addNdefListener = function (callback, success, failure) {
    "use strict";
    document.addEventListener("ndef", callback, false);
    cordova.exec(proxy, failure, "com.chariotsolutions.nfc.plugin", "registerNdef", []);
    success(); // assume success
};
