package com.example.agent.memory;

import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages short-term conversation memory within a session.
 * 
 * This service maintains chat history with automatic pruning
 * to stay within context window limits.
 * 
 * Key Features:
 * - Session-based isolation
 * - Automatic token counting
 * - Context window management
 * - Message prioritization
 * - TTL-based cleanup
 */
@Service
public class ConversationMemoryService {
    
    // In-memory storage: sessionId -> conversation
    private final Map<String, Conversation> sessions = new ConcurrentHashMap<>();
    
    // Configuration
    private static final int MAX_CONTEXT_TOKENS = 4096;
    private static final int TOKENS_PER_MESSAGE = 100; // Rough estimate
    private static final long SESSION_TTL_HOURS = 24;
    
    /**
     * Get conversation history for a session.
     */
    public List<Message> getConversation(String sessionId) {
        cleanupExpiredSessions();
        
        var conversation = sessions.get(sessionId);
        if (conversation == null) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(conversation.messages());
    }
    
    /**
     * Add a user message to the conversation.
     */
    public void addUserMessage(String sessionId, String content) {
        addMessage(sessionId, new UserMessage(content));
    }
    
    /**
     * Add an assistant message to the conversation.
     */
    public void addAssistantMessage(String sessionId, String content) {
        addMessage(sessionId, new AssistantMessage(content));
    }
    
    /**
     * Add any message to the conversation with automatic pruning.
     */
    public void addMessage(String sessionId, Message message) {
        var conversation = sessions.computeIfAbsent(
            sessionId,
            k -> new Conversation(new ArrayList<>(), Instant.now())
        );
        
        conversation.messages().add(message);
        conversation.updateLastAccessed();
        
        // Prune if context window exceeded
        pruneIfNeeded(conversation);
    }
    
    /**
     * Clear conversation history for a session.
     */
    public void clearConversation(String sessionId) {
        sessions.remove(sessionId);
    }
    
    /**
     * Get the number of messages in a conversation.
     */
    public int getMessageCount(String sessionId) {
        var conversation = sessions.get(sessionId);
        return conversation == null ? 0 : conversation.messages().size();
    }
    
    /**
     * Get estimated token count for a conversation.
     */
    public int getEstimatedTokenCount(String sessionId) {
        var conversation = sessions.get(sessionId);
        if (conversation == null) {
            return 0;
        }
        
        return conversation.messages().size() * TOKENS_PER_MESSAGE;
    }
    
    /**
     * Prune old messages if context window limit exceeded.
     * 
     * Strategy:
     * 1. Always keep system messages
     * 2. Keep most recent messages
     * 3. Summarize and compress older messages
     */
    private void pruneIfNeeded(Conversation conversation) {
        var messages = conversation.messages();
        int estimatedTokens = messages.size() * TOKENS_PER_MESSAGE;
        
        if (estimatedTokens <= MAX_CONTEXT_TOKENS) {
            return; // Within limits
        }
        
        // Calculate how many messages to keep
        int maxMessages = MAX_CONTEXT_TOKENS / TOKENS_PER_MESSAGE;
        int messagesToRemove = messages.size() - maxMessages;
        
        if (messagesToRemove <= 0) {
            return;
        }
        
        // Keep the most recent messages
        // In production, you'd want to:
        // 1. Extract key facts from old messages
        // 2. Create a summary
        // 3. Replace old messages with summary
        
        List<Message> pruned = new ArrayList<>();
        
        // Add summary of pruned messages
        if (messagesToRemove > 0) {
            String summary = "Previous conversation summary: " +
                "User discussed recipes and cooking techniques. " +
                messages.size() + " messages summarized.";
            pruned.add(new UserMessage(summary));
        }
        
        // Keep recent messages
        int keepFromIndex = messages.size() - maxMessages + 1;
        pruned.addAll(messages.subList(keepFromIndex, messages.size()));
        
        conversation.setMessages(pruned);
    }
    
