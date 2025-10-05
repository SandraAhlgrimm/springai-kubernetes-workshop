package com.example.agent.mcp;

import org.springframework.web.bind.annotation.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Model Context Protocol (MCP) Server Implementation.
 * 
 * This server exposes recipe functionality that can be consumed by:
 * - Claude Desktop
 * - IDEs with MCP support
 * - Other AI assistants
 * - Custom MCP clients
 * 
 * MCP enables AI assistants to access external tools and data sources
 * in a standardized way.
 * 
 * Specification: https://modelcontextprotocol.io/
 */
@RestController
@RequestMapping("/mcp")
public class RecipeMcpServer {
    
    private final RecipeService recipeService;
    private final NutritionService nutritionService;
    
    public RecipeMcpServer(RecipeService recipeService, NutritionService nutritionService) {
        this.recipeService = recipeService;
        this.nutritionService = nutritionService;
    }
    
    /**
     * MCP Server Info Endpoint.
     * Returns server capabilities and metadata.
     */
    @GetMapping("/info")
    public McpServerInfo getInfo() {
        return new McpServerInfo(
            "recipe-finder-mcp",
            "1.0.0",
            "AI-powered recipe discovery and meal planning",
            new ServerCapabilities(true, true, true)
        );
    }
    
    /**
     * List Available Tools.
     * MCP clients call this to discover what tools this server provides.
     */
    @GetMapping("/tools")
    public ListToolsResponse listTools() {
        return new ListToolsResponse(List.of(
            new Tool(
                "search_recipes",
                "Search for recipes by ingredients, cuisine, or dietary restrictions",
                new ToolInput(
                    "object",
                    Map.of(
                        "query", new Property("string", "Search query (e.g., 'pasta with tomatoes')"),
                        "cuisine", new Property("string", "Filter by cuisine (e.g., 'Italian', 'Mexican')"),
                        "dietary", new Property("array", "Dietary restrictions (e.g., ['vegan', 'gluten-free'])"),
                        "maxPrepTime", new Property("integer", "Maximum prep time in minutes")
                    ),
                    List.of("query")
                )
            ),
            new Tool(
                "get_recipe_details",
                "Get full details for a specific recipe including ingredients and instructions",
                new ToolInput(
                    "object",
                    Map.of("recipeId", new Property("string", "Unique recipe identifier")),
                    List.of("recipeId")
                )
            ),
            new Tool(
                "analyze_nutrition",
                "Get nutritional information for a recipe",
                new ToolInput(
                    "object",
                    Map.of("recipeId", new Property("string", "Recipe identifier")),
                    List.of("recipeId")
                )
            ),
            new Tool(
                "generate_shopping_list",
                "Create a shopping list from selected recipes",
                new ToolInput(
                    "object",
                    Map.of(
                        "recipeIds", new Property("array", "List of recipe IDs"),
                        "servings", new Property("integer", "Number of servings per recipe")
                    ),
                    List.of("recipeIds")
                )
            ),
            new Tool(
                "check_ingredient_substitutes",
                "Find alternatives for a specific ingredient",
                new ToolInput(
                    "object",
                    Map.of(
                        "ingredient", new Property("string", "Ingredient to substitute"),
                        "reason", new Property("string", "Reason for substitution (e.g., 'allergy', 'preference')")
                    ),
                    List.of("ingredient")
                )
            )
        ));
    }
    
    /**
     * List Available Prompts.
     * Prompts are reusable prompt templates that MCP clients can use.
     */
    @GetMapping("/prompts")
    public ListPromptsResponse listPrompts() {
        return new ListPromptsResponse(List.of(
            new Prompt(
                "recipe_critic",
                "Professional recipe review and improvement suggestions",
                List.of(new PromptArgument(
                    "recipe_text",
                    "The recipe to review (full text)",
                    true
                ))
            ),
            new Prompt(
                "meal_planner",
                "Generate a weekly meal plan based on preferences",
                List.of(
                    new PromptArgument("dietary_restrictions", "Dietary restrictions", false),
                    new PromptArgument("cuisine_preferences", "Preferred cuisines", false),
                    new PromptArgument("servings", "Number of people to serve", false)
                )
            ),
            new Prompt(
                "cooking_instructor",
                "Step-by-step cooking guidance with tips and techniques",
                List.of(new PromptArgument(
                    "recipe_name",
                    "Name of the recipe to get guidance for",
                    true
                ))
            )
        ));
    }
    
