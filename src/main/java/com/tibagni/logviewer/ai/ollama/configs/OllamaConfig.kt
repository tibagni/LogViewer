package com.tibagni.logviewer.ai.ollama.configs

import io.github.ollama4j.OllamaAPI

/**
 * Configuration for the Ollama AI integration.
 * This includes the host URL and model name.
 */
object OllamaConfig {
  const val HOST = "http://localhost:11434"
  const val MODEL = "deepseek-coder:6.7b"

  // Create an instance of OllamaAPI with the configured host
  fun getAPI(): OllamaAPI = OllamaAPI(HOST)

  // Check if the Ollama server is reachable
  fun isServerReachable(): Boolean {
    return try {
      getAPI().ping()
    } catch (_: Exception) {
      false
    }
  }
}