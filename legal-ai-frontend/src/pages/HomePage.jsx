import React from 'react'
import { useNavigate } from 'react-router-dom'

const EXAMPLE_QUESTIONS = [
  { icon: '⚖️', text: 'What is Section 420 IPC?' },
  { icon: '📜', text: 'Explain contract law in India' },
  { icon: '🔨', text: 'Punishment for robbery?' },
  { icon: '🏛️', text: 'What is anticipatory bail?' },
  { icon: '💼', text: 'Duties of company directors' },
  { icon: '🛡️', text: 'Consumer protection rights' },
]

export default function HomePage() {
  const navigate = useNavigate()

  const handleStartChat = () => {
    navigate('/chat')
  }

  const handleExampleClick = (question) => {
    navigate('/chat', { state: { initialQuery: question } })
  }

  return (
    <div className="home-page">
      <div className="home-content">
        <div className="home-badge">
          <span className="home-badge-dot"></span>
          AI-Powered Legal Research
        </div>

        <h1 className="home-title">
          Your <span className="home-title-gradient">Legal AI</span>
          <br />Assistant
        </h1>

        <p className="home-subtitle">
          Ask questions, explore sections, and discuss Indian laws with an intelligent 
          assistant powered by advanced AI. Covers IPC, CrPC, Contract Act, Companies Act, 
          IT Act, and 18+ more Acts.
        </p>

        <div className="home-actions">
          <button className="btn btn-primary" onClick={handleStartChat} id="start-chat-btn">
            <span className="btn-icon">💬</span>
            Start Chat
          </button>
          <button className="btn btn-secondary" onClick={() => document.getElementById('examples').scrollIntoView({ behavior: 'smooth' })} id="examples-btn">
            <span className="btn-icon">📚</span>
            Example Questions
          </button>
        </div>
      </div>

      <div className="examples-section" id="examples">
        <div className="examples-title">Try asking about</div>
        <div className="examples-grid">
          {EXAMPLE_QUESTIONS.map((q, idx) => (
            <div
              key={idx}
              className="example-card"
              onClick={() => handleExampleClick(q.text)}
              id={`example-card-${idx}`}
            >
              <span className="example-icon">{q.icon}</span>
              {q.text}
            </div>
          ))}
        </div>
      </div>

      <footer className="home-footer">
        Legal AI Assistant — For educational and informational purposes only.
      </footer>
    </div>
  )
}
