/*jshint  bitwise: false, camelcase: false, quotmark: false, unused: vars */
/*global cordova, console */
"use strict";

function handleNfcFromIntentFilter() {

    // This was historically done in cordova.addConstructor but broke with PhoneGap-2.2.0.
    // We need to handle NFC from an Intent that launched the application, but *after*
    // the code in the application's deviceready has run.  After upgrading to 2.2.0,
    // addConstructor was finishing *before* deviceReady was complete and the
    // ndef listeners had not been registered.
    // It seems like there should be a better solution.
    if (cordova.platformId === "android") {
        setTimeout(
            function () {
                cordova.exec(
                    function () {
                        console.log("Initialized the NfcPlugin");
                    },
                    function (reason) {
                        console.log("Failed to initialize the NfcPlugin " + reason);
                    },
                    "NfcPlugin", "init", []
                );
            }, 10
        );
    }
}

document.addEventListener('deviceready', handleNfcFromIntentFilter, false);

var ndef = {

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

        // handle null values
        if (!tnf) { tnf = ndef.TNF_EMPTY; }
        if (!type) { type = []; }
        if (!id) { id = []; }
        if (!payload) { payload = []; }

        // convert strings to arrays
        if (!(type instanceof Array)) {
            type = nfc.stringToBytes(type);
        }
        if (!(id instanceof Array)) {
            id = nfc.stringToBytes(id);
        }
        if (!(payload instanceof Array)) {
            payload = nfc.stringToBytes(payload);
        }

        return {
            tnf: tnf,
            type: type,
            id: id,
            payload: payload
        };
    },

    /**
     * Helper that creates an NDEF record containing plain text.
     *
     * @text String of text to encode
     * @languageCode ISO/IANA language code. Examples: “fi”, “en-US”, “fr- CA”, “jp”. (optional)
     * @id byte[] (optional)
     */
    textRecord: function (text, languageCode, id) {
        var payload = textHelper.encodePayload(text, languageCode);
        if (!id) { id = []; }
        return ndef.record(ndef.TNF_WELL_KNOWN, ndef.RTD_TEXT, id, payload);
    },

    /**
     * Helper that creates a NDEF record containing a URI.
     *
     * @uri String
     * @id byte[] (optional)
     */
    uriRecord: function (uri, id) {
        var payload = uriHelper.encodePayload(uri);
        if (!id) { id = []; }
        return ndef.record(ndef.TNF_WELL_KNOWN, ndef.RTD_URI, id, payload);
    },

    /**
     * Helper that creates a NDEF record containing an absolute URI.
     *
     * An Absolute URI record means the URI describes the payload of the record.
     *
     * For example a SOAP message could use "http://schemas.xmlsoap.org/soap/envelope/"
     * as the type and XML content for the payload.
     *
     * Absolute URI can also be used to write LaunchApp records for Windows.
     *
     * See 2.4.2 Payload Type of the NDEF Specification
     * http://www.nfc-forum.org/specs/spec_list#ndefts
     *
     * Note that by default, Android will open the URI defined in the type
     * field of an Absolute URI record (TNF=3) and ignore the payload.
     * BlackBerry and Windows do not open the browser for TNF=3.
     *
     * To write a URI as the payload use ndef.uriRecord(uri)
     *
     * @uri String
     * @payload byte[] or String
     * @id byte[] (optional)
     */
    absoluteUriRecord: function (uri, payload, id) {
        if (!id) { id = []; }
        if (!payload) { payload = []; }
        return ndef.record(ndef.TNF_ABSOLUTE_URI, uri, id, payload);
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
        return ndef.record(ndef.TNF_MIME_MEDIA, nfc.stringToBytes(mimeType), id, payload);
    },

    /**
     * Helper that creates an NDEF record containing an Smart Poster.
     *
     * @ndefRecords array of NDEF Records
     * @id byte[] (optional)
     */
    smartPoster: function (ndefRecords, id) {
        var payload = [];

        if (!id) { id = []; }

        if (ndefRecords)
        {
            // make sure we have an array of something like NDEF records before encoding
            if (ndefRecords[0] instanceof Object && ndefRecords[0].hasOwnProperty('tnf')) {
                payload = ndef.encodeMessage(ndefRecords);
            } else {
                // assume the caller has already encoded the NDEF records into a byte array
                payload = ndefRecords;
            }
        } else {
            console.log("WARNING: Expecting an array of NDEF records");
        }

        return ndef.record(ndef.TNF_WELL_KNOWN, ndef.RTD_SMART_POSTER, id, payload);
    },

    /**
     * Helper that creates an empty NDEF record.
     *
     */
    emptyRecord: function() {
        return ndef.record(ndef.TNF_EMPTY, [], [], []);
    },

    /**
     * Encodes an NDEF Message into bytes that can be written to a NFC tag.
     *
     * @ndefRecords an Array of NDEF Records
     *
     * @returns byte array
     *
     * @see NFC Data Exchange Format (NDEF) http://www.nfc-forum.org/specs/spec_list/
     */
    encodeMessage: function (ndefRecords) {

        var encoded = [],
            tnf_byte,
            type_length,
            payload_length,
            id_length,
            i,
            mb, me, // messageBegin, messageEnd
            cf = false, // chunkFlag TODO implement
            sr, // boolean shortRecord
            il; // boolean idLengthFieldIsPresent

        for(i = 0; i < ndefRecords.length; i++) {

            mb = (i === 0);
            me = (i === (ndefRecords.length - 1));
            sr = (ndefRecords[i].payload.length < 0xFF);
            il = (ndefRecords[i].id.length > 0);
            tnf_byte = ndef.encodeTnf(mb, me, cf, sr, il, ndefRecords[i].tnf);
            encoded.push(tnf_byte);

            type_length = ndefRecords[i].type.length;
            encoded.push(type_length);

            if (sr) {
                payload_length = ndefRecords[i].payload.length;
                encoded.push(payload_length);
            } else {
                payload_length = ndefRecords[i].payload.length;
                // 4 bytes
                encoded.push((payload_length >> 24));
                encoded.push((payload_length >> 16));
                encoded.push((payload_length >> 8));
                encoded.push((payload_length & 0xFF));
            }

            if (il) {
                id_length = ndefRecords[i].id.length;
                encoded.push(id_length);
            }

            encoded = encoded.concat(ndefRecords[i].type);

            if (il) {
                encoded = encoded.concat(ndefRecords[i].id);
            }

            encoded = encoded.concat(ndefRecords[i].payload);
        }

        return encoded;
    },

    /**
     * Decodes an array bytes into an NDEF Message
     *
     * @bytes an array bytes read from a NFC tag
     *
     * @returns array of NDEF Records
     *
     * @see NFC Data Exchange Format (NDEF) http://www.nfc-forum.org/specs/spec_list/
     */
    decodeMessage: function (bytes) {

        var bytes = bytes.slice(0), // clone since parsing is destructive
            ndef_message = [],
            tnf_byte,
            header,
            type_length = 0,
            payload_length = 0,
            id_length = 0,
            record_type = [],
            id = [],
            payload = [];

        while(bytes.length) {
            tnf_byte = bytes.shift();
            header = ndef.decodeTnf(tnf_byte);

            type_length = bytes.shift();

            if (header.sr) {
                payload_length = bytes.shift();
            } else {
                // next 4 bytes are length
                payload_length = ((0xFF & bytes.shift()) << 24) |
                    ((0xFF & bytes.shift()) << 26) |
                    ((0xFF & bytes.shift()) << 8) |
                    (0xFF & bytes.shift());
            }

            if (header.il) {
                id_length = bytes.shift();
            }

            record_type = bytes.splice(0, type_length);
            id = bytes.splice(0, id_length);
            payload = bytes.splice(0, payload_length);

            ndef_message.push(
                ndef.record(header.tnf, record_type, id, payload)
            );

            if (header.me) { break; } // last message
        }

        return ndef_message;
    },

    /**
     * Decode the bit flags from a TNF Byte.
     *
     * @returns object with decoded data
     *
     *  See NFC Data Exchange Format (NDEF) Specification Section 3.2 RecordLayout
     */
    decodeTnf: function (tnf_byte) {
        return {
            mb: (tnf_byte & 0x80) !== 0,
            me: (tnf_byte & 0x40) !== 0,
            cf: (tnf_byte & 0x20) !== 0,
            sr: (tnf_byte & 0x10) !== 0,
            il: (tnf_byte & 0x8) !== 0,
            tnf: (tnf_byte & 0x7)
        };
    },

    /**
     * Encode NDEF bit flags into a TNF Byte.
     *
     * @returns tnf byte
     *
     *  See NFC Data Exchange Format (NDEF) Specification Section 3.2 RecordLayout
     */
    encodeTnf: function (mb, me, cf, sr, il, tnf) {

        var value = tnf;

        if (mb) {
            value = value | 0x80;
        }

        if (me) {
            value = value | 0x40;
        }

        // note if cf: me, mb, li must be false and tnf must be 0x6
        if (cf) {
            value = value | 0x20;
        }

        if (sr) {
            value = value | 0x10;
        }

        if (il) {
            value = value | 0x8;
        }

        return value;
    }

};

