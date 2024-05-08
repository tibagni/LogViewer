package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.SubscriptionInfo
import com.tibagni.logviewer.bugreport.section.SubscriptionsSection
import com.tibagni.logviewer.getOrNull
import com.tibagni.logviewer.stringView
import com.tibagni.logviewer.util.StringView

class SubscriptionsSectionParser : BugReportSectionParser {
  override val name = "Subscriptions"

  companion object {
    private const val MAX_PROPERTY_LINE_LENGTH = 80
  }

  override fun parse(bugreportPath: String, bugReportText: String): BugReportSection? {
    val isubSectionStart = "\\nDUMP OF SERVICE isub:\\n"
    val isubSectionEnd = "\\n--------- .*isub.*\\n"
    val isubStartMatch = isubSectionStart.toRegex().find(bugReportText) ?: return null
    val isubEndMatch = isubSectionEnd.toRegex().find(bugReportText, isubStartMatch.range.last) ?: return null
    val isubSection = bugReportText.stringView(isubStartMatch.range.first + 1, isubEndMatch.range.last)

    val properties = parseControllerProperties(isubSection)
    val activeSubs = parseActiveSubs(isubSection)
    val allSubs = parseAllSubs(isubSection)

    return SubscriptionsSection(properties, activeSubs, allSubs, isubSection.toString())
  }

  private fun parseControllerProperties(isubSection: StringView): Map<String, String> {
    return isubSection.lines()
      .filter { it.contains("=") }
      .filter { it.length < MAX_PROPERTY_LINE_LENGTH } // Discard big lines as they are probably not properties
      .associate { it.split("=")[0].trim() to it.split("=")[1] }
  }

  private fun parseActiveSubs(isubSection: StringView): List<SubscriptionInfo> {
    return parseSubsList("\\nActive subscriptions:\\n", isubSection)
  }

  private fun parseAllSubs(isubSection: StringView): List<SubscriptionInfo> {
    return parseSubsList("\\nAll subscriptions:\\n", isubSection)
  }

  private fun parseSubsList(startRegex: String, isubSection: StringView): List<SubscriptionInfo> {
    val subsSectionEnd = "\\n\\n"
    val subsStartMatch = startRegex.toRegex().find(isubSection) ?: return listOf()
    val subsEndMatch = subsSectionEnd.toRegex().find(isubSection, subsStartMatch.range.last) ?: return listOf()
    val subsSection = isubSection.subStringView(subsStartMatch.range.last, subsEndMatch.range.first)
    return subsSection.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { parseSubscriptionInfo(it) }
  }

  /* VisibleForTesting */
  fun parseSubscriptionInfo(subscriptionText: String): SubscriptionInfo {
    val subId = findInt(subscriptionText, "id=(\\d+)", -1)
    val mcc = findInt(subscriptionText, "mcc=(\\d+)", -1)
    val mnc = findInt(subscriptionText, "mnc=(\\d+)", -1)
    val carrierId = findInt(subscriptionText, "carrierId=(\\d+)", -1)
    val number = "number=([^ ]*) \\w+=".toRegex().find(subscriptionText)?.groupValues?.getOrNull(1) ?: ""
    val imsNumber = "numberFromIms=([^ ]*) \\w+=".toRegex().find(subscriptionText)?.groupValues?.getOrNull(1) ?: ""
    val carrierNumber =
      "numberFromCarrier=([^ ]*) \\w+=".toRegex().find(subscriptionText)?.groupValues?.getOrNull(1) ?: ""
    val displayName = "displayName=(.+?) \\w+=".toRegex().find(subscriptionText)?.groupValues?.getOrNull(1) ?: ""
    val carrierName = "carrierName=(.+?) \\w+=".toRegex().find(subscriptionText)?.groupValues?.getOrNull(1) ?: ""
    val nameSource = "displayNameSource=(\\w+)".toRegex().find(subscriptionText)?.groupValues?.getOrNull(1) ?: ""

    return SubscriptionInfo(
      subId,
      mcc,
      mnc,
      carrierId,
      number,
      imsNumber,
      carrierNumber,
      displayName,
      carrierName,
      nameSource,
      subscriptionText
    )
  }

  private fun findInt(text: String, pattern: String, defaultValue: Int): Int {
    return pattern.toRegex().find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: defaultValue
  }
}