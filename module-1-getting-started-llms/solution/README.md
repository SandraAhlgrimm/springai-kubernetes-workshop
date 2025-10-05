# Module 1 Solution - Basic Recipe Generator

This is the complete solution for Module 1, demonstrating basic Spring AI integration with Ollama.

## What This Solution Includes

- ✅ Spring Boot 3.5.6 application
- ✅ Spring AI 1.0.2 with Ollama integration
- ✅ ChatClient with structured output
- ✅ Prompt templates
- ✅ REST API endpoint for recipe generation
- ✅ Configuration for model parameters

## Project Structure

```
solution/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── RecipeFinderApplication.java    # Main Spring Boot app
│   │   │   ├── RecipeFinderConfiguration.java  # ChatClient configuration
│   │   │   └── recipe/
│   │   │       ├── Recipe.java                  # Data model
│   │   │       ├── RecipeService.java           # Business logic
│   │   │       └── RecipeResource.java          # REST controller
│   │   └── resources/
│   │       ├── application.yaml                 # Spring configuration
│   │       └── prompts/
│   │           ├── fix-json-response            # System prompt
│   │           └── recipe-for-ingredients       # User prompt template
│   └── test/
├── build.gradle                                  # Gradle build file
└── compose.yaml                                  # Docker Compose for Ollama
```

## Running the Solution

### Option 1: Using Docker Compose (Recommended)

```bash
cd solution
./gradlew bootRun
```

Docker Compose will automatically start Ollama for you!

### Option 2: Using Existing Ollama

If you have Ollama running already:

```bash
# Make sure Ollama is running
ollama serve

# Pull the model
ollama pull llama3.2

# Run the application
./gradlew bootRun
```

## Testing the API

### Generate a Recipe

```bash
curl "http://localhost:8080/api/recipes?ingredients=chicken,tomatoes,garlic"
```

### Example Response

```json
{
  "name": "Garlic Tomato Chicken",
  "description": "A flavorful and aromatic chicken dish with fresh tomatoes and garlic",
  "ingredients": [
    "4 chicken breasts",
    "4 large tomatoes, diced",
    "6 cloves garlic, minced",
    "2 tbsp olive oil",
    "1 tsp dried basil",
    "Salt and pepper to taste"
  ],
  "instructions": [
    "Heat olive oil in a large skillet over medium heat",
    "Season chicken breasts with salt and pepper",
    "Cook chicken for 6-7 minutes per side until golden",
    "Remove chicken and set aside",
    "In the same skillet, sauté garlic for 1 minute",
    "Add diced tomatoes and basil, cook for 5 minutes",
    "Return chicken to skillet and simmer for 10 minutes",
    "Serve hot with your favorite side dish"
  ],
  "imageUrl": ""
}
```

## Key Features Demonstrated

### 1. Structured Output with `.entity()`

Instead of parsing JSON manually, Spring AI automatically converts the LLM response to a Java object:

```java
Recipe recipe = chatClient.prompt()
    .user(prompt)
    .call()
    .entity(Recipe.class);
```

### 2. Prompt Templates

Prompts are externalized as resource files with placeholders:

```
Ingredients: """
{ingredients}
"""
```

And populated dynamically:

```java
.user(us -> us
    .text(recipeForIngredientsPromptResource)
    .param("ingredients", String.join(",", ingredients)))
```

### 3. System Prompts

Global instructions for the model are configured via `defaultSystem()`:

```java
ChatClient chatClient(ChatClient.Builder builder) {
    return builder
        .defaultSystem(fixJsonResponsePromptResource)
        .build();
}
```

## Configuration

### application.yaml

```yaml
spring:
  ai:
    model:
      chat: ollama
    ollama:
      chat:
        model: llama3.2
        options.temperature: 0.7
```

### Adjusting Temperature

- **0.0-0.3**: Consistent, deterministic recipes
- **0.5-0.7**: Balanced creativity (recommended)
- **0.8-1.0**: Maximum creativity and variation

Try different temperatures:
```bash
# Edit application.yaml and change temperature value
# Then restart the application
```

## Dependencies

Key dependencies in `build.gradle`:

```gradle
implementation 'org.springframework.boot:spring-boot-starter-web'
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'org.springframework.ai:spring-ai-starter-model-ollama'
```

## What's Different in Module 2?

In Module 2, we'll add:
- **RAG**: Vector database integration with Redis
- **Function Calling**: Let AI call Java methods
- **Web UI**: Thymeleaf-based user interface
- **Document Ingestion**: Load custom recipe PDFs

## Troubleshooting

**Error: Connection refused to localhost:11434**
- Ensure Ollama is running: `ollama serve`
- Or use Docker Compose (it starts Ollama automatically)

**Error: Model not found**
```bash
ollama pull llama3.2
```

**Slow first response**
- First request loads the model into memory
- Subsequent requests will be faster

**Invalid JSON response**
- Check that the system prompt is loaded
- Try adjusting temperature (lower = more consistent)

## Learn More

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Ollama Documentation](https://ollama.ai/docs)
- [Spring AI Ollama Integration](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
