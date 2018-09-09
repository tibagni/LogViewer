# LogViewer
LogViewer is a simple tool to help you analyze Android logs.
It allows you to analyze multiple log files at once and create (and apply) different filters on this set of log files showing a single filtered output differentiating each filter by a different color (defined by you)

Check out more details on https://tibagni.github.io/LogViewer/

### Increasing app version
To increase app version, change it on _app.properties_
* _app.properties_ - This file is located on _src/main/resources/properties/app.properties_ and there is a symbolik link for it on root folder to make it easier to access

### Making a stand alone bundle
Run _makeBundle.sh_ to create a standalone java Bundle (.dmg). It is possible to create bundles for other platforms changing the _-native_ param passed to _javapackager_ inside the script. The script will read _app.properties_ to set the bundle version.