# Module 2: Building Intelligent Enterprise Applications

## Overview
Learn how to build production-grade enterprise applications with AI capabilities. This module covers advanced Spring AI features, integration patterns, RAG (Retrieval Augmented Generation), and real-world use cases.

## Learning Objectives
- Build enterprise-grade AI applications with Spring AI
- Implement Retrieval Augmented Generation (RAG)
- Integrate vector databases for semantic search
- Implement streaming responses for better UX
- Handle structured outputs and function calling
- Design AI-powered enterprise features
- Implement proper error handling and fallbacks

## Topics Covered
- Spring AI Architecture and Components
- Retrieval Augmented Generation (RAG)
- Vector databases (Redis, PostgreSQL with pgvector)
- Document processing and embeddings
- Function calling and tool use
- Building Web UIs for AI applications
- Combining RAG with function calling
- Production-ready Spring AI applications

## Enterprise AI Use Cases

### 1. Intelligent Document Search (RAG)
Enable semantic search across your company's documents, manuals, and knowledge base.

### 2. Code Assistant
Help developers with code generation, documentation, and bug fixes.

### 3. Customer Support Automation
Provide intelligent responses based on your product documentation and support history.

### 4. Data Analysis and Reporting
Generate insights and reports from structured and unstructured data.

### 5. Content Generation
Create marketing copy, documentation, or reports tailored to your domain.

## Implementing RAG (Retrieval Augmented Generation)

RAG combines the power of LLMs with your own data:

1. **Ingest**: Load your documents
2. **Chunk**: Split into manageable pieces
3. **Embed**: Convert to vector embeddings
4. **Store**: Save in a vector database
5. **Retrieve**: Find relevant chunks for user queries
6. **Generate**: Augment LLM with retrieved context

### Why RAG?
- ✅ Reduce hallucinations with factual data
- ✅ Keep knowledge up-to-date without retraining
- ✅ Provide source citations
- ✅ Work with proprietary/confidential data

## Hands-On Tasks

In this module, you'll enhance the Recipe Finder from Module 1 by adding:
1. **RAG**: Search your own recipe collection (PDF documents)
2. **Function Calling**: Check what ingredients are available at home
3. **Web UI**: Build a user-friendly interface with Thymeleaf

### Prerequisites
- Completed Module 1 or start with the Module 1 solution code
- Basic understanding of vector databases

### Task 1: Add Redis Vector Store for RAG

**Goal**: Set up a vector database to store and search recipe documents.

1. Update `build.gradle` to add RAG dependencies:
```gradle
dependencies {
    // ... existing dependencies
    implementation 'org.springframework.ai:spring-ai-advisors-vector-store'
    implementation 'org.springframework.ai:spring-ai-starter-vector-store-redis'
    implementation 'org.springframework.ai:spring-ai-pdf-document-reader'
}
```

2. Update `compose.yaml` to include Redis:
```yaml
services:
  ollama:
    image: ollama/ollama
    ports:
      - "11434:11434"
  redis:
    image: redis/redis-stack-server
    ports:
      - "6379:6379"
```

3. Update `application.yaml`:
```yaml
spring:
  ai:
    vectorstore.redis.initialize-schema: true
  data.redis:
    host: localhost
    port: 6379
```

4. Add VectorStore configuration to `RecipeFinderConfiguration.java`:
```java
@Configuration
class RecipeFinderConfiguration {
    
    @Value("classpath:/prompts/fix-json-response")
    private Resource fixJsonResponsePromptResource;

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder
            .defaultSystem(fixJsonResponsePromptResource)
            .build();
    }

    // Use SimpleVectorStore if Redis is not available
    @ConditionalOnMissingBean(VectorStore.class)
    @Bean
    VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
```

### Task 2: Implement Document Ingestion (ETL Pipeline)

**Goal**: Load recipe PDFs into the vector database for semantic search.

1. Update `RecipeService.java` to add document ingestion:
```java
@Service
class RecipeService {
    
    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);
    private final ChatClient chatClient;
    private final VectorStore vectorStore;  // Add this

    @Value("classpath:/prompts/recipe-for-ingredients")
    private Resource recipeForIngredientsPromptResource;

    RecipeService(ChatClient chatClient, VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }

    // ETL pipeline: Extract, Transform, Load
    void addRecipeDocumentForRag(Resource pdfResource, 
                                  int pageTopMargin, 
                                  int pageBottomMargin) {
        log.info("Adding recipe document {} for RAG", 
                 pdfResource.getFilename());
        
        // Configure PDF reader
        var documentReaderConfig = PdfDocumentReaderConfig.builder()
            .withPageTopMargin(pageTopMargin)
            .withPageBottomMargin(pageBottomMargin)
            .build();
        
        // Extract: Parse PDF documents
        var documentReader = new PagePdfDocumentReader(
            pdfResource, documentReaderConfig);
        
        // Transform: Split text into chunks based on token count
        var documents = new TokenTextSplitter().apply(documentReader.get());
        
        // Load: Store in vector database
        vectorStore.accept(documents);
    }

    Recipe fetchRecipeFor(List<String> ingredients) {
        log.info("Generating recipe for: {}", ingredients);
        return chatClient.prompt()
                .user(us -> us
                        .text(recipeForIngredientsPromptResource)
                        .param("ingredients", String.join(",", ingredients)))
                .call()
                .entity(Recipe.class);
    }
}
```

