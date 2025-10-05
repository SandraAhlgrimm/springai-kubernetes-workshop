package com.example.streaming;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Example of streaming LLM responses in real-time.
 * 
 * Streaming is essential for:
 * - Long-form content generation
 * - Improved user experience (progressive display)
 * - Lower perceived latency
 */
@RestController
class StreamingRecipeController {
    
    private final ChatClient chatClient;
    
    StreamingRecipeController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    
    /**
     * Stream recipe generation token by token.
     * 
     * The TEXT_EVENT_STREAM media type enables Server-Sent Events (SSE),
     * which pushes updates to the client as they become available.
     * 
     * Usage:
     * curl -N "http://localhost:8080/api/recipes/stream?ingredients=pasta,tomatoes"
     */
    @GetMapping(value = "/api/recipes/stream", 
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamRecipe(@RequestParam String ingredients) {
        return chatClient.prompt()
            .user(us -> us
                .text("""
                    Create a detailed recipe using: {ingredients}
                    Include prep time, ingredients, and step-by-step instructions.
                    """)
                .param("ingredients", ingredients))
            .stream()
            .content();
    }
    
    /**
     * Stream with chat memory for conversation context.
     * 
     * This example maintains conversation history across multiple
     * streaming requests within the same session.
     */
    @GetMapping(value = "/api/recipes/stream-conversation",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamConversation(
            @RequestParam String message,
            @RequestParam(required = false) String conversationId) {
        
        return chatClient.prompt()
            .user(message)
            // Add conversation memory here if implemented
            .stream()
            .content();
    }
    
    /**
     * Stream structured data (JSON chunks).
     * 
     * Useful when you want progressive structured output,
     * e.g., streaming a recipe object field by field.
     */
    @GetMapping(value = "/api/recipes/stream-json",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamRecipeJson(@RequestParam String ingredients) {
        return chatClient.prompt()
            .user("""
                Generate a recipe JSON for: {ingredients}
                Format: {"name": "...", "ingredients": [...], "instructions": [...]}
                """)
            .param("ingredients", ingredients)
            .stream()
            .content();
    }
}

/*
 * CLIENT-SIDE USAGE EXAMPLES
 * 
 * 1. JavaScript EventSource API:
 * 
 * const eventSource = new EventSource('/api/recipes/stream?ingredients=pasta,tomatoes');
 * 
 * eventSource.onmessage = (event) => {
 *     document.getElementById('recipe').innerHTML += event.data;
 * };
 * 
 * eventSource.onerror = () => {
 *     console.error('Stream error');
 *     eventSource.close();
 * };
 * 
 * 
 * 2. Fetch API with streaming:
 * 
 * const response = await fetch('/api/recipes/stream?ingredients=pasta,tomatoes');
 * const reader = response.body.getReader();
 * const decoder = new TextDecoder();
 * 
 * while (true) {
 *     const { done, value } = await reader.read();
 *     if (done) break;
 *     
 *     const chunk = decoder.decode(value);
 *     document.getElementById('recipe').innerHTML += chunk;
 * }
 * 
 * 
 * 3. React example with Server-Sent Events:
 * 
 * function StreamingRecipe({ ingredients }) {
 *     const [recipe, setRecipe] = useState('');
 *     
 *     useEffect(() => {
 *         const eventSource = new EventSource(
 *             `/api/recipes/stream?ingredients=${ingredients}`
 *         );
 *         
 *         eventSource.onmessage = (event) => {
 *             setRecipe(prev => prev + event.data);
 *         };
 *         
 *         return () => eventSource.close();
 *     }, [ingredients]);
 *     
 *     return <div className="recipe">{recipe}</div>;
 * }
 */
