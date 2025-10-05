# Module 2 Solution - Recipe Finder with RAG and Function Calling

This solution extends Module 1 with enterprise features: RAG (Retrieval Augmented Generation), function calling, and a web UI.

## What's New in This Solution

- ✅ **RAG**: Search your own recipe collection (PDF documents)
- ✅ **Vector Store**: Redis-based semantic search
- ✅ **Function Calling**: AI can check your fridge ingredients
- ✅ **Web UI**: Beautiful Thymeleaf interface
- ✅ **Multiple AI Strategies**: Basic, RAG-only, function calling, or both

## Project Structure

```
solution/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── RecipeFinderApplication.java
│   │   │   ├── RecipeFinderConfiguration.java
│   │   │   └── recipe/
│   │   │       ├── Recipe.java
│   │   │       ├── RecipeService.java          # ⭐ RAG + Function Calling
│   │   │       ├── RecipeResource.java         # REST API
│   │   │       ├── RecipeUiController.java     # Web UI
│   │   │       └── FetchRecipeData.java        # Form data
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── prompts/
│   │       │   ├── fix-json-response
│   │       │   ├── recipe-for-ingredients
│   │       │   ├── recipe-for-available-ingredients
│   │       │   └── prefer-own-recipe           # RAG prompt
│   │       ├── templates/
│   │       │   └── index.html                  # Thymeleaf UI
│   │       └── static/
│   │           ├── custom.css
│   │           └── placeholder.png
├── build.gradle
├── compose.yaml                                 # Ollama + Redis
└── german_recipes.pdf                          # Sample document for RAG
```

## Running the Solution

### Start the Application

```bash
cd solution
./gradlew bootRun
```

Docker Compose will automatically start:
- Ollama (port 11434)
- Redis (port 6379)

### Access the Application

- **Web UI**: http://localhost:8080
- **REST API**: http://localhost:8080/api/recipes?ingredients=chicken,tomatoes

## Features Deep Dive

### 1. Basic Recipe Generation

**API**:
```bash
curl "http://localhost:8080/api/recipes?ingredients=pasta,tomatoes"
```

**What happens**:
- LLM generates a creative recipe
- No external data sources used

### 2. Function Calling

**UI**: Check "Use available ingredients"

**What happens**:
```java
@Tool(description = "Fetches ingredients that are available at home")
List<String> fetchIngredientsAvailableAtHome() {
    return List.of("bacon", "onions");  // From config
}
```

The AI autonomously decides to call this method and includes fridge ingredients in the recipe!

**Log output**:
```
AI called fetchIngredientsAvailableAtHome tool!
```

### 3. RAG (Retrieval Augmented Generation)

**UI**: Check "Prefer own recipes"

**What happens**:
1. Your query is converted to an embedding vector
2. Redis finds similar recipe chunks (semantic search)
3. QuestionAnswerAdvisor injects context into the prompt
4. LLM generates answer using your documents

**How to add documents**:
```java
recipeService.addRecipeDocumentForRag(
    new ClassPathResource("german_recipes.pdf"),
    10,   // Top margin
    10    // Bottom margin
);
```

### 4. Combined: RAG + Function Calling

**UI**: Check both boxes!

**What happens**:
- AI searches your recipe collection
- AI checks your fridge ingredients
- Generates recipe using both sources

**This is powerful**: Custom knowledge + real-time data!

## Configuration

### application.yaml

```yaml
spring:
  ai:
    model:
      chat: ollama
      embedding: ollama
    ollama:
      chat:
        model: llama3.2
        options.temperature: 0.5
      embedding.model: llama3.2
    vectorstore.redis.initialize-schema: true
  data.redis:
    host: localhost
    port: 6379

app:
  available-ingredients-in-fridge: bacon,onions
```

### Adjusting RAG Parameters

In `RecipeService.java`:
```java
var ragSearchRequest = SearchRequest.builder()
    .topK(2)                    // Number of documents to retrieve
    .similarityThreshold(0.7)   // Minimum similarity (0-1)
    .build();
```

- **Higher topK**: More context, slower, more tokens
- **Higher threshold**: Fewer but more relevant results
- **Lower threshold**: More results, may include less relevant

## Testing Each Feature

### Test 1: Basic Generation
```
Ingredients: chicken, rice
☐ Use available ingredients
☐ Prefer own recipes
```
Result: Creative AI-generated recipe

