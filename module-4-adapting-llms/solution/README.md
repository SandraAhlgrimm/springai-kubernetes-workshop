# Module 4 Solution - Advanced LLM Optimization

This solution demonstrates advanced techniques for optimizing and customizing LLMs, including prompt engineering, streaming responses, structured outputs, and vector database optimization.

## What's Included

- ✅ Custom Ollama Modelfiles
- ✅ Prompt template examples
- ✅ Streaming response implementation
- ✅ Structured output examples
- ✅ Vector database optimization configurations
- ✅ Model comparison and evaluation scripts
- ✅ Advanced RAG patterns

## Solution Structure

```
solution/
├── README.md                           # This file
├── examples/
│   ├── Modelfile.java-reviewer        # Custom model for code review
│   ├── Modelfile.recipe-expert        # Recipe-specialized model
│   ├── streaming-example.java         # Streaming responses
│   ├── structured-output-example.java # Type-safe outputs
│   ├── hybrid-search-example.java     # Keyword + semantic search
│   ├── multi-vector-example.java      # Multiple embeddings
│   ├── reranking-example.java         # Document re-ranking
│   └── evaluation-framework.java      # Model benchmarking
└── configs/
    ├── redis-hnsw.yaml                # HNSW index config
    └── redis-flat.yaml                # FLAT index config
```

## 1. Custom Modelfiles

### Java Code Reviewer Model

**File**: `examples/Modelfile.java-reviewer`

```modelfile
# Modelfile for Java Code Reviewer
FROM llama3.2

# Optimize for code review
PARAMETER temperature 0.3
PARAMETER top_p 0.9
PARAMETER num_ctx 8192

# System prompt for code review
SYSTEM """
You are an expert Java code reviewer with 15 years of experience in Spring Boot applications.

Your responsibilities:
- Identify security vulnerabilities (SQL injection, XSS, authentication issues)
- Spot performance problems (N+1 queries, memory leaks, inefficient algorithms)
- Enforce best practices (SOLID principles, design patterns, naming conventions)
- Detect potential bugs (null pointer exceptions, race conditions, edge cases)

Output format:
Issue: [Clear description]
Severity: [Critical/High/Medium/Low]
Line: [Line number if applicable]
Recommendation: [Specific actionable fix]

Be constructive and educational. Explain WHY something is an issue.
"""
```

**Usage**:
```bash
cd solution/examples
ollama create java-reviewer -f Modelfile.java-reviewer
ollama run java-reviewer

# Test it
> Review this code:
> public String getUser(String id) {
>   return "SELECT * FROM users WHERE id = " + id;
> }
```

### Recipe Expert Model

**File**: `examples/Modelfile.recipe-expert`

```modelfile
# Modelfile for Recipe Expert
FROM llama3.2

# Higher creativity for recipes
PARAMETER temperature 0.8
PARAMETER top_p 0.95
PARAMETER num_ctx 4096

SYSTEM """
You are a professional chef specializing in creating detailed, practical recipes.

Your expertise:
- Balanced flavors and proper seasoning
- Cooking techniques and timing
- Ingredient substitutions
- Dietary accommodations (vegan, gluten-free, etc.)
- Presentation and plating

Always provide:
1. Prep time and cook time
2. Serving size
3. Difficulty level
4. Complete ingredient list with measurements (metric)
5. Step-by-step instructions
6. Pro tips for best results

Your recipes should be tested, practical, and achievable for home cooks.
"""
```

**Usage**:
```bash
ollama create recipe-expert -f Modelfile.recipe-expert
ollama run recipe-expert "Create a pasta carbonara recipe"
```

## 2. Streaming Responses

**File**: `examples/streaming-example.java`

```java
package com.example.streaming;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
class StreamingRecipeController {
    
    private final ChatClient chatClient;
    
    StreamingRecipeController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    
    // Stream recipe generation token by token
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
    
    // Stream structured recipe (JSON chunks)
    @GetMapping(value = "/api/recipes/stream-json",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamRecipeJson(@RequestParam String ingredients) {
        return chatClient.prompt()
            .user("Generate recipe JSON for: " + ingredients)
            .stream()
            .content();
    }
}

// Client-side usage (JavaScript)
/*
const eventSource = new EventSource('/api/recipes/stream?ingredients=pasta,tomatoes');

eventSource.onmessage = (event) => {
    document.getElementById('recipe').innerHTML += event.data;
};

eventSource.onerror = () => {
    eventSource.close();
};
*/
```

## 3. Structured Outputs

**File**: `examples/structured-output-example.java`

