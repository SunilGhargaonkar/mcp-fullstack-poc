# MCP POC

This project is a simple **Model Context Protocol (MCP)** proof of concept consisting of three local applications.

| Component | Description | URL |
|---|---|---|
| **backend** | Spring Boot MCP server exposing tool APIs | `http://localhost:8085/mcp` |
| **frontend/mcp-client** | Spring Boot MCP client connected to the LLM and MCP server | `http://localhost:8086` |
| **frontend** | React/Vite UI for interacting with the system | `http://localhost:5173` |

---

# Architecture Overview

```text
React UI (frontend)
        |
        v
MCP Client + LLM (frontend/mcp-client)
        |
        v
MCP Server (backend)
        |
        +--> WeatherTool
        +--> CalendarTool
```

## How It Works

- The **React UI** sends prompts to the **MCP client**
- The **MCP client** communicates with the LLM
- The LLM dynamically discovers and invokes tools exposed by the **MCP server**
- If no tool is needed, the MCP client answers directly using the LLM

> The MCP server does **not** contain any chat logic or ChatTool.

---

# Available Tools

## `getWeather`

Returns the current weather for a city using OpenWeather.

### Example

```text
What's the weather in London?
```

---

## `bookEvent`

Creates a Google Calendar event.

### Required Inputs

- Title
- UTC start time
- Duration

### Example

```text
Book a meeting tomorrow at 10:00 UTC for 30 minutes
```

---

# Running the Project

Start each service in the following order.

---

## 1. Start the MCP Server

```bash
cd backend
mvn spring-boot:run
```

Runs at:

```text
http://localhost:8085/mcp
```

---

## 2. Start the MCP Client

```bash
cd frontend/mcp-client
mvn spring-boot:run
```

Runs at:

```text
http://localhost:8086
```

---

## 3. Start the React UI

```bash
cd frontend
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

---

# Configuration

## Backend Configuration

File:

```text
backend/src/main/resources/application.properties
```

Required environment variables:

```bash
export OPENWEATHER_API_KEY=<your-openweather-key>
export GOOGLE_CREDENTIALS_PATH=src/main/resources/credentials.json
```

---

## MCP Client / LLM Configuration

File:

```text
frontend/mcp-client/src/main/resources/application.properties
```

Required environment variables:

```bash
export POC_LLM_API_KEY=<your-api-key>
export POC_LLM_BASE_URL=https://openrouter.ai/api
export POC_LLM_MODEL=deepseek/deepseek-r1-distill-llama-70b:free
export POC_MCP_ENDPOINT=http://localhost:8085/mcp
```

---

# Notes

- The MCP server exposes only:
    - `CalendarTool`
    - `WeatherTool`

- General conversational prompts are handled directly by the LLM in `frontend/mcp-client`

- The UI never communicates directly with the MCP server