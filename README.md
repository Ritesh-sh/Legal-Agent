# 🏛️ Legal AI Assistant — Indian Law

A **full-stack conversational Agentic RAG Legal AI Assistant** for Indian laws, powered by **Gemma 4B** via Gemini API.

---

## 🚀 Features

- **📄 PDF Parsing** — Automatically scans and extracts sections from 23 Indian Act PDFs
- **🔍 BM25 Retrieval** — Fast, accurate retrieval using PageIndex + BM25 scoring
- **🤖 AI-Powered** — Generates answers using Gemma 4B via Gemini API
- **💬 Multi-turn Conversations** — Maintains conversation history per session (last 10 messages)
- **🛡️ Legal Intent Filter** — Rejects non-legal queries automatically
- **🎓 Google Scholar Links** — Provides academic research links for each query
- **⚡ Fast** — Retrieval < 50ms, total response < 2 seconds

---

## 📁 Project Structure

```
legal/
├── legal-ai-backend/          # Spring Boot Backend
│   ├── src/main/java/com/legalai/
│   │   ├── LegalAiApplication.java
│   │   ├── agent/
│   │   │   └── LegalAgent.java
│   │   ├── config/
│   │   │   ├── GeminiConfig.java
│   │   │   └── WebConfig.java
│   │   ├── controller/
│   │   │   └── QueryController.java
│   │   ├── model/
│   │   │   ├── ChatMessage.java
│   │   │   ├── LegalDocument.java
│   │   │   ├── QueryRequest.java
│   │   │   └── QueryResponse.java
│   │   └── service/
│   │       ├── BM25Service.java
│   │       ├── ConversationService.java
│   │       ├── DocumentService.java
│   │       ├── GeminiService.java
│   │       ├── IndexBuilderService.java
│   │       ├── IntentService.java
│   │       ├── PDFParserService.java
│   │       ├── RetrievalService.java
│   │       └── ScholarService.java
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── legal_acts/             # Place PDF files here
│   └── pom.xml
│
└── legal-ai-frontend/         # Vite + React Frontend
    ├── src/
    │   ├── components/
    │   │   ├── ChatWindow.jsx
    │   │   ├── MessageBubble.jsx
    │   │   └── SourcePanel.jsx
    │   ├── pages/
    │   │   ├── HomePage.jsx
    │   │   └── ChatPage.jsx
    │   ├── App.jsx
    │   ├── main.jsx
    │   └── index.css
    ├── index.html
    ├── vite.config.js
    └── package.json
```

---

## 🛠️ Setup Instructions

### Prerequisites

- **Java 21+** (JDK)
- **Maven 3.8+**
- **Node.js 18+** and **npm**
- **Gemini API Key** (from [Google AI Studio](https://aistudio.google.com/apikey))

### 1. Set Your Gemini API Key

Edit `legal-ai-backend/src/main/resources/application.properties`:

```properties
gemini.api.key=YOUR_ACTUAL_API_KEY_HERE
```

Or set it as an environment variable:

```bash
set GEMINI_API_KEY=your_api_key_here
```

### 2. Add Legal Act PDFs (Optional)

Place your 23 Indian Act PDFs into:

```
legal-ai-backend/legal_acts/
```

Example files: `IPC.pdf`, `CrPC.pdf`, `EvidenceAct.pdf`, `ContractAct.pdf`, etc.

> **Note:** If no PDFs are found, the system automatically creates **80+ comprehensive sample legal documents** covering IPC, CrPC, Evidence Act, Contract Act, Companies Act, IT Act, Consumer Protection Act, Hindu Marriage Act, Motor Vehicles Act, NDPS Act, RTI Act, Arbitration Act, Negotiable Instruments Act, Transfer of Property Act, Specific Relief Act, Dowry Prohibition Act, Domestic Violence Act, Limitation Act, and Constitution of India.

### 3. Start the Backend

```bash
cd legal-ai-backend
mvn spring-boot:run
```

Backend runs on **http://localhost:8080**

### 4. Start the Frontend

```bash
cd legal-ai-frontend
npm install
npm run dev
```

Frontend runs on **http://localhost:5173**

---

## 🔗 API Endpoints

### POST `/api/chat`

```json
// Request
{
  "sessionId": "session_123",
  "query": "What is the punishment for robbery?"
}

// Response
{
  "answer": "Under Section 392 of the Indian Penal Code...",
  "sources": ["Indian Penal Code Section 392"],
  "scholarLink": "https://scholar.google.com/scholar?q=robbery+Indian+law"
}
```

### GET `/api/health`

Returns system health and document count.

### GET `/api/stats`

Returns system statistics and feature flags.

---

## 🏗️ Architecture

```
React Frontend (Vite)
       ↓ POST /api/chat
Spring Boot API
       ↓
 Intent Filter ──→ Non-legal? → Rejection
       ↓
  Legal Agent (decides mode)
       ↓
 ┌─────┼─────┐
 ↓     ↓     ↓
Search Get   Scholar
Sections Section Search
 ↓     ↓     ↓
 └─────┼─────┘
       ↓
 BM25 Scoring
       ↓
 Gemma 4B (Gemini API)
       ↓
 Response + Sources
```

---

## 📋 Tech Stack

| Layer      | Technology              |
|------------|------------------------|
| Frontend   | Vite + React 18        |
| Backend    | Spring Boot 3.2 (Java 21) |
| LLM        | Gemma 4B via Gemini API |
| PDF Parsing| Apache PDFBox 3.x      |
| Retrieval  | PageIndex + BM25       |
| Styling    | Vanilla CSS (Dark Theme)|

---

## ⚡ Performance

| Metric           | Target    |
|-----------------|-----------|
| PDF Parsing      | Startup   |
| Retrieval        | < 50ms    |
| Total Response   | < 2s      |
| Max LLM Tokens   | 250       |

---

## 📝 License

For educational and informational purposes only.
