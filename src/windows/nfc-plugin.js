
"use strict";

var ndefUtils = {
    // convert Uint8Array to []
    toArray: function (bytes) {
        var output = [], i = 0;
        for (; i < bytes.length; i += 1) {
            output.push(bytes[i]);
        }

        return output;
    }
};

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
            var bytes = ndef.encodeMessage(records);

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
                }
            );

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
            var bytes = ndef.encodeMessage(records);

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
    showSettings: function(win, fail, args) {

        // WARNING: this isn't documented, so it might break
        var nfcSettingsUri = "ms-settings-proximity:";
        var uri = new Windows.Foundation.Uri(nfcSettingsUri);

        Windows.System.Launcher.launchUriAsync(uri).then(
            function (success) {
                if (success) {
                    win();
                } else {
                    fail();
                }
            }
        );
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

        var byteArray = ndefUtils.toArray(bytes);
        var ndefMessage = ndef.decodeMessage(byteArray);
        // on windows, tag only contains the ndef message
        // other platforms have tag data
        var tag = {
            ndefMessage: ndefMessage
        };

        // TODO fire event from here
        fireNfcTagEvent("ndef", JSON.stringify(tag));
    }
}; // exports

require("cordova/exec/proxy").add("NfcPlugin", module.exports);
