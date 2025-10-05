# Module 5 Solution - AI Agents & Enterprise Patterns

This solution demonstrates building production-ready AI agents with memory, error handling, enterprise integration patterns, and Model Context Protocol (MCP).

## What's Included

- ✅ Conversation memory systems
- ✅ Multi-agent coordination
- ✅ MCP server and client implementations
- ✅ Enterprise integration patterns (Circuit Breaker, Retry, Bulkhead)
- ✅ Domain-Driven Design with LLMs
- ✅ Error handling and observability
- ✅ Production-ready agent framework

## Solution Structure

```
solution/
├── README.md                                # This file
├── src/main/java/com/example/agent/
│   ├── memory/
│   │   ├── ConversationMemoryService.java  # Chat history management
│   │   ├── LongTermMemoryService.java      # Persistent user preferences
│   │   └── ContextWindowManager.java       # Token limit management
│   ├── agents/
│   │   ├── RecipeAgent.java                # Main recipe agent
│   │   ├── NutritionAgent.java             # Specialized nutrition agent
│   │   ├── InventoryAgent.java             # Pantry management agent
│   │   └── AgentOrchestrator.java          # Multi-agent coordinator
│   ├── mcp/
│   │   ├── RecipeMcpServer.java            # MCP server implementation
│   │   ├── McpClient.java                  # MCP client
│   │   ├── McpTool.java                    # Tool definition
│   │   └── McpPrompt.java                  # Prompt definition
│   ├── resilience/
│   │   ├── CircuitBreakerConfig.java       # Fault tolerance
│   │   ├── RetryConfig.java                # Retry strategies
│   │   └── RateLimiterConfig.java          # Rate limiting
│   ├── observability/
│   │   ├── AgentMetrics.java               # Prometheus metrics
│   │   ├── AgentTracing.java               # Distributed tracing
│   │   └── AuditLog.java                   # Audit logging
│   └── ddd/
│       ├── RecipeAggregate.java            # Domain aggregate
│       ├── RecipeRepository.java           # Repository pattern
│       └── RecipeService.java              # Domain service
└── src/main/resources/
    ├── application.yaml                     # Configuration
    └── mcp-server.json                      # MCP server manifest
```

## 1. Conversation Memory

### In-Memory Chat History

**File**: `src/main/java/com/example/agent/memory/ConversationMemoryService.java`

Manages short-term conversation context within a single session.

**Key Features**:
- Session-based memory management
- Token counting and context window management
- Automatic pruning of old messages
- Integration with Spring AI ChatClient

### Persistent Long-Term Memory

**File**: `src/main/java/com/example/agent/memory/LongTermMemoryService.java`

Stores user preferences, past interactions, and learned information across sessions.

**Key Features**:
- User profile storage (dietary restrictions, preferences)
- Interaction history with embeddings for semantic retrieval
- Preference learning over time
- Redis-backed persistence

### Context Window Management

**File**: `src/main/java/com/example/agent/memory/ContextWindowManager.java`

Handles the LLM's context window limits intelligently.

**Key Features**:
- Token counting (tiktoken-compatible)
- Smart summarization of old context
- Priority-based message retention
- Context compression strategies

## 2. Multi-Agent System

### Recipe Agent

**File**: `src/main/java/com/example/agent/agents/RecipeAgent.java`

Main conversational agent for recipe discovery and generation.

**Capabilities**:
- Natural language recipe search
- Recipe generation from ingredients
- Conversational refinement
- Integration with specialized agents

### Nutrition Agent

**File**: `src/main/java/com/example/agent/agents/NutritionAgent.java`

Specialized agent for nutritional analysis and dietary planning.

**Capabilities**:
- Calorie and macro calculations
- Dietary restriction compliance
- Meal plan generation
- Nutritional advice

### Inventory Agent

**File**: `src/main/java/com/example/agent/agents/InventoryAgent.java`

Manages pantry inventory and suggests recipes based on available ingredients.

**Capabilities**:
- Ingredient inventory tracking
- Expiration date monitoring
- Shopping list generation
- "What can I make with..." queries

### Agent Orchestrator

**File**: `src/main/java/com/example/agent/agents/AgentOrchestrator.java`

Coordinates multiple agents to handle complex queries.

**Key Features**:
- Intent routing (which agent should handle this?)
- Multi-agent collaboration
- Result aggregation
- Conflict resolution

## 3. Model Context Protocol (MCP)

### MCP Server Implementation

**File**: `src/main/java/com/example/agent/mcp/RecipeMcpServer.java`

Exposes recipe functionality as an MCP server that can be consumed by Claude Desktop, IDEs, or other MCP clients.

**Exposed Tools**:
- `search_recipes`: Search recipe database
- `get_recipe_details`: Get full recipe information
- `analyze_nutrition`: Get nutritional information
- `generate_shopping_list`: Create shopping list
- `check_ingredient_substitutes`: Find ingredient alternatives

