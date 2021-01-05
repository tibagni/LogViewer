package com.tibagni.logviewer.view

import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun JTextField.whenTextChanges(callback: (newText: String) -> Unit) {
  document.addDocumentListener(object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent?) {
      callback(text)
    }

    override fun removeUpdate(e: DocumentEvent?) {
      callback(text)
    }

    override fun changedUpdate(e: DocumentEvent?) {
      callback(text)
    }
  })
}