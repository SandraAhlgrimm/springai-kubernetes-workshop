package com.example.structured;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Examples of type-safe structured outputs with Spring AI.
 * 
 * Benefits:
 * - Type safety at compile time
 * - Automatic JSON parsing
 * - Validation support
 * - IDE autocomplete
 */

// ========== Data Models ==========

record Recipe(
    String name,
    String cuisine,
    Difficulty difficulty,
    int prepTimeMinutes,
    int cookTimeMinutes,
    int servings,
    List<Ingredient> ingredients,
    List<String> instructions,
    NutritionInfo nutrition,
    List<String> tags
) {}

record Ingredient(
    String name,
    double amount,
    String unit,
    boolean optional
) {}

record NutritionInfo(
    int calories,
    int protein,
    int carbs,
    int fat,
    int fiber,
    int sodium
) {}

enum Difficulty {
    EASY, MEDIUM, HARD, EXPERT
}

record RecipeAnalysis(
    String cuisine,
    Difficulty difficulty,
    CostLevel cost,
    List<String> dietaryRestrictions,
    SkillLevel skillLevel,
    List<String> requiredEquipment,
    List<String> techniques
) {}

enum CostLevel { LOW, MEDIUM, HIGH, PREMIUM }
enum SkillLevel { BEGINNER, INTERMEDIATE, ADVANCED, PROFESSIONAL }

record RecipeComparison(
    String criteria,
    List<RankedRecipe> rankedRecipes
) {}

record RankedRecipe(
    String name,
    int rank,
    double score,
    String reasoning
) {}

// ========== Service Implementation ==========

@Service
class StructuredRecipeService {
    
    private final ChatClient chatClient;
    
    StructuredRecipeService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
            .defaultSystem("""
                You are a culinary expert. When asked for structured data,
                provide accurate, complete information in the requested format.
                All measurements should use metric units.
                """)
            .build();
    }
    
    /**
     * Get a complete type-safe Recipe object.
     * 
     * The LLM generates JSON matching the Recipe structure,
     * which is automatically parsed and validated.
     */
    public Recipe getRecipe(String ingredients) {
        return chatClient.prompt()
            .user("""
                Create a detailed recipe using these ingredients: {ingredients}
                
                Provide complete information including:
                - Realistic prep and cook times
                - Exact measurements for all ingredients
                - Step-by-step instructions
                - Accurate nutritional information
                - Relevant tags (e.g., "quick", "healthy", "vegetarian")
                """)
            .param("ingredients", ingredients)
            .call()
            .entity(Recipe.class);
    }
    
    /**
     * Extract multiple recipes from unstructured text.
     * 
     * Useful for parsing recipe books, blog posts, or PDFs.
     */
    public List<Recipe> extractRecipesFromText(String text) {
        return chatClient.prompt()
            .user("""
                Extract all recipes from the following text.
                Convert each recipe into structured format.
                
                Text: {text}
                """)
            .param("text", text)
            .call()
            .entity(new ParameterizedTypeReference<List<Recipe>>() {});
    }
    
    /**
     * Analyze a recipe and extract metadata.
     * 
     * This provides insights without generating a full recipe.
     */
    public RecipeAnalysis analyzeRecipe(String recipeText) {
        return chatClient.prompt()
            .user("""
                Analyze this recipe and extract:
                - Cuisine type (e.g., Italian, French, Japanese)
                - Difficulty level
                - Estimated cost (LOW/MEDIUM/HIGH/PREMIUM)
                - Dietary restrictions (vegan, gluten-free, dairy-free, etc.)
                - Required skill level
                - Necessary equipment
                - Cooking techniques involved
                
                Recipe:
                {recipe}
                """)
            .param("recipe", recipeText)
            .call()
            .entity(RecipeAnalysis.class);
    }
    
    /**
     * Compare multiple recipes based on criteria.
     * 
     * The LLM ranks and scores recipes, providing reasoning.
     */
    public RecipeComparison compareRecipes(List<String> recipes, String criteria) {
        return chatClient.prompt()
            .user("""
                Compare these recipes based on: {criteria}
                
                Rank them from best to worst, providing:
                - Numerical score (0-100)
                - Clear reasoning for the ranking
                
                Recipes:
                {recipes}
                """)
            .param("criteria", criteria)
            .param("recipes", String.join("\n\n---\n\n", recipes))
            .call()
            .entity(RecipeComparison.class);
    }
    
    /**
     * Generate recipe variations as a list.
     * 
     * Demonstrates working with collections of structured data.
     */
    public List<Recipe> generateVariations(Recipe baseRecipe, int count) {
        return chatClient.prompt()
            .user("""
                Create {count} variations of this recipe:
                
                {recipe}
                
                Each variation should:
                - Change 2-3 ingredients
                - Modify the preparation technique
                - Maintain similar difficulty level
                - Have a unique name
                """)
            .param("count", count)
            .param("recipe", baseRecipe.name() + ": " + 
                   String.join(", ", baseRecipe.ingredients()
                       .stream()
                       .map(Ingredient::name)
                       .toList()))
            .call()
            .entity(new ParameterizedTypeReference<List<Recipe>>() {});
    }
    
    /**
     * Convert recipe between measurement systems.
     * 
     * LLM handles the complex conversion logic.
     */
    public Recipe convertToImperial(Recipe metricRecipe) {
        return chatClient.prompt()
            .user("""
                Convert all measurements in this recipe from metric to imperial units:
                - grams to ounces/pounds
                - milliliters to cups/tablespoons
                - Celsius to Fahrenheit
                
                Keep everything else the same.
                
                Recipe: {recipe}
                """)
            .param("recipe", formatRecipe(metricRecipe))
            .call()
            .entity(Recipe.class);
    }
    
    // Helper method
    private String formatRecipe(Recipe recipe) {
        return """
            %s (%s, %s)
            Ingredients: %s
            """.formatted(
                recipe.name(),
                recipe.cuisine(),
                recipe.difficulty(),
                recipe.ingredients().stream()
                    .map(i -> i.amount() + " " + i.unit() + " " + i.name())
                    .collect(java.util.stream.Collectors.joining(", "))
            );
    }
}

