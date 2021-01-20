package com.tibagni.logviewer.bugreport.section

/**
 * This is a sections with the raw bugreport "section".
 */
data class PlainTextSection(val bugreportText: String): BugReportSection(SectionNames.PLAIN_TEXT)