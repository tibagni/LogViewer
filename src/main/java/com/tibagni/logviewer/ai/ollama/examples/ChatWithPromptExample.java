package com.tibagni.logviewer.ai.ollama.examples;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.OptionsBuilder;
import io.github.ollama4j.utils.PromptBuilder;

public class ChatWithPromptExample {

  public static void main(String[] args) throws Exception {
    String host = "http://localhost:11434/";
    String model = "deepseek-coder:6.7b"; // Replace with your model name if different

    OllamaAPI ollamaAPI = new OllamaAPI(host);
//    ollamaAPI.setVerbose(false);
    ollamaAPI.setRequestTimeoutSeconds(60);

    PromptBuilder promptBuilder =
        new PromptBuilder()
            .addLine("You are an expert coder and understand different programming languages.")
            .addLine("Given a question, answer ONLY with code.")
            .addLine("Produce clean, formatted and indented code in markdown format.")
            .addSeparator()
            .addLine("Example: Sum 2 numbers in Python")
            .addLine("Answer:")
            .addLine("```python")
            .addLine("def sum(num1: int, num2: int) -> int:")
            .addLine("    return num1 + num2")
            .addLine("```")
            .addSeparator()
            .add("How do I read a file in Go and print its contents to stdout?");

    boolean raw = false;
    OllamaResult response = ollamaAPI.generate(model, promptBuilder.build(), raw, new OptionsBuilder().build());
    System.out.println(response.getResponse());
  }
}