import React, { useState, useRef, useEffect } from 'react';
import { v4 as uuidv4 } from 'uuid';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';

const DEFAULT_MCP_URL = 'http://localhost:8085/mcp';

function Message({ from, text, meta }) {

  const isUser = from === 'user';

  return (

    <div className="message" style={{display: 'flex', flexDirection: 'column', alignItems: isUser ? 'flex-end' : 'flex-start', marginBottom: 10,}}>

      <div className="meta" style={{fontSize: 12, marginBottom: 4, color: '#555', fontWeight: 'bold', textAlign: isUser ? 'right' : 'left', }}>
        {meta}
      </div>
      <div className={from === 'user' ? 'user' : 'assistant'}>
        <ReactMarkdown
          remarkPlugins={[remarkGfm]}
          components={{
            code({ node, inline, className, children, ...props }) {
              const match = /language-(\w+)/.exec(className || '');
              return !inline && match ? (
                <SyntaxHighlighter
                  style={oneLight}
                  language={match[1]}
                  PreTag="div"
                  {...props}
                >
                  {String(children).replace(/\n$/, '')}
                </SyntaxHighlighter>
              ) : (
                <code
                  className={className}
                  {...props}
                  style={{ background: '#f1f1f1', padding: '2px 4px', borderRadius: '4px' }}
                >
                  {children}
                </code>
              );
            },
            a({ node, ...props }) {
              return (
                <a {...props} style={{ color: '#1a73e8' }} target="_blank" rel="noopener noreferrer" />
              );
            },
            strong({ node, ...props }) {
              return <strong style={{ fontWeight: 600 }} {...props} />;
            },
            em({ node, ...props }) {
              return <em style={{ fontStyle: 'italic' }} {...props} />;
            },
          }}
        >
          {text}
        </ReactMarkdown>
      </div>
    </div>
  );
}

export default function App() {
  const [mcpUrl, setMcpUrl] = useState(DEFAULT_MCP_URL);
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState('');

  const containerRef = useRef(null);
  const textareaRef = useRef(null);

  useEffect(() => {
    if (containerRef.current) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight;
    }
  }, [messages]);

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = textareaRef.current.scrollHeight + 'px';
    }
  }, [input]);

  const sendRequest = async (prompt) => {
    const id = uuidv4();
    setMessages((prev) => [...prev, { from: 'user', text: prompt, meta: 'You' }]);
    setInput('');

    // Add placeholder for assistant message
    setMessages((prev) => [...prev, { from: 'assistant', text: '', meta: 'assistant', typing: true }]);
    const placeholderIndex = messages.length + 1;

    try {
      const res = await fetch(mcpUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ prompt }),
      });

      const textContent = await res.text();

      let contentToShow = textContent;
      try {
        const parsed = JSON.parse(textContent);
        if (Array.isArray(parsed)) {
          contentToShow = parsed.join(' ');
        } else if (typeof parsed === 'string') {
          contentToShow = parsed;
        }
      } catch {
        // not JSON, keep as-is
      }

      let i = 0;
      const interval = setInterval(() => {
        if (i > contentToShow.length) {
          clearInterval(interval);
          setMessages((prev) =>
            prev.map((msg, idx) =>
              idx === placeholderIndex ? { ...msg, typing: false } : msg
            )
          );
          return;
        }
        setMessages((prev) =>
          prev.map((msg, idx) =>
            idx === placeholderIndex
              ? { ...msg, text: contentToShow.slice(0, i), typing: true }
              : msg
          )
        );
        i += 2;
      }, 30);

    } catch (err) {
      setMessages((prev) =>
        prev.map((msg, idx) =>
          idx === placeholderIndex
            ? { ...msg, text: `Error: ${err?.message || 'Unknown error'}`, typing: false }
            : msg
        )
      );
    }
  };

  const handleSend = () => {
    if (!input.trim()) return;
    sendRequest(input.trim());
  };

  return (
    <div className="app">
      <div className="header">
        <div className="logo">M</div>
        <div>
          <div className="title">MCP Chat Client UI</div>
        </div>
      </div>

      <div style={{ marginBottom: 12 }}>
        <label className="small">MCP Server URL: </label>
        <input
          style={{ width: '60%' }}
          value={mcpUrl}
          onChange={(e) => setMcpUrl(e.target.value)}
        />
      </div>

      <div className="container">
        <div className="messages" ref={containerRef}>
          {messages.map((m, i) => (
            <Message key={i} from={m.from} text={m.text} meta={m.meta}/>
          ))}
        </div>

        <div className="input-area">
          <textarea
            ref={textareaRef}
            placeholder="Type your question or prompt..."
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                handleSend();
              }
            }}
            rows={1}
          />
          <button className="send-btn" onClick={handleSend}>
            Send
          </button>
        </div>
      </div>
    </div>
  );
}