### Test 2: Function Calling
```
Ingredients: chicken, rice
☑ Use available ingredients
☐ Prefer own recipes
```
Result: Recipe includes bacon & onions from fridge!

### Test 3: RAG
```
Ingredients: german potato
☐ Use available ingredients
☑ Prefer own recipes
```
Result: Recipe from `german_recipes.pdf`

### Test 4: Everything!
```
Ingredients: pasta
☑ Use available ingredients
☑ Prefer own recipes
```
Result: Italian pasta recipe using fridge ingredients

## Architecture

```
User Input
    ↓
RecipeService.fetchRecipeFor()
    ↓
┌───────────────┴────────────────┐
│  preferAvailableIngredients?   │
│  preferOwnRecipes?              │
└─┬──────────┬──────────┬────────┘
  │          │          │
  v          v          v
Basic    Function   RAG    RAG+Function
  │        Call      │         │
  │          │       │         │
  └──────────┴───────┴─────────┘
              ↓
         ChatClient
        .prompt()
        .user(prompt)
        .tools(this)      ← Function Calling
        .advisors(rag)    ← RAG
        .call()
        .entity(Recipe)
              ↓
         Ollama LLM
```

## Key Code Patterns

### Pattern 1: Conditional Feature Activation

```java
Recipe fetchRecipeFor(List<String> ingredients, 
                      boolean useFunctionCalling, 
                      boolean useRAG) {
    var prompt = chatClient.prompt().user(...);
    
    if (useFunctionCalling) {
        prompt = prompt.tools(this);
    }
    
    if (useRAG) {
        prompt = prompt.advisors(ragAdvisor);
    }
    
    return prompt.call().entity(Recipe.class);
}
```

### Pattern 2: ETL Pipeline for Documents

```java
// Extract
var documentReader = new PagePdfDocumentReader(pdf, config);

// Transform
var documents = new TokenTextSplitter().apply(documentReader.get());

// Load
vectorStore.accept(documents);
```

### Pattern 3: Tool Definition

```java
@Tool(description = "Clear description for AI")
ReturnType methodName(Parameters params) {
    // Implementation
}
```

## Dependencies

### Core Dependencies
```gradle
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
implementation 'org.springframework.ai:spring-ai-starter-model-ollama'
```

### RAG Dependencies
```gradle
implementation 'org.springframework.ai:spring-ai-advisors-vector-store'
implementation 'org.springframework.ai:spring-ai-starter-vector-store-redis'
implementation 'org.springframework.ai:spring-ai-pdf-document-reader'
```

## Troubleshooting

**Redis connection error**:
```bash
docker-compose up redis -d
redis-cli ping  # Should return PONG
```

**No documents found in RAG**:
```bash
# Check vector store
redis-cli
> KEYS *
> HGETALL <key>
```

**Function not called**:
- Check `@Tool` annotation is present
- Verify `.tools(this)` in prompt chain
- Check LLM model supports function calling (llama3.2 does!)

**Slow responses**:
- First request loads model (normal)
- RAG adds overhead (semantic search + more tokens)
- Consider using smaller model for development

**PDF not loading**:
- Check file is in `src/main/resources/`
- Verify margins in `addRecipeDocumentForRag()`
- Check PDF is text-based (not scanned images)

## What's Different from Module 1?

| Feature | Module 1 | Module 2 |
|---------|----------|----------|
| Vector Store | ❌ | ✅ Redis |
| RAG | ❌ | ✅ PDF ingestion |
| Function Calling | ❌ | ✅ @Tool methods |
| UI | ❌ REST only | ✅ Thymeleaf |
| Dependencies | 3 | 8 |
| Complexity | Simple | Enterprise |

## Performance Tips

1. **Cache embeddings**: Don't re-embed same documents
2. **Adjust chunk size**: Balance context vs. speed
3. **Use Redis properly**: It's fast but configure connection pooling
4. **Monitor token usage**: RAG adds significant tokens
5. **Consider model quantization**: Faster inference

## Next Steps

In **Module 3**, you'll deploy this to Kubernetes:
- Containerize the application
- Deploy Ollama as a service
- Configure Redis persistence
- Implement horizontal scaling
- Add health checks and monitoring

## Resources

- [Spring AI RAG Documentation](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- [Redis Vector Similarity](https://redis.io/docs/stack/search/reference/vectors/)
- [Spring AI Function Calling](https://docs.spring.io/spring-ai/reference/api/functions.html)
- [Thymeleaf Documentation](https://www.thymeleaf.org/documentation.html)
