//
//  NfcPlugin.m
//  PhoneGap NFC - Cordova Plugin
//
//  (c) 2107 Don Coleman

#import "NfcPlugin.h"

@interface NfcPlugin() {
    NSString* ndefStartSessionCallbackId;
}
@property (strong, nonatomic) NFCNDEFReaderSession *nfcSession;
@end

@implementation NfcPlugin

- (void)pluginInitialize {

    NSLog(@"PhoneGap NFC - Cordova Plugin");
    NSLog(@"(c)2017 Don Coleman");

    [super pluginInitialize];
    
    // TODO fail quickly if not supported
    if (![NFCNDEFReaderSession readingAvailable]) {
        NSLog(@"NFC Support is NOT available");
    }
}

#pragma mark -= Cordova Plugin Methods

// Unfortunately iOS users need to start a session to read tags
- (void)beginSession:(CDVInvokedUrlCommand*)command {
    NSLog(@"beginSession");

    _nfcSession = [[NFCNDEFReaderSession new]initWithDelegate:self queue:nil invalidateAfterFirstRead:TRUE];
    ndefStartSessionCallbackId = [command.callbackId copy];
    [_nfcSession beginSession];
}

- (void)invalidateSession:(CDVInvokedUrlCommand*)command {
    NSLog(@"invalidateSession");
    if (_nfcSession) {
        [_nfcSession invalidateSession];
    }
    // Always return OK. Alternately could send status from the NFCNDEFReaderSessionDelegate
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

// Nothing happens here, the event listener is registered in JavaScript
- (void)registerNdef:(CDVInvokedUrlCommand *)command {
    NSLog(@"registerNdef");
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

// Nothing happens here, the event listener is removed in JavaScript
- (void)removeNdef:(CDVInvokedUrlCommand *)command {
    NSLog(@"removeNdef");
    CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)enabled:(CDVInvokedUrlCommand *)command {
    NSLog(@"enabled");
    CDVPluginResult *pluginResult;
    if ([NFCNDEFReaderSession readingAvailable]) {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    } else {
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"NO_NFC"];
    }
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

#pragma mark - NFCNDEFReaderSessionDelegate

- (void) readerSession:(NFCNDEFReaderSession *)session didDetectNDEFs:(NSArray<NFCNDEFMessage *> *)messages {
    NSLog(@"NFCNDEFReaderSession didDetectNDEFs");
    
    for (NFCNDEFMessage *message in messages) {
        [self fireNdefEvent: message];
    }
}

- (void) readerSession:(NFCNDEFReaderSession *)session didInvalidateWithError:(NSError *)error {
    NSLog(@"didInvalidateWithError %@ %@", error.localizedDescription, error.localizedFailureReason);
    if (ndefStartSessionCallbackId) {
        NSString* errorMessage = [NSString stringWithFormat:@"error: %@", error.localizedDescription];
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorMessage];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:ndefStartSessionCallbackId];
    }
}

- (void) readerSessionDidBecomeActive:(nonnull NFCReaderSession *)session {
    NSLog(@"readerSessionDidBecomeActive");
    if (ndefStartSessionCallbackId) {
        CDVPluginResult *pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        //[pluginResult setKeepCallback:[NSNumber numberWithBool:YES]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:ndefStartSessionCallbackId];
        ndefStartSessionCallbackId = NULL;
    }
}

#pragma mark - internal implementation

// Create a JSON description of the NFC NDEF tag and call a JavaScript function fireNfcTagEvent.
// The event handler registered by addNdefListener will handle the JavaScript event fired by fireNfcTagEvent().
// This is a bit convoluted and based on how PhoneGap 0.9 worked. A new implementation would send the data
// in a success callback.
-(void) fireNdefEvent:(NFCNDEFMessage *) ndefMessage {
    NSString *ndefMessageAsJSONString = [self ndefMessagetoJSONString:ndefMessage];
    NSLog(@"%@", ndefMessageAsJSONString);

    // construct string to call JavaScript function fireNfcTagEvent(eventType, tagAsJson);
    NSString *function = [NSString stringWithFormat:@"fireNfcTagEvent('ndef', '%@')", ndefMessageAsJSONString];
    dispatch_async(dispatch_get_main_queue(), ^{
        if ([[self webView] isKindOfClass:WKWebView.class])
          [(WKWebView*)[self webView] evaluateJavaScript:function completionHandler:^(id result, NSError *error) {}];
        else
          [(UIWebView*)[self webView] stringByEvaluatingJavaScriptFromString: function];
    });
}

-(NSString *) ndefMessagetoJSONString:(NFCNDEFMessage *) ndefMessage {
    
    NSMutableArray *array = [NSMutableArray new];
    for (NFCNDEFPayload *record in ndefMessage.records){
        NSDictionary* recordDictionary = [self ndefRecordToNSDictionary:record];
        [array addObject:recordDictionary];
    }
    
    // The JavaScript tag object expects a key with ndefMessage
    NSMutableDictionary *wrapper = [NSMutableDictionary new];
    [wrapper setObject:array forKey:@"ndefMessage"];
    return dictionaryAsJSONString(wrapper);
}

-(NSDictionary *) ndefRecordToNSDictionary:(NFCNDEFPayload *) ndefRecord {
    NSMutableDictionary *dict = [NSMutableDictionary new];
    dict[@"tnf"] = [NSNumber numberWithInt:(int)ndefRecord.typeNameFormat];
    dict[@"type"] = uint8ArrayFromNSData(ndefRecord.type);
    dict[@"id"] = uint8ArrayFromNSData(ndefRecord.identifier);
    dict[@"payload"] = uint8ArrayFromNSData(ndefRecord.payload);
    NSDictionary *copy = [dict copy];
    return copy;
}

// returns an NSArray of uint8_t representing the bytes in the NSData object.
NSArray *uint8ArrayFromNSData(NSData *data) {
    const void *bytes = [data bytes];
    NSMutableArray *array = [NSMutableArray array];
    for (NSUInteger i = 0; i < [data length]; i += sizeof(uint8_t)) {
        uint8_t elem = OSReadLittleInt(bytes, i);
        [array addObject:[NSNumber numberWithInt:elem]];
    }
    return array;
}

NSString* dictionaryAsJSONString(NSDictionary *dict) {
    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:dict options:0 error:&error];
    NSString *jsonString;
    if (! jsonData) {
        jsonString = [NSString stringWithFormat:@"Error creating JSON for NDEF Message: %@", error];
        NSLog(@"%@", jsonString);
    } else {
        jsonString = [[NSString alloc] initWithData:jsonData encoding:NSUTF8StringEncoding];
    }
    return jsonString;
}

@end
