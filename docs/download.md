# Download & Installation

LogViewer is distributed as a self-contained, multiplatform executable JAR. It runs on **macOS**, **Linux**, and **Windows**.

---

## Prerequisites

LogViewer requires a Java Runtime Environment (JRE) version **8 or higher**:

```bash
java -version
```

If you do not have Java installed, you can download OpenJDK from [Adoptium (Temurin)](https://adoptium.net/) or install it via your OS package manager:

=== "macOS"
    ```bash
    brew install openjdk
    ```

=== "Ubuntu / Debian"
    ```bash
    sudo apt update && sudo apt install -y default-jre
    ```

=== "Windows"
    Download and run the installer from [Adoptium](https://adoptium.net/).

---

## Standalone Executable JAR

No compilation is necessary. Simply download the latest release:

* 👉 **[Download Latest Release (`LogViewer-*-all.jar`)](https://github.com/tibagni/LogViewer/releases/latest)**
* 📦 **[View All Previous Releases](https://github.com/tibagni/LogViewer/releases)**

### Launching the Application

Double-click the downloaded `.jar` file, or run it from your command line:

```bash
java -jar LogViewer-*-all.jar
```

You can also pass log files directly when launching:

```bash
java -jar LogViewer-*-all.jar bugreport.txt main.log
```

---

## Building from Source

If you want to contribute or build the latest development snapshot:

1. Clone the repository:
   ```bash
   git clone https://github.com/tibagni/LogViewer.git
   cd LogViewer
   ```

2. Build the fat JAR using Gradle:
   ```bash
   ./gradlew shadowJar
   ```

3. The generated executable will be located in `build/libs/LogViewer-<version>-all.jar`.
