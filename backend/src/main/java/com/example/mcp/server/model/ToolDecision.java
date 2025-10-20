package com.example.mcp.server.model;

import java.util.Map;

public record ToolDecision(String toolName, Map<String, Object> arguments) {}
