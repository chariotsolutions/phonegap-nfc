/*global PhoneGap*/

PhoneGap.addConstructor(
    function () {
        PhoneGap.exec(
            function () {
                console.log("Initialized the NdefPlugin");
            },
            function (reason) {
                alert("Failed to initialize the NdefPlugin " + reason);
            },
            "NdefPlugin", "init", []
        )
    }
);

var Ndef = {
    // see android.nfc.NdefRecord for documentation about constants
    // http://developer.android.com/reference/android/nfc/NdefRecord.html
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

    /**
     * Creates a JSON representation of a NDEF Record.
     * 
     * @tnf 3-bit TNF (Type Name Format) - use one of the TNF_* constants
     * @type byte array, containing zero to 255 bytes, must not be null
     * @id byte array, containing zero to 255 bytes, must not be null
     * @payload byte array, containing zero to (2 ** 32 - 1) bytes, must not be null
     *
     * @returns JSON representation of a NDEF record
     * 
     * @see Ndef.textRecord, Ndef.uriRecord and Ndef.mimeMediaRecord for examples        
     */
    record: function (tnf, type, id, payload) {
        return {
            tnf: tnf,
            type: type,
            id: id,
            payload: payload
        };
    },

    /**
     * Helper that creates a NDEF record containing plain text.
     *
     * @text String
     * @id byte[] (optional)
     */
    textRecord: function (text, id) {
        var languageCode = 'en', // TODO get from browser
            payload = [];         
            
        if (!id) { id = []; }   
        
        payload.push(languageCode.length);        
        Ndef.concatArray(payload, Ndef.stringToBytes(languageCode));
        Ndef.concatArray(payload, Ndef.stringToBytes(text));

        return Ndef.record(Ndef.TNF_WELL_KNOWN, Ndef.RTD_TEXT, id, payload);
    },

    /**
     * Helper that creates a NDEF record containing an absolute URI.
     *
     * @text String
     * @id byte[] (optional)
     */
    uriRecord: function (text, id) {
        if (!id) { id = []; }   
        return Ndef.record(Ndef.TNF_ABSOLUTE_URI, Ndef.RTD_URI, id, Ndef.stringToBytes(text));
    },

    /**
     * Helper that creates a NDEF record containing an mimeMediaRecord.
     *
     * @mimeType String
     * @payload byte[]
     * @id byte[] (optional)
     */    
    mimeMediaRecord: function (mimeType, payload, id) {
        if (!id) { id = []; }   
        return Ndef.record(Ndef.TNF_MIME_MEDIA, Ndef.stringToBytes(mimeType), id, payload);
    },
    
    concatArray: function (a1, a2) { // this isn't built in?
        for (var i = 0; i < a2.length; i++) {
            a1.push(a2[i]);
        }
        return a1;
    },
    
    bytesToString: function (bytes) {
      var bytesAsString = "";
      for (var i = 0; i < bytes.length; i++) {
        bytesAsString += String.fromCharCode(bytes[i]);
      }
      return bytesAsString;
    },
    
    // http://stackoverflow.com/questions/1240408/reading-bytes-from-a-javascript-string#1242596
    stringToBytes: function ( str ) {
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
    
};

navigator.nfc = {

    addMimeTypeListener: function (mimeType, callback, win, fail) {
        document.addEventListener("ndef-mime", callback, false);    
        PhoneGap.exec(win, fail, "NdefPlugin", "registerMimeType", [mimeType]);
    },
    
    addNdefListener: function (callback, win, fail) {
        document.addEventListener("ndef", callback, false);                
        PhoneGap.exec(win, fail, "NdefPlugin", "registerNdef", []);
    },
    
    addNdefFormatableListener: function (callback, win, fail) {
        document.addEventListener("ndef-formatable", callback, false);
        PhoneGap.exec(win, fail, "NdefPlugin", "registerNdefFormatable", []);
    },
    
    writeTag: function (ndefMessage, win, fail) {
      PhoneGap.exec(win, fail, "NdefPlugin", "writeTag", [ndefMessage]);
    },

    shareTag: function (ndefMessage, win, fail) {
      PhoneGap.exec(win, fail, "NdefPlugin", "shareTag", [ndefMessage]);
    },

    unshareTag: function (win, fail) {
      PhoneGap.exec(win, fail, "NdefPlugin", "unshareTag", []);
    },

    eraseTag: function (win, fail) {
      PhoneGap.exec(win, fail, "NdefPlugin", "writeTag", [[]]);
    }

};
