package com.example.hybrid;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Hybrid search combines semantic (vector) search with keyword filtering.
 * 
 * Use cases:
 * - Finding recipes by ingredients (semantic) AND dietary requirements (keyword)
 * - Searching by cooking technique (semantic) AND max prep time (numeric filter)
 * - Discovering similar recipes (semantic) AND specific cuisine (category filter)
 */
@Service
class HybridSearchService {
    
    private final VectorStore vectorStore;
    
    HybridSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    
    /**
     * Combine semantic search with metadata filtering.
     * 
     * The vector search finds semantically similar documents,
     * then filters are applied to narrow results by exact criteria.
     */
    public List<Document> searchRecipes(String query, SearchFilters filters) {
        var searchRequest = SearchRequest.builder()
            .query(query)
            .topK(20)  // Get more candidates
            .similarityThreshold(0.65)  // Semantic similarity cutoff
            .filterExpression(buildFilterExpression(filters))
            .build();
        
        return vectorStore.similaritySearch(searchRequest);
    }
    
    /**
     * Build a filter expression from search criteria.
     * 
     * Filter syntax supports:
     * - Equality: field == 'value'
     * - Comparison: field <= 30
     * - Contains: field contains 'value'
     * - Boolean logic: AND, OR, NOT
     */
    private String buildFilterExpression(SearchFilters filters) {
        var expressions = new ArrayList<String>();
        
        // Filter by cuisine
        if (filters.cuisine() != null) {
            expressions.add("cuisine == '" + filters.cuisine() + "'");
        }
        
        // Filter by max prep time
        if (filters.maxPrepTime() != null) {
            expressions.add("prepTime <= " + filters.maxPrepTime());
        }
        
        // Filter by difficulty
        if (filters.difficulty() != null) {
            expressions.add("difficulty == '" + filters.difficulty() + "'");
        }
        
        // Filter by dietary restrictions (must match ALL)
        if (filters.dietary() != null && !filters.dietary().isEmpty()) {
            var dietaryFilters = filters.dietary().stream()
                .map(d -> "dietary contains '" + d + "'")
                .collect(Collectors.joining(" AND "));
            expressions.add("(" + dietaryFilters + ")");
        }
        
        // Filter by required ingredients (must have ALL)
        if (filters.requiredIngredients() != null && !filters.requiredIngredients().isEmpty()) {
            var ingredientFilters = filters.requiredIngredients().stream()
                .map(ing -> "ingredients contains '" + ing + "'")
                .collect(Collectors.joining(" AND "));
            expressions.add("(" + ingredientFilters + ")");
        }
        
        // Filter by excluded ingredients (must have NONE)
        if (filters.excludedIngredients() != null && !filters.excludedIngredients().isEmpty()) {
            var exclusionFilters = filters.excludedIngredients().stream()
                .map(ing -> "NOT (ingredients contains '" + ing + "')")
                .collect(Collectors.joining(" AND "));
            expressions.add("(" + exclusionFilters + ")");
        }
        
        // Filter by servings range
        if (filters.minServings() != null && filters.maxServings() != null) {
            expressions.add("servings >= " + filters.minServings() + 
                          " AND servings <= " + filters.maxServings());
        }
        
        // Filter by rating
        if (filters.minRating() != null) {
            expressions.add("rating >= " + filters.minRating());
        }
        
        // Combine all filters with AND
        return expressions.isEmpty() ? null : String.join(" AND ", expressions);
    }
    
    /**
     * Multi-stage search: broad semantic search then precise filtering.
     * 
     * This is useful when filters are very restrictive - we cast a wide
     * semantic net first, then narrow down.
     */
    public List<Document> multiStageSearch(String query, SearchFilters filters) {
        // Stage 1: Broad semantic search
        var broadResults = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(50)  // Get many candidates
                .similarityThreshold(0.5)  // Lower threshold
                .build()
        );
        
        // Stage 2: Apply filters in-memory
        return broadResults.stream()
            .filter(doc -> matchesFilters(doc, filters))
            .limit(10)
            .toList();
    }
    
    private boolean matchesFilters(Document doc, SearchFilters filters) {
        var metadata = doc.getMetadata();
        
        if (filters.cuisine() != null && 
            !filters.cuisine().equals(metadata.get("cuisine"))) {
            return false;
        }
        
        if (filters.maxPrepTime() != null) {
            Integer prepTime = (Integer) metadata.get("prepTime");
            if (prepTime == null || prepTime > filters.maxPrepTime()) {
                return false;
            }
        }
        
        // Add more filter logic as needed
        
        return true;
    }
    
    /**
     * Search with dynamic scoring based on preferences.
     * 
     * Adjusts the relevance score based on how well the recipe
     * matches user preferences (not hard filters).
     */
    public List<ScoredDocument> searchWithPreferences(
            String query, 
            UserPreferences preferences) {
        
        var results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(30)
                .similarityThreshold(0.6)
                .build()
        );
        
        // Re-score based on preferences
        return results.stream()
            .map(doc -> new ScoredDocument(
                doc,
                calculatePreferenceScore(doc, preferences)
            ))
            .sorted(Comparator.comparingDouble(ScoredDocument::score).reversed())
            .limit(10)
            .toList();
    }
    
    private double calculatePreferenceScore(Document doc, UserPreferences prefs) {
        double score = 0.0;
        var metadata = doc.getMetadata();
        
        // Boost score for preferred cuisines
        if (prefs.favoriteCuisines().contains(metadata.get("cuisine"))) {
            score += 0.2;
        }
        
        // Boost for quick recipes if user prefers them
        Integer prepTime = (Integer) metadata.get("prepTime");
        if (prefs.prefersQuickRecipes() && prepTime != null && prepTime <= 30) {
            score += 0.15;
        }
        
        // Boost for difficulty match
        if (metadata.get("difficulty").equals(prefs.skillLevel())) {
            score += 0.1;
        }
        
        // Penalty for excluded ingredients
        @SuppressWarnings("unchecked")
        List<String> ingredients = (List<String>) metadata.get("ingredients");
        if (ingredients != null) {
            long excludedCount = ingredients.stream()
                .filter(prefs.dislikedIngredients()::contains)
                .count();
            score -= (excludedCount * 0.1);
        }
        
        return score;
    }
}

