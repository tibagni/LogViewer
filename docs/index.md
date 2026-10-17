# LogViewer

<p align="center">
  <img src="images/splash.png" alt="LogViewer Splash Logo" width="300"/>
</p>

**LogViewer** is an open-source, multiplatform desktop application designed to make Android log analysis fast, intuitive, and structured. 

Whether you are debugging a live device over USB/Wi-Fi, analyzing a 100MB+ multi-file Android bugreport, or searching through gigabytes of chronological logs, LogViewer provides color-coded filtering, chronological stream merging, and dedicated diagnostic inspectors.

[![GitHub Release](https://badgen.net/github/release/tibagni/LogViewer?icon=github&label=Release)](https://github.com/tibagni/LogViewer/releases/latest)
[![Java 8+](https://img.shields.io/badge/Java-8%2B-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/tibagni/LogViewer/blob/master/LICENSE)

---

## Key Highlights

<div class="grid cards" markdown>

-   :material-palette: **Multi-Color Filter Canvas**

    ---

    Apply multiple regex and keyword filters simultaneously. Match results are highlighted in distinct, custom colors with individual line counts and dedicated navigation.

-   :material-cellphone-link: **Live Logcat Streaming**

    ---

    Connect directly to physical or emulated Android devices via ADB over USB or Wi-Fi. Choose specific buffer streams (`main`, `system`, `events`, `crash`, `radio`, `kernel`), auto-scroll, and open root terminals with a single click.

-   :material-file-clock: **Chronological Log Merging**

    ---

    Open multiple log files at once. LogViewer automatically orders every line chronologically by timestamp across `main`, `system`, `events`, and `radio` streams.

-   :material-bug-check: **Android Bug Report Intelligence**

    ---

    Inspect device properties (`getprop`), installed packages, permissions, carrier configurations, and background services in dedicated, searchable inspector tabs.

-   :material-bookmark-multiple: **"My Logs" Bookmarking**

    ---

    Save critical log lines to a persistent bookmark list for targeted deep-dive analysis without clutter.

-   :material-theme-light-dark: **Modern Light & Dark Modes**

    ---

    Seamless FlatLaf themes, customizable typography, and full keyboard navigation.

</div>

---

## Quick Interface Overview

[![LogViewer Main Interface](images/main_view.png)](images/main_view.png)

LogViewer divides its primary interface into two synchronized views:
1. **All Logs View**: Shows every single log line across all loaded files or live streams.
2. **Filtered Logs View**: Displays only the log lines that match your active filters, color-coded for instant visual triage.

Clicking on any line in the Filtered Logs view immediately scrolls to and highlights that exact line in the All Logs view, preserving full chronological context.

---

## Getting Started

### 1. Download & Launch
LogViewer runs on macOS, Linux, and Windows. It requires Java 8 or higher.

1. Download the latest standalone JAR (`LogViewer-all.jar`) from the **[Releases Page](https://github.com/tibagni/LogViewer/releases/latest)**.
2. Run it by double-clicking the JAR file or executing in your terminal:
   ```bash
   java -jar LogViewer-*-all.jar
   ```

### 2. Loading Logs
You can load logs into LogViewer through several convenient methods:

* **Drag and Drop**: Simply drag `.txt`, `.log`, or `.zip` bugreport files from your file manager directly into the LogViewer window.
* **File Menu**: Go to `Logs > Open Logs` (or press ++cmd+o++ / ++ctrl+o++).
* **Command Line**: Pass log paths when launching from terminal:
  ```bash
  java -jar LogViewer-*-all.jar bugreport.txt main.log radio.log
  ```

---

## Next Steps

* **[Viewing & Analyzing Logs](analyzing-logs.md)**: Explore the synchronized dual-pane view, in-table find, and context menus.
* **[Filters & Groups](filters-panel.md)**: Organize queries, share filter sets, and color-code components.
* **["My Logs" Bookmarks](my-logs.md)**: Pin critical log milestones into an incident timeline.
* **[Live Logcat Streaming](live-logs.md)**: Connect devices and stream live logcat buffers.
* **[Bug Report Analysis](bugreport.md)**: Explore packages, properties, and carrier configurations.
* **[Keyboard Shortcuts](shortcuts.md)**: Master high-speed log navigation.
