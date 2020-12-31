package com.tibagni.logviewer.bugreport.parser

import com.tibagni.logviewer.bugreport.section.SectionNames

class SystemHiddenPackagesSectionParser : PackagesSectionParser(
  SectionNames.SYSTEM_HIDDEN_PKG,
  "Hidden system packages:"
)