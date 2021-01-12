package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.SectionNames

class ApplicationPackagesSectionParser : PackagesSectionParser(
  SectionNames.APPLICATION_PKG,
  "Packages:"
) {
  override val name: String
    get() = "Application packages"
}