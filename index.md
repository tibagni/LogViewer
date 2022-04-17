---
layout: default
title: Home
nav_order: 1
description: "An easy-to-use tool that helps you analyze Android logs."
permalink: /
---

# What is LogViewer.
{: .fs-9 }

LogViewer is an easy-to-use tool intended to help with the analysis of Android logs.
{: .fs-6 .fw-300 }

With Log Viewer, you can open multiple log files at once and it will present you a single merged output containing all the logs already ordered by timestamp. This is very useful when you have different files for ‘main’, ‘system’ and ‘radio’ logs, for example. Or simply if your logs are broken down in multiple files.

It also allows you to apply different filters on the set of logs, displaying a single filtered output, differentiating each filter by color. Filters can be as simple as a single word, or as complex as you want using regex.

[Get started now](#getting-started){: .btn .btn-primary .fs-5 .mb-4 .mb-md-0 .mr-2 } [View it on GitHub](https://github.com/tibagni/LogViewer){: .btn .fs-5 .mb-4 .mb-md-0 }

---

## Getting started

### Downloading
LogViewer is multiplatform and you ou can download the latest version [here](https://github.com/tibagni/LogViewer/releases/latest) or choose an older version in [releases](https://github.com/tibagni/LogViewer/releases)

### Opening logs
LogViewer will automatically merge and sort all the log files you open and present it to you in a chronological way. The Logs View is divided in 2: the “All Logs” View and the “Filtered Logs” View. The first one will contain all the logs from all the opened files, and the secon one will contain only the log lines that matches one or more of the applied filters identified by the filter color. This is a great way to only read what matters (Filtered Logs View) without losing the context of what happened (All Logs View)

#### Menu
Choose ‘Logs > Open Logs’ from the toolbar menu, and select all the logs you want to open

#### Drag n' Drop
Alternatively, you can simply select all the log files from your file system and drag and drop them to the ‘All Logs’ panel

#### Command Line
If you are starting LogViewer from the command line, you can also pass the log files names via command line argument like “java -jar LogViewer.jar log1.txt log2.txt bugreport.txt”

### Creating and applying filters
With LogViewer it is possible to create one or more filters and organize them into groups. Each group can be saved in a different file, making it possible to create different sets of filters and use the ones are more appropriate each time.

Filters can be expressed in the form of regular expressions so it is easy to perform complex and specific searches on the logs. They are identified by a name (Which can be the query itself if readable) and are distinctable by color, so it is really easy to identify which log lines are a result of which applied filter.

To apply/unapply one or more filters, just click the check box next to it (Or select all filters you want to apply/unapply and hit space bar)

## About the project

LogViewer is &copy; 2017-{{ "now" | date: "%Y" }} by Tiago Bagni.