MCP POC
This project is split into three local pieces:

backend: pure Spring Boot MCP server at http://localhost:8085/mcp. It exposes only CalendarTool and WeatherTool.
frontend/mcp-client: lightweight Spring Boot MCP client at http://localhost:8086. It connects to the LLM and discovers tools from the backend MCP server.
frontend: lightweight React/Vite UI at http://localhost:5173. It sends prompts to frontend/mcp-client, not directly to the MCP server.
There is no ChatTool in the MCP server. General prompts are answered by the LLM in frontend/mcp-client when no MCP tool is appropriate.

Tools
getWeather: current weather for a city through OpenWeather.
bookEvent: Google Calendar booking with title, UTC start time, and duration.
Run Order
Start the MCP server:
cd backend
mvn spring-boot:run
Start the MCP client:
cd frontend/mcp-client
mvn spring-boot:run
Start the UI:
cd frontend
npm install
npm run dev
Open http://localhost:5173.

Configuration
Backend tool configuration lives in backend/src/main/resources/application.properties:

export OPENWEATHER_API_KEY=<your-openweather-key>
export GOOGLE_CREDENTIALS_PATH=src/main/resources/credentials.json
LLM and MCP client configuration lives in frontend/mcp-client/src/main/resources/application.properties:

export POC_LLM_API_KEY=<your-key>
export POC_LLM_BASE_URL=https://openrouter.ai/api
export POC_LLM_MODEL=deepseek/deepseek-r1-distill-llama-70b:free
export POC_MCP_ENDPOINT=http://localhost:8085/mcp
POC_LLM_BASE_URL should be the provider base URL, not the full /v1/chat/completions URL.