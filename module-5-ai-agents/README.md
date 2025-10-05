# Module 5: AI Agents

## Overview
Learn how to build autonomous AI agents that can plan, use tools, and accomplish complex tasks. This module covers agent architectures, tool integration, the Model Context Protocol (MCP), and creating intelligent systems that go beyond simple question-answering.

## Learning Objectives
- Understand AI agent architectures and patterns
- Implement tool/function calling for AI agents
- Build agents that can plan and execute multi-step tasks
- Create and integrate MCP (Model Context Protocol) servers
- Implement agent memory and state management
- Design autonomous workflows with AI agents
- Handle agent errors and fallback strategies
- Build multi-agent systems

## Topics Covered
- What are AI Agents?
- Agent architectures: ReAct, Plan-and-Execute, Reflexion
- Tool/Function calling with Spring AI
- Model Context Protocol (MCP) overview
- Creating MCP servers and clients
- Agent memory and conversation state
- Planning and reasoning
- Multi-agent collaboration
- Agent orchestration patterns
- Error handling and recovery
- Memory and conversation history
- Enterprise integration patterns
- Retry strategies and fallbacks
- Domain-Driven Design with AI Agents
- Real-world agent use cases

## What are AI Agents?

AI Agents are autonomous systems that can:
- 🎯 **Set Goals**: Understand and pursue objectives
- 🔧 **Use Tools**: Call APIs, query databases, execute code
- 🧠 **Reason**: Plan multi-step solutions
- 💾 **Remember**: Maintain context across interactions
- 🔄 **Iterate**: Try different approaches if one fails

### Agent vs. Simple AI
| Feature | Simple AI | AI Agent |
|---------|-----------|----------|
| Interaction | Single Q&A | Multi-step tasks |
| Tools | None | Can use many tools |
| Planning | None | Can create plans |
| Memory | None/Limited | Persistent memory |
| Autonomy | Low | High |

## Agent Architectures

### 1. ReAct (Reason + Act)
The agent alternates between reasoning and taking actions:
```
1. Think: "I need to find the weather"
2. Act: Call weather API
3. Observe: "Temperature is 72°F"
4. Think: "Now I can answer"
5. Answer: "It's 72°F today"
```

### 2. Plan-and-Execute
Agent creates a complete plan, then executes it:
```
Plan:
1. Get user's location
2. Call weather API
3. Format response

Execute each step...
```

### 3. Reflexion
Agent self-reflects and improves:
```
1. Attempt task
2. Evaluate success
3. If failed, reflect on why
4. Try again with improved approach
```

## Model Context Protocol (MCP)

MCP is an open protocol that standardizes how AI applications connect to data sources and tools.

### Why MCP?
- ✅ **Standardized**: One protocol for all tools
- ✅ **Reusable**: Share MCP servers across applications
- ✅ **Secure**: Fine-grained access control
- ✅ **Composable**: Combine multiple MCP servers

### MCP Architecture
```
┌──────────────┐         ┌──────────────┐
│  AI Agent    │◀───────▶│ MCP Server   │
│  (Client)    │   MCP   │   (Tools)    │
└──────────────┘         └──────────────┘
                                │
                         ┌──────┴──────┐
                         │             │
                    ┌────▼───┐   ┌────▼───┐
                    │Database│   │  APIs  │
                    └────────┘   └────────┘
```

## Tasks

### 1. Implement Basic Tool Calling

```java
@Component
public class WeatherTool implements Function<WeatherRequest, WeatherResponse> {
    
    @Override
    @Description("Get current weather for a location")
    public WeatherResponse apply(
        @JsonProperty(value = "location", required = true)
        @JsonPropertyDescription("City name, e.g., 'London'")
        WeatherRequest request
    ) {
        // Call actual weather API
        return weatherService.getWeather(request.location());
    }
}

record WeatherRequest(String location) {}
record WeatherResponse(String location, double temperature, String condition) {}
```

