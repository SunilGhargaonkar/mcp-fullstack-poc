package com.example.mcp.server.tools;

import java.lang.reflect.Method;
import java.util.Objects;

import com.example.mcp.server.model.ToolDecision;
import com.example.mcp.server.model.ToolMethod;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class ReflectionToolExecutor implements ToolExecutor {
    private final ToolRegistry registry;

    public ReflectionToolExecutor(ToolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean canExecute(String toolName) {
        return registry.getTool(toolName) != null;
    }

    @Override
    public Flux<String> execute(ToolDecision decision) {
        final ToolMethod toolMethod = registry.getTool(decision.toolName());
        if (toolMethod == null) {
            return Flux.just("Tool not found: " + decision.toolName());
        }

        try {
            final Method method = toolMethod.method();
            final Object bean = toolMethod.bean();

            final Object[] args = mapArguments(method, decision);

            for (int i = 0; i < args.length; i++) {
                if (args[i] == null) {
                    return Flux.just("Missing argument '" +
                                     method.getParameters()[i].getName() +
                                     "' for tool: " + decision.toolName());
                }
            }

            final Object result = method.invoke(bean, args);
            if (result instanceof Flux<?> flux) {
                return flux.map(Objects::toString);
            }
            if (result instanceof String str) {
                return Flux.just(str);
            }

            return Flux.just("Tool returned unknown type");

        } catch (Exception e) {
            return Flux.just("Error invoking tool: " + e.getMessage());
        }
    }

    private Object[] mapArguments(Method method, ToolDecision decision) {
        return java.util.Arrays.stream(method.getParameters())
                               .map(p -> decision.arguments().get(p.getName()))
                               .toArray();
    }
}
