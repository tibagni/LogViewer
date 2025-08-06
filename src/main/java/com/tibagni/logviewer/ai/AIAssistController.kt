package com.tibagni.logviewer.ai

import com.tibagni.logviewer.ServiceLocator.logViewerPrefs
import com.tibagni.logviewer.ai.ollama.Config
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.parser.Parser
import io.github.ollama4j.models.generate.OllamaStreamHandler
import io.github.ollama4j.utils.OptionsBuilder
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

/*
 * This class handles the interaction between the AI assist model and the view.
 * It sends user messages to the Ollama server and updates the chat view with AI responses.
 */
class AIAssistController(
  private val model: AIAssistModel,
  private val view: AIAssistView
) {
  private val parser = Parser.builder().build()
  private val renderer = HtmlRenderer.builder().build()

  // Initializes the controller with the view and sets up action listeners
  init {
    view.sendButton.addActionListener { sendMessage() }
    view.inputField.addActionListener { sendMessage() }
  }

  // Sends the user's message to the Ollama server and updates the chat view
  private fun sendMessage() {
    val userMessage = view.inputField.text.trim()
    if (userMessage.isEmpty()) return

    // Check if the Ollama server is reachable before sending the messages
    if (!Config.isServerReachable()) {
      JOptionPane.showMessageDialog(view, "Ollama server is not reachable.", "Error", JOptionPane.ERROR_MESSAGE)
      return
    }

    // Append the user's message to the chat history and clear the input field
    appendMarkdown("**$userMessage**")
    view.inputField.text = ""
    updateAIMessage("...")
    updateInputStatus(false)

    // Start a new thread to handle the AI response
    Thread {
      val ollama = Config.getAPI()
      val options = OptionsBuilder().build()

      val streamHandler = OllamaStreamHandler { token ->
        SwingUtilities.invokeLater {
          updateAIMessage(token)
        }
      }

      try {
        val result = ollama.generate(logViewerPrefs.aiModel, userMessage, false, options, streamHandler)
        SwingUtilities.invokeLater {
          appendMarkdown("${result.response}")
        }
      } catch (ex: Exception) {
        SwingUtilities.invokeLater {
          appendMarkdown("[Error: ${ex.message}]")
        }
      } finally {
        SwingUtilities.invokeLater {
          appendBreak()
          updateInputStatus(true)
        }
      }
    }.start()
  }

  // Updates the AI message in the chat view
  private fun updateAIMessage(aiText: String) {
    val tempHistory = model.cloneHistory()
    val document = parser.parse("$aiText\n")
    val html = renderer.render(document)
    view.setChatText("$tempHistory$html")
  }

  // Appends a message to the chat history
  private fun appendMarkdown(markdown: String) {
    val document = parser.parse(markdown)
    val html = renderer.render(document)
    model.append(html)
    view.setChatText(model.getHistory())
  }

  // Appends a break in the chat history
  private fun appendBreak() {
    appendMarkdown("<br>")
    appendMarkdown("---")
  }

  // Updates the input field and send button status
  private fun updateInputStatus(status: Boolean) {
    view.inputField.isEnabled = status
    view.sendButton.isEnabled = status
  }
}