// nfc provides javascript wrappers to the native phonegap implementation
var nfc = {

    addTagDiscoveredListener: function (callback, win, fail) {
        document.addEventListener("tag", callback, false);
        cordova.exec(win, fail, "NfcPlugin", "registerTag", []);
    },

    addMimeTypeListener: function (mimeType, callback, win, fail) {
        document.addEventListener("ndef-mime", callback, false);
        cordova.exec(win, fail, "NfcPlugin", "registerMimeType", [mimeType]);
    },

    addNdefListener: function (callback, win, fail) {
        document.addEventListener("ndef", callback, false);
        cordova.exec(win, fail, "NfcPlugin", "registerNdef", []);
    },

    addNdefFormatableListener: function (callback, win, fail) {
        document.addEventListener("ndef-formatable", callback, false);
        cordova.exec(win, fail, "NfcPlugin", "registerNdefFormatable", []);
    },

    write: function (ndefMessage, win, fail) {
        cordova.exec(win, fail, "NfcPlugin", "writeTag", [ndefMessage]);
    },

    makeReadOnly: function (win, fail) {
        cordova.exec(win, fail, "NfcPlugin", "makeReadOnly", []);
    },

    share: function (ndefMessage, win, fail) {
        cordova.exec(win, fail, "NfcPlugin", "shareTag", [ndefMessage]);
    },

    unshare: function (win, fail) {
        cordova.exec(win, fail, "NfcPlugin", "unshareTag", []);
    },

    handover: function (uris, win, fail) {
        // if we get a single URI, wrap it in an array
        if (!Array.isArray(uris)) {
            uris = [ uris ];
        }
        cordova.exec(win, fail, "NfcPlugin", "handover", uris);
    },

    stopHandover: function (win, fail) {
        cordova.exec(win, fail, "NfcPlugin", "stopHandover", []);
    },

    erase: function (win, fail) {
        cordova.exec(win, fail, "NfcPlugin", "eraseTag", [[]]);
    },

    removeTagDiscoveredListener: function (win, fail) {
        document.removeEventListener("tag", callback, false);
        cordova.exec(win, fail, "NfcPlugin", "removeTag", []);
    },

    removeMimeTypeListener: function(mimeType, win, fail) {
        document.removeEventListener("ndef-mime", callback, false);
        cordova.exec(win, fail, "NfcPlugin", "removeMimeType", [mimeType]);
    },

    removeNdefListener: function (win, fail) {
        document.removeEventListener("ndef", callback, false);
        cordova.exec(win, fail, "NfcPlugin", "removeNdef", []);
    }

};

