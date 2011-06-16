var NdefReaderPlugin = function() {
};

NdefReaderPlugin.prototype.register = function(mime_type, win, fail) {
  PhoneGap.exec(win, fail, "NdefReaderPlugin", "register", [mime_type]);
};

NdefReaderPlugin.bytesToString = function (bytes) {
  var bytesAsString = "";
  for (var i = 0; i < bytes.length; i++) {
    bytesAsString += String.fromCharCode(bytes[i]);
  }
  return bytesAsString;
};

NdefReaderPlugin.fireNfc = function (tagData) {
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
  PhoneGap.addPlugin('NdefReaderPlugin', new NdefReaderPlugin());

  // Register the native class of plugin with PhoneGap
  navigator.app.addService("NdefReaderPlugin", "com.chariotsolutions.nfc.plugin.NdefReaderPlugin"); 
});
