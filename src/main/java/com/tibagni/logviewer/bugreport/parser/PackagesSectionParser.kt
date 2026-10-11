package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.content.AppPackage
import com.tibagni.logviewer.bugreport.section.BugReportSection
import com.tibagni.logviewer.bugreport.section.PackagesSection
import com.tibagni.logviewer.stringView
import com.tibagni.logviewer.util.StringView

abstract class PackagesSectionParser(private val sectionName: String, private val matchString: String) :
  BugReportSectionParser {
  companion object {
    private const val NOT_FOUND = "Not Found"
    private val PACKAGES_SPLIT_REGEX = "Package \\[.+]".toRegex()
    private val PACKAGE_NAME_REGEX = "Package \\[(.+)]".toRegex()
    private val VERSION_CODE_REGEX = "versionCode=(\\d+) ".toRegex()
    private val VERSION_NAME_REGEX = "versionName=([\\d\\.\\w-]+)".toRegex()
    private val DATA_DIR_REGEX = "dataDir=([\\d\\w/\\.]+)".toRegex()
  }

  override fun parse(bugreportPath: String, bugReportText: String): BugReportSection? {
    val packagesMatch = "\\n$matchString\\n".toRegex().find(bugReportText) ?: return null
    val nextBlankLineMatch = "\\n\\n".toRegex().find(bugReportText, packagesMatch.range.last) ?: return null

    val packages = bugReportText.stringView(packagesMatch.range.last, nextBlankLineMatch.range.first)
    val seq = PACKAGES_SPLIT_REGEX.findAll(packages)

    val rawPackages = ArrayList<StringView>()
    var prev: MatchResult? = null
    seq.forEach {
      run {
        val prevMatchResult = prev
        if (prevMatchResult != null) {
          rawPackages.add(packages.subStringView(prevMatchResult.range.first, it.range.first))
        }
        prev = it
      }
    }
    // Now get the last package
    prev?.let {
      rawPackages.add(packages.subStringView(it.range.first))
    }

    val contentPackages = rawPackages.map {
      val packageName = PACKAGE_NAME_REGEX.find(it)?.groupValues?.getOrNull(1) ?: NOT_FOUND
      val versionCode = VERSION_CODE_REGEX.find(it)?.groupValues?.getOrNull(1) ?: NOT_FOUND
      val versionName = VERSION_NAME_REGEX.find(it)?.groupValues?.getOrNull(1) ?: NOT_FOUND
      val dataDir = DATA_DIR_REGEX.find(it)?.groupValues?.getOrNull(1) ?: NOT_FOUND
      AppPackage(
        packageName,
        versionCode,
        versionName,
        dataDir,
        it // Use the StringView. We don't want to copy the whole package text until it is necessary. So don't do it now
      )
    }

    return PackagesSection(sectionName, contentPackages)
  }
}