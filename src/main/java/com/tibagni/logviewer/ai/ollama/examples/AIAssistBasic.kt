package com.tibagni.logviewer.ai.ollama.examples

import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import io.github.ollama4j.OllamaAPI
import io.github.ollama4j.models.chat.OllamaChatMessageRole
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.SwingUtilities

/**
 * AIAssistBasic is a simple AI chat assistant that interacts with the Ollama server.
 * It allows users to send messages and receive AI-generated responses in a chat-like interface.
 */
class AIAssistBasic : JPanel() {
  private val chatArea = JEditorPane("text/html", "html")
  private val inputField = JTextField()
  private val parser = Parser.builder().build()
  private val renderer = HtmlRenderer.builder().build()
  private val chatHistory = StringBuilder()

  init {
    layout = BorderLayout()
    chatArea.isEditable = false
    val scrollPane = JScrollPane(chatArea)

    val sendButton = JButton("Send")
    sendButton.addActionListener { sendMessage() }

    val inputPanel = JPanel(BorderLayout())
    inputPanel.add(inputField, BorderLayout.CENTER)
    inputPanel.add(sendButton, BorderLayout.EAST)

    add(scrollPane, BorderLayout.CENTER)
    add(inputPanel, BorderLayout.SOUTH)

    inputField.addActionListener { sendMessage() }
  }

  // Sends the user's message to the Ollama server and updates the chat view
  private fun sendMessage() {
    val userMessage = inputField.text.trim()
    if (userMessage.isEmpty()) return

    appendMarkdown("**You:** $userMessage\n")
    inputField.text = ""
    Thread {
      val aiResponse = askOllama(userMessage)
      SwingUtilities.invokeLater { appendMarkdown("**AI:** $aiResponse\n") }
    }.start()
  }

  // Appends Markdown text to the chat area and updates the HTML view
  private fun appendMarkdown(markdown: String) {
    val document = parser.parse(markdown)
    val html = renderer.render(document)
    chatHistory.append(html)
    chatArea.text = "<html><body>${chatHistory}</body></html>"
  }

  // Asks the Ollama server for a response to the given message
  private fun askOllama(message: String): String {
    return try {
      val ollama = OllamaAPI("http://localhost:11434")
      val chatRequest = OllamaChatRequestBuilder.getInstance("deepseek-coder:6.7b")
        .withMessage(OllamaChatMessageRole.USER, message)
        .build()
      val chatResponse = ollama.chat(chatRequest)
      chatResponse.response
    } catch (ex: Exception) {
      "[Error: ${ex.message}]"
    }
  }
}