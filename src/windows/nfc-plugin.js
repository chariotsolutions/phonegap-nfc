"use strict";

var ndefUtils = {
	parse: function (bytes) {
		var records = [],
			index = 0,
			tnf_byte, mb, me, cf, sr, il, tnf, typeLength, idLength, payloadLength;

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
            type = bytes.slice(index, typeLength);
            index += typeLength;

            id = bytes.slice(index, idLength);
            index += idLength;

            payload = bytes.slice(index, payloadLength);
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
            me = (i == (records.Length - 1));
            cf = false; // TODO
            sr = (records[i].payload.Length < 0xFF);
            il = (records[i].id.Length > 0);

            tnf_byte = this.encodeTnf(mb, me, cf, sr, il, records[i].tnf);
            encoded.push(tnf_byte);

            type_length = records[i].type.Length;
            encoded.push(type_length);

            payload_length = records[i].payload.Length;
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
                id_length = records[i].id.Length;
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


function messageReceivedHandler(device, message) {
	var bytes = message.Data;
}

module.exports = {
	init: function (args) {
		this.subscribedMessageId = -1;
		this.publishedMessageId = -1;
		this.proximityDevice = Windows.Networking.Proximity.ProximityDevice.getDefault();

		if (!this.proximityDevice) {
			console.log("WARNING: proximity device is null");
		}
	},
	registerNdef: function(args) {
		this.subscribedMessageId = this.proximityDevice.subscribeForMessage("NDEF", messageReceivedHandler)
	},
	removeNdef: function(args) {

	}
}; // exports

require("cordova/exec/proxy").add("NfcPlugin", module.exports);