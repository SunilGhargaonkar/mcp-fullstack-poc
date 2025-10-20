package com.example.mcp.server.tools;

import com.example.mcp.server.model.ToolMethod;
import lombok.Getter;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Component
public class ToolRegistry implements SmartInitializingSingleton {
    @Getter
    private final Map<String, ToolMethod> tools = new HashMap<>();
    private final ApplicationContext context;

    public ToolRegistry(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (String beanName : context.getBeanDefinitionNames()) {
            Object bean = context.getBean(beanName);
            for (Method method : bean.getClass().getMethods()) {
                if (method.isAnnotationPresent(Tool.class)) {
                    Tool toolAnnotation = method.getAnnotation(Tool.class);
                    tools.put(toolAnnotation.name(), new ToolMethod(bean, method));
                }
            }
        }
    }

    public ToolMethod getTool(String name) {
        return tools.get(name);
    }
}
