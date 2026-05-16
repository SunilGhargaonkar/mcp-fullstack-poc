# MCP POC Client

Small Spring Boot client service for the React UI.

Responsibilities:

- Connect to one OpenAI-compatible LLM.
- Discover MCP tools from `mcp-poc/backend`.
- Send user prompts to the LLM with tool choice set to `auto`.
- Let the LLM answer directly when no MCP tool is needed.

The implementation is intentionally compact:

- `McpClientApplication`: starts Spring Boot and configures CORS.
- `McpChatService`: creates the OpenAI-compatible chat model and MCP tool callbacks.
- `ChatController`: exposes the tiny API consumed by the React UI.

Default endpoints:

- Client API: `http://localhost:8086`
- MCP server: `http://localhost:8085/mcp`

## Configuration

```bash
export POC_LLM_API_KEY=<your-key>
export POC_LLM_BASE_URL=https://openrouter.ai/api
export POC_LLM_MODEL=deepseek/deepseek-r1-distill-llama-70b:free
export POC_MCP_ENDPOINT=http://localhost:8085/mcp
```

`POC_LLM_BASE_URL` should be the provider base URL. Do not set it to `/v1/chat/completions`; Spring AI adds the OpenAI-compatible chat path itself.

## Run

```bash
mvn spring-boot:run
```

Useful endpoints:

```bash
curl http://localhost:8086/api/config
curl http://localhost:8086/api/tools
curl -X POST http://localhost:8086/api/chat \
  -H "Content-Type: application/json" \
  -d '{"prompt":"What is the weather in London?"}'
```
