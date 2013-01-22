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

        [DataContract]
        public class NdefMessage
        {
            [DataMember]
            public NdefRecord[] records { get; set; }
        }

        [DataContract]
        public class NdefRecord
        {
            [DataMember]
            public byte tnf { get; set; }
            [DataMember]
            public byte[] type { get; set; }
            [DataMember]
            public byte[] id { get; set; }
            [DataMember]
            public byte[] payload { get; set; }
        }

        public void init(string args)
        {
            // not used for WP8
        }

        // no args
        public void registerNdef(string args)
        {
            Debug.WriteLine("Registering for NDEF");
            // Initialize NFC (should really be done elsewhere)
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
            byte[] data = toBytes(records);
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

        // TODO move to Ndef Class 
        // note if c, mb and mb must be false
        // il must be false
        // tnf must be 0x6
        private byte encodeTnf(bool mb, bool me, bool cf, bool sr, bool il, byte tnf)
        {
            byte value = tnf;

            if (mb)
            {
                value = (byte)(value | 0x80);
            }

            if (me)
            {
                value = (byte)(value | 0x40);
            }

            if (cf)
            {
                value = (byte)(value | 0x20);
            }

            if (sr)
            {
                value = (byte)(value | 0x10);
            }

            if (il)
            {
                value = (byte)(value | 0x8);
            }

            return value;
        }

        // todo move to Ndef Class
        private byte[] toBytes(NdefRecord[] records)
        {
            MemoryStream encoded = new MemoryStream();

            for (int i = 0; i < records.Length; i++)
            {

                bool mb = (i == 0);
                bool me = (i == (records.Length - 1));
                bool cf = false; // TODO
                bool sr = true; // TODO
                bool il = (records[i].id.Length > 0);

                byte tnf_byte = encodeTnf(mb, me, cf, sr, il, records[i].tnf);
                encoded.WriteByte(tnf_byte);

                int type_length = records[i].type.Length;
                encoded.WriteByte((byte)type_length);

                int payload_length;
                if (sr)
                {
                    payload_length = records[i].payload.Length;
                    encoded.WriteByte((byte)payload_length);
                }
                else
                {
                    throw new IOException("SR is not implemented");
                }

                int id_length = 0;
                if (il)
                {
                    id_length = records[i].id.Length;
                    encoded.WriteByte((byte)id_length);
                }

                encoded.Write(records[i].type, 0, type_length);
                if (il)
                {
                    encoded.Write(records[i].id, 0, id_length);
                }

                encoded.Write(records[i].payload, 0, payload_length);
            }
            return encoded.ToArray();
        }

        // todo move to NdefClass
        private MemoryStream toMemoryStream(NdefRecord[] records)
        {
            MemoryStream encoded = new MemoryStream();

            for (int i = 0; i < records.Length; i++)
            {

                bool mb = (i == 0);
                bool me = (i == (records.Length - 1));
                bool cf = false; // TODO
                bool sr = true; // TODO
                bool il = (records[i].id.Length > 0);

                byte tnf_byte = encodeTnf(mb, me, cf, sr, il, records[i].tnf);
                encoded.WriteByte(tnf_byte);

                int type_length = records[i].type.Length;
                encoded.WriteByte((byte)type_length);

                int payload_length;
                if (sr)
                {
                    payload_length = records[i].payload.Length;
                    encoded.WriteByte((byte)payload_length);
                }
                else
                {
                    throw new IOException("SR is not implemented");
                }

                int id_length = 0;
                if (il)
                {
                    id_length = records[i].id.Length;
                    encoded.WriteByte((byte)id_length);
                }

                encoded.Write(records[i].type, 0, type_length);
                if (il)
                {
                    encoded.Write(records[i].id, 0, id_length);
                }

                encoded.Write(records[i].payload, 0, payload_length);
            }
            return encoded;
        }

        private void MessageReceivedHandler(ProximityDevice sender, ProximityMessage message)
        {
   
            List<NdefRecord> records = new List<NdefRecord>();

            var bytes = message.Data.ToArray();
            int index = 0;

            while (index <= bytes.Length)
            {
                byte tnf_byte = bytes[index];
                bool mb = (tnf_byte & 0x80) != 0;
                bool me = (tnf_byte & 0x40) != 0;
                bool cf = (tnf_byte & 0x20) != 0;
                bool sr = (tnf_byte & 0x10) != 0;
                bool il = (tnf_byte & 0x8) != 0;
                int tnf = tnf_byte & 0x7;

                // WARNING does not handle sr = false
                // cf = true or il = true

                index++;
                int typeLength = bytes[index];
                int idLength = 0;
                int payloadLength = 0;

                index++;
                payloadLength = bytes[index];

                if (il)
                {
                    index++;
                    idLength = bytes[index];
                }

                index++;
                IBuffer type = bytes.AsBuffer(index, typeLength);
                index += typeLength;

                //IBuffer id = bytes.AsBuffer(index, idLength);
                //index += idLength;

                IBuffer payload = bytes.AsBuffer(index, payloadLength);
                index += payloadLength;

                NdefRecord record = new NdefRecord();
                record.tnf = (byte)tnf;
                record.type = type.ToArray(); // TODO fix TNF_EMPTY fails here
                record.id = new byte[0];
                record.payload = payload.ToArray();

                records.Add(record);

                if (me) break;  // last message
            }

            // Build a JSON representation
            string json = JsonHelper.Serialize(records);

            Debug.WriteLine("json");
            Debug.WriteLine(json);

            // TODO add tag meta data here 
            // id, type, tech, capacity, isReadOnly, isLockable
            string tag = "{ \"ndefMessage\": " + json + " }"; // TODO use JsonHelper

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
