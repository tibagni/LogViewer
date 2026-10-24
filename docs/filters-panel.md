# Filters & Groups

Filters are the core of LogViewer. They allow you to sift through millions of log lines and isolate exactly what you need without losing the broader context of system events.

---

## Organizing Filters into Groups

You can organize filters into logical groups (for example: *Telephony*, *Bluetooth*, *Audio*, *System Crash*, or *App Startup*).

* **New Group**: Click the **New group** button above the filter panel.
* **Add Filter**: Click the **+** button on any group header to add a new filter to that specific group.
* **Reorder**: Drag and drop filters within a group to adjust their evaluation and display order.
* **Save & Share**: Groups can be saved to standalone filter files (`.filter` / `.reg`) and shared with teammates or committed alongside project repositories.

---

## Applying Filters

* **Toggle Single Filter**: Click the checkbox next to the filter name, or select the filter and press ++space++.
* **Toggle Entire Group**: Click the checkbox next to the group header to enable or disable all filters in that group simultaneously.

---

## Global Filter Search

When managing dozens of specialized filters across multiple groups, you can quickly locate and manage any filter using global keyboard search:

[![Global Filter Search](images/filter-search.png)](images/filter-search.png)

* Press ++cmd+shift+f++ (macOS) or ++ctrl+shift+f++ (Linux & Windows).
* Type keywords to filter the list instantly across all groups.
* Use the keyboard directly in the results table:
    * ++space++: Toggle the selected filter on or off.
    * ++e++: Edit the selected filter.
    * ++delete++: Delete the selected filter.
    * ++enter++: Jump directly to the filter in its group.

### Quick Inline Search
When a filter group list is focused in the Filters panel, press ++slash++ (`/`) and start typing to highlight matching filters inline within that group. Press ++escape++ to clear the inline search.

---

## Filter Statistics & Occurrence Navigation

When a filter is applied, LogViewer displays real-time match counts in a pill badge on the right side of each filter row and provides occurrence navigation controls:

[![Filter Details](images/filter-details.png)](images/filter-details.png)

In the example above (`Performance.filter (4)`):
* **Group Count `(4)`**: Shows the total number of filters inside the group header.
* **Match Count Badge (`90`, `1`, `2`, `0`)**: Displays the total number of matching log lines found in the active log set for each applied filter.
* **Previous Match (`◀` / ++comma++)**: Select an applied filter and press ++comma++ (or click `◀` on the group header) to jump backward to the previous matching occurrence in the Filtered Logs view.
* **Next Match (`▶` / ++period++)**: Select an applied filter and press ++period++ (or click `▶` on the group header) to jump forward to the next matching occurrence in the Filtered Logs view.

---

## Creating Filters from a Log Line

A rapid way to create a filter is directly from the log table:
1. Select any log line in the All Logs view.
2. Right-click and choose **Create Filter from this line...**.
3. LogViewer will open the Filter Editor pre-populated with the exact text or tags from that log line, allowing you to fine-tune your query.

---

## Bookmarking Lines

Need to save specific lines across different filters? Check out the dedicated **["My Logs" Bookmarks](my-logs.md)** guide.
