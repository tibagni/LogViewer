# LogViewer
LogViewer is a simple tool to help you analyze Android logs.
It allows you to analyze multiple log files at once and create (and apply) different filters on this set of log files showing a single filtered output differentiating each filter by a different color (defined by you)

Check out more details on https://tibagni.github.io/LogViewer/

### Increasing app version
To increase app version, change it on _app.properties_
* _app.properties_ - This file is located on _src/main/resources/properties/app.properties_ and there is a symbolik link for it on root folder to make it easier to access

### Making a stand alone bundle
Run _makeBundle.sh_ to create a standalone java Bundle (.dmg). It is possible to create bundles for other platforms changing the _-native_ param passed to _javapackager_ inside the script. The script will read _app.properties_ to set the bundle version.

# License
```
MIT License

Copyright (c) 2018 Tiago Bagni

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
