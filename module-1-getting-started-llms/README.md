# Module 1: Getting Started with Large Language Models

## Overview
In this module, you'll get hands-on experience with Large Language Models by setting up and running self-hosted LLMs locally. You'll learn how to interact with these models and integrate them into applications.

## Learning Objectives
- Install and run self-hosted LLMs locally using Ollama
- Understand different model sizes and their trade-offs
- Learn to interact with LLMs via API and CLI
- Configure model parameters (temperature, context window, etc.)
- Compare performance of different models
- Build a simple Spring Boot application with AI capabilities

## Topics Covered
- Installing Ollama for local LLM hosting
- Downloading and running popular models (Llama, Mistral, Phi)
- Model selection: understanding size vs. performance trade-offs
- Interacting with LLMs via CLI and REST API
- Model parameters: temperature, top-p, context window
- Introduction to Spring AI framework
- Creating your first AI-powered Spring Boot application
- Basic prompt engineering techniques

## Getting Started with Ollama

### What is Ollama?
Ollama is a tool that makes it easy to run large language models locally. It handles model downloads, provides a simple API, and manages model lifecycle.

### Installing Ollama

**macOS:**
```bash
brew install ollama
```

**Linux:**
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

**Windows:**
Download from [ollama.ai](https://ollama.ai/download)

### Running Your First Model

```bash
# Pull and run Llama 3.2 (3B parameters - good for laptops)
ollama run llama3.2

# Or try Mistral
ollama run mistral

# Or Phi-3 (Microsoft's small but powerful model)
ollama run phi3
```

## Model Comparison

| Model | Size | RAM Needed | Speed | Quality | Best For |
|-------|------|------------|-------|---------|----------|
| Phi-3 | 3.8GB | 8GB | Fast | Good | Development, laptops |
| Llama 3.2 | 2GB/7GB | 8GB/16GB | Fast/Medium | Good/Great | General purpose |
| Mistral | 4.1GB/7.2GB | 8GB/16GB | Medium | Great | Production |
| Llama 3.1 | 8GB-70GB | 16GB+ | Slow | Excellent | Complex tasks |

## Hands-On Tasks

In this module, you'll build a **Recipe Finder** application that generates recipes based on ingredients using AI. By the end, you'll have a working REST API that can suggest creative recipes!

### Task 1: Set Up Your Development Environment

**Goal**: Verify your environment is ready for AI development.

1. If using GitHub Codespaces, everything is already configured! Just verify:
```bash
java -version   # Should be Java 21+
ollama --version
```

2. Start Ollama (if not already running):
```bash
ollama serve
```

3. In a new terminal, pull the Llama 3.2 model:
```bash
ollama pull llama3.2
```

4. Test the model:
```bash
ollama run llama3.2 "Give me a quick recipe idea with chicken and tomatoes"
```

### Task 2: Create Your Spring Boot Project

**Goal**: Bootstrap a new Spring Boot application with Spring AI.

1. Create a new directory for your project:
```bash
mkdir recipe-finder
cd recipe-finder
```

2. Create a `build.gradle` file with these dependencies:
```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.6'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

ext {
    set('springAiVersion', "1.0.2")
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.ai:spring-ai-starter-model-ollama'
    
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}
```

3. Create the directory structure:
```bash
mkdir -p src/main/java/com/example/recipe
mkdir -p src/main/resources/prompts
```

### Task 3: Configure Spring AI with Ollama

**Goal**: Connect your application to the Ollama service.

1. Create `src/main/resources/application.yaml`:
```yaml
spring:
  application.name: RecipeFinder
  ai:
    model:
      chat: ollama
    ollama:
      chat:
        model: llama3.2
        options.temperature: 0.7
    chat.client.observations:
      log-prompt: true
      log-completion: true
```

**Understanding the configuration:**
- `temperature: 0.7` - Balanced creativity (0=focused, 1=creative)
- `log-prompt: true` - See what's sent to the model
- `log-completion: true` - See the model's full response

### Task 4: Create Your Main Application Class

**Goal**: Bootstrap the Spring Boot application.

Create `src/main/java/com/example/RecipeFinderApplication.java`:
```java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RecipeFinderApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecipeFinderApplication.class, args);
    }
}
```

### Task 5: Define the Recipe Data Model

**Goal**: Create a structured data model for recipes.

Create `src/main/java/com/example/recipe/Recipe.java`:
```java
package com.example.recipe;

import java.util.List;

record Recipe(
    String name,
    String description,
    List<String> ingredients,
    List<String> instructions,
    String imageUrl
) {}
```

**Why a record?** Java records are perfect for immutable data transfer objects!

### Task 6: Configure the ChatClient with a System Prompt

**Goal**: Set up the ChatClient with instructions for generating valid JSON.

Create `src/main/java/com/example/RecipeFinderConfiguration.java`:
```java
package com.example;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

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
}
```

Create `src/main/resources/prompts/fix-json-response`:
```
As you can see in the provided JSON schema, the ingredients are just a list of strings.
The imageUrl value should be an empty String
Ensure the JSON output is always valid without e.g. missing any parentheses.
```

### Task 7: Create the Recipe Service

**Goal**: Implement the business logic to generate recipes using AI.

Create `src/main/java/com/example/recipe/RecipeService.java`:
```java
package com.example.recipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);
    private final ChatClient chatClient;

    @Value("classpath:/prompts/recipe-for-ingredients")
    private Resource recipeForIngredientsPromptResource;

    RecipeService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    Recipe fetchRecipeFor(List<String> ingredients) {
        log.info("Generating recipe for ingredients: {}", ingredients);

        return chatClient.prompt()
                .user(us -> us
                        .text(recipeForIngredientsPromptResource)
                        .param("ingredients", String.join(",", ingredients)))
                .call()
                .entity(Recipe.class);  // 🎯 Structured output!
    }
}
```

Create `src/main/resources/prompts/recipe-for-ingredients`:
```
Provide a recipe that includes in the best case all of the following ingredients.

Ingredients: """
{ingredients}
"""

Add additional ingredients that are necessary for a good flavor or to create a more creative and complex meal.
The recipe should be translated to English, and with quantity in metric system.
```

**Key Concepts:**
- **Prompt Templates**: Use placeholders like `{ingredients}` for dynamic content
- **Structured Output**: `.entity(Recipe.class)` automatically parses JSON to your Java object!
- **Resource Loading**: Externalize prompts for easy modification

### Task 8: Create a REST Controller

**Goal**: Expose the recipe generation as a REST API endpoint.

Create `src/main/java/com/example/recipe/RecipeResource.java`:
```java
package com.example.recipe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
class RecipeResource {

    private final RecipeService recipeService;

    RecipeResource(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    Recipe fetchRecipeFor(@RequestParam String ingredients) {
        List<String> ingredientsList = Arrays.asList(
            ingredients.split("\\s*,\\s*")
        );
        return recipeService.fetchRecipeFor(ingredientsList);
    }
}
```

### Task 9: Run and Test Your Application

**Goal**: See your AI-powered application in action!

1. Build and run:
```bash
./gradlew bootRun
```

2. Test the API:
```bash
curl "http://localhost:8080/api/recipes?ingredients=chicken,tomatoes,garlic"
```

You should get a JSON response with a complete recipe!

3. Try different ingredients:
```bash
curl "http://localhost:8080/api/recipes?ingredients=pasta,mushrooms,cream"
curl "http://localhost:8080/api/recipes?ingredients=eggs,bacon,cheese"
```

### Task 10: Experiment with Model Parameters

**Goal**: Understand how temperature affects creativity.

1. Create `src/main/resources/application-creative.yaml`:
```yaml
spring:
  ai:
    ollama:
      chat:
        options.temperature: 1.0  # Maximum creativity!
```

2. Run with different profiles:
```bash
# Focused, consistent recipes
./gradlew bootRun

# Creative, varied recipes  
./gradlew bootRun --args='--spring.profiles.active=creative'
```

3. Compare the results - notice how temperature affects creativity!

## 🎉 Success Criteria

You've completed this module when:
- ✅ Your REST API returns structured recipe JSON
- ✅ The LLM includes all requested ingredients
- ✅ You understand how to configure model parameters
- ✅ You can explain what "structured output" means

## 💡 Solution

The complete solution code is available in the [`solution/`](./solution) directory. Check it if you get stuck or want to compare your implementation!

## Understanding Model Parameters

- **Temperature (0.0-1.0)**: Controls randomness and creativity
  - 0.0 = Deterministic, focused, consistent
  - 0.7 = Balanced (recommended for most tasks)
  - 1.0 = Creative, diverse, unpredictable
  
- **Top-p (0.0-1.0)**: Controls diversity via nucleus sampling
  - Lower = More focused on likely outputs
  - Higher = More diverse, considers less likely options

- **Context Window**: Maximum tokens the model can process
  - Llama 3.2: 8K-128K tokens
  - Mistral: 32K tokens
  - Phi-3: 4K-128K tokens

## Key Concepts Learned

### 1. ChatClient Builder Pattern
Spring AI uses a fluent builder pattern for flexibility:
```java
chatClient.prompt()
    .user("Tell me about {topic}")
    .system("You are a helpful assistant")
    .call()
    .content();
```

### 2. Structured Output
Instead of parsing strings manually, Spring AI can convert LLM responses directly to Java objects:
```java
Recipe recipe = chatClient.prompt()
    .user("Create a recipe")
    .call()
    .entity(Recipe.class);  // Magic! 🎯
```

### 3. Prompt Templates
Externalize and parameterize your prompts for maintainability:
```
Ingredients: """
{ingredients}
"""
```

## Troubleshooting

**Model not found?**
```bash
ollama pull llama3.2
```

**Connection refused?**
```bash
ollama serve  # Start Ollama server
```

**Slow responses?**
- Try a smaller model: `phi3` or `llama3.2:3b`
- Check your system resources
- First run downloads the model (may take time)

## Comparison: Before and After Spring AI

**Without Spring AI (Raw HTTP):**
```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://localhost:11434/api/generate"))
    .POST(HttpRequest.BodyPublishers.ofString("""
        {"model":"llama3.2","prompt":"..."}
        """))
    .build();
HttpResponse<String> response = client.send(request, 
    HttpResponse.BodyHandlers.ofString());
// Now parse JSON, handle streaming, retry logic...
```

**With Spring AI:**
```java
String response = chatClient.prompt()
    .user("Your question here")
    .call()
    .content();
```

Much cleaner! 🎉

## Code Examples
Check the complete working solution in the [`solution/`](./solution) directory.

## What's Next?

🎯 **In Module 2**, you'll enhance this Recipe Finder with:
- **RAG (Retrieval Augmented Generation)**: Load your own recipe PDFs and search them with vector databases
- **Function Calling**: Let the AI check what ingredients are in your fridge
- **Web UI**: Build a beautiful Thymeleaf interface

## Next Steps
Continue to [Module 2: Building Intelligent Enterprise Applications](../module-2-building-enterprise-apps/README.md) where you'll add RAG and function calling to make your application even smarter!

## Resources
- [Ollama Documentation](https://ollama.ai/)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Ollama Integration](https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html)
- [Prompt Engineering Guide](https://www.promptingguide.ai/)
- [Hugging Face LLM Leaderboard](https://huggingface.co/spaces/HuggingFaceH4/open_llm_leaderboard)
