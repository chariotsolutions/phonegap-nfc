exports.defineAutoTests = function () {
    
    describe('NFC object', function () {
        it("nfc should exist", function () {
            expect(nfc).toBeDefined();
        });

        it("should contain a addNdefListener function", function () {
            expect(typeof nfc.addNdefListener).toBeDefined();
            expect(typeof nfc.addNdefListener).toBe("function");
        });
    });
    
    describe('UTF-8 Encoding and Decoding', function() {
        // https://github.com/don/ndef-js/blob/master/test/util.js

        it('should encode UTF-8', function() {

            var bytes=[ 0x54, 0x65, 0x73, 0x74, 0x73, 0xd7, 0x90, 0xc2, 0xa2];

            var encoded = util.stringToBytes("Testsא¢");
            expect(encoded).toEqual(bytes);

        });

        it('should decode UTF-8', function() {

            var bytes=[ 0x54, 0x65, 0x73, 0x74, 0x73, 0xd7, 0x90, 0xc2, 0xa2];

            var decoded = util.bytesToString(bytes);
            expect(decoded).toEqual("Testsא¢");

        });

        it('should encode and decode Russian', function() {

            // http://www.columbia.edu/~kermit/utf8.html
            var russian = "На берегу пустынных волн";
            var russianBytes = [ 0xD0, 0x9D, 0xD0, 0xB0, 0x20, 0xD0, 0xB1, 0xD0, 0xB5, 0xD1, 0x80, 0xD0, 0xB5,
                0xD0, 0xB3, 0xD1, 0x83, 0x20, 0xD0, 0xBF, 0xD1, 0x83, 0xD1, 0x81, 0xD1, 0x82, 0xD1, 0x8B, 0xD0,
                0xBD, 0xD0, 0xBD, 0xD1, 0x8B, 0xD1, 0x85, 0x20, 0xD0, 0xB2, 0xD0, 0xBE, 0xD0, 0xBB, 0xD0, 0xBD ];

            var encoded = util.stringToBytes(russian);
            expect(encoded).toEqual(russianBytes);
            

            var decoded = util.bytesToString(russianBytes);
            expect(decoded).toEqual(russian);
            
        });

        it('should round trip encode and decode UTF-8', function() {

            // http://www.columbia.edu/~kermit/utf8.html
            var chinese = "我能吞下玻璃而不伤身体。";
            var roundTrip = util.bytesToString(util.stringToBytes(chinese));
            expect(roundTrip).toEqual(chinese);

            var korean = "나는 유리를 먹을 수 있어요. 그래도 아프지 않아요";
            roundTrip = util.bytesToString(util.stringToBytes(korean));            
            expect(roundTrip).toEqual(korean);            

            var url = "http://example.com/with-utf8-✓";
            roundTrip = util.bytesToString(util.stringToBytes(url));                        
            expect(roundTrip).toEqual(url);

        });
    });
    
};

exports.defineManualTests = function (contentEl, createActionButton) {
    // TODO
};