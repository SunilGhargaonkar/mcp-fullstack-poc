package com.example.mcp.server.service;

import com.example.mcp.server.model.ToolDecision;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
public class LlmService {
    private final WebClient webClient;
    private final LlmResponseParser parser;

    @Value("${openrouter.api.url}")
    private String llmUrl;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.model}")
    private String model;

    public LlmService(WebClient.Builder builder, LlmResponseParser parser) {
        this.webClient = builder.build();
        this.parser = parser;
    }

    public Mono<ToolDecision> decideTool(String userPrompt) {
        String systemPrompt = """
                You are an MCP LLM Router. User may ask for:
                1. Weather info
                2. Calendar booking
                3. General questions

                Respond ONLY in JSON with NO extra text:
                {
                  "toolName": "<ToolName or null>",
                  "arguments": { key-value arguments for tool OR answer: <direct answer if no tool> }
                }
                Tools available:
                - getWeather -> arguments: {"city": "London"}
                - bookEvent -> arguments: {"title": "Meeting", "startIsoUtc": "2025-10-19T12:00:00Z", "durationMin": 60}
                - askChat -> arguments: {"prompt": "Your question"}
                """;

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", new Object[]{
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                },
                "stream", false
        );

        return webClient.post()
                        .uri(llmUrl)
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .timeout(Duration.ofSeconds(60))
                        .flatMap(parser::parse)
                        .onErrorResume(ex -> Mono.just(new ToolDecision(null, Map.of("answer", "LLM error: " + ex))));
    }
}
