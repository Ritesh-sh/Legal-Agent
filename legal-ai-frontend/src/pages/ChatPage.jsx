import React, { useState, useRef, useEffect, useCallback } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import ChatWindow from '../components/ChatWindow.jsx'

const API_URL = '/api/chat'

function generateSessionId() {
  return 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
}

export default function ChatPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [messages, setMessages] = useState([])
  const [inputValue, setInputValue] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [sessionId] = useState(() => generateSessionId())
  const textareaRef = useRef(null)
  const initialQueryHandled = useRef(false)

  // Handle initial query from home page
  useEffect(() => {
    if (location.state?.initialQuery && !initialQueryHandled.current) {
      initialQueryHandled.current = true
      handleSendMessage(location.state.initialQuery)
    }
  }, [location.state])

  const handleSendMessage = useCallback(async (messageText) => {
    const query = messageText || inputValue.trim()
    if (!query || isLoading) return

    setInputValue('')

    // Add user message
    const userMessage = {
      id: Date.now(),
      role: 'user',
      content: query,
      timestamp: new Date()
    }
    setMessages(prev => [...prev, userMessage])
    setIsLoading(true)

    try {
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId, query })
      })

      if (!response.ok) {
        throw new Error(`Server error: ${response.status}`)
      }

      const data = await response.json()

      const assistantMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: data.answer || 'No response received.',
        sources: data.sources || [],
        scholarLink: data.scholarLink || null,
        timestamp: new Date()
      }
      setMessages(prev => [...prev, assistantMessage])
    } catch (error) {
      console.error('Error:', error)
      const errorMessage = {
        id: Date.now() + 1,
        role: 'assistant',
        content: `I apologize, but I encountered a connection error. Please ensure the backend server is running on port 8080 and try again.\n\nError: ${error.message}`,
        sources: [],
        scholarLink: null,
        timestamp: new Date()
      }
      setMessages(prev => [...prev, errorMessage])
    } finally {
      setIsLoading(false)
    }
  }, [inputValue, isLoading, sessionId])

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSendMessage()
    }
  }

  // Auto-resize textarea
  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
      textareaRef.current.style.height = textareaRef.current.scrollHeight + 'px'
    }
  }, [inputValue])

  const handleExampleClick = (question) => {
    handleSendMessage(question)
  }

  return (
    <div className="chat-page">
      {/* Header */}
      <header className="chat-header">
        <div className="chat-header-left">
          <button className="chat-header-back" onClick={() => navigate('/')} id="back-btn" title="Back to Home">
            ←
          </button>
          <div className="chat-header-logo">
            <div className="chat-header-icon">⚖️</div>
            <div>
              <div className="chat-header-title">Legal AI Assistant</div>
              <div className="chat-header-subtitle">Indian Law Expert</div>
            </div>
          </div>
        </div>
        <div className="chat-header-status">
          <span className="status-dot"></span>
          Online
        </div>
      </header>

      {/* Chat Window */}
      <ChatWindow
        messages={messages}
        isLoading={isLoading}
        onExampleClick={handleExampleClick}
      />

      {/* Input Area */}
      <div className="chat-input-area">
        <div className="chat-input-container">
          <textarea
            ref={textareaRef}
            className="chat-input"
            placeholder="Ask about Indian laws, sections, or legal topics..."
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            onKeyDown={handleKeyDown}
            rows={1}
            disabled={isLoading}
            id="chat-input"
          />
          <button
            className="chat-send-btn"
            onClick={() => handleSendMessage()}
            disabled={!inputValue.trim() || isLoading}
            id="send-btn"
            title="Send message"
          >
            ➤
          </button>
        </div>
        <div className="chat-input-hint">
          Press Enter to send • Shift+Enter for new line
        </div>
      </div>
    </div>
  )
}
