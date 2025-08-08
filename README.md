[![CI](https://github.com/tibagni/LogViewer/actions/workflows/main.yml/badge.svg)](https://github.com/tibagni/LogViewer/actions/workflows/main.yml)
[![Coverage Status](https://coveralls.io/repos/github/tibagni/LogViewer/badge.svg?branch=master)](https://coveralls.io/github/tibagni/LogViewer?branch=master)
# LogViewer 💻📝🔍
LogViewer is a simple tool to help you analyze Android logs. It allows you to analyze multiple log files at once and create (and apply) different filters on this set of log files showing a single filtered output differentiating each filter by a different color (defined by you).

Check out more details on [https://tibagni.github.io/LogViewer/](https://tibagni.github.io/LogViewer/).

## 💻 Features
* 🔍 Analyze multiple log files at once
* 🎨 Create and apply different filters on the set of log files, showing a single filtered output differentiating each filter by a different color (defined by you)
* 🤖 Read bugreport information such as system properties, application packages, hidden system packages, carrier config, subscriptions etc.
* 📝 Note the important log entries in my log view
* ⏱️ Limit all logs by ignoring everything before or after a selected timestamp
* 🛠️ Adjust the log viewer preferences to match your workflow

## 📚 Requirements
* Java 8 or later
* Gradle (optional)

## 🏠 Getting Started
To get started with LogViewer, follow these steps:
1. Clone this repository and open it in IntelliJ IDEA or any other Java development environment of your choice.
2. Install the required dependencies by running `./gradlew install` on the command line.
3. Build the application by running `./gradlew build` on the command line.
4. Run the application by running `./gradlew shadowJar` on the command line, and running the jar file on `build/libs` (Or just run directly from IntelliJ).
5. Use the UI to analyze log files, appling filters and more...

## 📦 Creating a JAR File
To create a JAR file for the project, you can use the `shadowJar` task provided by Gradle. Run `./gradlew shadowJar` on the command line to create a JAR file in the `build/libs` directory. The JAR file will be named `LogViewer-{version}-all.jar`, where `{version}` is the current version of the project.

## 📈 Increasing App Version
To increase the app version, change it on `_app.properties_`. This file is located in `src/main/resources/properties/app.properties` and there is a symbolic link for it on the root folder to make it easier to access. You can use any text editor to edit this file, or you can use IntelliJ IDEA to open it and edit it directly from within the IDE.

## 🧪 Running Tests
You can run tests on IntelliJ by right-clicking on the `test` folder (it is under `src/test`) and selecting "Run 'All Tests'". Alternatively, you can run `./gradlew test` on the command line to run all tests.

## 📄 License
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
