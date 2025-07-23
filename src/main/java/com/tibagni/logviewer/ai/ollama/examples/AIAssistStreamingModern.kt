package com.tibagni.logviewer.ai.ollama.examples

import io.github.ollama4j.OllamaAPI
import io.github.ollama4j.models.generate.OllamaStreamHandler
import io.github.ollama4j.utils.OptionsBuilder
import java.awt.*
import javax.swing.*
import javax.swing.border.AbstractBorder
import javax.swing.border.EmptyBorder

/**
 * AIAssistStreamingModern is a modern AI chat assistant interface that interacts with the Ollama server.
 * It allows users to send messages and receive AI-generated responses in a chat-like interface with streaming support.
 */
class AIAssistStreamingModern : JPanel() {
  private val chatHistoryPanel = JPanel()
  private val inputField = JTextField()
  private var aiStreamingBubble: JPanel? = null

  init {
    layout = BorderLayout()
    background = Color(34, 34, 34)

    chatHistoryPanel.layout = BoxLayout(chatHistoryPanel, BoxLayout.Y_AXIS)
    chatHistoryPanel.background = Color(34, 34, 34)
    chatHistoryPanel.border = EmptyBorder(10, 10, 10, 10)

    val scrollPane = JScrollPane(chatHistoryPanel)
    scrollPane.verticalScrollBar.unitIncrement = 16
    scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    scrollPane.background = Color(34, 34, 34)
    scrollPane.border = null

    val sendButton = JButton("Send")
    sendButton.background = Color(60, 60, 60)
    sendButton.foreground = Color.WHITE
    sendButton.isFocusPainted = false
    sendButton.addActionListener { sendMessage() }
    inputField.addActionListener { sendMessage() }
    inputField.background = Color(44, 44, 44)
    inputField.foreground = Color.WHITE
    inputField.caretColor = Color.WHITE
    inputField.border = EmptyBorder(8, 12, 8, 12)

    val inputPanel = JPanel(BorderLayout())
    inputPanel.background = Color(34, 34, 34)
    inputPanel.add(inputField, BorderLayout.CENTER)
    inputPanel.add(sendButton, BorderLayout.EAST)
    inputPanel.border = EmptyBorder(10, 10, 10, 10)

    add(scrollPane, BorderLayout.CENTER)
    add(inputPanel, BorderLayout.SOUTH)
  }

  // Sends the user's message to the Ollama server and updates the chat view
  private fun sendMessage() {
    val userMessage = inputField.text.trim()
    if (userMessage.isEmpty()) return

    addChatBubble(userMessage, isUser = true)
    inputField.text = ""

    // Create/reuse AI streaming bubble for the answer
    SwingUtilities.invokeLater {
      aiStreamingBubble = addChatBubble("", isUser = false, streaming = true)
      chatHistoryPanel.revalidate()
      scrollToBottom()
    }

    Thread {
      val ollama = OllamaAPI("http://localhost:11434")
      val model = "deepseek-coder:6.7b"
      val prompt = userMessage
      val options = OptionsBuilder().build()
      val aiMessage = StringBuilder()

      val streamHandler = OllamaStreamHandler { token ->
        aiMessage.append(token)
        SwingUtilities.invokeLater {
          updateStreamingBubble(aiMessage.toString())
          scrollToBottom()
        }
      }

      try {
        ollama.generate(model, prompt, false, options, streamHandler)
        SwingUtilities.invokeLater {
          finalizeStreamingBubble(aiMessage.toString())
          scrollToBottom()
        }
      } catch (ex: Exception) {
        SwingUtilities.invokeLater {
          finalizeStreamingBubble("[Error: ${ex.message}]")
          scrollToBottom()
        }
      }
    }.start()
  }

  // Returns the bubble panel for possible reuse
  private fun addChatBubble(message: String, isUser: Boolean, streaming: Boolean = false): JPanel {
    val bubble = createChatBubble(message, isUser, streaming)
    val wrapper = JPanel(BorderLayout())
    wrapper.isOpaque = false
    val width = (chatHistoryPanel.width * 0.9).toInt().coerceAtLeast(300)
    bubble.preferredSize = Dimension(width, bubble.preferredSize.height)
    wrapper.add(bubble, if (isUser) BorderLayout.WEST else BorderLayout.EAST)
    chatHistoryPanel.add(wrapper)
    chatHistoryPanel.revalidate()
    return bubble
  }

  // Creates a chat bubble panel with the given message and style
  private fun createChatBubble(message: String, isUser: Boolean, streaming: Boolean = false): JPanel {
    val bubbleLabel = JLabel("<html>${escapeHtml(message)}</html>")
    bubbleLabel.font = bubbleLabel.font.deriveFont(15f)
    bubbleLabel.border = EmptyBorder(10, 18, 10, 18)
    bubbleLabel.foreground = Color.WHITE

    val bubblePanel = JPanel()
    bubblePanel.layout = BorderLayout()
    bubblePanel.add(bubbleLabel, BorderLayout.CENTER)
    bubblePanel.setOpaque(false)

    val bgColor = when {
      isUser -> Color(60, 120, 60)
      streaming -> Color(60, 60, 120)
      else -> Color(60, 60, 60)
    }

    bubblePanel.background = bgColor
    bubblePanel.setBorder(RoundedBubbleBorder(bgColor, 18))
    return bubblePanel
  }

  // Updates the streaming bubble with the current AI text
  private fun updateStreamingBubble(aiText: String) {
    aiStreamingBubble?.let {
      val label = it.components[0] as JLabel
      label.text = "<html>${escapeHtml(aiText)}</html>"
    }
    // print the current text to the console for debugging
    println("Streaming AI text: $aiText")
  }

  // Finalizes the streaming bubble with the complete AI text
  private fun finalizeStreamingBubble(aiText: String) {
    aiStreamingBubble?.let {
      val label = it.components[0] as JLabel
      label.text = "<html>${escapeHtml(aiText)}</html>"
      it.background = Color(60, 60, 60)
      it.setBorder(RoundedBubbleBorder(Color(60, 60, 60), 18))
      aiStreamingBubble = null
    }
  }

  // Scrolls the chat area to the bottom
  private fun scrollToBottom() {
    val scrollPane = this.components.find { it is JScrollPane } as? JScrollPane
    scrollPane?.verticalScrollBar?.value = scrollPane?.verticalScrollBar?.maximum!!
  }

  // Escapes HTML special characters in the text
  private fun escapeHtml(text: String): String =
    text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")

  /**
   * Custom border for rounded chat bubbles
   *
   * @param color The background color of the bubble
   * @param radius The corner radius for the bubble
   */
  class RoundedBubbleBorder(private val color: Color, private val radius: Int) : AbstractBorder() {
    override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
      val g2 = g as Graphics2D
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.color = color
      g2.fillRoundRect(x, y, width - 1, height - 1, radius, radius)
    }
    override fun getBorderInsets(c: Component): Insets = Insets(radius, radius, radius, radius)
    override fun isBorderOpaque(): Boolean = true
  }
}