// ========== Data Models ==========

record SearchFilters(
    String cuisine,
    Integer maxPrepTime,
    String difficulty,
    List<String> dietary,
    List<String> requiredIngredients,
    List<String> excludedIngredients,
    Integer minServings,
    Integer maxServings,
    Double minRating
) {
    // Builder for convenience
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String cuisine;
        private Integer maxPrepTime;
        private String difficulty;
        private List<String> dietary = new ArrayList<>();
        private List<String> requiredIngredients = new ArrayList<>();
        private List<String> excludedIngredients = new ArrayList<>();
        private Integer minServings;
        private Integer maxServings;
        private Double minRating;
        
        public Builder cuisine(String cuisine) {
            this.cuisine = cuisine;
            return this;
        }
        
        public Builder maxPrepTime(Integer maxPrepTime) {
            this.maxPrepTime = maxPrepTime;
            return this;
        }
        
        public Builder difficulty(String difficulty) {
            this.difficulty = difficulty;
            return this;
        }
        
        public Builder addDietary(String restriction) {
            this.dietary.add(restriction);
            return this;
        }
        
        public Builder requireIngredient(String ingredient) {
            this.requiredIngredients.add(ingredient);
            return this;
        }
        
        public Builder excludeIngredient(String ingredient) {
            this.excludedIngredients.add(ingredient);
            return this;
        }
        
        public Builder servings(int min, int max) {
            this.minServings = min;
            this.maxServings = max;
            return this;
        }
        
        public Builder minRating(Double rating) {
            this.minRating = rating;
            return this;
        }
        
        public SearchFilters build() {
            return new SearchFilters(
                cuisine, maxPrepTime, difficulty, dietary,
                requiredIngredients, excludedIngredients,
                minServings, maxServings, minRating
            );
        }
    }
}

record UserPreferences(
    List<String> favoriteCuisines,
    List<String> dislikedIngredients,
    String skillLevel,
    boolean prefersQuickRecipes
) {}

record ScoredDocument(
    Document document,
    double score
) {}

/*
 * USAGE EXAMPLES
 * 
 * 1. Basic hybrid search:
 * 
 * var filters = SearchFilters.builder()
 *     .cuisine("Italian")
 *     .maxPrepTime(30)
 *     .addDietary("vegetarian")
 *     .build();
 * 
 * var results = hybridSearch.searchRecipes("creamy pasta", filters);
 * 
 * 
 * 2. Search with exclusions:
 * 
 * var filters = SearchFilters.builder()
 *     .requireIngredient("chicken")
 *     .excludeIngredient("peanuts")
 *     .excludeIngredient("shellfish")
 *     .minRating(4.0)
 *     .build();
 * 
 * var results = hybridSearch.searchRecipes("spicy Asian dish", filters);
 * 
 * 
 * 3. Search with user preferences:
 * 
 * var preferences = new UserPreferences(
 *     List.of("Italian", "French", "Japanese"),
 *     List.of("cilantro", "olives"),
 *     "INTERMEDIATE",
 *     true  // prefers quick recipes
 * );
 * 
 * var scored = hybridSearch.searchWithPreferences("pasta dinner", preferences);
 * scored.forEach(s -> 
 *     System.out.println(s.document().getContent() + " (score: " + s.score() + ")")
 * );
 * 
 * 
 * WHEN TO USE HYBRID SEARCH:
 * 
 * ✅ Use semantic search when:
 *    - Query is natural language ("hearty winter comfort food")
 *    - Looking for conceptual matches ("quick weeknight dinner")
 *    - Fuzzy matching is desired ("pasta" should match "spaghetti", "penne")
 * 
 * ✅ Use keyword filters when:
 *    - Exact matches required (cuisine, dietary restrictions)
 *    - Numerical constraints (prep time, servings, calories)
 *    - Boolean criteria (has ingredient X, excludes ingredient Y)
 * 
 * ✅ Combine both when:
 *    - "Find healthy Asian recipes under 30 minutes" 
 *      → Semantic: "healthy Asian" | Filters: prepTime <= 30
 *    - "Vegan pasta dishes similar to carbonara"
 *      → Semantic: "pasta carbonara" | Filters: dietary contains 'vegan'
 * 
 * 
 * PERFORMANCE TIPS:
 * 
 * 1. Always apply filters at the vector store level when possible
 * 2. Use lower topK with restrictive filters
 * 3. Cache common filter combinations
 * 4. Index metadata fields that you filter on frequently
 * 5. Monitor query latency and adjust thresholds
 */
