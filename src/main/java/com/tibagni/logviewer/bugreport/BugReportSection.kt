package com.tibagni.logviewer.bugreport

/**
 * An abstract representation of a section in the bug report.
 * A section is parsed from BugReport text and contain a collection of useful information about the device
 * e.g: A PackagesSection will contain information related to the devices packages that is extracted from the BugReport
 */
abstract class BugReportSection(val sectionName: String)