```java
package com.example.structured;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.List;

// Define typed data structures
record Recipe(
    String name,
    String cuisine,
    Difficulty difficulty,
    int prepTimeMinutes,
    int cookTimeMinutes,
    int servings,
    List<Ingredient> ingredients,
    List<String> instructions,
    NutritionInfo nutrition
) {}

record Ingredient(
    String name,
    double amount,
    String unit
) {}

record NutritionInfo(
    int calories,
    int protein,
    int carbs,
    int fat
) {}

enum Difficulty {
    EASY, MEDIUM, HARD
}

@Service
class StructuredRecipeService {
    
    private final ChatClient chatClient;
    
    StructuredRecipeService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    
    // Get type-safe Recipe object
    Recipe getRecipe(String ingredients) {
        return chatClient.prompt()
            .user("Create a recipe with: " + ingredients)
            .call()
            .entity(Recipe.class);
    }
    
    // Analyze multiple recipes
    List<Recipe> analyzeRecipeBook(String pdfContent) {
        return chatClient.prompt()
            .user("Extract all recipes from this text: " + pdfContent)
            .call()
            .entity(new ParameterizedTypeReference<List<Recipe>>() {});
    }
    
    // Get recipe analysis
    RecipeAnalysis analyzeRecipe(String recipeText) {
        return chatClient.prompt()
            .user("""
                Analyze this recipe and extract:
                - Cuisine type
                - Difficulty level
                - Estimated cost
                - Dietary restrictions
                - Skill level required
                
                Recipe: %s
                """.formatted(recipeText))
            .call()
            .entity(RecipeAnalysis.class);
    }
}

record RecipeAnalysis(
    String cuisine,
    Difficulty difficulty,
    CostLevel cost,
    List<String> dietaryRestrictions,
    SkillLevel skillLevel,
    List<String> requiredEquipment
) {}

enum CostLevel { LOW, MEDIUM, HIGH }
enum SkillLevel { BEGINNER, INTERMEDIATE, ADVANCED, PROFESSIONAL }
```

## 4. Hybrid Search (Keyword + Semantic)

**File**: `examples/hybrid-search-example.java`

```java
package com.example.hybrid;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
class HybridSearchService {
    
    private final VectorStore vectorStore;
    
    HybridSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    
    // Combine semantic search with metadata filtering
    List<Recipe> searchRecipes(String query, SearchFilters filters) {
        var searchRequest = SearchRequest.builder()
            .query(query)
            .topK(20)
            .similarityThreshold(0.65)
            .filterExpression(buildFilterExpression(filters))
            .build();
        
        return vectorStore.similaritySearch(searchRequest)
            .stream()
            .map(doc -> parseRecipe(doc.getContent()))
            .toList();
    }
    
    private String buildFilterExpression(SearchFilters filters) {
        var expressions = new ArrayList<String>();
        
        if (filters.cuisine() != null) {
            expressions.add("cuisine == '" + filters.cuisine() + "'");
        }
        if (filters.maxPrepTime() != null) {
            expressions.add("prepTime <= " + filters.maxPrepTime());
        }
        if (filters.difficulty() != null) {
            expressions.add("difficulty == '" + filters.difficulty() + "'");
        }
        if (filters.dietary() != null && !filters.dietary().isEmpty()) {
            var dietaryFilters = filters.dietary().stream()
                .map(d -> "dietary contains '" + d + "'")
                .collect(Collectors.joining(" AND "));
            expressions.add("(" + dietaryFilters + ")");
        }
        
        return expressions.isEmpty() ? null : String.join(" AND ", expressions);
    }
}

record SearchFilters(
    String cuisine,
    Integer maxPrepTime,
    String difficulty,
    List<String> dietary
) {}
```

## 5. Multi-Vector Strategy

**File**: `examples/multi-vector-example.java`