Register and use the tool:
```java
@Service
public class AgentService {
    
    private final ChatClient chatClient;
    private final List<Function<?, ?>> tools;
    
    public String executeWithTools(String userMessage) {
        return chatClient.call(
            new UserMessage(userMessage),
            ChatOptions.builder()
                .withFunctions(tools)
                .build()
        );
    }
}
```

### 2. Build a ReAct Agent

```java
@Service
public class ReActAgent {
    
    private final ChatClient chatClient;
    private final Map<String, Function<?, ?>> tools;
    private static final int MAX_ITERATIONS = 5;
    
    public String execute(String goal) {
        List<Message> conversation = new ArrayList<>();
        conversation.add(new SystemMessage(REACT_SYSTEM_PROMPT));
        conversation.add(new UserMessage(goal));
        
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            ChatResponse response = chatClient.call(
                new Prompt(conversation, 
                    ChatOptions.builder().withFunctions(tools.values()).build())
            );
            
            Message assistantMessage = response.getResult().getOutput();
            conversation.add(assistantMessage);
            
            // Check if agent is done
            if (isDone(assistantMessage)) {
                return extractFinalAnswer(assistantMessage);
            }
            
            // Execute any function calls
            if (hasFunctionCall(assistantMessage)) {
                String toolResult = executeTool(assistantMessage);
                conversation.add(new UserMessage("Tool result: " + toolResult));
            }
        }
        
        return "Agent exceeded maximum iterations";
    }
    
    private static final String REACT_SYSTEM_PROMPT = """
        You are an AI agent that can use tools to accomplish tasks.
        For each task, think step by step:
        1. Thought: Reason about what to do next
        2. Action: Use a tool if needed
        3. Observation: Analyze the tool result
        4. Repeat until you can answer
        
        When you have the final answer, respond with:
        Final Answer: [your answer]
        """;
}
```

### 3. Create an MCP Server

```java
@RestController
@RequestMapping("/mcp")
public class McpServerController {
    
    private final DatabaseService databaseService;
    
    // List available tools
    @GetMapping("/tools")
    public List<McpTool> listTools() {
        return List.of(
            new McpTool(
                "query_database",
                "Execute SQL query on the database",
                new JsonSchema(/* schema for parameters */)
            ),
            new McpTool(
                "get_table_schema",
                "Get schema information for a table",
                new JsonSchema(/* schema */)
            )
        );
    }
    
    // Execute tool
    @PostMapping("/execute")
    public McpToolResult executeTool(@RequestBody McpToolRequest request) {
        return switch (request.tool()) {
            case "query_database" -> 
                queryDatabase(request.parameters());
            case "get_table_schema" -> 
                getTableSchema(request.parameters());
            default -> 
                throw new IllegalArgumentException("Unknown tool: " + request.tool());
        };
    }
    
    private McpToolResult queryDatabase(Map<String, Object> params) {
        String sql = (String) params.get("query");
        List<Map<String, Object>> results = databaseService.executeQuery(sql);
        return new McpToolResult(true, results, null);
    }
}

record McpTool(String name, String description, JsonSchema parameters) {}
record McpToolRequest(String tool, Map<String, Object> parameters) {}
record McpToolResult(boolean success, Object data, String error) {}
```

### 4. Create an MCP Client

```java
@Service
public class McpClient {
    
    private final RestTemplate restTemplate;
    private final String mcpServerUrl;
    
    public List<McpTool> getAvailableTools() {
        return restTemplate.exchange(
            mcpServerUrl + "/mcp/tools",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<McpTool>>() {}
        ).getBody();
    }
    
    public McpToolResult callTool(String toolName, Map<String, Object> parameters) {
        McpToolRequest request = new McpToolRequest(toolName, parameters);
        return restTemplate.postForObject(
            mcpServerUrl + "/mcp/execute",
            request,
            McpToolResult.class
        );
    }
}
```

### 5. Implement Agent Memory

