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
    private const val LOCAL_LOG_PATTERN = """^\s*[\d:A-Z.-]+ - .*"""
    private const val LOGCAT_PATTERN = """^\s*\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+.*"""

    private val LOCAL_LOG_REGEX = LOCAL_LOG_PATTERN.toRegex()
    private val LOGCAT_REGEX = LOGCAT_PATTERN.toRegex()

    private const val ACTIVE_SUBS_START_REGEX = "\\nActive subscriptions:\\n"
    private const val ALL_SUBS_START_REGEX = "\\nAll subscriptions:\\n"

    private const val SUB_ID_PATTERN = "id=(\\d+)"
    private const val MCC_PATTERN = "mcc=(\\d+)"
    private const val MNC_PATTERN = "mnc=(\\d+)"
    private const val CARRIER_ID_PATTERN = "carrierId=(\\d+)"
    private const val IS_EMBEDDED_PATTERN = "isEmbedded=(\\d+)"
    private const val PROFILE_CLASS_PATTERN = "profileClass=(-?\\d+)"

    private const val PROPERTY_VALUE_SUFFIX = """(.*?)(?: \w+=|[\]}]\s*$)"""
    private const val NUMBER_PATTERN = "number=$PROPERTY_VALUE_SUFFIX"
    private const val IMS_NUMBER_PATTERN = "numberFromIms=$PROPERTY_VALUE_SUFFIX"
    private const val CARRIER_NUMBER_PATTERN = "numberFromCarrier=$PROPERTY_VALUE_SUFFIX"
    private const val DISPLAY_NAME_PATTERN = "displayName=$PROPERTY_VALUE_SUFFIX"
    private const val CARRIER_NAME_PATTERN = "carrierName=$PROPERTY_VALUE_SUFFIX"
    private const val GID1_PATTERN = "gid1=$PROPERTY_VALUE_SUFFIX"
    private const val NAME_SOURCE_PATTERN = "displayNameSource=(\\w+)"

    private val SUB_ID_REGEX = SUB_ID_PATTERN.toRegex()
    private val MCC_REGEX = MCC_PATTERN.toRegex()
    private val MNC_REGEX = MNC_PATTERN.toRegex()
    private val CARRIER_ID_REGEX = CARRIER_ID_PATTERN.toRegex()
    private val IS_EMBEDDED_REGEX = IS_EMBEDDED_PATTERN.toRegex()
    private val PROFILE_CLASS_REGEX = PROFILE_CLASS_PATTERN.toRegex()
    private val NUMBER_REGEX = NUMBER_PATTERN.toRegex()
    private val IMS_NUMBER_REGEX = IMS_NUMBER_PATTERN.toRegex()
    private val CARRIER_NUMBER_REGEX = CARRIER_NUMBER_PATTERN.toRegex()
    private val DISPLAY_NAME_REGEX = DISPLAY_NAME_PATTERN.toRegex()
    private val CARRIER_NAME_REGEX = CARRIER_NAME_PATTERN.toRegex()
    private val GID1_REGEX = GID1_PATTERN.toRegex()
    private val NAME_SOURCE_REGEX = NAME_SOURCE_PATTERN.toRegex()
  }

  override fun parse(bugreportPath: String, bugReportText: String): BugReportSection? {
    val isubSectionStart = "\\nDUMP OF SERVICE isub:\\n"
    val isubSectionEnd = "\\n--------- .*isub.*\\n"
    val isubStartMatch = isubSectionStart.toRegex().find(bugReportText) ?: return null
    val isubEndMatch = isubSectionEnd.toRegex().find(
      bugReportText,
      isubStartMatch.range.last
    ) ?: return null
    val isubSection = bugReportText.stringView(
      isubStartMatch.range.first + 1,
      isubEndMatch.range.last
    )

    val properties = parseControllerProperties(isubSection)
    val activeSubs = parseActiveSubs(isubSection)
    val allSubs = parseAllSubs(isubSection)

    return SubscriptionsSection(properties, activeSubs, allSubs, isubSection.toString())
  }

  private fun parseControllerProperties(isubSection: StringView): Map<String, String> {
    return isubSection.lines()
      .filter { it.contains("=") }
      .filter { it.length < MAX_PROPERTY_LINE_LENGTH }
      .filterNot { isLogLine(it) }
      .filterNot { isSubscriptionLine(it) }
      .associate {
        val parts = it.split("=", limit = 2)
        parts[0].trim() to parts[1]
      }
  }

  private fun isLogLine(line: CharSequence): Boolean {
    return LOCAL_LOG_REGEX.matches(line) || LOGCAT_REGEX.matches(line)
  }

  private fun isSubscriptionLine(line: CharSequence): Boolean {
    val trimmed = line.trim()
    return (trimmed.startsWith("[SubscriptionInfo") || trimmed.startsWith("{")) &&
        trimmed.contains("id=")
  }

  private fun parseActiveSubs(isubSection: StringView): List<SubscriptionInfo> {
    return parseSubsList(ACTIVE_SUBS_START_REGEX, isubSection)
  }

  private fun parseAllSubs(isubSection: StringView): List<SubscriptionInfo> {
    return parseSubsList(ALL_SUBS_START_REGEX, isubSection)
  }

  private fun parseSubsList(startRegex: String, isubSection: StringView): List<SubscriptionInfo> {
    val subsStartMatch = startRegex.toRegex().find(isubSection) ?: return emptyList()
    val startIndex = subsStartMatch.range.last + 1
    if (startIndex >= isubSection.length) return emptyList()

    val remainder = isubSection.subStringView(startIndex)
    return remainder.lineSequence()
      .map { it.trim() }
      .takeWhile { trimmed ->
        trimmed.isNotEmpty() &&
            (trimmed.startsWith("[SubscriptionInfo") || trimmed.startsWith("{")) &&
            trimmed.contains("id=")
      }
      .map { parseSubscriptionInfo(it.toString()) }
      .toList()
  }

  /* VisibleForTesting */
  fun parseSubscriptionInfo(subscriptionText: String): SubscriptionInfo {
    val subId = findInt(subscriptionText, SUB_ID_REGEX, -1)
    val mcc = findInt(subscriptionText, MCC_REGEX, -1)
    val mnc = findInt(subscriptionText, MNC_REGEX, -1)
    val carrierId = findInt(subscriptionText, CARRIER_ID_REGEX, -1)
    val number = findString(subscriptionText, NUMBER_REGEX)
    val imsNumber = findString(subscriptionText, IMS_NUMBER_REGEX)
    val carrierNumber = findString(subscriptionText, CARRIER_NUMBER_REGEX)
    val displayName = findString(subscriptionText, DISPLAY_NAME_REGEX)
    val carrierName = findString(subscriptionText, CARRIER_NAME_REGEX)
    val nameSource = findString(subscriptionText, NAME_SOURCE_REGEX)
    val gid1 = findString(subscriptionText, GID1_REGEX)
    val isEmbedded = findInt(subscriptionText, IS_EMBEDDED_REGEX, -1)
    val profileClass = findInt(subscriptionText, PROFILE_CLASS_REGEX, -1)

    return SubscriptionInfo(
      id = subId,
      mcc = mcc,
      mnc = mnc,
      carrierId = carrierId,
      number = number,
      imsNumber = imsNumber,
      carrierNumber = carrierNumber,
      displayName = displayName,
      carrierName = carrierName,
      nameSource = nameSource,
      fullText = subscriptionText,
      gid1 = gid1,
      isEmbedded = isEmbedded,
      profileClass = profileClass
    )
  }

  private fun findInt(text: String, regex: Regex, defaultValue: Int): Int {
    return regex.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: defaultValue
  }

  private fun findString(text: String, regex: Regex): String {
    return regex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""
  }
}
