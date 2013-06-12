Install cordova-cli if necessary

	$ npm install cordova -g

Ensure your version of cordova-cli is new enough

	$ cordova -v
	2.8.14

Create a project

	$ cordova create foo com.example.foo Foo

Add android

	$ cd foo	
	$ cordova platform add android
	
Install the NFC plugin

	$ cordova plugin add ~/phonegap-nfc
	
Edit `index.js` to scan tags

	TODO link to the other document

Run the code and scan a tag	

	$ cordova run