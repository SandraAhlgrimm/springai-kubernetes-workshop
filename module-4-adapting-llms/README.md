# Module 4: Adapting Large Language Models

## Overview
Learn how to customize and adapt LLMs to your specific domain and use cases. This module covers fine-tuning, prompt engineering, model quantization, and creating domain-specific models that excel at your particular tasks.

## Learning Objectives
- Understand different adaptation techniques
- Master advanced prompt engineering
- Learn about fine-tuning and when to use it
- Implement model quantization for efficiency
- Create custom model configurations
- Build domain-specific AI assistants
- Evaluate and compare model performance

## Topics Covered
- Advanced prompt engineering techniques
- Few-shot and zero-shot learning
- Prompt templates and chaining
- Fine-tuning vs. RAG: when to use each
- Model quantization (GGUF, GPTQ)
- Creating Modelfiles for custom configurations
- Parameter-efficient fine-tuning (LoRA, QLoRA)
- Evaluation metrics and benchmarking
- Model selection for specific tasks
- Context length optimization
- Streaming responses for better UX
- Structured outputs with Spring AI
- Advanced RAG patterns
- Vector database optimization
- Hybrid search strategies

## Adaptation Strategies

### 1. Prompt Engineering (Easiest, No Training)
- **Best for**: Quick wins, general tasks
- **Pros**: Fast, no training required, easily modified
- **Cons**: Limited customization, token costs
- **Techniques**: Few-shot examples, chain-of-thought, role playing

### 2. RAG (Medium, No Training)
- **Best for**: Knowledge-intensive tasks, up-to-date information
- **Pros**: Easy to update, source attribution, factual
- **Cons**: Requires vector DB, retrieval overhead
- **Use cases**: Q&A systems, documentation search

### 3. Fine-tuning (Hard, Requires Training)
- **Best for**: Specialized domains, consistent style/format
- **Pros**: Best performance, smallest models
- **Cons**: Time-consuming, requires data and expertise
- **Use cases**: Domain-specific language, custom formats

### 4. Quantization (Efficiency)
- **Best for**: Resource-constrained environments
- **Pros**: Faster inference, less memory
- **Cons**: Slight quality degradation
- **Formats**: GGUF (4-bit, 5-bit, 8-bit)

## Advanced Prompt Engineering

### Chain-of-Thought Prompting
```
Think step by step to solve this problem:
1. First, identify the key information
2. Then, apply the relevant formula
3. Finally, calculate the result

Problem: If a train travels 120 miles in 2 hours, what is its average speed?
```

### Few-Shot Learning
```java
String fewShotPrompt = """
    Convert natural language to SQL:
    
    Example 1:
    Input: "Show me all customers from Germany"
    Output: SELECT * FROM customers WHERE country = 'Germany';
    
    Example 2:
    Input: "Count orders in January 2024"
    Output: SELECT COUNT(*) FROM orders WHERE MONTH(order_date) = 1 AND YEAR(order_date) = 2024;
    
    Now convert:
    Input: "%s"
    Output:
    """.formatted(userInput);
```

### Role-Based Prompting
```
You are an expert Java developer with 15 years of experience in Spring Boot.
Your responses are concise, follow best practices, and include code examples.
Always consider security, performance, and maintainability.

User: How do I implement JWT authentication in Spring Boot?
```

## Tasks

### 1. Create Custom Modelfile

```modelfile
# Modelfile
FROM llama3.2

# Set temperature
PARAMETER temperature 0.7

# Set context window
PARAMETER num_ctx 4096

# Set system prompt
SYSTEM """
You are a Java code reviewer focused on Spring Boot applications.
You provide constructive feedback on:
- Code quality and best practices
- Security vulnerabilities
- Performance optimizations
- Spring Boot specific patterns

Keep responses concise and actionable.
"""
```

Create the custom model:
```bash
ollama create java-reviewer -f Modelfile
ollama run java-reviewer
```

### 2. Implement Prompt Templates in Spring AI

```java
@Service
public class PromptTemplateService {
    
    private final ChatClient chatClient;
    
    public String reviewCode(String code) {
        PromptTemplate template = new PromptTemplate("""
            Review this Java code and provide feedback:
            
            Code:
            ```java
            {code}
            ```
            
            Focus on:
            1. Security issues
            2. Performance problems
            3. Best practice violations
            4. Potential bugs
            
            Format your response as:
            - Issue: [description]
            - Severity: [High/Medium/Low]
            - Recommendation: [what to do]
            """);
        
        Prompt prompt = template.create(Map.of("code", code));
        return chatClient.call(prompt).getResult().getOutput().getContent();
    }
}
```

