package com.example.mcp.client;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
public class ChatController {
    private final McpChatService chatService;

    public ChatController(McpChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/api/chat")
    public ResponseEntity<McpChatService.ChatResult> chat(@RequestBody ChatRequest request) {
        if (request == null || !StringUtils.hasText(request.prompt())) {
            throw new ResponseStatusException(BAD_REQUEST, "Prompt is required.");
        }

        return ResponseEntity.ok(chatService.chat(request.prompt(), request.conversationId()));
    }

    @GetMapping("/api/tools")
    public List<McpChatService.ToolSummary> tools() {
        return chatService.tools();
    }

    @PostMapping("/api/tools/refresh")
    public List<McpChatService.ToolSummary> refreshTools() {
        chatService.refreshTools();
        return chatService.tools();
    }

    @GetMapping("/api/config")
    public ClientConfig config() {
        return new ClientConfig(chatService.model(), chatService.mcpEndpoint());
    }

    public record ChatRequest(String prompt, String conversationId) {}

    public record ClientConfig(String model, String mcpEndpoint) {}
}
