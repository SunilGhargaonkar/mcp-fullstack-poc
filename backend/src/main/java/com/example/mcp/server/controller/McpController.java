package com.example.mcp.server.controller;

import com.example.mcp.server.helper.ToolInvoker;
import com.example.mcp.server.model.ToolDecision;
import com.example.mcp.server.service.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpController {
    private final LlmService llmService;
    private final ToolInvoker toolInvoker;

    public McpController(LlmService llmService, ToolInvoker toolInvoker) {
        this.llmService = llmService;
        this.toolInvoker = toolInvoker;
    }

    @PostMapping
    public Flux<String> handlePrompt(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        log.info("Received prompt: {}", prompt);

        if (prompt == null || prompt.isBlank()) {
            return Flux.just("No prompt provided.");
        }

        return llmService.decideTool(prompt)
                         .flatMapMany(this::handleToolDecision)
                         .onErrorResume(ex -> Flux.just("LLM could not decide tool: " + ex.getMessage()));
    }

    private Flux<String> handleToolDecision(ToolDecision decision) {
        if (decision.toolName() == null) {
            Object answer = decision.arguments().get("answer");
            log.info("LLM returned direct answer: {}", answer);

            return Flux.just("Answer: " + (answer != null ? answer.toString() : "null"));
        }

        log.info("Delegating to ToolInvoker for tool: {}", decision.toolName());
        return toolInvoker.invokeTool(decision.toolName(), decision.arguments());
    }
}
