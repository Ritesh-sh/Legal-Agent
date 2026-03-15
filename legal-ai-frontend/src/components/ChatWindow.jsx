import React, { useRef, useEffect } from 'react'
import MessageBubble from './MessageBubble.jsx'

const WELCOME_EXAMPLES = [
  'What is Section 420 IPC?',
  'Explain anticipatory bail',
  'Punishment for robbery',
  'Consumer protection rights',
]

export default function ChatWindow({ messages, isLoading, onExampleClick }) {
  const bottomRef = useRef(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isLoading])

  return (
    <div className="chat-window" id="chat-window">
      {messages.length === 0 && !isLoading ? (
        <div className="chat-welcome">
          <div className="chat-welcome-icon">⚖️</div>
          <h2>How can I help you today?</h2>
          <p>Ask about Indian laws, legal sections, punishments, rights, or any legal topic.</p>
          <div className="chat-welcome-examples">
            {WELCOME_EXAMPLES.map((q, idx) => (
              <button
                key={idx}
                className="chat-welcome-tag"
                onClick={() => onExampleClick(q)}
                id={`welcome-tag-${idx}`}
              >
                {q}
              </button>
            ))}
          </div>
        </div>
      ) : (
        <>
          {messages.map((msg) => (
            <MessageBubble key={msg.id} message={msg} />
          ))}
          {isLoading && (
            <div className="typing-indicator">
              <div className="typing-avatar">⚖️</div>
              <div className="typing-dots">
                <div className="typing-dot"></div>
                <div className="typing-dot"></div>
                <div className="typing-dot"></div>
              </div>
            </div>
          )}
        </>
      )}
      <div ref={bottomRef} />
    </div>
  )
}
