/*global PhoneGap*/
// http://stackoverflow.com/questions/1240408/reading-bytes-from-a-javascript-string#1242596
function stringToBytes ( str ) {
    var ch, st, re = [];
    for (var i = 0; i < str.length; i++ ) {
      ch = str.charCodeAt(i);  // get char 
      st = [];                 // set up "stack"
      do {
        st.push( ch & 0xFF );  // push byte to stack
        ch = ch >> 8;          // shift value down by 1 byte
      }  
      while ( ch );
      // add stack contents to result
      // done because chars have "wrong" endianness
      re = re.concat( st.reverse() );
    }
    // return an array of bytes
    return re;
}

var Ndef = {
    // see android.nfc.NdefRecord for documentation about constants
    TNF_EMPTY: 0x0,
    TNF_WELL_KNOWN: 0x01,
    TNF_MIME_MEDIA: 0x02,
    TNF_ABSOLUTE_URI: 0x03,
    TNF_EXTERNAL_TYPE: 0x04,
    TNF_UNKNOWN: 0x05,
    TNF_UNCHANGED: 0x06,
    TNF_RESERVED: 0x07,

    RTD_TEXT: [0x54], // "T"
    RTD_URI: [0x55], // "U" 
    RTD_SMART_POSTER: [0x53, 0x70], // "Sp"
    RTD_ALTERNATIVE_CARRIER: [0x61, 0x63], // "ac"
    RTD_HANDOVER_CARRIER: [0x48, 0x63], // "Hc"
    RTD_HANDOVER_REQUEST: [0x48, 0x72], // "Hr"
    RTD_HANDOVER_SELECT: [0x48, 0x73], // "Hs"

    record: function (tnf, type, id, payload) { // TODO get a better name
        return {
            tnf: tnf,
            type: type,
            id: id,
            payload: payload
        };
    },

    // TODO people should build their own helper methods and call Ndef.record(...)
    // textRecord, uriRecord and mimeMediaRecord are provided for convenience and as examples
        
    textRecord: function (text) {
        var languageCode = 'en', // TODO get from browser
            payload = [];            

        payload.push(languageCode.length);        
        Ndef.concatArray(payload, stringToBytes(languageCode));
        Ndef.concatArray(payload, stringToBytes(text));

        return Ndef.record(Ndef.TNF_WELL_KNOWN, Ndef.RTD_TEXT, [], payload);
    },
    
    uriRecord: function (text) {
        return Ndef.record(Ndef.TNF_ABSOLUTE_URI, Ndef.RTD_URI, [], stringToBytes(text));
    },
    
    mimeMediaRecord: function (mimeType, payload) {
        return Ndef.record(Ndef.TNF_MIME_MEDIA, stringToBytes(mimeType), [], payload);
    },
    
    concatArray: function (a1, a2) { // this isn't built in?
        for (var i = 0; i < a2.length; i++) {
            a1.push(a2[i]);
        }
        return a1;
    }
    
};

var NdefPlugin = function() {
};

NdefPlugin.prototype.register = function(mime_type, win, fail) {
    console.log('registering');
  PhoneGap.exec(win, fail, "NdefPlugin", "register", [mime_type]);
};

// TODO rename this shite
NdefPlugin.prototype.registerForWrite = function(win, fail) {
    PhoneGap.exec(win, fail, "NdefPlugin", "registerForWrite", []);
};

// Message should be an Array of NDEF records
// Must be called from the event handler so we have a tag
NdefPlugin.writeTag = function (ndefMessage, win, fail) {
  console.log('writeTag');
  PhoneGap.exec(win, fail, "NdefPlugin", "writeTag", [ndefMessage]);
};

NdefPlugin.bytesToString = function (bytes) {
  var bytesAsString = "";
  for (var i = 0; i < bytes.length; i++) {
    bytesAsString += String.fromCharCode(bytes[i]);
  }
  return bytesAsString;
};

NdefPlugin.fireNfc = function (tagData) {
  var e = document.createEvent('Events'),
  type = 'ndef';
  
  e.initEvent(type);
  e.tagData = tagData;
  
  document.dispatchEvent(e);
};

/**
 * <ul>
 * <li>Register the NFC Javascript plugin.</li>
 * <li>Also register native call which will be called when this plugin runs</li>
 * </ul>
 */
PhoneGap.addConstructor(function() { 
  // Register the javascript plugin with PhoneGap
  PhoneGap.addPlugin('NdefPlugin', new NdefPlugin());

  // Register the native class of plugin with PhoneGap
  navigator.app.addService("NdefPlugin", "com.chariotsolutions.nfc.plugin.NdefPlugin"); 
});