```java
@Service
public class AgentMemoryService {
    
    private final Map<String, AgentMemory> sessions = new ConcurrentHashMap<>();
    private final VectorStore vectorStore;
    
    public void storeMemory(String sessionId, String content, String type) {
        AgentMemory memory = sessions.computeIfAbsent(
            sessionId, 
            k -> new AgentMemory()
        );
        
        // Short-term memory (conversation history)
        memory.addToHistory(content);
        
        // Long-term memory (vector store for retrieval)
        if (type.equals("IMPORTANT")) {
            Document doc = new Document(content);
            doc.getMetadata().put("sessionId", sessionId);
            doc.getMetadata().put("timestamp", Instant.now());
            vectorStore.add(List.of(doc));
        }
    }
    
    public String getRelevantMemory(String sessionId, String query) {
        // Retrieve from vector store
        List<Document> relevant = vectorStore.similaritySearch(
            SearchRequest.query(query)
                .withTopK(5)
                .withFilterExpression(
                    Filter.expression("sessionId == '" + sessionId + "'")
                )
        );
        
        return relevant.stream()
            .map(Document::getContent)
            .collect(Collectors.joining("\n\n"));
    }
}

class AgentMemory {
    private final Deque<String> conversationHistory = new ArrayDeque<>(100);
    
    public void addToHistory(String content) {
        if (conversationHistory.size() >= 100) {
            conversationHistory.removeFirst();
        }
        conversationHistory.addLast(content);
    }
    
    public List<String> getHistory() {
        return new ArrayList<>(conversationHistory);
    }
}
```

### 6. Build a Planning Agent

```java
@Service
public class PlanningAgent {
    
    private final ChatClient chatClient;
    private final Map<String, Function<?, ?>> tools;
    
    public String executeTask(String goal) {
        // Step 1: Create plan
        List<String> plan = createPlan(goal);
        System.out.println("Plan: " + plan);
        
        // Step 2: Execute each step
        Map<String, Object> context = new HashMap<>();
        for (int i = 0; i < plan.size(); i++) {
            String step = plan.get(i);
            System.out.println("Executing step " + (i+1) + ": " + step);
            
            String result = executeStep(step, context);
            context.put("step_" + i + "_result", result);
        }
        
        // Step 3: Synthesize final answer
        return synthesizeFinalAnswer(goal, context);
    }
    
    private List<String> createPlan(String goal) {
        String planPrompt = """
            Create a step-by-step plan to accomplish this goal: %s
            
            Available tools: %s
            
            Provide a numbered list of steps.
            Each step should be clear and actionable.
            """.formatted(goal, getToolDescriptions());
        
        String response = chatClient.call(planPrompt);
        return parsePlan(response);
    }
    
    private String executeStep(String step, Map<String, Object> context) {
        String prompt = """
            Execute this step: %s
            
            Context from previous steps: %s
            
            Use tools if needed to complete the step.
            """.formatted(step, context);
        
        return chatClient.call(
            new Prompt(prompt, 
                ChatOptions.builder().withFunctions(tools.values()).build())
        );
    }
}
```

### 7. Implement Multi-Agent System

```java
@Service
public class MultiAgentSystem {
    
    private final Map<String, Agent> agents;
    
    public MultiAgentSystem(
        ResearchAgent researchAgent,
        WriterAgent writerAgent,
        ReviewerAgent reviewerAgent
    ) {
        this.agents = Map.of(
            "researcher", researchAgent,
            "writer", writerAgent,
            "reviewer", reviewerAgent
        );
    }
    
    public String executeWorkflow(String task) {
        // Agent 1: Research
        String research = agents.get("researcher").execute(
            "Research information about: " + task
        );
        
        // Agent 2: Write
        String draft = agents.get("writer").execute(
            "Write an article based on this research: " + research
        );
        
        // Agent 3: Review and improve
        String finalArticle = agents.get("reviewer").execute(
            "Review and improve this article: " + draft
        );
        
        return finalArticle;
    }
}

interface Agent {
    String execute(String task);
}
```

