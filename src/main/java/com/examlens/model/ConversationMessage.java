package com.examlens.model;

import java.time.LocalDateTime;

public class ConversationMessage {
    private long id;
    private long conversationId;
    private String role;
    private String content;
    private String intent;
    private Long aiJobId;
    private LocalDateTime createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getConversationId() { return conversationId; }
    public void setConversationId(long conversationId) { this.conversationId = conversationId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public Long getAiJobId() { return aiJobId; }
    public void setAiJobId(Long aiJobId) { this.aiJobId = aiJobId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isUser() { return "USER".equals(role); }
}
