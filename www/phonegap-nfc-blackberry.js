/*jslint browser: true, unused: vars, quotmark: double */
/*global cordova, nfc, ndef, blackberry */

// blackberry required the com.blackberry.invoke plugin installed
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

nfc.unshare = function(success, failure) {
    "use strict";
    blackberry.invoke.closeChildCard();
    if (success) { // no idea if it worked. assume success.
        success();
    }
};

// need to override addNdefListener because service name is different for BB10 (due to a bug)
nfc.addNdefListener = function (callback, win, fail) {
    "use strict";
    document.addEventListener("ndef", callback, false);
    cordova.exec(win, fail, "com.chariotsolutions.nfc.plugin", "registerNdef", []);
};