    /**
     * Execute a Tool.
     * MCP clients call this to invoke a specific tool with arguments.
     */
    @PostMapping("/tools/execute")
    public ToolExecutionResponse executeTool(@RequestBody ToolExecutionRequest request) {
        try {
            Object result = switch (request.toolName()) {
                case "search_recipes" -> executeSearchRecipes(request.arguments());
                case "get_recipe_details" -> executeGetRecipeDetails(request.arguments());
                case "analyze_nutrition" -> executeAnalyzeNutrition(request.arguments());
                case "generate_shopping_list" -> executeGenerateShoppingList(request.arguments());
                case "check_ingredient_substitutes" -> executeCheckSubstitutes(request.arguments());
                default -> throw new IllegalArgumentException("Unknown tool: " + request.toolName());
            };
            
            return new ToolExecutionResponse(true, result, null);
            
        } catch (Exception e) {
            return new ToolExecutionResponse(false, null, e.getMessage());
        }
    }
    
    /**
     * Get a Prompt.
     * Returns the full prompt text with arguments filled in.
     */
    @PostMapping("/prompts/get")
    public GetPromptResponse getPrompt(@RequestBody GetPromptRequest request) {
        try {
            String promptText = switch (request.promptName()) {
                case "recipe_critic" -> buildRecipeCriticPrompt(request.arguments());
                case "meal_planner" -> buildMealPlannerPrompt(request.arguments());
                case "cooking_instructor" -> buildCookingInstructorPrompt(request.arguments());
                default -> throw new IllegalArgumentException("Unknown prompt: " + request.promptName());
            };
            
            return new GetPromptResponse(true, promptText, null);
            
        } catch (Exception e) {
            return new GetPromptResponse(false, null, e.getMessage());
        }
    }
    
    // ========== Tool Implementations ==========
    
    private Object executeSearchRecipes(Map<String, Object> args) {
        String query = (String) args.get("query");
        String cuisine = (String) args.get("cuisine");
        @SuppressWarnings("unchecked")
        List<String> dietary = (List<String>) args.get("dietary");
        Integer maxPrepTime = (Integer) args.get("maxPrepTime");
        
        // Call actual recipe service
        return recipeService.search(query, cuisine, dietary, maxPrepTime);
    }
    
    private Object executeGetRecipeDetails(Map<String, Object> args) {
        String recipeId = (String) args.get("recipeId");
        return recipeService.getById(recipeId);
    }
    
    private Object executeAnalyzeNutrition(Map<String, Object> args) {
        String recipeId = (String) args.get("recipeId");
        return nutritionService.analyze(recipeId);
    }
    
    private Object executeGenerateShoppingList(Map<String, Object> args) {
        @SuppressWarnings("unchecked")
        List<String> recipeIds = (List<String>) args.get("recipeIds");
        Integer servings = (Integer) args.getOrDefault("servings", 4);
        return recipeService.generateShoppingList(recipeIds, servings);
    }
    
    private Object executeCheckSubstitutes(Map<String, Object> args) {
        String ingredient = (String) args.get("ingredient");
        String reason = (String) args.getOrDefault("reason", "preference");
        return recipeService.findSubstitutes(ingredient, reason);
    }
    
    // ========== Prompt Builders ==========
    
    private String buildRecipeCriticPrompt(Map<String, Object> args) {
        String recipeText = (String) args.get("recipe_text");
        
        return """
            You are a professional chef with 20 years of experience in fine dining.
            Review the following recipe and provide constructive feedback on:
            
            1. Ingredient proportions and balance
            2. Cooking techniques and timing
            3. Flavor profile and seasoning
            4. Presentation suggestions
            5. Potential improvements
            
            Recipe:
            %s
            
            Provide specific, actionable feedback that will help improve this recipe.
            """.formatted(recipeText);
    }
    
    private String buildMealPlannerPrompt(Map<String, Object> args) {
        String dietary = (String) args.getOrDefault("dietary_restrictions", "none");
        String cuisines = (String) args.getOrDefault("cuisine_preferences", "varied");
        String servings = (String) args.getOrDefault("servings", "4");
        
        return """
            Create a balanced weekly meal plan (7 days, dinner only) with:
            
            - Dietary restrictions: %s
            - Cuisine preferences: %s
            - Servings per meal: %s people
            
            For each day, provide:
            1. Recipe name
            2. Brief description
            3. Prep + cook time
            4. Key ingredients
            
            Ensure variety in proteins, vegetables, and cuisines throughout the week.
            """.formatted(dietary, cuisines, servings);
    }
    
