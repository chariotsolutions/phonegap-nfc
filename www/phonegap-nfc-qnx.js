if (navigator.userAgent.indexOf("BB10") > -1) {
    //only run this code if on BlackBerry 10

    cordova.define('cordova/plugin/qnx/nfc', function (require, exports, module) {
        var alphabet = "ABCDEFGHIJKLM" +
                       "NOPQRSTUVWXYZ" +
                       "abcdefghijklm" +
                       "nopqrstuvwxyz" +
                       "0123456789+/=";

        function b64toArray(input) {
            var result = [],
                workValue = [],
                bytes = [],
                offset = 0;

            while (offset < input.length) {
                for (var i = 0; i < 4; ++i) {
                    if (offset >= input.length) {
                        workValue[i] = 64;
                    } else {
                        var index = alphabet.indexOf(input.substring(offset++, offset));
                        if (index === -1) {
                            --i;
                            continue;
                        }
                        workValue[i] = index;
                    }
                }
                bytes[0] = (workValue[0] << 2 | workValue[1] >> 4) & 255;
                bytes[1] = (workValue[1] << 4 | workValue[2] >> 2) & 255; 
                bytes[2] = (workValue[2] << 6 | workValue[3]) & 255;
                
                if (workValue[3] === 64 && workValue[2] === 64) {
                    result.push(bytes[0]);
                } else if (workValue[3] === 64) {
                    result.push(bytes[0]);
                    result.push(bytes[1]);
                } else {
                    result.push(bytes[0]);
                    result.push(bytes[1]);
                    result.push(bytes[2]);
                }
            }
            
            return result;
        }

        function decodeNdefRecord(encoded) {

            var ndefRecord = { 
                    tnf: encoded[0] & 7
                },
                flags = encoded[0],
                isShortRecord = (flags & 16) !== 0, //  short record
                hasIdLength = (flags & 8) !== 0, // identification length
                offset = 1,
                typeLength = encoded[offset++],
                idLength = 0,
                payloadLength = 0;

            if (isShortRecord) {
                payloadLength = encoded[offset++];
            } else {
                for ( var i = 0; i < 4; ++i) {
                    payloadLength *= 256;
                    payloadLength |= encoded[offset++];
                }
            }
            
            if (hasIdLength) {
                idLength = encoded[offset++];
            }

            ndefRecord.type = encoded.slice(offset, offset + typeLength);

            offset += typeLength;
            ndefRecord.id = encoded.slice(offset, offset + idLength);            

            offset += idLength;
            ndefRecord.payload = encoded.slice(offset, offset + payloadLength);

            return ndefRecord;
        }

        function decode(encoding) {
            var decoded = [];
            var offset = 0;

            while (offset < encoding.length) {
                var start = offset;
                var remaining = encoding.length - offset;
                var flags = encoding[offset++];
                var minLength = 1 + 1;
                var sr = (flags & 16) !== 0;
                var il = (flags & 8) !== 0;

                minLength += sr ? 1 : 4;
                minLength += il ? 1 : 0;
                if (minLength <= remaining) {
                    var typeLength = encoding[offset++];
                    var payloadLength = 0;
                    if (sr) {
                        payloadLength = encoding[offset++];
                    } else {
                        for ( var i = 0; i < 4; ++i) {
                            payloadLength <<= 8;
                            payloadLength |= encoding[offset++];
                        }
                    }
                    var idLength = il ? encoding[offset++] : 0;
                    var totalLength = minLength + typeLength + payloadLength + idLength;
                    if (totalLength <= remaining) {
                        var encoded = encoding.slice(start, start + totalLength);

                        decoded.push(decodeNdefRecord(encoded));

                        offset = start + totalLength;
                    }
                }
            }
            return decoded;
        }

        module.exports = {
            init: function (args, win, fail) {
                blackberry.event.addEventListener("invoked", function (payload) {
                    cordova.fireDocumentEvent("ndef", {
                        type: 'ndef',
                        tag: {
                            ndefMessage: decode(b64toArray(payload.data))
                        }
                    });
                });
                win();
                return { "status" : cordova.callbackStatus.OK, "message" : "" };
            },
            registerTag: function (args, win, fail) {
                return { "status" : cordova.callbackStatus.ERROR, 
                    "message" : "addTagDiscoveredListener is no longer supported. Use addNdefListener." };
            },
            registerNdef: function (args, win, fail) {
                // do nothing 
                // init() handles the blackberry event and creates a phonegap-nfc event
                // and nfc.addNdefListener adds the listener for the client code
                return { "status" : cordova.callbackStatus.OK, "message" : "" };
            },
            shareTag: function(args, win, fail) {
                var message = args[0],
                    byteArray = ndef.encodeMessage(message),
                    data = "";
                
                for (var i=0; i< byteArray.length; ++i) {
                    data += String.fromCharCode(byteArray[i]);
                }

                var query = {
                        "action": "bb.action.SHARE",
                        "type": "application/vnd.rim.nfc.ndef",
                        "data": data
                };

                // http://developer.blackberry.com/html5/api/blackberry.invoke.html#.invoke
                blackberry.invoke.invoke(query, win, fail);
                
                return { "status" : cordova.callbackStatus.NO_RESULT, "message" : "" };
            },
            unshareTag: function(args, win, fail) {
                blackberry.invoke.closeChildCard();
                // no idea if it worked, assume success
                return { "status" : cordova.callbackStatus.OK, "message" : "" };                
            }
        };
    });

    cordova.addConstructor(function () {
        var manager = cordova.require('cordova/plugin/qnx/manager');
        if (manager.addPlugin) {
            manager.addPlugin("NfcPlugin", 'cordova/plugin/qnx/nfc');
        }
        else {
            console.log("BB10 Support will only work with version 2.3 and higher");
        }
    });
}