### Task 3: Implement RAG-Enhanced Recipe Generation

**Goal**: Use QuestionAnswerAdvisor to find recipes from your own collection.

1. Create a new prompt template `src/main/resources/prompts/prefer-own-recipe`:
```
Context information is below.

---------------------
{question_answer_context}
---------------------

Given the context information and no prior knowledge, answer the query.

Follow these rules:
1. If there are recipes in the context, return the first of these recipes unchanged.
2. Otherwise, answer the query by generating a recipe.
```

2. Add RAG method to `RecipeService`:
```java
@Value("classpath:/prompts/prefer-own-recipe")
private Resource preferOwnRecipePromptResource;

Recipe fetchRecipeWithRagFor(List<String> ingredients) {
    log.info("Fetching recipe with RAG for: {}", ingredients);
    
    // Configure RAG advisor
    var ragPromptTemplate = PromptTemplate.builder()
        .resource(preferOwnRecipePromptResource)
        .build();
        
    var ragSearchRequest = SearchRequest.builder()
        .topK(2)                    // Return top 2 matches
        .similarityThreshold(0.7)   // Minimum 70% similarity
        .build();
        
    var ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
        .searchRequest(ragSearchRequest)
        .promptTemplate(ragPromptTemplate)
        .build();
    
    return chatClient.prompt()
            .user(us -> us
                    .text(recipeForIngredientsPromptResource)
                    .param("ingredients", String.join(",", ingredients)))
            .advisors(ragAdvisor)  // Add RAG capability!
            .call()
            .entity(Recipe.class);
}
```

### Task 4: Implement Function Calling

**Goal**: Let the AI check what ingredients are available at home.

1. Add configuration to `application.yaml`:
```yaml
app:
  available-ingredients-in-fridge: bacon,onions
```

2. Add to `RecipeService`:
```java
@Value("${app.available-ingredients-in-fridge}")
private List<String> availableIngredientsInFridge;

// Define a tool that the AI can call
@Tool(description = "Fetches ingredients that are available at home")
List<String> fetchIngredientsAvailableAtHome() {
    log.info("AI called fetchIngredientsAvailableAtHome tool!");
    return availableIngredientsInFridge;
}
```

3. Create new prompt `src/main/resources/prompts/recipe-for-available-ingredients`:
```
Provide a recipe that includes in the best case all of the following ingredients plus the ingredients available at home which are ordered by highest priority.

Ingredients: """
{ingredients}
"""

Add additional ingredients that are necessary for a good flavor or to create a more creative and complex meal.
The recipe should be translated to English, and with quantity in metric system.
```

4. Implement function calling method:
```java
@Value("classpath:/prompts/recipe-for-available-ingredients")
private Resource recipeForAvailableIngredientsPromptResource;

Recipe fetchRecipeWithToolCallingFor(List<String> ingredients) {
    log.info("Fetching recipe with function calling for: {}", ingredients);
    
    return chatClient.prompt()
            .user(us -> us
                    .text(recipeForAvailableIngredientsPromptResource)
                    .param("ingredients", String.join(",", ingredients)))
            .tools(this)  // Make @Tool methods available to AI!
            .call()
            .entity(Recipe.class);
}
```

### Task 5: Combine RAG and Function Calling

**Goal**: Use both techniques together for the ultimate recipe finder!

```java
Recipe fetchRecipeWithRagAndToolCallingFor(List<String> ingredients) {
    log.info("Fetching recipe with RAG AND function calling");
    
    var ragPromptTemplate = PromptTemplate.builder()
        .resource(preferOwnRecipePromptResource)
        .build();
    var ragSearchRequest = SearchRequest.builder()
        .topK(2)
        .similarityThreshold(0.7)
        .build();
    var ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
        .searchRequest(ragSearchRequest)
        .promptTemplate(ragPromptTemplate)
        .build();
    
    return chatClient.prompt()
            .user(us -> us
                    .text(recipeForAvailableIngredientsPromptResource)
                    .param("ingredients", String.join(",", ingredients)))
            .tools(this)         // Function calling
            .advisors(ragAdvisor) // RAG
            .call()
            .entity(Recipe.class);
}
```

### Task 6: Create a Unified Service Method

**Goal**: One method that handles all scenarios.

