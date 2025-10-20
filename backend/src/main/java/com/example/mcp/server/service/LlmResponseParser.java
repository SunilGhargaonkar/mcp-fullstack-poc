package com.example.mcp.server.service;

import com.example.mcp.server.model.ToolDecision;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class LlmResponseParser {
    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public Mono<ToolDecision> parse(Map<String, Object> llmResponse) {
        try {
            Object choicesObj = llmResponse.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                String fallback = extractTextFallback(llmResponse).orElse("LLM returned empty response");
                return Mono.just(new ToolDecision(null, Map.of("answer", fallback)));
            }

            String content = null;
            for (Object rawChoice : choices) {
                content = extractContentFromChoice(rawChoice);
                if (content != null && !content.isBlank()) break;
            }

            if (content == null || content.isBlank()) {
                String fallback = extractTextFallback(llmResponse).orElse("LLM returned empty content");
                return Mono.just(new ToolDecision(null, Map.of("answer", fallback)));
            }

            String jsonFragment = content.strip()
                                         // remove leading non { characters and trailing non } characters
                                         .replaceAll("^[^\\{\\[]*", "")
                                         .replaceAll("[^\\}\\]]*$", "");

            if ((jsonFragment.startsWith("{") && jsonFragment.endsWith("}")) ||
                (jsonFragment.startsWith("[") && jsonFragment.endsWith("]"))) {

                Map<String, Object> map = mapper.readValue(jsonFragment, Map.class);
                String toolName = map.get("toolName") != null ? map.get("toolName").toString() : null;
                Map<String, Object> arguments = (Map<String, Object>) map.get("arguments");

                return Mono.just(new ToolDecision(toolName, arguments));
            } else {
                return Mono.just(new ToolDecision(null, Map.of("answer", content)));
            }

        } catch (Exception e) {
            return Mono.just(new ToolDecision(null,
                    Map.of("answer", "Failed to parse LLM response: " + e.getMessage())));
        }
    }

    /**
     * Attempt to extract assistant content from one choice element.
     * Supports several common shapes:
     * - choice is a Map containing "message" -> Map -> "content"
     * - choice is a Map containing "message" -> String
     * - choice is a Map containing "content" or "text"
     * - choice is a raw String
     */
    private String extractContentFromChoice(Object rawChoice) {
        try {
            if (rawChoice == null) return null;

            // If the choice itself is a string, return it
            if (rawChoice instanceof String s) {
                return s;
            }

            if (rawChoice instanceof Map<?, ?> choiceMap) {
                Object messageObj = choiceMap.get("message");
                if (messageObj instanceof Map<?, ?> messageMap) {
                    Object contentObj = messageMap.get("content");
                    if (contentObj instanceof String) return (String) contentObj;
                } else if (messageObj instanceof String) {
                    return (String) messageObj;
                }

                Object content = choiceMap.get("content");
                if (content instanceof String) return (String) content;

                Object text = choiceMap.get("text");
                if (text instanceof String) return (String) text;

                Object output = choiceMap.get("output");
                if (output instanceof String) return (String) output;
                if (output instanceof List<?> outputList && !outputList.isEmpty() && outputList.get(0) instanceof String) {
                    return (String) outputList.get(0);
                }
            }

        } catch (Exception ignored) {
            // ignore and return null to try other fallbacks
        }
        return null;
    }

    private Optional<String> extractTextFallback(Map<String, Object> resp) {
        if (resp == null || resp.isEmpty()) return Optional.empty();

        Object topText = resp.get("text");
        if (topText instanceof String) return Optional.of((String) topText);

        Object output = resp.get("output");
        if (output instanceof String) return Optional.of((String) output);

        if (output instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof String) {
            return Optional.of((String) list.get(0));
        }

        final Object response = resp.get("response");
        if (response instanceof String) return Optional.of((String) response);

        return Optional.empty();
    }
}
