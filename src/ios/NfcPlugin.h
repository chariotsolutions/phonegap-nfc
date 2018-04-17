//
//  NfcPlugin.h
//  PhoneGap NFC - Cordova Plugin
//
//  (c) 2107 Don Coleman

#ifndef NfcPlugin_h
#define NfcPlugin_h

#import <Cordova/CDV.h>
#import <CoreNFC/CoreNFC.h>
#import <WebKit/WebKit.h>

@interface NfcPlugin : CDVPlugin <NFCNDEFReaderSessionDelegate> {
}

// iOS Specific API
- (void)beginSession:(CDVInvokedUrlCommand *)command;
- (void)invalidateSession:(CDVInvokedUrlCommand *)command;

// Standard PhoneGap NFC API
- (void)registerNdef:(CDVInvokedUrlCommand *)command;
- (void)removeNdef:(CDVInvokedUrlCommand *)command;
- (void)enabled:(CDVInvokedUrlCommand *)command;

@end

#endif
