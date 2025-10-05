package com.example.agent.agents;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Multi-Agent Orchestrator for complex queries.
 * 
 * Coordinates specialized agents to handle requests that require
 * multiple capabilities:
 * - Recipe discovery
 * - Nutritional analysis
 * - Inventory management
 * - Meal planning
 * 
 * Architecture:
 * - Intent Classification: Route to appropriate agent(s)
 * - Parallel Execution: Run independent agents concurrently
 * - Result Aggregation: Combine outputs intelligently
 * - Conflict Resolution: Handle contradicting information
 */
@Service
public class AgentOrchestrator {
    
    private final RecipeAgent recipeAgent;
    private final NutritionAgent nutritionAgent;
    private final InventoryAgent inventoryAgent;
    private final ChatClient chatClient;
    
    public AgentOrchestrator(
            RecipeAgent recipeAgent,
            NutritionAgent nutritionAgent,
            InventoryAgent inventoryAgent,
            ChatClient.Builder chatClientBuilder) {
        this.recipeAgent = recipeAgent;
        this.nutritionAgent = nutritionAgent;
        this.inventoryAgent = inventoryAgent;
        this.chatClient = chatClientBuilder.build();
    }
    
    /**
     * Process a user query by routing to appropriate agents.
     * 
     * Steps:
     * 1. Classify intent (which agents are needed?)
     * 2. Execute agents (parallel when possible)
     * 3. Aggregate results
     * 4. Format final response
     */
    public AgentResponse process(String query, String userId) {
        // Step 1: Classify intent
        var intent = classifyIntent(query);
        
        // Step 2: Execute agents based on intent
        var results = executeAgents(query, userId, intent);
        
        // Step 3: Aggregate and format
        return aggregateResults(query, results, intent);
    }
    
    /**
     * Classify user intent using LLM.
     * 
     * Determines which agents should be involved:
     * - RECIPE_SEARCH: Find recipes
     * - NUTRITION_ANALYSIS: Calculate nutrition
     * - INVENTORY_CHECK: Check what's available
     * - MEAL_PLANNING: Plan meals
     * - GENERAL: Generic chat
     */
    private Intent classifyIntent(String query) {
        String classification = chatClient.prompt()
            .system("""
                Classify the user's intent into one or more categories:
                - RECIPE_SEARCH: Finding or generating recipes
                - NUTRITION_ANALYSIS: Nutritional information or diet planning
                - INVENTORY_CHECK: Checking available ingredients or pantry
                - MEAL_PLANNING: Planning multiple meals or weekly plans
                - GENERAL: General conversation or questions
                
                Return a comma-separated list of applicable categories.
                """)
            .user(query)
            .call()
            .content();
        
        var categories = Arrays.stream(classification.split(","))
            .map(String::trim)
            .map(IntentType::valueOf)
            .toList();
        
        return new Intent(categories, estimateComplexity(query));
    }
    
    /**
     * Execute relevant agents based on classified intent.
     * 
     * Uses CompletableFuture for parallel execution when agents
     * don't depend on each other.
     */
    private Map<String, AgentResult> executeAgents(
            String query, 
            String userId, 
            Intent intent) {
        
        var futures = new HashMap<String, CompletableFuture<AgentResult>>();
        
        // Recipe agent
        if (intent.hasType(IntentType.RECIPE_SEARCH)) {
            futures.put("recipe", CompletableFuture.supplyAsync(() -> 
                recipeAgent.process(query, userId)
            ));
        }
        
        // Nutrition agent
        if (intent.hasType(IntentType.NUTRITION_ANALYSIS)) {
            futures.put("nutrition", CompletableFuture.supplyAsync(() ->
                nutritionAgent.process(query, userId)
            ));
        }
        
        // Inventory agent
        if (intent.hasType(IntentType.INVENTORY_CHECK)) {
            futures.put("inventory", CompletableFuture.supplyAsync(() ->
                inventoryAgent.process(query, userId)
            ));
        }
        
        // Meal planning (may need results from other agents)
        if (intent.hasType(IntentType.MEAL_PLANNING)) {
            // Wait for inventory to complete if present
            CompletableFuture<AgentResult> inventoryFuture = 
                futures.get("inventory");
            
            futures.put("mealplan", 
                (inventoryFuture != null ? inventoryFuture : CompletableFuture.completedFuture(null))
                .thenApplyAsync(inventoryResult -> {
                    // Use inventory results in meal planning if available
                    return planMeals(query, userId, inventoryResult);
                })
            );
        }
        
        // Wait for all futures to complete
        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
            .join();
        
        // Collect results
        var results = new HashMap<String, AgentResult>();
        futures.forEach((key, future) -> 
            results.put(key, future.join())
        );
        
        return results;
    }
    