## Real-World Agent Use Cases

1. **Code Assistant Agent**: Understand requirements, write code, run tests, fix bugs
2. **Research Agent**: Search multiple sources, synthesize information, cite sources
3. **Data Analysis Agent**: Query databases, perform analysis, generate visualizations
4. **Customer Support Agent**: Understand issue, search knowledge base, provide solution
5. **DevOps Agent**: Monitor systems, diagnose issues, execute fixes

## Memory and Conversation History

### Implementing Conversation Memory

AI agents need to remember previous interactions for coherent conversations:

```java
@Service
public class ConversationService {
    
    private final ChatClient chatClient;
    private final Map<String, MessageHistory> sessions = new ConcurrentHashMap<>();
    
    public String chat(String sessionId, String userMessage) {
        // Get or create conversation history
        MessageHistory history = sessions.computeIfAbsent(
            sessionId, 
            k -> new MessageHistory()
        );
        
        // Add user message
        history.add(new UserMessage(userMessage));
        
        // Get AI response with full history
        String response = chatClient.prompt()
            .messages(history.getMessages())
            .call()
            .content();
        
        // Store AI response
        history.add(new AssistantMessage(response));
        
        return response;
    }
    
    public void clearHistory(String sessionId) {
        sessions.remove(sessionId);
    }
}
```

### Memory Types

#### 1. Short-Term Memory (Conversation Buffer)
```java
class ConversationBuffer {
    private final Deque<Message> messages = new ArrayDeque<>();
    private final int maxMessages;
    
    void add(Message message) {
        messages.addLast(message);
        if (messages.size() > maxMessages) {
            messages.removeFirst();  // Keep only recent messages
        }
    }
}
```

#### 2. Long-Term Memory (Vector Store)
```java
@Service
class AgentMemoryService {
    
    private final VectorStore memoryStore;
    
    void remember(String sessionId, String interaction) {
        var document = new Document(
            interaction,
            Map.of("sessionId", sessionId, "timestamp", Instant.now())
        );
        memoryStore.add(List.of(document));
    }
    
    List<String> recall(String sessionId, String query) {
        return memoryStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(5)
                .filterExpression("sessionId == '" + sessionId + "'")
                .build()
        ).stream()
         .map(Document::getContent)
         .toList();
    }
}
```

#### 3. Summary Memory (Token Optimization)
```java
class SummaryMemory {
    private final ChatClient chatClient;
    private String conversationSummary = "";
    
    void updateSummary(List<Message> recentMessages) {
        String messagesToSummarize = formatMessages(recentMessages);
        
        conversationSummary = chatClient.prompt()
            .system("""
                Summarize the key points from this conversation.
                Include important facts, decisions, and context.
                Keep it concise but comprehensive.
                """)
            .user("Previous summary: " + conversationSummary + 
                  "\n\nNew messages: " + messagesToSummarize)
            .call()
            .content();
    }
}
```

## Enterprise Integration Patterns

### 1. Event-Driven Architecture with AI

```java
@Service
class AIEventProcessor {
    
    private final ChatClient chatClient;
    private final ApplicationEventPublisher eventPublisher;
    
    @EventListener
    public void handleBusinessEvent(OrderCreatedEvent event) {
        // AI analyzes the order
        var analysis = chatClient.prompt()
            .user("Analyze this order for potential issues: " + event.getOrder())
            .call()
            .entity(OrderAnalysis.class);
        
        if (analysis.hasPotentialFraud()) {
            eventPublisher.publishEvent(new FraudAlertEvent(analysis));
        }
    }
}
```

### 2. AI as a Microservice

