package com.tibagni.logviewer.bugreport

import com.tibagni.logviewer.bugreport.content.AppPackage

/**
 * Represents a BugReport section containing information about application packages
 */
class PackagesSection(sectionName: String, val packages: List<AppPackage>) : BugReportSection(sectionName)