    /**
     * Aggregate results from multiple agents into a coherent response.
     * 
     * Uses LLM to synthesize information from different sources.
     */
    private AgentResponse aggregateResults(
            String originalQuery,
            Map<String, AgentResult> results,
            Intent intent) {
        
        if (results.isEmpty()) {
            return new AgentResponse(
                "I'm not sure how to help with that. Could you rephrase?",
                List.of(),
                Map.of()
            );
        }
        
        // If only one agent involved, return its result directly
        if (results.size() == 1) {
            var result = results.values().iterator().next();
            return new AgentResponse(
                result.content(),
                result.sources(),
                Map.of("agent", results.keySet().iterator().next())
            );
        }
        
        // Multiple agents: synthesize results
        var synthesis = synthesizeResults(originalQuery, results);
        
        return new AgentResponse(
            synthesis,
            extractAllSources(results),
            Map.of(
                "agents_used", results.keySet(),
                "intent", intent.types()
            )
        );
    }
    
    /**
     * Use LLM to synthesize multiple agent results into coherent response.
     */
    private String synthesizeResults(
            String query,
            Map<String, AgentResult> results) {
        
        var context = new StringBuilder();
        results.forEach((agent, result) -> {
            context.append("=== ").append(agent.toUpperCase()).append(" ===\n");
            context.append(result.content()).append("\n\n");
        });
        
        return chatClient.prompt()
            .system("""
                You are synthesizing information from multiple specialized agents.
                Create a coherent, natural response that combines their insights.
                
                Guidelines:
                - Maintain factual accuracy from all sources
                - Resolve any contradictions logically
                - Present information in a user-friendly way
                - Highlight key insights from each agent
                """)
            .user("""
                Original question: {query}
                
                Agent results:
                {context}
                
                Provide a comprehensive response that addresses the user's question.
                """)
            .param("query", query)
            .param("context", context.toString())
            .call()
            .content();
    }
    
    // ========== Helper Methods ==========
    
    private Complexity estimateComplexity(String query) {
        int wordCount = query.split("\\s+").length;
        boolean hasMultipleCriteria = query.contains("and") || query.contains("or");
        boolean hasConstraints = query.matches(".*\\b(under|over|less|more|within)\\b.*");
        
        if (wordCount > 20 || (hasMultipleCriteria && hasConstraints)) {
            return Complexity.HIGH;
        } else if (wordCount > 10 || hasMultipleCriteria) {
            return Complexity.MEDIUM;
        }
        return Complexity.LOW;
    }
    
    private AgentResult planMeals(String query, String userId, AgentResult inventoryResult) {
        // Simplified meal planning logic
        // In production, this would be a full MealPlanningAgent
        String context = inventoryResult != null ? 
            "Available ingredients: " + inventoryResult.content() : 
            "No inventory information available";
        
        String plan = chatClient.prompt()
            .system("You are a meal planning expert.")
            .user(query + "\n\n" + context)
            .call()
            .content();
        
        return new AgentResult(plan, List.of(), Map.of());
    }
    
    private List<String> extractAllSources(Map<String, AgentResult> results) {
        return results.values().stream()
            .flatMap(r -> r.sources().stream())
            .distinct()
            .toList();
    }
}

// ========== Specialized Agents ==========

@Service
class RecipeAgent {
    private final ChatClient chatClient;
    
    RecipeAgent(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("You are a recipe expert specializing in creative cooking.")
            .build();
    }
    