```java
package com.example.multivector;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
class MultiVectorRecipeService {
    
    private final EmbeddingModel embeddingModel;
    private final VectorStore titleStore;
    private final VectorStore ingredientStore;
    private final VectorStore instructionStore;
    
    // Store different aspects with different embeddings
    void indexRecipe(Recipe recipe) {
        // Title embedding - optimized for short text
        var titleEmbedding = embeddingModel.embed(recipe.name());
        titleStore.add(List.of(new Document(
            recipe.name(),
            Map.of("recipeId", recipe.id(), "type", "title"),
            titleEmbedding
        )));
        
        // Ingredient embedding - optimized for ingredient matching
        var ingredientText = String.join(", ", recipe.ingredients());
        var ingredientEmbedding = embeddingModel.embed(ingredientText);
        ingredientStore.add(List.of(new Document(
            ingredientText,
            Map.of("recipeId", recipe.id(), "type", "ingredients"),
            ingredientEmbedding
        )));
        
        // Instruction embedding - optimized for technique matching
        var instructionText = String.join("\n", recipe.instructions());
        var instructionEmbedding = embeddingModel.embed(instructionText);
        instructionStore.add(List.of(new Document(
            instructionText,
            Map.of("recipeId", recipe.id(), "type", "instructions"),
            instructionEmbedding
        )));
    }
    
    // Search across all vector stores and combine results
    List<Recipe> searchAllAspects(String query) {
        var titleMatches = titleStore.similaritySearch(query, 5);
        var ingredientMatches = ingredientStore.similaritySearch(query, 5);
        var instructionMatches = instructionStore.similaritySearch(query, 5);
        
        // Combine and deduplicate by recipeId
        return Stream.of(titleMatches, ingredientMatches, instructionMatches)
            .flatMap(List::stream)
            .map(doc -> doc.getMetadata().get("recipeId"))
            .distinct()
            .map(this::loadRecipe)
            .toList();
    }
}
```

## 6. Document Re-ranking

**File**: `examples/reranking-example.java`

```java
package com.example.reranking;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.IntStream;

@Service
class ReRankingService {
    
    private final ChatClient chatClient;
    
    // Two-stage retrieval: broad search then precise ranking
    List<Document> searchWithReRanking(String query, VectorStore store) {
        // Stage 1: Get more candidates with lower threshold
        var candidates = store.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(20)  // Get 20 candidates
                .similarityThreshold(0.5)  // Lower threshold
                .build()
        );
        
        // Stage 2: Use LLM to re-rank by relevance
        var reranked = reRankWithLLM(query, candidates);
        
        // Return top 5 after re-ranking
        return reranked.stream().limit(5).toList();
    }
    
    private List<Document> reRankWithLLM(String query, List<Document> candidates) {
        // Create scoring prompt
        String scoringPrompt = buildScoringPrompt(query, candidates);
        
        // Get relevance scores from LLM
        var scores = chatClient.prompt()
            .user(scoringPrompt)
            .call()
            .entity(new ParameterizedTypeReference<List<Double>>() {});
        
        // Combine documents with scores and sort
        return IntStream.range(0, candidates.size())
            .boxed()
            .sorted((i, j) -> Double.compare(scores.get(j), scores.get(i)))
            .map(candidates::get)
            .toList();
    }
    
    private String buildScoringPrompt(String query, List<Document> candidates) {
        var numbered = IntStream.range(0, candidates.size())
            .mapToObj(i -> "Document " + i + ":\n" + candidates.get(i).getContent())
            .collect(Collectors.joining("\n\n"));
        
        return """
            Query: %s
            
            Rate the relevance of each document to the query on a scale of 0.0 to 1.0.
            Return only a JSON array of scores, e.g., [0.9, 0.7, 0.5, ...]
            
            %s
            """.formatted(query, numbered);
    }
}
```

## 7. Vector Database Optimization

### Redis HNSW Configuration

**File**: `configs/redis-hnsw.yaml`

```yaml
spring:
  ai:
    vectorstore:
      redis:
        # Use HNSW for large datasets (faster, approximate)
        index-type: HNSW
        
        # HNSW parameters
        hnsw:
          m: 16                    # Connections per node (default: 16)
                                    # Higher = better recall, more memory
          ef-construction: 200      # Build-time quality (default: 200)
                                    # Higher = better index, slower build
          ef-runtime: 10            # Search-time quality (default: 10)
                                    # Higher = better recall, slower search
        
        # Embedding settings
        embedding-dimension: 1536   # For text-embedding-3-small
        distance-metric: COSINE     # COSINE, L2, IP
        
        initialize-schema: true
```

### Redis FLAT Configuration

**File**: `configs/redis-flat.yaml`

```yaml
spring:
  ai:
    vectorstore:
      redis:
        # Use FLAT for smaller datasets (exact search)
        index-type: FLAT
        
        # Embedding settings
        embedding-dimension: 1536
        distance-metric: COSINE
        
        initialize-schema: true
        
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2s
      
      # Connection pool settings
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 2
          max-wait: 2s
```

## 8. Evaluation Framework

**File**: `examples/evaluation-framework.java`

