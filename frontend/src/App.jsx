import React, { useEffect, useRef, useState } from 'react';

const DEFAULT_CLIENT_URL = 'http://localhost:8086';

function newConversationId() {
  return window.crypto?.randomUUID?.() || `${Date.now()}`;
}

export default function App() {
  const [clientUrl, setClientUrl] = useState(DEFAULT_CLIENT_URL);
  const [conversationId, setConversationId] = useState(newConversationId);
  const [prompt, setPrompt] = useState('');
  const [messages, setMessages] = useState([]);
  const [tools, setTools] = useState([]);
  const [model, setModel] = useState('');
  const [status, setStatus] = useState('Ready');
  const [sending, setSending] = useState(false);
  const logRef = useRef(null);

  const baseUrl = clientUrl.replace(/\/$/, '');

  useEffect(() => {
    loadConfig();
  }, [baseUrl]);

  useEffect(() => {
    if (logRef.current) {
      logRef.current.scrollTop = logRef.current.scrollHeight;
    }
  }, [messages]);

  async function loadConfig() {
    try {
      const [configResponse, toolsResponse] = await Promise.all([
        fetch(`${baseUrl}/api/config`),
        fetch(`${baseUrl}/api/tools`),
      ]);

      if (configResponse.ok) {
        const config = await configResponse.json();
        setModel(config.model || '');
      }
      setTools(toolsResponse.ok ? await toolsResponse.json() : []);
      setStatus('Ready');
    } catch (error) {
      setTools([]);
      setStatus(error?.message || 'Client unavailable');
    }
  }

  async function sendPrompt() {
    const text = prompt.trim();
    if (!text || sending) {
      return;
    }

    setPrompt('');
    setSending(true);
    setStatus('Sending');
    setMessages((current) => [...current, { role: 'You', text }, { role: 'Assistant', text: '' }]);

    try {
      const response = await fetch(`${baseUrl}/api/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt: text, conversationId }),
      });

      if (!response.ok) {
        throw new Error(await response.text());
      }

      const result = await response.json();
      setConversationId(result.conversationId || conversationId);
      setModel(result.model || model);
      setTools(Array.isArray(result.tools) ? result.tools : tools);
      setMessages((current) => current.map((message, index) => (
        index === current.length - 1 ? { ...message, text: result.answer || '' } : message
      )));
      setStatus('Ready');
    } catch (error) {
      setMessages((current) => current.map((message, index) => (
        index === current.length - 1
          ? { ...message, text: `Error: ${error?.message || 'Request failed'}` }
          : message
      )));
      setStatus('Error');
    } finally {
      setSending(false);
    }
  }

  function newChat() {
    setConversationId(newConversationId());
    setMessages([]);
    setStatus('Ready');
  }

  return (
    <main className="app">
      <header className="topbar">
        <div>
          <h1>MCP POC</h1>
          <p>{status}</p>
        </div>
        <button type="button" onClick={newChat}>New</button>
      </header>

      <section className="settings">
        <label>
          Client URL
          <input value={clientUrl} onChange={(event) => setClientUrl(event.target.value)} />
        </label>
        <div>
          <span>Model</span>
          <strong>{model || 'Not loaded'}</strong>
        </div>
        <div>
          <span>Tools</span>
          <strong>{tools.length ? tools.map((tool) => tool.name).join(', ') : 'None'}</strong>
        </div>
      </section>

      <section className="messages" ref={logRef}>
        {messages.length === 0 ? (
          <p className="empty">Ask for weather, ask to book a meeting, or ask a normal question.</p>
        ) : messages.map((message, index) => (
          <article className={message.role === 'You' ? 'message user' : 'message'} key={`${message.role}-${index}`}>
            <small>{message.role}</small>
            <p>{message.text}</p>
          </article>
        ))}
      </section>

      <footer className="composer">
        <textarea
          value={prompt}
          placeholder="Type a prompt"
          onChange={(event) => setPrompt(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter' && !event.shiftKey) {
              event.preventDefault();
              sendPrompt();
            }
          }}
        />
        <button type="button" disabled={sending} onClick={sendPrompt}>
          {sending ? '...' : 'Send'}
        </button>
      </footer>
    </main>
  );
}