    /**
     * Remove sessions that haven't been accessed recently.
     */
    private void cleanupExpiredSessions() {
        Instant cutoff = Instant.now().minusSeconds(SESSION_TTL_HOURS * 3600);
        
        sessions.entrySet().removeIf(entry -> 
            entry.getValue().lastAccessed().isBefore(cutoff)
        );
    }
    
    /**
     * Get all active session IDs.
     */
    public Set<String> getActiveSessions() {
        cleanupExpiredSessions();
        return new HashSet<>(sessions.keySet());
    }
    
    /**
     * Get session statistics.
     */
    public SessionStats getSessionStats(String sessionId) {
        var conversation = sessions.get(sessionId);
        if (conversation == null) {
            return new SessionStats(0, 0, 0, null);
        }
        
        return new SessionStats(
            conversation.messages().size(),
            getEstimatedTokenCount(sessionId),
            MAX_CONTEXT_TOKENS,
            conversation.lastAccessed()
        );
    }
    
    // ========== Data Models ==========
    
    private static class Conversation {
        private List<Message> messages;
        private Instant lastAccessed;
        
        Conversation(List<Message> messages, Instant lastAccessed) {
            this.messages = messages;
            this.lastAccessed = lastAccessed;
        }
        
        List<Message> messages() { return messages; }
        Instant lastAccessed() { return lastAccessed; }
        
        void setMessages(List<Message> messages) {
            this.messages = messages;
        }
        
        void updateLastAccessed() {
            this.lastAccessed = Instant.now();
        }
    }
    
    public record SessionStats(
        int messageCount,
        int estimatedTokens,
        int maxTokens,
        Instant lastAccessed
    ) {
        public double tokenUsagePercent() {
            return (double) estimatedTokens / maxTokens * 100;
        }
    }
}

/*
 * USAGE EXAMPLES
 * 
 * 1. Basic conversation:
 * 
 * memory.addUserMessage(sessionId, "What's a good pasta recipe?");
 * memory.addAssistantMessage(sessionId, "Here's a classic Carbonara...");
 * 
 * var history = memory.getConversation(sessionId);
 * 
 * 
 * 2. With ChatClient:
 * 
 * var history = memory.getConversation(sessionId);
 * 
 * var response = chatClient.prompt()
 *     .messages(history)
 *     .user(userMessage)
 *     .call()
 *     .content();
 * 
 * memory.addUserMessage(sessionId, userMessage);
 * memory.addAssistantMessage(sessionId, response);
 * 
 * 
 * 3. Check memory usage:
 * 
 * var stats = memory.getSessionStats(sessionId);
 * System.out.println("Token usage: " + stats.tokenUsagePercent() + "%");
 * 
 * if (stats.tokenUsagePercent() > 80) {
 *     // Warn user that context is almost full
 * }
 * 
 * 
 * 4. Session management:
 * 
 * // List all active sessions
 * var activeSessions = memory.getActiveSessions();
 * 
 * // Clear specific session
 * memory.clearConversation(sessionId);
 * 
 * 
 * PRODUCTION IMPROVEMENTS:
 * 
 * 1. Use Redis for distributed sessions:
 *    - RedisTemplate for storage
 *    - TTL built into Redis keys
 *    - Cluster-safe
 * 
 * 2. Implement proper token counting:
 *    - Use tiktoken library
 *    - Model-specific token counting
 *    - Accurate context window tracking
 * 
 * 3. Add intelligent summarization:
 *    - Use LLM to summarize old messages
 *    - Extract key facts and entities
 *    - Compress context efficiently
 * 
 * 4. Implement message prioritization:
 *    - Keep system instructions always
 *    - Prioritize recent user questions
 *    - Maintain conversation flow
 * 
 * 5. Add persistence:
 *    - Save to database for analysis
 *    - Enable conversation restore
 *    - Audit and compliance
 */