/*
 * USAGE EXAMPLES
 * 
 * 1. Get a structured recipe:
 * 
 * Recipe recipe = service.getRecipe("chicken, broccoli, rice");
 * System.out.println("Recipe: " + recipe.name());
 * System.out.println("Difficulty: " + recipe.difficulty());
 * System.out.println("Prep time: " + recipe.prepTimeMinutes() + " min");
 * System.out.println("Calories: " + recipe.nutrition().calories());
 * 
 * recipe.ingredients().forEach(ing -> 
 *     System.out.println("- " + ing.amount() + " " + ing.unit() + " " + ing.name())
 * );
 * 
 * 
 * 2. Analyze a recipe:
 * 
 * RecipeAnalysis analysis = service.analyzeRecipe("""
 *     Beef Wellington
 *     Sear beef tenderloin, wrap in mushroom duxelles and puff pastry, bake at 200°C...
 *     """);
 * 
 * System.out.println("Skill level: " + analysis.skillLevel());
 * System.out.println("Cost: " + analysis.cost());
 * System.out.println("Equipment: " + String.join(", ", analysis.requiredEquipment()));
 * 
 * 
 * 3. Compare recipes:
 * 
 * RecipeComparison comparison = service.compareRecipes(
 *     List.of("Pasta Carbonara", "Spaghetti Bolognese", "Pesto Pasta"),
 *     "authenticity of Italian cuisine"
 * );
 * 
 * comparison.rankedRecipes().forEach(r -> 
 *     System.out.println(r.rank() + ". " + r.name() + 
 *                        " (score: " + r.score() + ") - " + r.reasoning())
 * );
 * 
 * 
 * 4. Extract from unstructured text:
 * 
 * String blogPost = "Today I made carbonara! I used 400g spaghetti, 200g guanciale...";
 * List<Recipe> extracted = service.extractRecipesFromText(blogPost);
 * extracted.forEach(r -> System.out.println("Found: " + r.name()));
 * 
 * 
 * BENEFITS:
 * 
 * ✅ Type Safety: Compile-time checks, no runtime surprises
 * ✅ Validation: Integrate Bean Validation (@NotNull, @Min, @Max)
 * ✅ Documentation: Self-documenting code with records
 * ✅ Tooling: Full IDE support (autocomplete, refactoring)
 * ✅ Testing: Easy to mock and verify structured data
 * 
 * 
 * BEST PRACTICES:
 * 
 * 1. Use records for immutable data
 * 2. Provide clear field names that the LLM can understand
 * 3. Use enums for fixed sets of values
 * 4. Add validation annotations (@NotBlank, @Positive, etc.)
 * 5. Include examples in the prompt for complex structures
 * 6. Handle parsing errors gracefully
 */
