package com.example.mcp.server.model;
import java.lang.reflect.Method;

public record ToolMethod(Object bean, Method method) {}