```java
@RestController
@RequestMapping("/ai-service")
class AIServiceController {
    
    private final AgentService agentService;
    
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResult> analyze(
        @RequestBody AnalysisRequest request,
        @RequestHeader("X-API-Key") String apiKey
    ) {
        // Validate API key
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // Rate limiting
        if (rateLimiter.isLimitExceeded(apiKey)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        var result = agentService.analyze(request);
        return ResponseEntity.ok(result);
    }
}
```

### 3. Async Processing with Message Queues

```java
@Service
class AsyncAIProcessor {
    
    @RabbitListener(queues = "ai-requests")
    public void processRequest(AIRequest request) {
        try {
            var result = chatClient.prompt()
                .user(request.getPrompt())
                .call()
                .content();
            
            // Publish result
            rabbitTemplate.convertAndSend("ai-results", 
                new AIResult(request.getId(), result));
                
        } catch (Exception e) {
            // Send to dead letter queue
            rabbitTemplate.convertAndSend("ai-requests-dlq", request);
        }
    }
}
```

### 4. Caching Strategy

```java
@Service
class CachedAIService {
    
    @Cacheable(value = "ai-responses", key = "#prompt")
    public String getCachedResponse(String prompt) {
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }
    
    // Cache semantic similarity
    @Cacheable(value = "semantic-cache", keyGenerator = "semanticKeyGenerator")
    public String getSemanticallyCachedResponse(String prompt) {
        // Check if semantically similar prompt exists in cache
        // Uses vector similarity to find matches
        return chatClient.prompt().user(prompt).call().content();
    }
}
```

## Error Handling and Retry Strategies

### 1. Circuit Breaker Pattern

```java
@Service
class ResilientAIService {
    
    @CircuitBreaker(name = "ai-service", fallbackMethod = "fallbackResponse")
    @Retry(name = "ai-service", fallbackMethod = "fallbackResponse")
    public String getResponse(String prompt) {
        return chatClient.prompt()
            .user(prompt)
            .call()
            .content();
    }
    
    private String fallbackResponse(String prompt, Exception e) {
        log.error("AI service failed, using fallback", e);
        return "I'm experiencing technical difficulties. Please try again later.";
    }
}
```

Configuration in `application.yaml`:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      ai-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 60s
        sliding-window-size: 10
  retry:
    instances:
      ai-service:
        max-attempts: 3
        wait-duration: 2s
        exponential-backoff-multiplier: 2
```

### 2. Timeout Handling

```java
@Service
class TimeoutAwareAIService {
    
    public String getResponseWithTimeout(String prompt) {
        try {
            return CompletableFuture.supplyAsync(() ->
                chatClient.prompt().user(prompt).call().content()
            ).get(30, TimeUnit.SECONDS);
            
        } catch (TimeoutException e) {
            log.warn("AI request timed out after 30s");
            return "Response took too long. Try a simpler query.";
        }
    }
}
```

### 3. Graceful Degradation

```java
@Service
class DegradableAIService {
    
    private final ChatClient primaryModel;
    private final ChatClient fallbackModel;
    
    public String getResponse(String prompt) {
        try {
            // Try primary (larger, better) model
            return primaryModel.prompt()
                .user(prompt)
                .call()
                .content();
                
        } catch (Exception e) {
            log.warn("Primary model failed, using fallback", e);
            
            try {
                // Fall back to smaller, faster model
                return fallbackModel.prompt()
                    .user(prompt)
                    .call()
                    .content();
                    
            } catch (Exception e2) {
                log.error("Both models failed", e2);
                // Final fallback: rule-based response
                return getRuleBasedResponse(prompt);
            }
        }
    }
}
```

### 4. Validation and Safety Checks

```java
@Service
class SafeAIService {
    
    private final ChatClient chatClient;
    private final ContentModerationService moderationService;
    