### 3. Implement Model Comparison

```java
@Service
public class ModelComparison {
    
    private final ChatClient chatClient;
    
    public Map<String, ModelResult> compareModels(String prompt) {
        List<String> models = List.of("llama3.2", "mistral", "phi3");
        
        return models.stream()
            .collect(Collectors.toMap(
                model -> model,
                model -> {
                    long start = System.currentTimeMillis();
                    String response = callModel(model, prompt);
                    long duration = System.currentTimeMillis() - start;
                    
                    return new ModelResult(
                        response,
                        duration,
                        response.split("\\s+").length
                    );
                }
            ));
    }
}
```

### 4. Quantize Models for Production

```bash
# Download full precision model
ollama pull llama3.2

# Create quantized version (4-bit)
ollama create llama3.2-q4 -q Q4_K_M -f Modelfile

# Compare sizes
ollama list
```

### 5. Fine-tune a Model (Advanced)

Using `ollama` with a training dataset:

```bash
# Prepare training data (JSONL format)
cat > training_data.jsonl << EOF
{"prompt": "What is Spring Boot?", "completion": "Spring Boot is..."}
{"prompt": "How to use @Autowired?", "completion": "@Autowired is..."}
EOF

# Fine-tune (requires appropriate tools)
# Note: This is simplified; actual fine-tuning may require additional tools
ollama create my-spring-expert -f Modelfile --adapter training_data.jsonl
```

### 6. Implement Evaluation Framework

```java
@Service
public class ModelEvaluator {
    
    public EvaluationResult evaluate(String model, List<TestCase> testCases) {
        int correct = 0;
        long totalLatency = 0;
        
        for (TestCase testCase : testCases) {
            long start = System.currentTimeMillis();
            String response = chatClient.call(testCase.prompt());
            long latency = System.currentTimeMillis() - start;
            
            totalLatency += latency;
            if (testCase.expectedAnswer().equals(response.trim())) {
                correct++;
            }
        }
        
        return new EvaluationResult(
            (double) correct / testCases.size(),  // Accuracy
            (double) totalLatency / testCases.size()  // Avg latency
        );
    }
}
```

### 7. Optimize Context Window Usage

```java
@Service
public class ContextOptimizer {
    
    public String optimizeContext(String fullContext, String query, int maxTokens) {
        // Rank context chunks by relevance
        List<String> chunks = splitIntoChunks(fullContext);
        List<ScoredChunk> scored = chunks.stream()
            .map(chunk -> new ScoredChunk(
                chunk,
                calculateRelevance(chunk, query)
            ))
            .sorted(Comparator.comparing(ScoredChunk::score).reversed())
            .toList();
        
        // Select top chunks that fit within token limit
        StringBuilder optimizedContext = new StringBuilder();
        int tokenCount = 0;
        
        for (ScoredChunk chunk : scored) {
            int chunkTokens = estimateTokens(chunk.text());
            if (tokenCount + chunkTokens > maxTokens) break;
            
            optimizedContext.append(chunk.text()).append("\n\n");
            tokenCount += chunkTokens;
        }
        
        return optimizedContext.toString();
    }
}
```

## Model Selection Guide

| Task Type | Recommended Model | Size | Notes |
|-----------|------------------|------|-------|
| Code Generation | Llama 3.2 3B | 2GB | Fast, good for simple code |
| Code Review | Mistral 7B | 4GB | Better reasoning |
| Documentation | Phi-3 | 2.3GB | Concise, accurate |
| Complex Analysis | Llama 3.1 70B | 40GB+ | Best quality, needs GPU |
| Classification | Phi-3 Mini | 2.3GB | Fast, efficient |
| Translation | Mistral | 4GB | Good multilingual |

## Best Practices

1. **Start Simple**: Try prompt engineering before fine-tuning
2. **Measure Everything**: Track accuracy, latency, and costs
3. **Use Appropriate Models**: Bigger isn't always better
4. **Quantize for Production**: 4-bit quantization gives 95% quality at 25% size
5. **Cache Aggressively**: Many queries are similar
6. **A/B Test**: Compare different approaches with real users
7. **Version Control**: Track prompt templates and model versions
8. **Monitor Drift**: Model performance can degrade over time

## Advanced RAG Patterns

### 1. Streaming Responses for Better UX

When dealing with long-form content, streaming provides immediate feedback:

```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamRecipe(@RequestParam String ingredients) {
    return chatClient.prompt()
        .user(us -> us
            .text("Generate a detailed recipe for: {ingredients}")
            .param("ingredients", ingredients))
        .stream()
        .content();
}
```

