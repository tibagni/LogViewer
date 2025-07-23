package com.tibagni.logviewer.ai.ollama.views

import com.tibagni.logviewer.ServiceLocator
import com.tibagni.logviewer.ServiceLocator.themeManager
import com.tibagni.logviewer.util.StringUtils
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.*

/**
 * AIAssistView is a Swing JPanel that provides a user interface for interacting with an AI assistant.
 * It includes a chat area for displaying messages, an input field for user input, and a send button.
 */
class AIAssistView : JPanel() {
  val chatArea = JEditorPane("text/html", "")
  val inputField = JTextField()
  val sendButton = JButton(StringUtils.ROCKET)

  // Initializes the views
  init {
    layout = BorderLayout()

    val themeManager = ServiceLocator.themeManager

    val titleLabel = JLabel("AI Assist")
    titleLabel.horizontalAlignment = SwingConstants.CENTER
    titleLabel.font = titleLabel.font.deriveFont(Font.PLAIN, 13f)
    val titlePanel = JPanel(BorderLayout())
    titlePanel.add(titleLabel, BorderLayout.CENTER)
//    border = TitledBorder("AI Assist")

    chatArea.background = if (themeManager.isDark) Color(68, 72, 72) else Color(255, 255, 255)
    chatArea.isEditable = false
    val scrollPane = JScrollPane(chatArea)
    scrollPane.isEnabled = false

    val inputPanel = JPanel(BorderLayout())
    inputField.background = if (themeManager.isDark) Color(60, 60, 60) else Color(240, 240, 240)
    inputPanel.add(inputField, BorderLayout.CENTER)
    inputPanel.add(sendButton, BorderLayout.EAST)

    add(titlePanel, BorderLayout.NORTH)
    add(scrollPane, BorderLayout.CENTER)
    add(inputPanel, BorderLayout.SOUTH)
  }

  // Sets the chat text with HTML formatting
  fun setChatText(html: String) {
    chatArea.text = ("<html><head>"
        + "<meta charset='UTF-8'>"
//        + "<link rel='stylesheet' href='https://cdnjs.cloudflare.com/ajax/libs/github-markdown-css/5.2.0/github-markdown-light.min.css'>"
        + "<style>\n" +
        "body {\n" +
        "  font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", sans-serif;\n" +
        "  margin: 2em;\n" +
        "  line-height: 1.6;\n" +
//        "  color: #eee;\n" +
        "}\n" +
        "h1, h2, h3 {\n" +
        "  color: #111;\n" +
        "}\n" +
        "pre, code {\n" +
//        "  background-color: #444444;\n" +
        "  padding: 0.2em 0.4em;\n" +
        "  font-family: \"Courier New\", monospace;\n" +
        "  border-radius: 4px;\n" +
        "}\n" +
        "pre {\n" +
        "  overflow-x: auto;\n" +
        "  padding: 1em;\n" +
        "}\n" +
        "blockquote {\n" +
        "  border-left: 4px solid #ddd;\n" +
        "  padding-left: 1em;\n" +
        "  color: #666;\n" +
        "  margin-left: 0;\n" +
        "}\n" +
        "ul, ol {\n" +
        "  padding-left: 2em;\n" +
        "}\n" +
        "table {\n" +
        "  border-collapse: collapse;\n" +
        "}\n" +
        "td, th {\n" +
        "  border: 1px solid #ccc;\n" +
        "  padding: 0.5em;\n" +
        "}\n" +
        "</style>"
        + "<script src='https://twemoji.maxcdn.com/v/latest/twemoji.min.js'></script>"
        + "</head><body class='markdown-body'>"
        + html
        + "<script>twemoji.parse(document.body);</script>"
        + "</body></html>");
  }
}