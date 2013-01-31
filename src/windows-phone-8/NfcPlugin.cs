using System.Runtime.Serialization;
using Windows.Networking.Proximity;
using WPCordovaClassLib.Cordova;
using WPCordovaClassLib.Cordova.Commands;
using WPCordovaClassLib.Cordova.JSON;

using System.Runtime.InteropServices.WindowsRuntime;
using Windows.Storage.Streams;
using System.Diagnostics;
using System.IO;

using System.Collections.Generic;
using System.Text;
using System.Collections;

using ChariotSolutions.NFC.NDEF;

// 
// http://www.nfc-forum.org/specs/spec_list/
// http://msdn.microsoft.com/en-us/library/windows/apps/br241250.aspx
// 

namespace Cordova.Extension.Commands
{
    class NfcPlugin : BaseCommand
    {
        private ProximityDevice proximityDevice;
        private long subscribedMessageId = -1;
        private long publishedMessageId = -1;

        public void init(string args)
        {
            // not used for WP8
        }

        // no args
        public void registerNdef(string args)
        {
            Debug.WriteLine("Registering for NDEF");
            proximityDevice = ProximityDevice.GetDefault();
            subscribedMessageId = proximityDevice.SubscribeForMessage("NDEF", MessageReceivedHandler);
            DispatchCommandResult();
        }

        // no args
        public void removeNdef(string args)
        {
            Debug.WriteLine("Removing NDEF");
            if (subscribedMessageId != -1)
            {
                proximityDevice.StopSubscribingForMessage(subscribedMessageId);
                subscribedMessageId = -1;
            }
            DispatchCommandResult();
        }

        // args[0] is a NdefMessage, which is a JSON array of NdefRecords
        public void writeTag(string args)
        {
            Debug.WriteLine("Write Tag");
            publish("NDEF:WriteTag", args);
            DispatchCommandResult();

            // TODO is there a callback after a successful write so we can stop publishing?
        }

        // args[0] is NdefMessage
        public void shareTag(string args)
        {
            Debug.WriteLine("Share Tag");
            publish("NDEF", args);
            DispatchCommandResult();
        }

        // no args
        public void unshare(string args)
        {
            stopPublishing();
            DispatchCommandResult();
        }

        private void publish(string type, string args)
        {
            string ndefMessage = JsonHelper.Deserialize<string[]>(args)[0];
            NdefRecord[] records = JsonHelper.Deserialize<NdefRecord[]>(ndefMessage);
            byte[] data = NdefMessage.toBytes(records);
            stopPublishing();
            publishedMessageId = proximityDevice.PublishBinaryMessage(type, data.AsBuffer());
        }

        private void stopPublishing()
        {
            if (publishedMessageId != -1)
            {
                proximityDevice.StopPublishingMessage(publishedMessageId);
                publishedMessageId = -1;
            }
        }

        private void MessageReceivedHandler(ProximityDevice sender, ProximityMessage message)
        {

            var bytes = message.Data.ToArray();
            NdefMessage ndefMessage = NdefMessage.parse(bytes);            

            string tag = JsonHelper.Serialize(ndefMessage);

            string[] argsForJavaScriptEvent = new string[] {
                "ndef",
                tag
           };

            // calling a global js method to fire an nfc event
            ScriptCallback script = new ScriptCallback("fireNfcTagEvent", argsForJavaScriptEvent);
            this.InvokeCustomScript(script, false);
        }

    }
}
