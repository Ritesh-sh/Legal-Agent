import React from 'react'

export default function SourcePanel({ sources, scholarLink }) {
  if ((!sources || sources.length === 0) && !scholarLink) {
    return null
  }

  return (
    <div className="source-panel">
      {sources && sources.map((source, idx) => (
        <span key={idx} className="source-tag" title={source}>
          <span className="source-tag-icon">📄</span>
          {source}
        </span>
      ))}
      {scholarLink && (
        <a
          href={scholarLink}
          target="_blank"
          rel="noopener noreferrer"
          className="scholar-link"
          id="scholar-link"
        >
          <span className="source-tag-icon">🎓</span>
          Google Scholar
        </a>
      )}
    </div>
  )
}
