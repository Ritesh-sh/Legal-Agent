import React from 'react'
import SourcePanel from './SourcePanel.jsx'

export default function MessageBubble({ message }) {
  const { role, content, sources, scholarLink, timestamp } = message
  const isUser = role === 'user'

  const formatTime = (date) => {
    if (!date) return ''
    const d = new Date(date)
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  }

  // Simple markdown-like rendering for assistant messages
  const renderContent = (text) => {
    if (isUser) return text

    // Process markdown-like formatting
    const lines = text.split('\n')
    const elements = []
    let inList = false

    lines.forEach((line, idx) => {
      const trimmed = line.trim()

      if (trimmed.startsWith('**') && trimmed.endsWith('**')) {
        // Bold heading
        elements.push(
          <p key={idx} style={{ marginTop: idx > 0 ? '0.5em' : 0 }}>
            <strong>{trimmed.replace(/\*\*/g, '')}</strong>
          </p>
        )
      } else if (trimmed.startsWith('- ') || trimmed.startsWith('• ')) {
        // List item
        if (!inList) {
          inList = true
        }
        elements.push(
          <li key={idx}>{formatInlineText(trimmed.substring(2))}</li>
        )
      } else if (trimmed.startsWith('* ')) {
        if (!inList) {
          inList = true
        }
        elements.push(
          <li key={idx}>{formatInlineText(trimmed.substring(2))}</li>
        )
      } else {
        if (inList) {
          inList = false
        }
        if (trimmed) {
          elements.push(
            <p key={idx}>{formatInlineText(trimmed)}</p>
          )
        }
      }
    })

    // Wrap list items
    const result = []
    let currentList = []

    elements.forEach((el, idx) => {
      if (el.type === 'li') {
        currentList.push(el)
      } else {
        if (currentList.length > 0) {
          result.push(<ul key={`list-${idx}`}>{currentList}</ul>)
          currentList = []
        }
        result.push(el)
      }
    })

    if (currentList.length > 0) {
      result.push(<ul key="list-end">{currentList}</ul>)
    }

    return result
  }

  const formatInlineText = (text) => {
    // Handle bold text **text**
    const parts = text.split(/(\*\*[^*]+\*\*)/g)
    return parts.map((part, idx) => {
      if (part.startsWith('**') && part.endsWith('**')) {
        return <strong key={idx}>{part.replace(/\*\*/g, '')}</strong>
      }
      // Handle inline code `code`
      const codeParts = part.split(/(`[^`]+`)/g)
      return codeParts.map((cp, cidx) => {
        if (cp.startsWith('`') && cp.endsWith('`')) {
          return <code key={`${idx}-${cidx}`}>{cp.replace(/`/g, '')}</code>
        }
        return cp
      })
    })
  }

  return (
    <div className={`message-group ${role}`}>
      <div className={`message-avatar ${role}`}>
        {isUser ? '👤' : '⚖️'}
      </div>
      <div className="message-content">
        <div className={`message-bubble ${role}`}>
          {renderContent(content)}
        </div>
        {!isUser && sources && sources.length > 0 && (
          <SourcePanel sources={sources} scholarLink={scholarLink} />
        )}
        <span className="message-time">{formatTime(timestamp)}</span>
      </div>
    </div>
  )
}
