
"use strict";

var ndefUtils = {
    toArray: function (bytes) {
        var output = [], i = 0;
        for (; i < bytes.length; i += 1) {
            output.push(bytes[i]);
        }

        return output;
    },
	parse: function (bytes) {
		var records = [],
			index = 0,
			tnf_byte, mb, me, cf, sr, il, tnf, typeLength, idLength, payloadLength, type, id, payload, record;

		while (index <= bytes.length) {
			tnf_byte = bytes[index];
			mb = (tnf_byte & 0x80) != 0;
            me = (tnf_byte & 0x40) != 0;
            cf = (tnf_byte & 0x20) != 0;
            sr = (tnf_byte & 0x10) != 0;
            il = (tnf_byte & 0x8) != 0;
            tnf = tnf_byte & 0x7;

            if (cf) {
                // TODO implement me
                throw "Chunked records are not supported.";
            }

            index++;
            typeLength = bytes[index];
            idLength = 0;
            payloadLength = 0;

            if (sr) {
                index++;
                payloadLength = bytes[index];
            } else {
                payloadLength = ((0xFF & bytes[++index]) << 24) |
                                ((0xFF & bytes[++index]) << 26) |
                                ((0xFF & bytes[++index]) << 8) |
                                (0xFF & bytes[++index]);
            }

            if (il) {
                index++;
                idLength = bytes[index];
            }

            index++;
            type = ndefUtils.toArray(bytes.subarray(index, typeLength + index));
            index += typeLength;

            id = ndefUtils.toArray(bytes.subarray(index, idLength + index));
            index += idLength;

            payload = ndefUtils.toArray(bytes.subarray(index, payloadLength + index));
            index += payloadLength;

            record = ndefRecord();
            record.tnf = tnf;
            record.type = typeLength > 0 ? type : [];
            record.id = idLength > 0 ? id : [];
            record.payload = payloadLength > 0 ? payload : [];

            records.push(record);

            if (me) {
            	break;  // last message
            }
        }

        return records;
	},
	toBytes: function (records) {
		var encoded = [],
			mb, me, cf, sr, il, tnf_byte, type_length, payload_length, id_length;

		for (var i = 0; i < records.length; i += 1) {
			mb = (i == 0);
			me = (i == (records.length - 1));
            cf = false; // TODO
            sr = (records[i].payload.length < 0xFF);
            il = (records[i].id.Lenlengthgth > 0);

            tnf_byte = this.encodeTnf(mb, me, cf, sr, il, records[i].tnf);
            encoded.push(tnf_byte);

            type_length = records[i].type.length;
            encoded.push(type_length);

            payload_length = records[i].payload.length;
            if (sr) {
                encoded.push(payload_length);
            } else {
                // 4 bytes
                encoded.push((payload_length >> 24));
                encoded.push((payload_length >> 16));
                encoded.push((payload_length >> 8));
                encoded.push((payload_length & 0xFF));
            }

            id_length = 0;
            if (il) {
                id_length = records[i].id.length;
                encoded.push(id_length);
            }

            encoded = encoded.concat(records[i].type);

            if (il) {
                encoded = encoded.concat(records[i].id);
            }

            encoded = encoded.concat(records[i].payload);
		}

		return encoded;
	},
	encodeTnf: function(mb, me, cf, sr, il, tnf) {
		var value = tnf;

        if (mb) {
            value = (value | 0x80);
        }

        if (me) {
            value = (value | 0x40);
        }

        if (cf) {
            value = (value | 0x20);
        }

        if (sr) {
            value = (value | 0x10);
        }

        if (il) {
            value = (value | 0x8);
        }

        if (cf) {
            if (!(tnf == 0x06 && !mb && !me && !il))
            {
                throw "When cf is true, mb, me and il must be false and tnf must be 0x6";
            }
        }

        return value;
	}
};

function ndefRecord() {
	return {
		tnf: [],
		type: [],
		id: [],
		payload: []
	};
}

var self = module.exports = {
    init: function (win, fail, args) {
        if (self._initialized) {
            if (win) {
                win();
            }

            return;
        }

        self.subscribedMessageId = -1;
        self.publishedMessageId = -1;
        self.proximityDevice = Windows.Networking.Proximity.ProximityDevice.getDefault();

        if (!self.proximityDevice) {
            console.log("WARNING: proximity device is null");

            if (fail) {
                fail();
            }
		}

        self._initialized = true;
    },
    registerNdef: function (win, fail, args) {
        self.init();

        console.log("Registering for NDEF");

        try {
            self.subscribedMessageId = self.proximityDevice.subscribeForMessage("NDEF", self.messageReceivedHandler);
            win();
        } catch (e) {
            console.log(e);
            fail(e);
        } 
    },
    removeNdef: function (win, fail, args) {
        self.init();

        console.log("Removing NDEF");

        try {
            if (self.subscribedMessageId !== -1) {
                self.proximityDevice.stopSubscribingForMessage(self.subscribedMessageId);
                self.subscribedMessageId = -1;
            }

            win();
        } catch (e) {
            console.log(e);
            fail(e);
        } 
    },
    writeTag: function (win, fail, args) {
        self.init();

        console.log("Write Tag");

        try {
            var records = args[0];
            var bytes = ndefUtils.toBytes(records);

            self.stopPublishing();

            var dataWriter = new Windows.Storage.Streams.DataWriter();
            dataWriter.unicodeEncoding = Windows.Storage.Streams.UnicodeEncoding.utf16LE;
            dataWriter.writeBytes(bytes);

            self.publishedMessageId = self.proximityDevice.publishBinaryMessage("NDEF:WriteTag",
                dataWriter.detachBuffer(),
                function (sender, messageId) {
                	console.log("Successfully wrote message to the NFC tag.");
			        self.stopPublishing();
            		
            		win();
                });

        } catch (e) {
            console.log(e);
            fail(e);
        }
    },
    shareTag: function(win, fail, args) {
        self.init();

        console.log("Share Tag");

        try {
            var records = args[0];
            var bytes = ndefUtils.toBytes(records);

            self.stopPublishing();

            var dataWriter = new Windows.Storage.Streams.DataWriter();
            dataWriter.unicodeEncoding = Windows.Storage.Streams.UnicodeEncoding.utf16LE;
            dataWriter.writeBytes(bytes);

            self.publishedMessageId = self.proximityDevice.publishBinaryMessage("NDEF",
                dataWriter.detachBuffer(), 
                function (sender, messageId) {
                	console.log("Successfully shared message over peer-to-peer.");
			        self.stopPublishing();

	                win();
	            });

        } catch (e) {
            console.log(e);
            fail(e);
        } 
    },
    unshareTag: function(win, fail, args) {
        self.init();

        console.log("Unshare Tag");

        try {
            self.stopPublishing();
            win();
        } catch (e) {
            console.log(e);
            fail(e);
        }
    },
    stopPublishing: function() {
        if (self.publishedMessageId !== -1) {
            self.proximityDevice.stopPublishingMessage(self.publishedMessageId);
            self.publishedMessageId = -1;
        }
    },
    messageReceivedHandler: function (sender, message) {
        var bytes = new Uint8Array(message.data.length);
        var dataReader = Windows.Storage.Streams.DataReader.fromBuffer(message.data);
        dataReader.readBytes(bytes);
        dataReader.close();

        var json = ndefUtils.parse(bytes);

        fireNfcTagEvent("ndef", JSON.stringify(json));
    }
}; // exports
    
require("cordova/exec/proxy").add("NfcPlugin", module.exports);