```java
Recipe fetchRecipeFor(List<String> ingredients, 
                      boolean preferAvailableIngredients, 
                      boolean preferOwnRecipes) {
    if (!preferAvailableIngredients && !preferOwnRecipes) {
        return fetchRecipeFor(ingredients);
    } else if (preferAvailableIngredients && !preferOwnRecipes) {
        return fetchRecipeWithToolCallingFor(ingredients);
    } else if (!preferAvailableIngredients && preferOwnRecipes) {
        return fetchRecipeWithRagFor(ingredients);
    } else {
        return fetchRecipeWithRagAndToolCallingFor(ingredients);
    }
}
```

### Task 7: Build a Web UI with Thymeleaf

**Goal**: Create a user-friendly interface for the recipe finder.

1. Add Thymeleaf dependency to `build.gradle`:
```gradle
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
implementation 'org.apache.commons:commons-lang3'
```

2. Create form data class `FetchRecipeData.java`:
```java
package com.example.recipe;

import java.util.Arrays;
import java.util.List;

class FetchRecipeData {
    private String ingredientsStr;
    private boolean preferAvailableIngredients = false;
    private boolean preferOwnRecipes = false;

    public List<String> ingredients() {
        return Arrays.asList(ingredientsStr.split("\\s*,\\s*"));
    }

    // Getters and setters
    public String getIngredientsStr() { return ingredientsStr; }
    public void setIngredientsStr(String ingredientsStr) { 
        this.ingredientsStr = ingredientsStr; 
    }
    public boolean isPreferAvailableIngredients() { 
        return preferAvailableIngredients; 
    }
    public void setPreferAvailableIngredients(boolean value) { 
        this.preferAvailableIngredients = value; 
    }
    public boolean isPreferOwnRecipes() { 
        return preferOwnRecipes; 
    }
    public void setPreferOwnRecipes(boolean preferOwnRecipes) { 
        this.preferOwnRecipes = preferOwnRecipes; 
    }
}
```

3. Create `RecipeUiController.java`:
```java
package com.example.recipe;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
class RecipeUiController {
    
    private final RecipeService recipeService;
    private final ChatModel chatModel;

    RecipeUiController(RecipeService recipeService, ChatModel chatModel) {
        this.recipeService = recipeService;
        this.chatModel = chatModel;
    }

    @GetMapping
    String fetchUI(Model model) {
        var aiModel = chatModel.getClass().getSimpleName()
            .replace("ChatModel", "");
        model.addAttribute("aiModel", aiModel);
        
        if (!model.containsAttribute("fetchRecipeData")) {
            model.addAttribute("fetchRecipeData", new FetchRecipeData());
        }
        return "index";
    }

    @PostMapping
    String fetchRecipeFor(FetchRecipeData fetchRecipeData, Model model) {
        Recipe recipe = recipeService.fetchRecipeFor(
            fetchRecipeData.ingredients(),
            fetchRecipeData.isPreferAvailableIngredients(),
            fetchRecipeData.isPreferOwnRecipes()
        );
        model.addAttribute("recipe", recipe);
        model.addAttribute("fetchRecipeData", fetchRecipeData);
        return fetchUI(model);
    }
}
```

4. Create `src/main/resources/templates/index.html`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Recipe Finder</title>
    <link href="custom.css" rel="stylesheet">
</head>
<body>
    <div class="wrapper">
        <header>
            <span class="title">Recipe Finder</span>
            <span class="subtitle">powered by <span th:utext="${aiModel}" /></span>
        </header>
        <div class="content-wrapper">
            <form action="#" th:action="@{/}" th:object="${fetchRecipeData}" method="post">
                <label for="ingredients">Ingredients (comma separated):</label>
                <input type="text" id="ingredients" th:field="*{ingredientsStr}" />
                
                <input type="checkbox" id="prefer-available" 
                       th:field="*{preferAvailableIngredients}" />
                <label for="prefer-available">Use available ingredients</label>
                
                <input type="checkbox" id="prefer-own-recipes" 
                       th:field="*{preferOwnRecipes}" />
                <label for="prefer-own-recipes">Prefer own recipes</label>
                
                <button type="submit">Find Recipe</button>
            </form>
            
            <div class="content" th:if="${recipe != null}">
                <div class="text">
                    <h2 th:text="${recipe.name}">Recipe Name</h2>
                    <p th:text="${recipe.description}">Description</p>
                    
                    <h4>Ingredients</h4>
                    <ul>
                        <li th:each="ingredient : ${recipe.ingredients}" 
                            th:text="${ingredient}">Ingredient</li>
                    </ul>
                    
                    <h4>Instructions</h4>
                    <ul>
                        <li th:each="instruction : ${recipe.instructions}" 
                            th:text="${instruction}">Instruction</li>
                    </ul>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
