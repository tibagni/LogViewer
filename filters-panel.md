---
layout: page
title: Filters Panel
nav_order: 2
permalink: /filters-panel/
---

# Filter Groups
You can easily organize all your filters by categorizing them in groups.

- To add a new group, simply click the “New group” button above the filters list
- To add a new filter, simply click the “+” button on the group in which that filter should belong to

# Applying Filters
In order to apply/unapply one or more filters, just click the check box next to it (Or select all filters you want to apply/unapply and hit space bar). You can also apply/unapply all filters by selecting the group's checkbox

# Applied Filters information
You will notice, when applying one or more filters, that some information is presented next to it

![Filter Details](../images/filter-details.png)

On the above image, the applied filter is ‘ClassLoader’, and you can see that some aditional information is shown: ClassLoader {905} ❙ ← (,) / → (.)_
- {11} - Means there are 905 occurrences of this filter on the st of logs
- ← (,) - Means you can use , to navigate backward through the occurrences of this filter
- → (.) - Means you can use . to navigate forward through the occurrences of this filter

# Navigating through filtered logs
You can click ‘<’ and ‘>’ buttons (or use , and . keyboard shortcuts) to navigate through all the occurrences of the filtered logs matching the selected filter.

# Manage Filters
- Simply Drag n’ Drop the filters (on the same group) to re-order them
- Double click a filter or right click > Edit to edit an existing filter
- Select the filter(s) you want to delete and press Delete or right click > Delete

# Create Filter from Log
A nice shortcut to creating a new filter is by selecting one log line (In the All Logs View) and right click > Create Filter from this line…. This will automatically take you to the Regex Editor prepopulating it with the contents of that particular log line so you can edit and create a filter based on it.