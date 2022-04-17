---
layout: page
title: Limiting the Time Window
nav_order: 6
permalink: /limiting-time-window/
---

# Specifying a Window of Visible Logs
Sometimes the available logs are too long and we want to limit our analysis to a specific time window.
This is possible on LogViewer by ignoring logs.

When you right click on any lig line, you will have options to ignore logs before or after that line
![Menu](../images/visible-logs-menu.png)

To clean up the selection, go to ‘Logs > Visible Logs’ and clear the selection (start or end point)
![Dialog](../images/visible-logs-dialog.png)

# Going Directly to a specific timestamp
You can also go directly to a log line at a specifi timestamp (or the nearest logline if the timestamp is not found). For that, go to ‘Logs > Go to Timestamp’ and specify the exact time. You can also use the Ctrl+G shortcut (If a log line is selected, the timestamp of the selected line will be used to pre-fill the go to dialog input field)
![Go To Timestamp](../images/go-to-dialog.png)