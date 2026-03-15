package com.legalai.model;

import java.time.Instant;

public class ChatMessage {
    private String role; // "user" or "assistant"
    private String content;
    private Instant timestamp;

    public ChatMessage() {
        this.timestamp = Instant.now();
    }

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = Instant.now();
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
