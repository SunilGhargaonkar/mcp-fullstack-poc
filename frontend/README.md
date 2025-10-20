# MCP Chat Client UI (React + Vite)
React based frontend to interact with an MCP server.  
It supports three tools: 
- **Chat with LLM** (`askChat`)
- **Weather lookup** (`getWeather`)
- **Google Calendar booking** (`bookEvent`)

## Features

- Chat UI with **Markdown** support using `react-markdown` and `remark-gfm`.
- **Syntax highlighting** for code blocks using `react-syntax-highlighter`.
- **Streaming support**: partial responses displayed as they arrive.
- **Automatic scrolling** to latest messages.

## Quick start
### Prerequisites
- Node.js >= 18

1. Install dependencies:
```bash
cd frontend
npm install
```
3. Run the dev server:
```bash
npm run dev
```
4. Open http://localhost:5173 in your browser.


## How it connects to your MCP server
- By default the UI points to `http://localhost:8085/mcp`.


## How it works
1. User types a prompt.
`What is the weather in Lodon` Or
`Book a meeting in my calandar at 1pm on 25th Oct 2025 for Lunch` Or
`Explain me how LLM works?`
2. Backend streams responses back. 
3. Frontend appends chunks to the assistant message in real-time.
