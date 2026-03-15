package com.legalai.service;

import com.legalai.config.GeminiConfig;
import com.legalai.model.LegalDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final WebClient webClient;
    private final GeminiConfig geminiConfig;

    private static final String SYSTEM_PROMPT = """
            You are an expert Indian legal assistant.
            
            You must only answer questions related to Indian law.
            
            If the query is unrelated to law, respond:
            'I'm designed to assist only with legal questions related to Indian law.'
            
            Use the provided legal context when available.
            
            Keep answers concise, professional, and accurate.
            
            Limit the response to 250 tokens maximum.
            
            When citing sections, always mention the Act name and section number.
            
            Format your response clearly with proper structure.
            """;

    public GeminiService(WebClient geminiWebClient, GeminiConfig geminiConfig) {
        this.webClient = geminiWebClient;
        this.geminiConfig = geminiConfig;
    }

    public String generateResponse(String query, String conversationHistory, List<LegalDocument> context) {
        String contextText = formatContext(context);

        String prompt = String.format("""
                %s
                
                Conversation History:
                %s
                
                User Question:
                %s
                
                Legal Context:
                %s
                
                Provide a clear, accurate, and well-structured response based on the legal context above.
                Cite specific sections and acts when relevant.
                """, SYSTEM_PROMPT, conversationHistory, query, contextText);

        return callGeminiApi(prompt);
    }

    public String generateDiscussionResponse(String query, String conversationHistory) {
        String prompt = String.format("""
                %s
                
                Conversation History:
                %s
                
                User Question:
                %s
                
                The user wants a general discussion or explanation about a legal topic.
                Provide a comprehensive yet concise explanation.
                Reference relevant Indian laws and sections where applicable.
                """, SYSTEM_PROMPT, conversationHistory, query);

        return callGeminiApi(prompt);
    }

    private String callGeminiApi(String prompt) {
        try {
            String model = geminiConfig.getModel();
            String apiKey = geminiConfig.getApiKey();

            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "maxOutputTokens", 250,
                            "temperature", 0.2
                    )
            );

            String url = "/models/" + model + ":generateContent?key=" + apiKey;

            log.debug("Calling Gemini API with model: {}", model);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null) {
                log.error("Null response from Gemini API");
                return "I apologize, but I was unable to generate a response. Please try again.";
            }

            return extractTextFromResponse(response);

        } catch (Exception e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            return "I apologize, but I encountered an error while generating a response. " +
                   "Please check the API configuration and try again. Error: " + e.getMessage();
        }
    }

    @SuppressWarnings("unchecked")
    private String extractTextFromResponse(Map<String, Object> response) {
        try {
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                Map<String, Object> content = (Map<String, Object>) candidate.get("content");
                if (content != null) {
                    List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }

            // Check for error
            if (response.containsKey("error")) {
                Map<String, Object> error = (Map<String, Object>) response.get("error");
                String errorMsg = (String) error.getOrDefault("message", "Unknown error");
                log.error("Gemini API error: {}", errorMsg);
                return "API Error: " + errorMsg;
            }

            log.warn("Unexpected Gemini response format: {}", response);
            return "Unable to parse the response. Please try again.";

        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            return "Error parsing response. Please try again.";
        }
    }

    private String formatContext(List<LegalDocument> context) {
        if (context == null || context.isEmpty()) {
            return "No specific legal context available.";
        }

        StringBuilder sb = new StringBuilder();
        for (LegalDocument doc : context) {
            sb.append("--- ").append(doc.getActName())
              .append(" Section ").append(doc.getSectionNumber())
              .append(": ").append(doc.getTitle()).append(" ---\n");
            sb.append(doc.getContent()).append("\n\n");
        }
        return sb.toString();
    }
}
