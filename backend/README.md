# MCP Server (SpringBoot AI)

Java based backend to interact with Chat Assistant via http://localhost:5173.

## Features

- MCP Server: Handles tool invocation and streams responses.
- Tool discovery: Uses @Tool annotation + reflection.
- Tools implemented:
    - CalendarTool → Google Calendar API integration
    - WeatherTool → OpenWeather API
    - ChatTool → OpenRouter LLM
- Streaming responses via Flux<String> (SSE).
- CORS enabled for http://localhost:5173.

### Prerequisites

- Java 21+
- Maven 3.9+
- Google Cloud Project with Calendar API enabled
- OpenRouter API key → https://openrouter.ai
- OpenWeather API key → https://openweathermap.org/api

## Quick start

1. Install dependencies:

```bash
cd backend
mvn clean install
```

2. Run the Application:

```bash
mvn spring-boot:run

```

3. Default MCP endpoint: http://localhost:8085/mcp.

## How it works

1. MCP server receives a Prompt from freontend.
2. LlmService decides the tool and returns the ToolDecision.
3. ToolInvoker then calls the appropriate tool depending up on LLM's decision.
4. Returns result as Flux<String> (for streaming)

## Example Request

```bash
curl -X POST http://localhost:8085/mcp \
-H "Content-Type: application/json" \
-d '{"prompt": "Book a meeting tomorrow at 3 PM"}'
```