```java
package com.example.evaluation;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
class ModelEvaluationService {
    
    record TestCase(
        String prompt,
        String expectedAnswer,
        List<String> acceptableAnswers
    ) {}
    
    record EvaluationResult(
        String modelName,
        double accuracy,
        double avgLatencyMs,
        int totalTests,
        int passedTests,
        List<FailedTest> failures
    ) {}
    
    record FailedTest(
        String prompt,
        String expected,
        String actual,
        long latencyMs
    ) {}
    
    // Evaluate model on test suite
    EvaluationResult evaluate(String modelName, List<TestCase> testCases) {
        int passed = 0;
        long totalLatency = 0;
        var failures = new ArrayList<FailedTest>();
        
        for (var testCase : testCases) {
            long start = System.currentTimeMillis();
            
            String response = callModel(modelName, testCase.prompt());
            
            long latency = System.currentTimeMillis() - start;
            totalLatency += latency;
            
            boolean isCorrect = testCase.acceptableAnswers().stream()
                .anyMatch(expected -> similarity(response, expected) > 0.8);
            
            if (isCorrect) {
                passed++;
            } else {
                failures.add(new FailedTest(
                    testCase.prompt(),
                    testCase.expectedAnswer(),
                    response,
                    latency
                ));
            }
        }
        
        return new EvaluationResult(
            modelName,
            (double) passed / testCases.size(),
            (double) totalLatency / testCases.size(),
            testCases.size(),
            passed,
            failures
        );
    }
    
    // Compare multiple models
    Map<String, EvaluationResult> compareModels(
        List<String> models,
        List<TestCase> testCases
    ) {
        return models.parallelStream()
            .collect(Collectors.toMap(
                model -> model,
                model -> evaluate(model, testCases)
            ));
    }
    
    // Benchmark latency
    LatencyBenchmark benchmarkLatency(String modelName, int iterations) {
        var latencies = new ArrayList<Long>();
        
        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            callModel(modelName, "Hello, how are you?");
            long duration = (System.nanoTime() - start) / 1_000_000; // ms
            latencies.add(duration);
        }
        
        latencies.sort(Long::compareTo);
        
        return new LatencyBenchmark(
            modelName,
            latencies.stream().mapToLong(Long::longValue).average().orElse(0),
            latencies.get(0),
            latencies.get(latencies.size() - 1),
            latencies.get(latencies.size() / 2)  // median
        );
    }
}

record LatencyBenchmark(
    String modelName,
    double avgMs,
    long minMs,
    long maxMs,
    long medianMs
) {}
```

## Usage Examples

### Test Streaming
```bash
curl -N "http://localhost:8080/api/recipes/stream?ingredients=pasta,garlic,olive oil"
```

### Test Structured Output
```java
var service = context.getBean(StructuredRecipeService.class);
Recipe recipe = service.getRecipe("chicken, broccoli, rice");

System.out.println("Recipe: " + recipe.name());
System.out.println("Difficulty: " + recipe.difficulty());
System.out.println("Prep time: " + recipe.prepTimeMinutes() + " min");
recipe.ingredients().forEach(ing -> 
    System.out.println("- " + ing.amount() + " " + ing.unit() + " " + ing.name())
);
```

### Run Evaluation
```java
var evaluator = context.getBean(ModelEvaluationService.class);
var testCases = List.of(
    new TestCase("What is 2+2?", "4", List.of("4", "four")),
    new TestCase("Capital of France?", "Paris", List.of("Paris"))
);

var results = evaluator.compareModels(
    List.of("llama3.2", "mistral", "phi3"),
    testCases
);

results.forEach((model, result) -> 
    System.out.println(model + ": " + result.accuracy() + " accuracy, " + 
                       result.avgLatencyMs() + "ms avg latency")
);
```

## Performance Tips

1. **Streaming**: Always use for >100 token responses
2. **HNSW Index**: Use for >10,000 documents
3. **Re-ranking**: Only for top candidates, not all results
4. **Multi-vector**: When different aspects matter (title vs content)
5. **Caching**: Cache expensive embeddings and LLM calls

## Next Steps

- Experiment with different temperature settings
- Test various chunk sizes for your use case
- Benchmark models on your specific tasks
- Implement A/B testing for prompt variations
- Monitor production metrics (latency, accuracy, cost)

## Resources

- [Ollama Modelfile Docs](https://github.com/ollama/ollama/blob/main/docs/modelfile.md)
- [Spring AI Streaming](https://docs.spring.io/spring-ai/reference/api/streaming.html)
- [Redis Vector Similarity](https://redis.io/docs/stack/search/reference/vectors/)
- [HNSW Algorithm](https://arxiv.org/abs/1603.09320)
