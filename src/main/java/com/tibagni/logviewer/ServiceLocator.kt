package com.tibagni.logviewer

import com.tibagni.logviewer.bugreport.BugReportRepository
import com.tibagni.logviewer.bugreport.BugReportRepositoryImpl
import com.tibagni.logviewer.bugreport.parser.*
import com.tibagni.logviewer.preferences.LogViewerPreferences
import com.tibagni.logviewer.preferences.LogViewerPreferencesImpl
import com.tibagni.logviewer.theme.LogViewerThemeManager

object ServiceLocator {
  val themeManager: LogViewerThemeManager = LogViewerThemeManager
  val logViewerPrefs: LogViewerPreferences = LogViewerPreferencesImpl
  val logsRepository: LogsRepository by lazy { LogsRepositoryImpl() }
  val filtersRepository: FiltersRepository by lazy { FiltersRepositoryImpl() }
  val bugReportRepository: BugReportRepository by lazy {
    BugReportRepositoryImpl(
      BugReportParserImpl(
        listOf(
          PlainTextContentParser(),
          PropertiesSectionParser(),
          ApplicationPackagesSectionParser(),
          SystemHiddenPackagesSectionParser(),
          CarrierConfigSectionParser()
        )
      )
    )
  }
}