package com.tibagni.logviewer.ai.ollama.models

/**
 * Model for managing AI chat history and interactions.
 */
class AIAssistModel {
  private val chatHistory = StringBuilder()

  // Appends a markdown formatted message to the chat history
  fun append(html: String) { chatHistory.append(html) }

  // Gets the current chat history as a string
  fun getHistory(): String = chatHistory.toString()

  // Clones the chat history into a new StringBuilder
  fun cloneHistory(): StringBuilder = StringBuilder(chatHistory)
}