package com.example.mcp.server.tools;

import com.example.mcp.server.model.ToolDecision;
import reactor.core.publisher.Flux;

public interface ToolExecutor {
    boolean canExecute(String toolName);
    Flux<String> execute(ToolDecision decision);
}
