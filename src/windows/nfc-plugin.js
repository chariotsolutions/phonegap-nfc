/* globals:  export, module, console, document, ndef, Windows, Uint8Array */

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
    init: function (success, failure, args) {
        if (self._initialized) {
            if (success) {
                success();
            }

            return;
        }

        self.subscribedMessageId = -1;
        self.publishedMessageId = -1;
        self.proximityDevice = Windows.Networking.Proximity.ProximityDevice.getDefault();

        if (!self.proximityDevice) {
            console.log("WARNING: proximity device is null");

            if (failure) {
                failure();
            }
        }

        // TODO this never calls success on the first time
        self._initialized = true;
    },
    registerNdef: function (success, failure, args) {
        self.init();

        console.log("Registering for NDEF");

        try {
            self.subscribedMessageId = self.proximityDevice.subscribeForMessage("NDEF", self.messageReceivedHandler);
            success();
        } catch (e) {
            console.log(e);
            failure(e);
        }
    },
    removeNdef: function (success, failure, args) {
        self.init();

        console.log("Removing NDEF");

        try {
            if (self.subscribedMessageId !== -1) {
                self.proximityDevice.stopSubscribingForMessage(self.subscribedMessageId);
                self.subscribedMessageId = -1;
            }

            success();
        } catch (e) {
            console.log(e);
            failure(e);
        }
    },
    writeTag: function (success, failure, args) {
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

                      success();
                }
            );

        } catch (e) {
            console.log(e);
            failure(e);
        }
    },
    shareTag: function(success, failure, args) {
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

                    success();
                });

        } catch (e) {
            console.log(e);
            failure(e);
        }
    },
    unshareTag: function(success, failure, args) {
        self.init();

        console.log("Unshare Tag");

        try {
            self.stopPublishing();
            success();
        } catch (e) {
            console.log(e);
            failure(e);
        }
    },
    showSettings: function(success, failure, args) {

        // WARNING: this isn't documented, so it might break
        var nfcSettingsUri = "ms-settings-proximity:";
        var uri = new Windows.Foundation.Uri(nfcSettingsUri);

        Windows.System.Launcher.launchUriAsync(uri).then(
            function (settingsAppeared) {
                if (settingsAppeared) {
                    success();
                } else {
                    failure();
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
        console.log(JSON.stringify(ndefMessage));

        // on windows, tag only contains the ndef message
        // other platforms have tag data
        var tag = {
            ndefMessage: ndefMessage
        };

        // fire JavaScript event with NDEF data
        var e = document.createEvent('Events');
        e.initEvent("ndef", true, false);
        e.tag = tag;
        document.dispatchEvent(e);
    }
}; // exports

require("cordova/exec/proxy").add("NfcPlugin", module.exports);
