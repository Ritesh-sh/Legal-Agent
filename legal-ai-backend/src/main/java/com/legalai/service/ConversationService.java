package com.legalai.service;

import com.legalai.model.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationService {

    private static final int MAX_HISTORY = 10;

    // Session-based conversation storage
    private final Map<String, List<ChatMessage>> sessions = new ConcurrentHashMap<>();

    public List<ChatMessage> getHistory(String sessionId) {
        return sessions.getOrDefault(sessionId, new ArrayList<>());
    }

    public void addMessage(String sessionId, ChatMessage message) {
        sessions.computeIfAbsent(sessionId, k -> Collections.synchronizedList(new ArrayList<>()));
        List<ChatMessage> history = sessions.get(sessionId);
        history.add(message);

        // Keep only last MAX_HISTORY messages
        if (history.size() > MAX_HISTORY) {
            // Remove oldest messages
            int excess = history.size() - MAX_HISTORY;
            for (int i = 0; i < excess; i++) {
                history.remove(0);
            }
        }
    }

    public void addUserMessage(String sessionId, String content) {
        addMessage(sessionId, new ChatMessage("user", content));
    }

    public void addAssistantMessage(String sessionId, String content) {
        addMessage(sessionId, new ChatMessage("assistant", content));
    }

    public String formatHistory(String sessionId) {
        List<ChatMessage> history = getHistory(sessionId);
        if (history.isEmpty()) return "No previous conversation.";

        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : history) {
            sb.append(msg.getRole().equals("user") ? "User: " : "Assistant: ");
            sb.append(msg.getContent());
            sb.append("\n");
        }
        return sb.toString();
    }

    public void clearSession(String sessionId) {
        sessions.remove(sessionId);
    }

    public boolean sessionExists(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public String getLastUserQuery(String sessionId) {
        List<ChatMessage> history = getHistory(sessionId);
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("user".equals(history.get(i).getRole())) {
                return history.get(i).getContent();
            }
        }
        return "";
    }
}
