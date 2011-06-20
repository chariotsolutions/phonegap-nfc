var NdefPlugin = function() {
};

NdefPlugin.prototype.register = function(mime_type, win, fail) {
    console.log('registering');
  PhoneGap.exec(win, fail, "NdefPlugin", "register", [mime_type]);
};

NdefPlugin.prototype.registerForWrite = function(win, fail) {
    PhoneGap.exec(win, fail, "NdefPlugin", "registerForWrite", []);
};

// TODO send bytes to NdefPlugin
// TODO allow writing multiple records
NdefPlugin.prototype.writeTag =  function (mime_type, tag_data, win, fail) {
  PhoneGap.exec(win, fail, "NdefPlugin", "writeTag", [mime_type, tag_data]);
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
