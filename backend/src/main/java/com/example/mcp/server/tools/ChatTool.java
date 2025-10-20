package com.example.mcp.server.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class ChatTool {
    private final WebClient webClient;

    @Value("${openrouter.api.url}")
    private String chatUrl;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String model;

    public ChatTool(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    @Tool(name = "askChat", description = "Ask a general question to the LLM.")
    public Flux<String> ask(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            return Flux.just("No OpenRouter API key configured.");
        }

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", new Object[]{ Map.of("role", "user", "content", prompt) },
                "stream", false // important: disable streaming to get clean text
        );
        return webClient.post()
                        .uri(chatUrl)
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(60))
                        .flatMapMany(resp -> {
                            try {
                                // Extract assistant content from LLM response
                                List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
                                if (choices == null || choices.isEmpty()) {
                                    return Flux.just("LLM returned no answer.");
                                }

                                Map<String, Object> choice = choices.get(0);
                                Map<String, Object> message = (Map<String, Object>) choice.get("message");
                                String content = message != null ? (String) message.get("content") : null;

                                if (content == null || content.isBlank()) {
                                    return Flux.just("LLM returned empty answer.");
                                }

                                // Stream word by word with a small delay for UI effect
                                return Flux.fromArray(content.trim().split("\\s+"))
                                           .delayElements(Duration.ofMillis(50));

                            } catch (Exception ex) {
                                return Flux.just("Failed to parse chat response: " + ex.getMessage());
                            }
                        })
                        .onErrorResume(ex -> Flux.just("Chat error: " + ex.getMessage()));
    }
}
