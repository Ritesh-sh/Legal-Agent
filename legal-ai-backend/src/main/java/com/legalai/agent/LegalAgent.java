package com.legalai.agent;

import com.legalai.model.LegalDocument;
import com.legalai.model.QueryResponse;
import com.legalai.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class LegalAgent {

    private static final Logger log = LoggerFactory.getLogger(LegalAgent.class);

    private final IntentService intentService;
    private final ConversationService conversationService;
    private final RetrievalService retrievalService;
    private final GeminiService geminiService;
    private final ScholarService scholarService;

    // Patterns to detect specific section queries
    private static final Pattern SECTION_QUERY_PATTERN = Pattern.compile(
            "(?i)(?:section|sec\\.?)\\s+(\\d+[A-Za-z]?)|" +
            "(?:ipc|crpc|cpc|bns)\\s+(\\d+[A-Za-z]?)|" +
            "(?:article)\\s+(\\d+[A-Za-z]?)"
    );

    // Patterns for discussion mode
    private static final Pattern DISCUSSION_PATTERN = Pattern.compile(
            "(?i)(explain|discuss|tell\\s+me\\s+about|describe|overview|" +
            "what\\s+is\\s+(?!section|sec|article)\\w+\\s+law|" +
            "what\\s+are\\s+the\\s+(?:types|kinds|categories)|" +
            "how\\s+does\\s+.*?law\\s+work|" +
            "difference\\s+between)"
    );

    public LegalAgent(IntentService intentService, ConversationService conversationService,
                      RetrievalService retrievalService, GeminiService geminiService,
                      ScholarService scholarService) {
        this.intentService = intentService;
        this.conversationService = conversationService;
        this.retrievalService = retrievalService;
        this.geminiService = geminiService;
        this.scholarService = scholarService;
    }

    /**
     * Main entry point: process a user query.
     */
    public QueryResponse processQuery(String sessionId, String query) {
        log.info("Processing query for session {}: {}", sessionId, query);

        // Step 1: Augment query with previous context for search/intent
        String lastQuery = conversationService.getLastUserQuery(sessionId);
        String searchQuery = query;

        // If this is a short follow-up question and we have history, augment it!
        if (lastQuery != null && !lastQuery.trim().isEmpty() && query.split("\\s+").length < 15) {
            searchQuery = query + " " + lastQuery;
            log.info("Augmented search query with context: {}", searchQuery);
        }

        // Step 2: Check if query is legal (using augmented search query)
        if (!intentService.isLegalQuery(searchQuery)) {
            log.info("Non-legal query detected: {}", query);
            return new QueryResponse(
                    IntentService.REJECTION_MESSAGE,
                    List.of(),
                    null
            );
        }

        // Step 3: Add original user message to conversation history
        conversationService.addUserMessage(sessionId, query);
        String history = conversationService.formatHistory(sessionId);

        // Step 4: Determine mode and select tools
        QueryResponse response;
        if (isSpecificSectionQuery(query)) {
            log.info("Mode: Legal Answer (specific section query)");
            response = handleLegalAnswerMode(query, searchQuery, history);
        } else if (isDiscussionQuery(query)) {
            log.info("Mode: Discussion");
            response = handleDiscussionMode(query, searchQuery, history);
        } else {
            log.info("Mode: Legal Answer (general legal query)");
            response = handleLegalAnswerMode(query, searchQuery, history);
        }

        // Step 5: Add assistant response to history
        conversationService.addAssistantMessage(sessionId, response.getAnswer());

        return response;
    }

    /**
     * Legal Answer Mode: retrieve sections, rank with BM25, generate answer.
     */
    private QueryResponse handleLegalAnswerMode(String originalQuery, String searchQuery, String history) {
        // Tool 1: Try direct section lookup (using original query)
        LegalDocument directSection = tryDirectSectionLookup(originalQuery);
        List<LegalDocument> retrievedDocs = new ArrayList<>();

        if (directSection != null) {
            retrievedDocs.add(directSection);
        }

        // Tool 2: Search sections using BM25 (using augmented search query)
        List<LegalDocument> searchResults = retrievalService.searchSections(searchQuery);
        for (LegalDocument doc : searchResults) {
            if (retrievedDocs.stream().noneMatch(d -> d.getId().equals(doc.getId()))) {
                retrievedDocs.add(doc);
            }
        }

        // Limit to top 3
        if (retrievedDocs.size() > 3) {
            retrievedDocs = retrievedDocs.subList(0, 3);
        }

        // Generate answer using LLM (using original query for the prompt)
        String answer = geminiService.generateResponse(originalQuery, history, retrievedDocs);

        // Extract source references
        List<String> sources = retrievedDocs.stream()
                .map(doc -> doc.getActName() + " Section " + doc.getSectionNumber())
                .collect(Collectors.toList());

        // Tool 3: Google Scholar link
        String scholarLink = scholarService.getScholarLink(searchQuery);

        return new QueryResponse(answer, sources, scholarLink);
    }

    /**
     * Discussion Mode: generate conversational explanation with optional context.
     */
    private QueryResponse handleDiscussionMode(String originalQuery, String searchQuery, String history) {
        // Optionally retrieve some context using augmented query
        List<LegalDocument> context = retrievalService.searchSections(searchQuery);

        String answer;
        if (!context.isEmpty()) {
            answer = geminiService.generateResponse(originalQuery, history, context);
        } else {
            answer = geminiService.generateDiscussionResponse(originalQuery, history);
        }

        List<String> sources = context.stream()
                .map(doc -> doc.getActName() + " Section " + doc.getSectionNumber())
                .collect(Collectors.toList());

        String scholarLink = scholarService.getScholarLink(searchQuery);

        return new QueryResponse(answer, sources, scholarLink);
    }

    /**
     * Try to extract a specific section number from the query and look it up directly.
     */
    private LegalDocument tryDirectSectionLookup(String query) {
        Matcher matcher = SECTION_QUERY_PATTERN.matcher(query);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String sectionNum = matcher.group(i);
                if (sectionNum != null) {
                    LegalDocument doc = retrievalService.getSection(sectionNum);
                    if (doc != null) {
                        log.info("Direct section lookup found: {}", doc.getId());
                        return doc;
                    }
                }
            }
        }
        return null;
    }

    private boolean isSpecificSectionQuery(String query) {
        return SECTION_QUERY_PATTERN.matcher(query).find();
    }

    private boolean isDiscussionQuery(String query) {
        return DISCUSSION_PATTERN.matcher(query).find();
    }
}
