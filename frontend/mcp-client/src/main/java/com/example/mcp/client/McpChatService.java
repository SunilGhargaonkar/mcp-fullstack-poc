package com.example.mcp.client;

import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;

@Service
public class McpChatService {
    private static final int MAX_MEMORY_MESSAGES = 30;
    private static final String SYSTEM_PROMPT = """
            Current UTC time: %s.
            Use MCP tools for weather and calendar booking when they are relevant.
            Answer directly for normal questions. Ask a short clarification when a tool request is missing details.
            """;

    private final String llmBaseUrl;
    private final String llmApiKey;
    private final String llmModel;
    private final String mcpEndpoint;
    private final Duration mcpTimeout;
    private final AtomicReference<List<ToolCallback>> cachedTools = new AtomicReference<>();
    private final Map<String, MessageWindowChatMemory> memories = new ConcurrentHashMap<>();
    private volatile OpenAiApi openAiApi;

    public McpChatService(
            @Value("${poc.llm.base-url}") String llmBaseUrl,
            @Value("${poc.llm.api-key}") String llmApiKey,
            @Value("${poc.llm.model}") String llmModel,
            @Value("${poc.mcp.endpoint}") String mcpEndpoint,
            @Value("${poc.mcp.connection-timeout:60s}") Duration mcpTimeout
    ) {
        this.llmBaseUrl = llmBaseUrl;
        this.llmApiKey = llmApiKey;
        this.llmModel = llmModel;
        this.mcpEndpoint = mcpEndpoint;
        this.mcpTimeout = mcpTimeout;
    }

    public ChatResult chat(String prompt, String requestedConversationId) {
        String conversationId = StringUtils.hasText(requestedConversationId)
                ? requestedConversationId
                : UUID.randomUUID().toString();
        List<ToolCallback> toolCallbacks = toolCallbacks();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(llmModel)
                        .toolChoice(toolCallbacks.isEmpty() ? "none" : "auto")
                        .build())
                .build();

        String answer = ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory(conversationId))
                        .conversationId(conversationId)
                        .build())
                .build()
                .prompt()
                .system(SYSTEM_PROMPT.formatted(ZonedDateTime.now(ZoneOffset.UTC)))
                .toolCallbacks(toolCallbacks)
                .user(prompt)
                .call()
                .content();

        return new ChatResult(conversationId, answer, llmModel, tools());
    }

    public String model() {
        return llmModel;
    }

    public String mcpEndpoint() {
        return mcpEndpoint;
    }

    public List<ToolSummary> tools() {
        return toolCallbacks().stream()
                .map(callback -> new ToolSummary(
                        callback.getToolDefinition().name(),
                        callback.getToolDefinition().description()))
                .toList();
    }

    public void refreshTools() {
        cachedTools.set(null);
    }

    private List<ToolCallback> toolCallbacks() {
        List<ToolCallback> current = cachedTools.get();
        if (current != null) {
            return current;
        }

        try {
            McpClientTransport transport = HttpClientStreamableHttpTransport.builder(mcpEndpoint)
                    .connectTimeout(mcpTimeout)
                    .build();
            McpSyncClient client = McpClient.sync(transport).build();
            SyncMcpToolCallbackProvider provider = SyncMcpToolCallbackProvider.builder()
                    .toolFilter((name, definition) -> true)
                    .mcpClients(client)
                    .build();
            cachedTools.compareAndSet(null, Arrays.asList(provider.getToolCallbacks()));
        } catch (Exception ignored) {
            cachedTools.compareAndSet(null, List.of());
        }

        return cachedTools.get();
    }

    private OpenAiApi openAiApi() {
        if (!StringUtils.hasText(llmApiKey)) {
            throw new IllegalStateException("Set poc.llm.api-key or POC_LLM_API_KEY.");
        }
        if (!StringUtils.hasText(llmBaseUrl)) {
            throw new IllegalStateException("Set poc.llm.base-url or POC_LLM_BASE_URL.");
        }

        OpenAiApi current = openAiApi;
        if (current == null) {
            synchronized (this) {
                current = openAiApi;
                if (current == null) {
                    current = new OpenAiApi.Builder()
                            .baseUrl(normalizeBaseUrl(llmBaseUrl))
                            .apiKey(llmApiKey)
                            .build();
                    openAiApi = current;
                }
            }
        }
        return current;
    }

    private String normalizeBaseUrl(String baseUrl) {
        String value = baseUrl.trim();
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.endsWith("/chat/completions")) {
            value = value.substring(0, value.length() - "/chat/completions".length());
        }
        if (value.endsWith("/v1")) {
            value = value.substring(0, value.length() - "/v1".length());
        }
        return value;
    }

    private MessageWindowChatMemory memory(String conversationId) {
        return memories.computeIfAbsent(conversationId, ignored -> MessageWindowChatMemory.builder()
                .maxMessages(MAX_MEMORY_MESSAGES)
                .build());
    }

    public record ChatResult(String conversationId, String answer, String model, List<ToolSummary> tools) {}

    public record ToolSummary(String name, String description) {}
}