var util = {
    // i must be <= 256
    toHex: function (i) {
        var hex;

        if (i < 0) {
            i += 256;
        }

        hex = i.toString(16);

        // zero padding
        if (hex.length === 1) {
            hex = "0" + hex;
        }

        return hex;
    },

    toPrintable: function(i) {

        if (i >= 0x20 & i <= 0x7F) {
            return String.fromCharCode(i);
        } else {
            return '.';
        }
    },

    bytesToString: function(bytes) {
        // based on http://ciaranj.blogspot.fr/2007/11/utf8-characters-encoding-in-javascript.html

        var result = "";
        var i, c, c1, c2, c3;
        i = c = c1 = c2 = c3 = 0;

        // Perform byte-order check.
        if( bytes.length >= 3 ) {
            if( (bytes[0] & 0xef) == 0xef && (bytes[1] & 0xbb) == 0xbb && (bytes[2] & 0xbf) == 0xbf ) {
                // stream has a BOM at the start, skip over
                i = 3;
            }
        }

        while ( i < bytes.length ) {
            c = bytes[i] & 0xff;

            if ( c < 128 ) {

                result += String.fromCharCode(c);
                i++;

            } else if ( (c > 191) && (c < 224) ) {

                if ( i + 1 >= bytes.length ) {
                    throw "Un-expected encoding error, UTF-8 stream truncated, or incorrect";
                }
                c2 = bytes[i + 1] & 0xff;
                result += String.fromCharCode( ((c & 31) << 6) | (c2 & 63) );
                i += 2;

            } else {

                if ( i + 2 >= bytes.length  || i + 1 >= bytes.length ) {
                    throw "Un-expected encoding error, UTF-8 stream truncated, or incorrect";
                }
                c2 = bytes[i + 1] & 0xff;
                c3 = bytes[i + 2] & 0xff;
                result += String.fromCharCode( ((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63) );
                i += 3;

            }
        }
        return result;
    },

    stringToBytes: function(string) {
        // based on http://ciaranj.blogspot.fr/2007/11/utf8-characters-encoding-in-javascript.html

        var bytes = [];

        for (var n = 0; n < string.length; n++) {

            var c = string.charCodeAt(n);

            if (c < 128) {

                bytes[bytes.length]= c;

            } else if((c > 127) && (c < 2048)) {

                bytes[bytes.length] = (c >> 6) | 192;
                bytes[bytes.length] = (c & 63) | 128;

            } else {

                bytes[bytes.length] = (c >> 12) | 224;
                bytes[bytes.length] = ((c >> 6) & 63) | 128;
                bytes[bytes.length] = (c & 63) | 128;

            }

        }

        return bytes;
    },

    bytesToHexString: function (bytes) {
        var dec, hexstring, bytesAsHexString = "";
        for (var i = 0; i < bytes.length; i++) {
            if (bytes[i] >= 0) {
                dec = bytes[i];
            } else {
                dec = 256 + bytes[i];
            }
            hexstring = dec.toString(16);
            // zero padding
            if (hexstring.length === 1) {
                hexstring = "0" + hexstring;
            }
            bytesAsHexString += hexstring;
        }
        return bytesAsHexString;
    },

    // This function can be removed if record.type is changed to a String
    /**
     * Returns true if the record's TNF and type matches the supplied TNF and type.
     *
     * @record NDEF record
     * @tnf 3-bit TNF (Type Name Format) - use one of the TNF_* constants
     * @type byte array or String
     */
    isType: function(record, tnf, type) {
        if (record.tnf === tnf) { // TNF is 3-bit
            var recordType;
            if (typeof(type) === 'string') {
                recordType = type;
            } else {
                recordType = nfc.bytesToString(type);
            }
            return (nfc.bytesToString(record.type) === recordType);
        }
        return false;
    }

};

// this is a module in ndef-js
var textHelper = {

    decodePayload: function (data) {

        var languageCodeLength = (data[0] & 0x1F), // 5 bits
            languageCode = data.slice(1, 1 + languageCodeLength),
            utf16 = (data[0] & 0x80) !== 0; // assuming UTF-16BE

        // TODO need to deal with UTF in the future
        // console.log("lang " + languageCode + (utf16 ? " utf16" : " utf8"));

        return util.bytesToString(data.slice(languageCodeLength + 1));
    },

    // encode text payload
    // @returns an array of bytes
    encodePayload: function(text, lang, encoding) {

        // ISO/IANA language code, but we're not enforcing
        if (!lang) { lang = 'en'; }

        var encoded = util.stringToBytes(lang + text);
        encoded.unshift(lang.length);

        return encoded;
    }

};

// this is a module in ndef-js
var uriHelper = {
    // URI identifier codes from URI Record Type Definition NFCForum-TS-RTD_URI_1.0 2006-07-24
    // index in array matches code in the spec
    protocols: [ "", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:", "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://", "tcpobex://", "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:" ],

    // decode a URI payload bytes
    // @returns a string
    decodePayload: function (data) {
        var prefix = uriHelper.protocols[data[0]];
        if (!prefix) { // 36 to 255 should be ""
            prefix = "";
        }
        return prefix + util.bytesToString(data.slice(1));
    },

    // shorten a URI with standard prefix
    // @returns an array of bytes
    encodePayload: function (uri) {

        var prefix,
            protocolCode,
            encoded;

        // check each protocol, unless we've found a match
        // "urn:" is the one exception where we need to keep checking
        // slice so we don't check ""
        uriHelper.protocols.slice(1).forEach(function(protocol) {
            if ((!prefix || prefix === "urn:") && uri.indexOf(protocol) === 0) {
                prefix = protocol;
            }
        });

        if (!prefix) {
            prefix = "";
        }

        encoded = util.stringToBytes(uri.slice(prefix.length));
        protocolCode = uriHelper.protocols.indexOf(prefix);
        // prepend protocol code
        encoded.unshift(protocolCode);

        return encoded;
    }
};

// added since WP8 must call a named function
// TODO consider switching NFC events from JS events to using the PG callbacks
function fireNfcTagEvent(eventType, tagAsJson) {
    setTimeout(function () {
        var e = document.createEvent('Events');
        e.initEvent(eventType, true, false);
        e.tag = JSON.parse(tagAsJson);
        console.log(e.tag);
        document.dispatchEvent(e);
    }, 10);
}

// textHelper and uriHelper aren't exported, add a property
ndef.uriHelper = uriHelper;
ndef.textHelper = textHelper;

// create aliases
nfc.bytesToString = util.bytesToString;
nfc.stringToBytes = util.stringToBytes;
nfc.bytesToHexString = util.bytesToHexString;

// kludge some global variables for plugman js-module support
// eventually these should be replaced and referenced via the module
window.nfc = nfc;
window.ndef = ndef;
window.util = util;
window.fireNfcTagEvent = fireNfcTagEvent;
