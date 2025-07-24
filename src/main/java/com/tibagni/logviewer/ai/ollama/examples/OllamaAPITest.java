package com.tibagni.logviewer.ai.ollama.examples;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.response.Model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public class OllamaAPITest {

  public static void main(String[] args) throws OllamaBaseException, IOException, URISyntaxException, InterruptedException {
    String host = "http://localhost:11434/";

    OllamaAPI ollamaAPI = new OllamaAPI(host);

    ollamaAPI.setVerbose(true);

    boolean isOllamaServerReachable = ollamaAPI.ping();

    System.out.println("Is Ollama server running: " + isOllamaServerReachable);

    List<Model> models = ollamaAPI.listModels();

    models.forEach(model -> System.out.println(model.getName()));
  }
}