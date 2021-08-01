package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.CarrierConfigSection

class CarrierConfigSectionParser : BugReportSectionParser {
  override val name = "Carrier Config"

  override fun parse(bugreportPath: String, bugReportText: String): BugReportSection? {
    val ccSectionStart = "\\nDUMP OF SERVICE carrier_config:\\n"
    val ccSectionEnd = "\\n--------- .*carrier_config.*\\n"

    val ccStartMatch = ccSectionStart.toRegex().find(bugReportText) ?: return null
    val ccEndMatch = ccSectionEnd.toRegex().find(bugReportText, ccStartMatch.range.last) ?: return null

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
      if (line.matches("^[\\d:A-Z.-]+ - .*".toRegex())) {
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