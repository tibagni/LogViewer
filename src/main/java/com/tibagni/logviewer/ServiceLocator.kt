package com.tibagni.logviewer

import com.tibagni.logviewer.bugreport.parser.ApplicationPackagesSectionParser
import com.tibagni.logviewer.bugreport.parser.BugReportSectionParser
import com.tibagni.logviewer.bugreport.parser.SystemHiddenPackagesSectionParser
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.preferences.LogViewerPreferencesImpl

object ServiceLocator {
  val logViewerPrefs: LogViewerPreferences = LogViewerPreferencesImpl
  val logsRepository: LogsRepository = LogsRepositoryImpl()
  val filtersRepository: FiltersRepository = FiltersRepositoryImpl()

  val bugreportSectionParsers: List<BugReportSectionParser> = listOf(
    ApplicationPackagesSectionParser(),
    SystemHiddenPackagesSectionParser()
  )
}