    public AgentResult process(String query, String userId) {
        String response = chatClient.prompt()
            .user(query)
            .call()
            .content();
        
        return new AgentResult(
            response,
            List.of("Recipe Database", "Culinary Knowledge Base"),
            Map.of("confidence", 0.9)
        );
    }
}

@Service
class NutritionAgent {
    private final ChatClient chatClient;
    
    NutritionAgent(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("You are a nutritionist providing evidence-based dietary advice.")
            .build();
    }
    
    public AgentResult process(String query, String userId) {
        String response = chatClient.prompt()
            .user(query)
            .call()
            .content();
        
        return new AgentResult(
            response,
            List.of("USDA Nutrition Database"),
            Map.of("confidence", 0.95)
        );
    }
}

@Service
class InventoryAgent {
    private final ChatClient chatClient;
    
    InventoryAgent(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("You help manage kitchen inventory and suggest recipes based on available ingredients.")
            .build();
    }
    
    public AgentResult process(String query, String userId) {
        // In production, would check actual user inventory from database
        String response = chatClient.prompt()
            .user("User inventory query: " + query)
            .call()
            .content();
        
        return new AgentResult(
            response,
            List.of("User Pantry"),
            Map.of("items_available", 15)
        );
    }
}

// ========== Data Models ==========

record Intent(List<IntentType> types, Complexity complexity) {
    boolean hasType(IntentType type) {
        return types.contains(type);
    }
}

enum IntentType {
    RECIPE_SEARCH,
    NUTRITION_ANALYSIS,
    INVENTORY_CHECK,
    MEAL_PLANNING,
    GENERAL
}

enum Complexity {
    LOW, MEDIUM, HIGH
}

record AgentResult(
    String content,
    List<String> sources,
    Map<String, Object> metadata
) {}

record AgentResponse(
    String content,
    List<String> sources,
    Map<String, Object> metadata
) {}

/*
 * USAGE EXAMPLES
 * 
 * 1. Simple query (single agent):
 * 
 * var response = orchestrator.process(
 *     "What's a good pasta recipe?",
 *     "user-123"
 * );
 * // Routes to: RecipeAgent only
 * 
 * 
 * 2. Complex query (multiple agents):
 * 
 * var response = orchestrator.process(
 *     "Find me a healthy dinner recipe under 500 calories " +
 *     "that I can make with what's in my pantry",
 *     "user-123"
 * );
 * // Routes to: RecipeAgent + NutritionAgent + InventoryAgent
 * // Executes in parallel, then synthesizes results
 * 
 * 
 * 3. Meal planning (agent chaining):
 * 
 * var response = orchestrator.process(
 *     "Plan my meals for the week based on what I have",
 *     "user-123"
 * );
 * // Routes to: InventoryAgent → MealPlanningAgent (sequential)
 * // MealPlanning waits for inventory results
 * 
 * 
 * 4. With REST API:
 * 
 * @PostMapping("/api/agent/query")
 * public ResponseEntity<AgentResponse> query(
 *         @RequestBody QueryRequest request,
 *         @AuthenticationPrincipal User user) {
 *     
 *     var response = orchestrator.process(
 *         request.query(),
 *         user.getId()
 *     );
 *     
 *     return ResponseEntity.ok(response);
 * }
 * 
 * 
 * ADVANCED PATTERNS:
 * 
 * 1. Agent Collaboration:
 *    - Agents can request help from each other
 *    - Shared context and memory
 *    - Hierarchical task delegation
 * 
 * 2. Confidence Scoring:
 *    - Each agent returns confidence score
 *    - Orchestrator weighs results by confidence
 *    - Falls back to human if confidence too low
 * 
 * 3. Caching:
 *    - Cache agent results for similar queries
 *    - Semantic similarity for cache key
 *    - TTL based on query type
 * 
 * 4. Feedback Loop:
 *    - User rates responses
 *    - Improve intent classification
 *    - Adjust agent routing
 * 
 * 5. Monitoring:
 *    - Track which agents are used
 *    - Measure response times
 *    - Identify bottlenecks
 *    - A/B test different routing strategies
 */