**Exposed Prompts**:
- `recipe_critic`: Professional recipe review
- `meal_planner`: Weekly meal planning
- `cooking_instructor`: Step-by-step cooking guidance

### MCP Client

**File**: `src/main/java/com/example/agent/mcp/McpClient.java`

Consumes external MCP servers (e.g., database access, web search).

**Features**:
- Server discovery and connection
- Tool invocation
- Prompt execution
- Error handling and retries

### MCP Configuration

**File**: `src/main/resources/mcp-server.json`

```json
{
  "name": "recipe-finder-mcp",
  "version": "1.0.0",
  "description": "AI-powered recipe discovery and meal planning",
  "tools": [
    {
      "name": "search_recipes",
      "description": "Search for recipes by ingredients, cuisine, or dietary restrictions",
      "parameters": {
        "type": "object",
        "properties": {
          "query": {
            "type": "string",
            "description": "Search query"
          },
          "filters": {
            "type": "object",
            "properties": {
              "cuisine": {"type": "string"},
              "dietary": {"type": "array", "items": {"type": "string"}},
              "maxPrepTime": {"type": "integer"}
            }
          }
        },
        "required": ["query"]
      }
    }
  ],
  "prompts": [
    {
      "name": "recipe_critic",
      "description": "Professional recipe review and improvement suggestions",
      "arguments": {
        "recipe_text": {
          "type": "string",
          "description": "The recipe to review"
        }
      }
    }
  ]
}
```

## 4. Enterprise Resilience Patterns

### Circuit Breaker

**File**: `src/main/java/com/example/agent/resilience/CircuitBreakerConfig.java`

Prevents cascading failures when external services (LLM, vector DB) are unavailable.

**Configuration**:
- Failure threshold: 50% of requests failing
- Wait duration: 60 seconds in OPEN state
- Slow call threshold: 5 seconds
- Fallback responses

### Retry Strategy

**File**: `src/main/java/com/example/agent/resilience/RetryConfig.java`

Handles transient failures with exponential backoff.

**Configuration**:
- Max attempts: 3
- Backoff: Exponential (1s, 2s, 4s)
- Retry on: Timeout, 503, 429
- Skip retry on: 400, 401, 403

### Rate Limiting

**File**: `src/main/java/com/example/agent/resilience/RateLimiterConfig.java`

Protects backend services from overload.

**Configuration**:
- Per-user limits: 10 requests/minute
- Global limits: 100 requests/second
- Token bucket algorithm
- Custom limits per API key tier

## 5. Observability

### Metrics

**File**: `src/main/java/com/example/agent/observability/AgentMetrics.java`

Exposes Prometheus metrics for monitoring.

**Metrics Tracked**:
- `agent_requests_total`: Request counter by agent type
- `agent_request_duration_seconds`: Request latency histogram
- `llm_token_usage_total`: Token consumption counter
- `conversation_length_messages`: Active conversation sizes
- `circuit_breaker_state`: Circuit breaker state gauge

### Distributed Tracing

**File**: `src/main/java/com/example/agent/observability/AgentTracing.java`

OpenTelemetry integration for request tracing.

**Spans Created**:
- Agent invocation
- LLM API calls
- Vector database queries
- Memory operations
- External tool calls

### Audit Logging

**File**: `src/main/java/com/example/agent/observability/AuditLog.java`

Structured logging for compliance and debugging.

**Logged Events**:
- User queries (with PII handling)
- Agent responses
- Tool invocations
- Policy violations
- Errors and exceptions

## 6. Domain-Driven Design

### Recipe Aggregate

**File**: `src/main/java/com/example/agent/ddd/RecipeAggregate.java`

Rich domain model with business logic.

**Business Rules**:
- Recipes must have at least 2 ingredients
- Prep time + cook time = total time
- Servings must be positive
- Ingredients must have valid measurements

### Repository Pattern

**File**: `src/main/java/com/example/agent/ddd/RecipeRepository.java`

Abstraction over data persistence.

**Operations**:
- `save(Recipe)`: Store recipe
- `findById(UUID)`: Retrieve by ID
- `findByIngredients(List<String>)`: Semantic search
- `findByCuisine(String)`: Filter by cuisine

### Domain Service

**File**: `src/main/java/com/example/agent/ddd/RecipeService.java`

Orchestrates domain logic across aggregates.

**Services**:
- Recipe generation with validation
- Nutritional calculation
- Recipe variation generation
- Meal planning logic

## Usage Examples

### Basic Agent Conversation

```java
@RestController
class ChatController {
    private final RecipeAgent agent;
    
    @PostMapping("/api/chat")
    ResponseEntity<AgentResponse> chat(
            @RequestBody ChatRequest request,
            @RequestHeader("X-Session-Id") String sessionId) {
        
        var response = agent.chat(request.message(), sessionId);
        return ResponseEntity.ok(response);
    }
}
```

