# MCP POC Frontend

This folder contains a lightweight UI/client split:

- React/Vite UI in `src`. It uses only React, React DOM, and Vite.
- Small Spring Boot MCP client in `mcp-client`. It keeps the POC logic in a few classes instead of mirroring the larger `mcp-client` repo.

The UI sends prompts to the MCP client at `http://localhost:8086/api/chat`. The MCP client connects to the LLM, discovers tools from the backend MCP server at `http://localhost:8085/mcp`, and lets the model choose `getWeather` or `bookEvent` automatically.

## Run MCP Client

```bash
cd mcp-client
mvn spring-boot:run
```

Set LLM configuration here, not in `backend`:

```bash
export POC_LLM_API_KEY=<your-key>
export POC_LLM_BASE_URL=https://openrouter.ai/api
export POC_LLM_MODEL=deepseek/deepseek-r1-distill-llama-70b:free
export POC_MCP_ENDPOINT=http://localhost:8085/mcp
```

Use a provider base URL for `POC_LLM_BASE_URL`, not the full `/v1/chat/completions` endpoint. For Grok later, use the xAI-compatible base URL and model name.

## Run UI

```bash
npm install
npm run dev
```

Open `http://localhost:5173`.
