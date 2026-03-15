package com.legalai.controller;

import com.legalai.agent.LegalAgent;
import com.legalai.model.QueryRequest;
import com.legalai.model.QueryResponse;
import com.legalai.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class QueryController {

    private static final Logger log = LoggerFactory.getLogger(QueryController.class);

    private final LegalAgent legalAgent;
    private final DocumentService documentService;

    public QueryController(LegalAgent legalAgent, DocumentService documentService) {
        this.legalAgent = legalAgent;
        this.documentService = documentService;
    }

    @PostMapping("/chat")
    public ResponseEntity<QueryResponse> chat(@RequestBody QueryRequest request) {
        log.info("Chat request - session: {}, query: {}", request.getSessionId(), request.getQuery());

        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new QueryResponse("Please provide a valid query.", null, null));
        }

        // Generate session ID if not provided
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        QueryResponse response = legalAgent.processQuery(sessionId, request.getQuery().trim());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "documents", documentService.getDocumentCount(),
                "service", "Legal AI Assistant"
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "totalDocuments", documentService.getDocumentCount(),
                "service", "Legal AI Assistant",
                "model", "gemma-3-4b-it",
                "features", Map.of(
                        "pdfParsing", true,
                        "bm25Retrieval", true,
                        "multiTurnConversation", true,
                        "intentFiltering", true
                )
        ));
    }
}