### Multi-Agent Query

```java
var orchestrator = context.getBean(AgentOrchestrator.class);

// Complex query requiring multiple agents
var result = orchestrator.process(
    "Find me a healthy dinner recipe under 500 calories " +
    "that I can make with chicken, broccoli, and rice"
);

// Orchestrator coordinates:
// 1. RecipeAgent: Find matching recipes
// 2. NutritionAgent: Filter by calories
// 3. InventoryAgent: Check ingredient availability
```

### Using MCP Tools

```java
var mcpClient = context.getBean(McpClient.class);

// Connect to external MCP server
mcpClient.connect("filesystem");

// Use tool from connected server
var files = mcpClient.invokeTool("list_directory", 
    Map.of("path", "/recipes"));
```

### With Resilience

```java
@Service
class ResilientRecipeService {
    
    @CircuitBreaker(name = "llm", fallbackMethod = "fallback")
    @Retry(name = "llm")
    @RateLimiter(name = "llm")
    public String generateRecipe(String ingredients) {
        return chatClient.prompt()
            .user("Create recipe with: " + ingredients)
            .call()
            .content();
    }
    
    private String fallback(String ingredients, Exception e) {
        log.warn("LLM unavailable, using fallback", e);
        return "Service temporarily unavailable. Please try again.";
    }
}
```

### Memory-Enhanced Chat

```java
@Service
class MemoryEnhancedAgent {
    private final ConversationMemoryService memory;
    private final ChatClient chatClient;
    
    public String chat(String message, String userId) {
        // Load conversation history
        var history = memory.getConversation(userId);
        
        // Load user preferences
        var preferences = memory.getUserPreferences(userId);
        
        // Build context-aware prompt
        var response = chatClient.prompt()
            .system("User preferences: " + preferences)
            .messages(history)
            .user(message)
            .call()
            .content();
        
        // Save to memory
        memory.addMessage(userId, "user", message);
        memory.addMessage(userId, "assistant", response);
        
        return response;
    }
}
```

## Configuration

**File**: `src/main/resources/application.yaml`

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: llama3.2
          temperature: 0.7
    
    vectorstore:
      redis:
        index-type: HNSW
        embedding-dimension: 1536

resilience4j:
  circuitbreaker:
    instances:
      llm:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        permitted-number-of-calls-in-half-open-state: 3
        sliding-window-size: 10
  
  retry:
    instances:
      llm:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
  
  ratelimiter:
    instances:
      llm:
        limit-for-period: 10
        limit-refresh-period: 1m
        timeout-duration: 0s

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
```

## Testing

### Unit Tests

```java
@Test
void agentShouldHandleSimpleQuery() {
    var agent = new RecipeAgent(chatClient, memory);
    var response = agent.chat("pasta recipe", "test-session");
    assertThat(response.content()).contains("pasta");
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class AgentIntegrationTest {
    
    @Container
    static GenericContainer<?> ollama = new GenericContainer<>("ollama/ollama")
        .withExposedPorts(11434);
    
    @Test
    void fullAgentWorkflow() {
        // Test complete conversation flow with real LLM
    }
}
```

## Deployment

### Docker Compose

```yaml
services:
  agent:
    build: .
    ports:
      - "8080:8080"
    environment:
      - OLLAMA_URL=http://ollama:11434
      - REDIS_HOST=redis
    depends_on:
      - ollama
      - redis
  
  ollama:
    image: ollama/ollama
    ports:
      - "11434:11434"
  
  redis:
    image: redis/redis-stack
    ports:
      - "6379:6379"
```

## Production Checklist

- ✅ Conversation memory with cleanup
- ✅ Circuit breakers on all external calls
- ✅ Rate limiting per user
- ✅ Structured logging and tracing
- ✅ Metrics and alerting
- ✅ Input validation and sanitization
- ✅ Error handling and fallbacks
- ✅ Token usage monitoring
- ✅ Cost tracking
- ✅ PII handling and data privacy

## Performance Tips

1. **Memory Management**: Implement aggressive pruning for inactive sessions
2. **Caching**: Cache frequent queries and embeddings
3. **Async Processing**: Use async for independent agent calls
4. **Connection Pooling**: Reuse HTTP connections to LLM
5. **Batch Processing**: Batch embed operations when possible

## Next Steps

- Add authentication and authorization
- Implement user feedback loops
- Build A/B testing framework
- Add multi-language support
- Implement advanced RAG patterns
- Create admin dashboard for monitoring

## Resources

- [Model Context Protocol Specification](https://modelcontextprotocol.io/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/instrumentation/java/)
- [Domain-Driven Design](https://martinfowler.com/bliki/DomainDrivenDesign.html)
