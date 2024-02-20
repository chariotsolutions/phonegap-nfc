//
//  NfcPlugin.h
//  PhoneGap NFC - Cordova Plugin
//
//  (c) 2107-2020 Don Coleman

#ifndef NfcPlugin_h
#define NfcPlugin_h

#import <Cordova/CDV.h>
#import <CoreNFC/CoreNFC.h>
#import <WebKit/WebKit.h>

#import "AppDelegate.h"

@interface NfcPlugin : CDVPlugin <NFCNDEFReaderSessionDelegate, NFCTagReaderSessionDelegate> {
}

// iOS Specific API

// Cordova lifecycle events
- (void) onPause;
- (void) onResume;

// deprecated use scanNdef or scanTag
- (void)beginSession:(CDVInvokedUrlCommand *)command;
// deprecated use stopScan
- (void)invalidateSession:(CDVInvokedUrlCommand *)command;

// Handle launch data
- (void)parseLaunchIntent:(CDVInvokedUrlCommand *)command;
- (void)messageReceived:(NFCNDEFMessage *)message;

// Added iOS 13
- (void)scanNdef:(CDVInvokedUrlCommand *)command;
- (void)scanTag:(CDVInvokedUrlCommand *)command;
- (void)cancelScan:(CDVInvokedUrlCommand *)command;

// Standard PhoneGap NFC API
- (void)registerNdef:(CDVInvokedUrlCommand *)command;
- (void)removeNdef:(CDVInvokedUrlCommand *)command;
- (void)enabled:(CDVInvokedUrlCommand *)command;
- (void)writeTag:(CDVInvokedUrlCommand *)command;

// Internal implementation
- (void)channel:(CDVInvokedUrlCommand *)command;

@end

@interface AppDelegate (PhonegapNfc)
    - (BOOL)application:(UIApplication *)application swizzledContinueUserActivity:(NSUserActivity *)userActivity restorationHandler:(void (^)(NSArray *))restorationHandler ;
@end

#endif
