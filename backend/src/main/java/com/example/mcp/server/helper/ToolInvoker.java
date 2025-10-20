package com.example.mcp.server.helper;

import java.lang.reflect.Method;
import java.util.Map;

import com.example.mcp.server.model.ToolMethod;
import com.example.mcp.server.tools.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Slf4j
@Component
public class ToolInvoker {
    private final ToolRegistry toolRegistry;

    public ToolInvoker(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    /**
     * Invoke a tool by its name with given arguments.
     *
     * @param toolName the name of the tool
     * @param arguments arguments as key-value map
     * @return Flux<String> streaming output
     */
    public Flux<String> invokeTool(String toolName, Map<String, Object> arguments) {
        ToolMethod toolMethod = toolRegistry.getTool(toolName);
        log.info("Invoking tool: {}", toolName);

        if (toolMethod == null) {
            log.warn("Tool '{}' not found in registry", toolName);
            return Flux.just("Tool not found: " + toolName);
        }

        try {
            Method method = toolMethod.method();
            Object bean = toolMethod.bean();

            Object[] args = mapArguments(toolName, arguments);
            if (args == null) {
                return Flux.just("Could not map arguments for tool: " + toolName);
            }

            // Check for missing args
            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    String paramName = method.getParameters()[i].getName();
                    log.warn("Missing argument '{}' for tool '{}'", paramName, toolName);
                    return Flux.just("Missing argument '" + paramName + "' for tool: " + toolName);
                }
            }

            log.info("Invoking method {} on bean {}", method.getName(), bean.getClass().getSimpleName());
            Object result = method.invoke(bean, args);

            if (result instanceof Flux<?> fluxResult) {
                return fluxResult.map(Object::toString);
            } else if (result instanceof String str) {
                return Flux.just(str);
            } else {
                log.warn("Tool '{}' returned unknown type: {}", toolName, result != null ? result.getClass() : null);
                return Flux.just("Tool returned unknown type");
            }

        } catch (Exception e) {
            log.error("Error invoking tool '{}'", toolName, e);
            return Flux.just("Error invoking tool '" + toolName + "': " + e.getMessage());
        }
    }

    /**
     * Map arguments for specific tools.
     */
    private Object[] mapArguments(String toolName, Map<String, Object> arguments) {
        return switch (toolName) {
            case "getWeather" -> new Object[]{arguments.get("city")};
            case "askChat" -> new Object[]{arguments.get("prompt")};
            case "bookEvent" -> {
                Object durationObj = arguments.get("durationMin");
                int duration = durationObj != null ? Integer.parseInt(durationObj.toString()) : 0;
                yield new Object[]{arguments.get("title"), arguments.get("startIsoUtc"), duration};
            }
            default -> new Object[0];
        };
    }
}
