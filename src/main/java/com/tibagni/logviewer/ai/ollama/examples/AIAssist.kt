package com.tibagni.logviewer.ai.ollama.examples

import com.tibagni.logviewer.util.StringUtils
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import io.github.ollama4j.OllamaAPI
import io.github.ollama4j.models.generate.OllamaStreamHandler
import io.github.ollama4j.models.response.OllamaResult
import io.github.ollama4j.utils.OptionsBuilder
import java.awt.BorderLayout
import javax.swing.*

/**
 * AIAssist is a simple AI chat assistant that interacts with the Ollama server.
 * It allows users to send messages and receive AI-generated responses in a chat-like interface.
 */
class AIAssist : JPanel() {
  private val chatArea = JEditorPane("text/html", "")
  private val inputField = JTextField()
  private val parser = Parser.builder().build()
  private val renderer = HtmlRenderer.builder().build()
  private val chatHistory = StringBuilder()

  init {
    layout = BorderLayout()
    chatArea.isEditable = false
    val scrollPane = JScrollPane(chatArea)

    val sendButton = JButton(StringUtils.ROCKET)
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

    appendMarkdown("**$userMessage**")
    inputField.text = ""

    // Start streaming AI response
    Thread {
      val ollama = OllamaAPI("http://localhost:11434")
      val model = "deepseek-coder:6.7b"
      val prompt = userMessage
      val options = OptionsBuilder().build()

      val streamHandler = OllamaStreamHandler { token ->
        SwingUtilities.invokeLater {
          // Show the streaming AI message
          updateAIMessage(token)
        }
      }

      var ollamaResult: OllamaResult?
      try {
        ollamaResult = ollama.generate(model, prompt, false, options, streamHandler)
        // After streaming is done, finalize the message
        SwingUtilities.invokeLater {
          appendMarkdown("${ollamaResult.response}")
          appendBreak()
        }
      } catch (ex: Exception) {
        SwingUtilities.invokeLater {
          appendMarkdown("[Error: ${ex.message}]")
          appendBreak()
        }
      }
    }.start()
  }

  // Shows the current streaming AI message (not appended to history yet)
  private fun updateAIMessage(aiText: String) {
    val tempHistory = StringBuilder(chatHistory)
    val document = parser.parse("$aiText\n")
    val html = renderer.render(document)
    chatArea.text = "<html><body>${tempHistory}$html</body></html>"
  }

  // Appends a message to the chat history
  private fun appendMarkdown(markdown: String) {
    val document = parser.parse(markdown)
    val html = renderer.render(document)
    chatHistory.append(html)
    chatArea.text = "<html><body>${chatHistory}</body></html>"
  }

  // Appends a break in the chat history
  private fun appendBreak() {
    appendMarkdown("<br>")
    appendMarkdown("---")
  }
}