package com.tibagni.logviewer.bugreport.section.ui

import com.tibagni.logviewer.bugreport.section.PlainTextSection

class PlainTextBugreportPanel(private val plainTextSection: PlainTextSection) :
  SectionPanel(plainTextSection.sectionName) {

  init {
    buildUi()
  }

  private fun buildUi() {
    // TODO figure out a way to show large text. Maybe a customized component with lazy loading is needed
  }
}