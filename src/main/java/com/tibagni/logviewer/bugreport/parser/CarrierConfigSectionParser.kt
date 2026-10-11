package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.CarrierConfigSection

class CarrierConfigSectionParser : BugReportSectionParser {
  companion object {
    private val LOADING_LOGS_REGEX = "^[\\d:A-Z.-]+ - .*".toRegex()
    private val CC_SECTION_START_REGEX = "\\n+DUMP OF SERVICE carrier_config:\\n+".toRegex()
    private val CC_SECTION_END_REGEX = "\\n--------- .*carrier_config.*\\n".toRegex()
  }

  override val name = "Carrier Config"

  override fun parse(bugreportPath: String, bugReportText: String): BugReportSection? {
    val ccStartMatch = CC_SECTION_START_REGEX.find(bugReportText) ?: return null
    val ccEndMatch = CC_SECTION_END_REGEX.find(bugReportText, ccStartMatch.range.last) ?: return null

    val carrierConfigSection = bugReportText.substring(ccStartMatch.range.first, ccEndMatch.range.last)
    val lines = carrierConfigSection.lines()

    val ccData = HashMap<String, HashMap<String, String>>()
    var currentPhoneId = -1
    var currentConfigSource = ""
    val loadingLogs = StringBuilder()

    val phoneIdPrefix = "Phone Id = "
    val ccSourcePrefix = "    "
    val ccSourceSuffix = " : "
    val ccValuePrefix = "            "
    for (line in lines) {
      if (line.startsWith(phoneIdPrefix)) {
        currentPhoneId = line.substring(phoneIdPrefix.length).toInt()
      }
      if (line.startsWith(ccSourcePrefix) and line.endsWith(ccSourceSuffix)) {
        currentConfigSource = line.split(ccSourceSuffix)[0].trim()
      }
      if (line.startsWith(ccValuePrefix)) {
        val configParts = line.split("=")
        if (configParts.size == 2) {
          val key = currentConfigSource + "_$currentPhoneId"
          addConfig(ccData, key, configParts[0].trim(), configParts[1].trim())
        }
      }
      if (line.matches(LOADING_LOGS_REGEX)) {
        loadingLogs.append(line + "\n")
      }
    }

    return CarrierConfigSection(ccData, loadingLogs.toString())
  }

  private fun addConfig(
    configMap: HashMap<String, HashMap<String, String>>,
    key: String,
    configName: String,
    configValue: String
  ) {
    var existingConfigs = configMap[key]
    if (existingConfigs == null) {
      existingConfigs = HashMap()
      configMap[key] = existingConfigs
    }
    existingConfigs[configName] = configValue
  }
}