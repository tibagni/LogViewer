package com.tibagni.logviewer.view

import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun JTextField.whenTextChanges(callback: () -> Unit) {
  document.addDocumentListener(object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent?) {
      callback()
    }

    override fun removeUpdate(e: DocumentEvent?) {
      callback()
    }

    override fun changedUpdate(e: DocumentEvent?) {
      callback()
    }
  })
}