**Why streaming?**
- Users see results immediately
- Reduces perceived latency
- Better for mobile/slow connections
- Can display partial results

### 2. Structured Outputs with Spring AI

Instead of parsing strings, get typed objects:

```java
public record RecipeAnalysis(
    String cuisine,
    String difficulty,
    int prepTimeMinutes,
    List<String> dietaryRestrictions,
    double estimatedCost
) {}

RecipeAnalysis analysis = chatClient.prompt()
    .user("Analyze this recipe: " + recipeText)
    .call()
    .entity(RecipeAnalysis.class);

System.out.println("Cuisine: " + analysis.cuisine());
System.out.println("Prep time: " + analysis.prepTimeMinutes() + " minutes");
```

**Benefits:**
- Type safety
- No manual parsing
- Works with complex nested structures
- Handles arrays and objects

### 3. Advanced Vector Database Strategies

#### Hybrid Search (Keyword + Semantic)
```java
// Combine traditional keyword search with vector similarity
var hybridResults = vectorStore.similaritySearch(
    SearchRequest.builder()
        .query(userQuery)
        .topK(10)
        .similarityThreshold(0.7)
        .filterExpression("cuisine == 'Italian' AND difficulty == 'easy'")
        .build()
);
```

#### Multi-Vector Strategies
```java
// Use different embeddings for different aspects
var titleEmbedding = embeddingModel.embed("Recipe title");
var ingredientEmbedding = embeddingModel.embed("Ingredients list");
var instructionEmbedding = embeddingModel.embed("Instructions");

// Store all three vectors for better retrieval
```

#### Re-ranking Retrieved Documents
```java
// First pass: Get top 20 candidates
var candidates = vectorStore.similaritySearch(query, 20);

// Second pass: Re-rank with more sophisticated model
var reranked = rerankingService.rerank(candidates, query);
var topResults = reranked.stream().limit(5).toList();
```

### 4. Vector Database Optimization

#### Choosing the Right Index
```yaml
# Redis configuration for optimal performance
spring:
  data.redis:
    vectorstore:
      index-type: FLAT  # Exact search, slower but accurate
      # or
      index-type: HNSW  # Approximate, faster for large datasets
      hnsw:
        m: 16           # Number of connections
        ef-construction: 200  # Build quality
        ef-runtime: 10        # Search quality
```

#### Chunk Size Optimization
```java
// Experiment with different chunk sizes
var splitter = TokenTextSplitter.builder()
    .withChunkSize(512)        // Tokens per chunk
    .withChunkOverlap(50)      // Overlap between chunks
    .build();

// Smaller chunks: More precise, more results needed
// Larger chunks: More context, fewer results needed
```

### 5. RAG with Multiple Vector Stores

```java
@Service
class MultiStoreRAGService {
    
    private final VectorStore recipeStore;
    private final VectorStore ingredientStore;
    private final ChatClient chatClient;
    
    Recipe findRecipe(String query) {
        // Search recipes
        var recipes = recipeStore.similaritySearch(query, 3);
        
        // Search ingredients separately
        var ingredients = ingredientStore.similaritySearch(query, 5);
        
        // Combine contexts
        String context = """
            Recipes:
            %s
            
            Available Ingredients:
            %s
            """.formatted(
                recipes.stream().map(Document::getContent).collect(Collectors.joining("\n")),
                ingredients.stream().map(Document::getContent).collect(Collectors.joining("\n"))
            );
        
        return chatClient.prompt()
            .system("Use the provided context to answer")
            .user(context + "\n\nQuery: " + query)
            .call()
            .entity(Recipe.class);
    }
}
```

## Code Examples
Complete code examples will be provided in the workshop repository.

## Next Steps
Continue to [Module 5: AI Agents](../module-5-ai-agents/README.md) to learn about building autonomous AI agents with memory and MCP.

## Resources
- [Ollama Modelfile Documentation](https://github.com/ollama/ollama/blob/main/docs/modelfile.md)
- [Prompt Engineering Guide](https://www.promptingguide.ai/)
- [LoRA Fine-tuning](https://huggingface.co/docs/peft/conceptual_guides/lora)
- [Model Quantization Guide](https://huggingface.co/docs/optimum/concept_guides/quantization)
- [Spring AI Prompt Templates](https://docs.spring.io/spring-ai/reference/api/prompts.html)
- [Spring AI Structured Outputs](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
- [Spring AI Streaming](https://docs.spring.io/spring-ai/reference/api/streaming.html)
- [Vector Database Best Practices](https://www.pinecone.io/learn/vector-database/)
- [Evaluation Metrics for LLMs](https://www.deeplearning.ai/short-courses/evaluating-debugging-generative-ai/)