    public String getSafeResponse(String prompt) {
        // Pre-validation
        if (moderationService.isUnsafeInput(prompt)) {
            throw new UnsafeContentException("Input contains unsafe content");
        }
        
        var response = chatClient.prompt()
            .user(prompt)
            .call()
            .content();
        
        // Post-validation
        if (moderationService.isUnsafeOutput(response)) {
            log.warn("Unsafe AI response detected, filtering");
            return "I cannot provide that information.";
        }
        
        return response;
    }
}
```

## Domain-Driven Design with AI Agents

### Bounded Contexts with Specialized Agents

```java
// Customer Support Context
@Service
class CustomerSupportAgent {
    private final ChatClient supportSpecialist;
    
    @PostConstruct
    void init() {
        // This agent is fine-tuned for customer support
        supportSpecialist.setSystemPrompt("""
            You are a customer support specialist for TechCorp.
            Be empathetic, solution-oriented, and follow company policies.
            """);
    }
}

// Sales Context
@Service
class SalesAgent {
    private final ChatClient salesSpecialist;
    
    @PostConstruct
    void init() {
        // Different agent, different expertise
        salesSpecialist.setSystemPrompt("""
            You are a sales assistant for TechCorp.
            Be persuasive, highlight product benefits, and close deals.
            """);
    }
}
```

### Aggregate with AI Enhancement

```java
@Entity
class CustomerProfile {
    @Id
    private Long id;
    private String preferences;
    private List<Interaction> history;
    
    // Domain logic enhanced by AI
    public ProductRecommendation getRecommendation(AIAgent agent) {
        String context = buildContextFromHistory();
        return agent.recommend(context, this.preferences);
    }
}
```

### Domain Events with AI Processing

```java
@DomainEvents
class Order {
    
    public List<Object> registerEvents(AIAgent agent) {
        List<Object> events = new ArrayList<>();
        
        // AI analyzes order and generates events
        var analysis = agent.analyzeOrder(this);
        
        if (analysis.requiresApproval()) {
            events.add(new OrderRequiresApprovalEvent(this));
        }
        
        if (analysis.suggestsUpsell()) {
            events.add(new UpsellOpportunityEvent(this, analysis.getSuggestions()));
        }
        
        return events;
    }
}
```

## Enhanced MCP (Model Context Protocol)

### Building an MCP Server

```java
@RestController
@RequestMapping("/mcp")
class RecipeMCPServer {
    
    private final RecipeService recipeService;
    
    // MCP tool definition
    @PostMapping("/tools/list")
    public MCPToolsResponse listTools() {
        return MCPToolsResponse.builder()
            .tools(List.of(
                MCPTool.builder()
                    .name("search_recipes")
                    .description("Search for recipes by ingredients")
                    .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "ingredients", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string"),
                                "description", "List of ingredients"
                            )
                        ),
                        "required", List.of("ingredients")
                    ))
                    .build(),
                    
                MCPTool.builder()
                    .name("get_recipe_details")
                    .description("Get detailed recipe information")
                    .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                            "recipeId", Map.of(
                                "type", "string",
                                "description", "The recipe ID"
                            )
                        )
                    ))
                    .build()
            ))
            .build();
    }
    
    // MCP tool execution
    @PostMapping("/tools/call")
    public MCPToolResponse callTool(@RequestBody MCPToolRequest request) {
        return switch (request.getName()) {
            case "search_recipes" -> {
                var ingredients = (List<String>) request.getArguments().get("ingredients");
                var recipes = recipeService.search(ingredients);
                yield MCPToolResponse.success(recipes);
            }
            case "get_recipe_details" -> {
                var recipeId = (String) request.getArguments().get("recipeId");
                var recipe = recipeService.getById(recipeId);
                yield MCPToolResponse.success(recipe);
            }
            default -> MCPToolResponse.error("Unknown tool: " + request.getName());
        };
    }
    
    // MCP resources (for RAG)
    @PostMapping("/resources/list")
    public MCPResourcesResponse listResources() {
        return MCPResourcesResponse.builder()
            .resources(List.of(
                MCPResource.builder()
                    .uri("recipe://database/all")
                    .name("Recipe Database")
                    .description("All recipes in the system")
                    .mimeType("application/json")
                    .build()
            ))
            .build();
    }
}
```

### Using an MCP Server from an Agent

```java
@Service
class MCPAwareAgent {
    
