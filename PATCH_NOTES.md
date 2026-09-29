# LogViewer Patch Notes — Modernization, Session Recovery & Filter Deduplication

## Overview

This release introduces major improvements to **LogViewer**:
1. **Modernized FlatLaf UI & UX Polish**: Sleek, clean visual styling across Light and Dark themes.
2. **Session Persistence & Crash Recovery**: Continuous auto-save, crash detection, manual session export/import, and full **My Logs** state recovery.
3. **Smart Filter Deduplication & Similarity Guard**: Normalized Levenshtein similarity (90%+) detection, interactive cross-file duplicate cleanup, and duplicate prevention when creating or editing filters.
4. **Modern Java 21 / Gradle 8 Compatibility**: Upgraded build toolchain and standalone executable fat JAR packaging.

---

## 1. UI & UX Modernization

- **Refined FlatLaf Styling**:
  - Soft rounded corners on buttons, text fields, and combo boxes (`Component.arc = 8`, `Button.arc = 8`).
  - Streamlined modern scrollbars with subtle rounded thumb edges and clean gutter styling (`ScrollBar.thumbArc = 6`, `ScrollBar.showButtons = false`).
  - Polished tabbed panes with clean separator lines.
  - Subtle table gridlines (`Table.showHorizontalLines = true`, `Table.showVerticalLines = false`) and comfortable row spacing for long log-reading sessions.
- **Dialog Enhancements**:
  - Increased padding and consistent spacing across all modal dialogs (*Preferences*, *Visible Logs Configuration*, *Regex Editor*).
  - Modernized filter group containers and button layouts in the Filters pane.
  - Slim, non-intrusive split-pane dividers.

---

## 2. Session Persistence, Crash Recovery & Workspace Management

### Automatic Session Persistence & Crash Recovery
- **Continuous Workspace Auto-Save**:
  - Automatically captures open log file paths, loaded filter files, checked/applied filter selections per group, split pane divider positions, window placement, and **My Logs** entries during runtime with debounced background writes.
- **Crash & Abnormal Exit Detection**:
  - Distinguishes between clean shutdowns and unexpected terminations (system kill, crash, power loss).
- **Startup Restore Prompt**:
  - When starting LogViewer with a saved session, a clean prompt displays:
    - Last active date and time.
    - Number of log files and filter groups.
    - Warning banner if the previous session exited unexpectedly.
    - Action buttons: **"Restore Session"** and **"Discard Session"**.
- **User Preferences**:
  - Configurable startup restore behavior in Preferences (`PROMPT`, `AUTO`, or `DISABLE`).

### Full 'My Logs' Persistence
- Saved sessions now preserve all log lines bookmarked in **My Logs**, saving their line index and content.
- Restores entries accurately even if logs are slightly offset or refreshed.

### Manual Session Controls (File Menu)
- **Save Session (`Ctrl+Shift+S`)**: Instantly saves the active workspace state to the default session with a confirmation toast.
- **Save Session As...**: Export current workspace session to any custom `.json` file.
- **Restore Session...**: Open the restore modal on demand to reload the default session state.
- **Open Session From File...**: Browse and restore any previously saved `.json` session file.

---

## 3. Intelligent Filter Deduplication & Similarity Detection

### Similarity Engine (90%+ Match)
- Implemented `FilterSimilarityUtils` powered by normalized Levenshtein edit distance.
- Identifies exact duplicate patterns as well as near-duplicate patterns with **90% or greater text similarity** (accounting for minor typos, character changes, or flag variations).

### Workspace Deduplication Action
- Available under **Filters → Find & Clean Duplicate Filters...**:
  - Scans all opened filter files/groups using disjoint-set clustering.
  - Displays discovered clusters in an interactive resolution dialog (`FilterDeduplicationDialog`).
  - **Single-File Duplicates**: Easily consolidates duplicates within the same group by retaining one and removing redundancies.
  - **Cross-File Resolution**: When duplicate or similar filters exist in multiple files, allows the user to choose which file/group keeps the filter rule, automatically removing it from other files.
  - **Cross-File Cleaning Toggle**: Includes a *"Clean duplicates across different filter files"* checkbox. Unchecking this option automatically unmarks and hides cross-file duplicates so you can focus solely on cleaning duplicates within individual files.

### Filter Creation & Edit Guard
- When creating a new filter or editing an existing one in `EditFilterDialog`, LogViewer scans active filters for duplicates or >= 90% similar patterns.
- Displays a warning showing the existing filter pattern, its containing file/group, and provides **"Add Anyway"** and **"Cancel"** options.
- Safely ignores self-comparison when editing existing filters.

---

## 4. Collapsible Filters Search Bar

- **Quick Filter Search (`Ctrl+Shift+F`)**:
  - Added a collapsible search bar positioned above the filters list.
  - Activated by clicking **"Search"** in the filters toolbar, selecting **Filters → Search Filters...**, or pressing **`Ctrl+Shift+F`** (or `Cmd+Shift+F` on macOS).
- **Live List Filtering**:
  - Dynamically filters the visible items in each open group in real time as you type (case-insensitive search on pattern text and filter names).
  - Groups with matching filters automatically expand and display their match count (`X of Y`).
  - Groups with zero matching filters automatically collapse to keep your workspace clean.
- **Instant Reset**:
  - Pressing `Escape` or clicking `Clear` / `Close` restores the full filter list, group expansion states, and original ordering.

---

## 5. Build & Toolchain Upgrades

- **Java 21 & Gradle 8 Compatibility**: Upgraded Gradle wrapper to 8.7 and Kotlin plugin to 1.9.23.
- **Automated Verification**: Complete automated test suite with **357 passing unit tests** covering similarity calculation, deduplication resolution, session serialization, search filtering, and UI logic.
- **Standalone Fat JAR**: Rebuilt `build/libs/LogViewer-2.7-all.jar` containing bundled FlatLaf and dependencies.

---

## How to Run

To run the standalone application:

```bash
java -jar build/libs/LogViewer-2.7-all.jar
```