    private String buildCookingInstructorPrompt(Map<String, Object> args) {
        String recipeName = (String) args.get("recipe_name");
        
        return """
            You are a patient cooking instructor helping someone make: %s
            
            Provide detailed, step-by-step guidance including:
            
            1. Mise en place (prep work)
            2. Detailed cooking steps with timing
            3. Visual and sensory cues (what to look for, smell, hear)
            4. Common mistakes to avoid
            5. Professional tips and techniques
            
            Explain WHY each step matters for the final result.
            Use clear, encouraging language suitable for home cooks.
            """.formatted(recipeName);
    }
}

// ========== MCP Data Models ==========

record McpServerInfo(
    String name,
    String version,
    String description,
    ServerCapabilities capabilities
) {}

record ServerCapabilities(
    boolean tools,
    boolean prompts,
    boolean resources
) {}

record ListToolsResponse(List<Tool> tools) {}

record Tool(
    String name,
    String description,
    ToolInput inputSchema
) {}

record ToolInput(
    String type,
    Map<String, Property> properties,
    List<String> required
) {}

record Property(String type, String description) {}

record ListPromptsResponse(List<Prompt> prompts) {}

record Prompt(
    String name,
    String description,
    List<PromptArgument> arguments
) {}

record PromptArgument(
    String name,
    String description,
    boolean required
) {}

record ToolExecutionRequest(
    String toolName,
    Map<String, Object> arguments
) {}

record ToolExecutionResponse(
    boolean success,
    Object result,
    String error
) {}

record GetPromptRequest(
    String promptName,
    Map<String, Object> arguments
) {}

record GetPromptResponse(
    boolean success,
    String promptText,
    String error
) {}

// ========== Service Stubs ==========
// In a real implementation, these would be proper @Service beans

@Service
class RecipeService {
    public List<Map<String, Object>> search(String query, String cuisine, 
                                            List<String> dietary, Integer maxPrepTime) {
        // Implement actual search logic
        return List.of(
            Map.of("id", "1", "name", "Pasta Carbonara", "cuisine", "Italian")
        );
    }
    
    public Map<String, Object> getById(String id) {
        return Map.of("id", id, "name", "Sample Recipe");
    }
    
    public List<String> generateShoppingList(List<String> recipeIds, int servings) {
        return List.of("200g pasta", "100g bacon", "2 eggs");
    }
    
    public List<String> findSubstitutes(String ingredient, String reason) {
        return List.of("Alternative 1", "Alternative 2");
    }
}

@Service
class NutritionService {
    public Map<String, Object> analyze(String recipeId) {
        return Map.of("calories", 450, "protein", 20, "carbs", 50, "fat", 15);
    }
}

/*
 * USAGE WITH CLAUDE DESKTOP
 * 
 * 1. Add to Claude Desktop config (~/.config/claude/config.json):
 * 
 * {
 *   "mcpServers": {
 *     "recipe-finder": {
 *       "url": "http://localhost:8080/mcp",
 *       "description": "Recipe search and meal planning"
 *     }
 *   }
 * }
 * 
 * 2. Restart Claude Desktop
 * 
 * 3. In Claude, use the tools:
 *    "Search for Italian pasta recipes under 30 minutes"
 *    Claude will automatically use the search_recipes tool
 * 
 * 
 * USAGE WITH CUSTOM MCP CLIENT
 * 
 * // Discover tools
 * GET http://localhost:8080/mcp/tools
 * 
 * // Execute a tool
 * POST http://localhost:8080/mcp/tools/execute
 * {
 *   "toolName": "search_recipes",
 *   "arguments": {
 *     "query": "pasta carbonara",
 *     "cuisine": "Italian",
 *     "maxPrepTime": 30
 *   }
 * }
 * 
 * // Get a prompt
 * POST http://localhost:8080/mcp/prompts/get
 * {
 *   "promptName": "recipe_critic",
 *   "arguments": {
 *     "recipe_text": "Boil pasta, fry bacon, mix with eggs..."
 *   }
 * }
 * 
 * 
 * BENEFITS OF MCP:
 * 
 * ✅ Standard Protocol: Works with any MCP-compatible client
 * ✅ Discoverability: Tools and prompts are self-describing
 * ✅ Type Safety: Input schemas define expected parameters
 * ✅ Composability: AI can chain multiple tools together
 * ✅ Separation of Concerns: Logic stays in your app, AI handles the UX
 */
