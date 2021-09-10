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
  }

  override fun parse(bugreportPath: String, bugReportText: String): BugReportSection? {
    val packagesMatch = "\\n$matchString\\n".toRegex().find(bugReportText) ?: return null
    val nextBlankLineMatch = "\\n\\n".toRegex().find(bugReportText, packagesMatch.range.last) ?: return null

    val packages = bugReportText.stringView(packagesMatch.range.last, nextBlankLineMatch.range.first)
    val seq = "Package \\[.+]".toRegex().findAll(packages)

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
      val packageName = "Package \\[(.+)]".toRegex().find(it)?.groupValues?.getOrNull(1) ?: NOT_FOUND
      val versionCode = "versionCode=(\\d+) ".toRegex().find(it)?.groupValues?.getOrNull(1) ?: NOT_FOUND
      val versionName = "versionName=([\\d\\\\.\\w-]+)".toRegex().find(it)?.groupValues?.getOrNull(1) ?: NOT_FOUND
      val dataDir = "dataDir=([\\d\\w/\\\\.]+)".toRegex().find(it)?.groupValues?.getOrNull(1) ?: NOT_FOUND
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