    private final RestTemplate mcpClient;
    private final ChatClient chatClient;
    
    public Recipe findRecipe(String userQuery) {
        // 1. Discover available tools
        var tools = mcpClient.postForObject(
            "http://mcp-server/mcp/tools/list",
            null,
            MCPToolsResponse.class
        );
        
        // 2. AI decides which tool to use
        var decision = chatClient.prompt()
            .system("You have access to these tools: " + tools)
            .user("User wants: " + userQuery)
            .call()
            .entity(ToolDecision.class);
        
        // 3. Call the MCP tool
        var result = mcpClient.postForObject(
            "http://mcp-server/mcp/tools/call",
            new MCPToolRequest(decision.toolName(), decision.arguments()),
            MCPToolResponse.class
        );
        
        // 4. Format response for user
        return chatClient.prompt()
            .user("Format this data for the user: " + result.getContent())
            .call()
            .entity(Recipe.class);
    }
}
```

### MCP for Multi-Agent Systems

```java
@Service
class MultiAgentCoordinator {
    
    private final List<MCPServer> mcpServers;
    private final ChatClient orchestrator;
    
    public String handleComplexQuery(String query) {
        // Each agent exposes its capabilities via MCP
        var allTools = mcpServers.stream()
            .flatMap(server -> server.listTools().stream())
            .toList();
        
        // Orchestrator decides task breakdown
        var plan = orchestrator.prompt()
            .system("Available tools: " + allTools)
            .user("Create a plan to answer: " + query)
            .call()
            .entity(ExecutionPlan.class);
        
        // Execute plan across multiple agents
        var results = new HashMap<String, Object>();
        for (var step : plan.getSteps()) {
            var server = findServerForTool(step.getTool());
            var result = server.callTool(step.getTool(), step.getArguments());
            results.put(step.getName(), result);
        }
        
        // Synthesize final answer
        return orchestrator.prompt()
            .user("Synthesize answer from: " + results)
            .call()
            .content();
    }
}
```

## Best Practices

1. **Set Clear Goals**: Agents work best with specific, measurable objectives
2. **Limit Iterations**: Prevent infinite loops with max iteration counts
3. **Handle Errors**: Tools can fail; implement fallbacks
4. **Log Everything**: Track agent decisions for debugging
5. **Cost Control**: Monitor token usage carefully
6. **Human in the Loop**: For critical decisions, require human approval
7. **Test Thoroughly**: Agent behavior can be unpredictable
8. **Start Simple**: Begin with single-tool agents before building complex systems
9. **Use MCP**: Standardize tool interfaces for reusability
10. **Implement Memory**: Stateless agents are limited; add memory for context
11. **Monitor Performance**: Track success rates, latency, and costs
12. **Validate Inputs/Outputs**: Never trust AI blindly

## Code Examples
Complete code examples will be provided in the workshop repository.

## Workshop Complete! 🎉
Congratulations on completing the workshop! You now have the knowledge to build secure, scalable AI applications with self-hosted LLMs, autonomous agents, proper error handling, and enterprise integration patterns.

## Resources
- [Model Context Protocol Specification](https://spec.modelcontextprotocol.io/)
- [MCP Servers Repository](https://github.com/modelcontextprotocol/servers)
- [Spring AI Function Calling](https://docs.spring.io/spring-ai/reference/api/functions.html)
- [LangChain Agents Documentation](https://python.langchain.com/docs/modules/agents/)
- [ReAct Paper](https://arxiv.org/abs/2210.03629)
- [AutoGPT](https://github.com/Significant-Gravitas/AutoGPT)
- [CrewAI - Multi-Agent Framework](https://www.crewai.com/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Domain-Driven Design with AI](https://martinfowler.com/bliki/DomainDrivenDesign.html)