```

### Task 8: Test All Features

**Goal**: Verify RAG, function calling, and UI work together.

1. Start the application:
```bash
./gradlew bootRun
```

2. Visit http://localhost:8080

3. Test basic recipe generation:
   - Enter: `chicken, tomatoes, basil`
   - Click "Find Recipe"

4. Test function calling:
   - Enter: `pasta, tomatoes`
   - Check "Use available ingredients"
   - The AI should incorporate bacon/onions from your fridge!

5. Test RAG (if you've ingested PDFs):
   - Enter: `german potato`
   - Check "Prefer own recipes"
   - Should return recipes from your collection

## 🎯 Success Criteria

You've completed this module when:
- ✅ Vector store is configured and running
- ✅ You can ingest PDF documents
- ✅ RAG returns relevant recipes from your collection
- ✅ Function calling lets AI check your fridge
- ✅ Web UI displays recipes beautifully
- ✅ All features work together seamlessly

## 💡 Solution

The complete solution with all features is available in the [`solution/`](./solution) directory.

## Key Concepts Learned

### 1. RAG (Retrieval Augmented Generation)
Combines LLMs with your own data:
- **Extract**: Load documents with `PagePdfDocumentReader`
- **Transform**: Split into chunks with `TokenTextSplitter`
- **Load**: Store embeddings in vector database
- **Retrieve**: Find similar chunks with semantic search
- **Generate**: Augment LLM with relevant context

### 2. Function Calling (Tool Use)
Let AI call your Java methods:
```java
@Tool(description = "What this tool does")
ReturnType methodName(Parameters params) {
    // Your logic here
}
```
Then register: `.tools(this)`

### 3. Advisors
Intercept and modify AI interactions:
- `QuestionAnswerAdvisor` - Implements RAG
- Custom advisors - Add your own logic

### 4. Vector Similarity Search
Find relevant documents using:
- **topK**: Number of results to return
- **similarityThreshold**: Minimum similarity score (0-1)
- **Embeddings**: Numerical representations of text meaning

## Architecture Diagram

```
┌─────────────┐
│ User Input  │
└──────┬──────┘
       │
       v
┌─────────────────────┐
│  RecipeService      │
│  - Basic Gen        │
│  - RAG              │◄──────┐
│  - Function Call    │       │
│  - RAG + Function   │       │
└──────┬──────────────┘       │
       │                      │
       │                      │
       v                      │
┌─────────────────┐           │
│   ChatClient    │           │
│  + Advisors     │           │
│  + Tools        │           │
└──────┬──────────┘           │
       │                      │
       v                      │
┌─────────────────┐    ┌──────┴──────┐
│  Ollama LLM     │    │ VectorStore │
│  (llama3.2)     │    │   (Redis)   │
└─────────────────┘    └─────────────┘
```

## Troubleshooting

**Redis connection error?**
```bash
docker ps  # Check if Redis is running
docker-compose up redis  # Start Redis
```

**No documents found in RAG?**
```bash
# Check if documents were ingested
# Add logging to see what's stored
log.info("Vector store size: {}", vectorStore.similaritySearch("test", 100).size());
```

**Function not being called?**
- Check method has `@Tool` annotation
- Ensure `.tools(this)` is in the prompt chain
- Verify tool description is clear

**Slow responses?**
- Reduce `topK` in RAG search
- Use smaller embedding model
- Consider caching frequent queries

## Best Practices

1. **Always validate AI outputs** - Don't trust blindly
2. **Chunk documents appropriately** - Not too small, not too large (500-1000 tokens)
3. **Use clear tool descriptions** - Help AI understand when to call
4. **Set appropriate similarity thresholds** - Too high = no results, too low = irrelevant
5. **Monitor token usage** - RAG adds context to each request
6. **Implement error handling** - LLMs can fail or timeout
7. **Log AI interactions** - Debug and improve over time

## What's Next?

🎯 **In Module 3**, you'll learn to:
- Deploy this application to Kubernetes
- Scale based on load with KEDA
- Use async processing with message queues
- Implement health checks and monitoring
- Configure resource limits for LLMs

## Code Examples
The complete solution with RAG, function calling, and UI is in [`solution/`](./solution).

## Next Steps
Continue to [Module 3: Running AI Applications on Kubernetes](../module-3-running-on-kubernetes/README.md) to deploy and scale your application!

## Resources
- [Spring AI RAG Documentation](https://docs.spring.io/spring-ai/reference/api/vectordbs.html)
- [pgvector Documentation](https://github.com/pgvector/pgvector)
- [RAG Best Practices](https://www.promptingguide.ai/techniques/rag)
- [Spring AI Function Calling](https://docs.spring.io/spring-ai/reference/api/functions.html)
- [Structured Outputs Guide](https://docs.spring.io/spring-ai/reference/api/structured-output-converter.html)
