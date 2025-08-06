package com.tibagni.logviewer.ai.ollama

import com.tibagni.logviewer.ServiceLocator
import io.github.ollama4j.OllamaAPI

/**
 * Configuration for the Ollama AI integration.
 * This includes the host URL and model name.
 */
object Config {
  // Create an instance of OllamaAPI with the configured host
  fun getAPI(): OllamaAPI = OllamaAPI(ServiceLocator.logViewerPrefs.aiHost)

  // Check if the Ollama server is reachable
  fun isServerReachable(): Boolean {
    return try {
      getAPI().ping()
    } catch (_: Exception) {
      false
    }
  }

  // Get available models from the Ollama server
  fun getAvailableModels(): List<String> {
    return try {
      getAPI().listModels().map { it.name }
    } catch (_: Exception) {
      emptyList()
    }
  }
}