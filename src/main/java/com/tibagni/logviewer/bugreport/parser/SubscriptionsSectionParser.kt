package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.SubscriptionInfo
import com.tibagni.logviewer.bugreport.section.SubscriptionsSection
import com.tibagni.logviewer.getOrNull
import com.tibagni.logviewer.stringView
import com.tibagni.logviewer.util.StringView

class SubscriptionsSectionParser : BugReportSectionParser {
  override val name = "Subscriptions"

  override fun parse(bugreportPath: String, bugReportText: String): BugReportSection? {
    val isubSectionStart = "\\nDUMP OF SERVICE isub:\\n"
    val isubSectionEnd = "\\n--------- .*isub.*\\n"
    val isubStartMatch = isubSectionStart.toRegex().find(bugReportText) ?: return null
    val isubEndMatch = isubSectionEnd.toRegex().find(bugReportText, isubStartMatch.range.last) ?: return null
    val isubSection = bugReportText.stringView(isubStartMatch.range.first, isubEndMatch.range.last)

    val properties = parseControllerProperties(isubSection)
    val activeSubs = parseActiveSubs(isubSection)
    val allSubs = parseAllSubs(isubSection)
    val logs = parseSubsLogs(isubSection)

    return SubscriptionsSection(properties, activeSubs, allSubs, logs)
  }

  private fun parseControllerProperties(isubSection: StringView): Map<String, String> {
    val controllerSectionStart = "\\nSubscriptionController:\\n"
    val controllerSectionEnd = "\\n\\+{32}\\n"
    val controllerStartMatch = controllerSectionStart.toRegex().find(isubSection) ?: return mapOf()
    val controllerEndMatch = controllerSectionEnd.toRegex().find(isubSection, controllerStartMatch.range.last) ?: return mapOf()
    val controllerSection = isubSection.subStringView(controllerStartMatch.range.first, controllerEndMatch.range.last)

    val controllerLines = controllerSection.lines().filter { it.contains("=") }
    return controllerLines
      .filter { it.contains("=") }
      .associate { it.split("=")[0].trim() to it.split("=")[1] }

  }

  private fun parseActiveSubs(isubSection: StringView) : List<SubscriptionInfo> {
    return parseSubsList("\\n ?ActiveSubInfoList:\\n", isubSection)
  }

  private fun parseAllSubs(isubSection: StringView) : List<SubscriptionInfo> {
    return parseSubsList("\\n ?AllSubInfoList:\\n", isubSection)
  }

  private fun parseSubsList(startRegex: String, isubSection: StringView) : List<SubscriptionInfo> {
    val subsSectionEnd = "\\n\\+{32}\\n"
    val subsStartMatch = startRegex.toRegex().find(isubSection) ?: return listOf()
    val subsEndMatch = subsSectionEnd.toRegex().find(isubSection, subsStartMatch.range.last) ?: return listOf()
    val subsSection = isubSection.subStringView(subsStartMatch.range.last, subsEndMatch.range.first)
    return subsSection.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { parseSubscriptionInfo(it) }
  }

  private fun parseSubsLogs(isubSection: StringView): String {
    val logsSectionStart = "\\d{4}-\\d{2}-\\d+T?\\d+:\\d+:\\d+.\\d+ - "
    val logsSectionEnd = "\\n\\+{32}\\n"
    val logsSectionStartMatch = logsSectionStart.toRegex().find(isubSection) ?: return ""
    val logsSectionEndMatch = logsSectionEnd.toRegex().find(isubSection, logsSectionStartMatch.range.last) ?: return ""
    val logsSection = isubSection.subStringView(logsSectionStartMatch.range.first, logsSectionEndMatch.range.first)
    return logsSection.toString()
  }

  /* VisibleForTesting */
  fun parseSubscriptionInfo(subscriptionText: String) : SubscriptionInfo {
    val subId = findInt(subscriptionText, "id=(\\d+)", -1)
    val mcc = findInt(subscriptionText, "mcc=(\\d+)", -1)
    val mnc = findInt(subscriptionText, "mnc=(\\d+)", -1)
    val carrierId = findInt(subscriptionText, "carrierId=(\\d+)", -1)
    val displayName = "displayName=(.+?) \\w+=".toRegex().find(subscriptionText)?.groupValues?.getOrNull(1) ?: ""
    val carrierName = "carrierName=(.+?) \\w+=".toRegex().find(subscriptionText)?.groupValues?.getOrNull(1) ?: ""
    val nameSource = findInt(subscriptionText, "nameSource=(\\d+)", -1)

    return SubscriptionInfo(subId, mcc, mnc, carrierId, displayName, carrierName, nameSource, subscriptionText)
  }

  private fun findInt(text: String, pattern: String, defaultValue: Int): Int {
    return pattern.toRegex().find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: defaultValue
  }
}