# Module 2: Building Intelligent Enterprise Applications

## Overview
Learn how to build an enterprise application enhanced with AI capabilities.

In this module, you’ll create a **Recipe Finder application** that uses AI to generate recipes based on provided ingredients.
By the end, you’ll have a working REST API capable of suggesting creative and personalized recipes.

## Learning Objectives
- Build enterprise-grade AI applications with Java, Spring Boot, and Spring AI

## Setup
**Everything you need is already installed in the configured GitHub Codespace for this repository!**

For local installation, use the following commands.

### Java 25

**macOS or Linux**
SDKMan is the recommeded way to install 
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

```bash
sdk list java  
sdk install java 25-tem
sdk use java 25-tem
```

**Windows:**
Via Winget
```bash
winget search Temurin
winget install EclipseAdoptium.Temurin.25.JDK
java -version
```

Via Chocolatey
```bash
choco install temurin25
java -version
```

## Generate the Spring Boot project
**Spring Initializr** is the fastest way to start a new Spring Boot application. It provides a guided setup experience, either through the web interface or directly in your IDE, so you can quickly generate a ready-to-run project with all required dependencies and build configuration.

Instead of manually creating project folders, Gradle files, and dependency entries, Spring Initializr lets you define your project metadata (Java version, build system, dependencies, etc.) and instantly generates a complete, production-ready starter.

In this step, you’ll use Spring Initializr in your IDE to create a new Spring Boot project for our Recipe Finder application that integrates AI capabilities using Spring AI with this configuration:

- Set Project to Gradle – Groovy and Language to Java.
- Set Spring Boot to 3.5.6.
- Group com.example, Artifact recipe-finder, Package Name com.example.
- Java select 25
- Add dependencies: Spring Web, Spring Boot Actuator, Spring AI Ollama.

In GitHub Codespaces / VS Code:  
Press `Ctrl + Shift + P` or `F1` to open command palette.  
Type `Spring Initializr` to start generating a Maven or Gradle project.
**Save it to the `recipe-finder` folder in the root of your cloned repository.**


### Backup

[Here](https://start.spring.io/#!type=gradle-project&language=java&platformVersion=3.5.6&packaging=jar&jvmVersion=25&groupId=com.example&artifactId=recipe-finder&name=recipe-finder&description=Demo%20project%20for%20Spring%20Boot&packageName=com.example.recipe-finder&dependencies=web,spring-ai-ollama,actuator) are also a link to the configuration on https://start.spring.io.

Plus the copy-paste commands to download and unpack the project directly from Spring Initializr.
```bash
curl https://start.spring.io/starter.zip -d javaVersion=25 -d groupId=com.example -d artifactId=recipefinder -d type=gradle-project -d dependencies=web,actuator,spring-ai-ollama  -o recipe-finder.zip \
&& unzip recipe-finder.zip -d recipe-finder \
&& rm recipe-finder.zip
```

## Test Recipe Finder application

```bash
(cd module-2-building-enterprise-apps/solution && ./gradlew bootRun)
curl http://localhost:8080/api/v1/recipes?ingredients=waffles
```

## Step-by-step: Build the Recipe API

1. **Create model** `src/main/java/com/example/recipe/Recipe.java`
   ```java
   package com.example.recipe;
   import java.util.List;
   public record Recipe(String name, String description, List<String> ingredients, List<String> instructions, String imageUrl) {}
   ```

2. **Add prompt** `src/main/resources/prompts/recipe-for-ingredients`
   ```text
   Provide a JSON recipe for the following ingredients: {ingredients}
   ```

3. **Inject Spring AI ChatClient** `RecipeService.java`
   ```java
   @Service
   class RecipeService {
       private final ChatClient chatClient;
       @Value("classpath:/prompts/recipe-for-ingredients")
       private Resource recipeForIngredientsPromptResource;

       RecipeService(ChatClient chatClient) { this.chatClient = chatClient; }

       Recipe fetchRecipeFor(List<String> ingredients) {
           return chatClient.prompt()
               .user(us -> us.text(recipeForIngredientsPromptResource)
                             .param("ingredients", String.join(",", ingredients)))
               .call()
               .entity(Recipe.class);
       }
   }
   ```

4. **Expose REST endpoint** `RecipeController.java`
   ```java
   @RestController
   class RecipeController {
       private final RecipeService recipeService;
       RecipeController(RecipeService recipeService) { this.recipeService = recipeService; }

       @GetMapping("/api/v1/recipes")
       public ResponseEntity<Recipe> fetchRecipeFor(@RequestParam List<String> ingredients) {
           return ResponseEntity.ok(recipeService.fetchRecipeFor(ingredients));
       }
   }
   ```

5. **Run & test**
   ```bash
   ./gradlew bootRun
   curl "http://localhost:8080/api/v1/recipes?ingredients=waffles"
   ```

6. **Add tests (optional)**
   - `RecipeServiceTest` using a mocked `ChatClient`
   - `RecipeControllerTest` using `@WebMvcTest`

```bash
./gradlew test
```

## Appendix: Full Spring Boot setup (Module 1 Tasks 2-9)

1) **Project skeleton**
```bash
mkdir -p src/main/java/com/example/recipe
mkdir -p src/main/resources/prompts
```

2) **`build.gradle`**
```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.5.6'
    id 'io.spring.dependency-management' version '1.1.7'
}
group = 'com.example'
version = '0.0.1-SNAPSHOT'
java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
repositories { mavenCentral() }
ext { set('springAiVersion', "1.0.2") }
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.ai:spring-ai-starter-model-ollama'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
dependencyManagement { imports { mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}" } }
tasks.named('test') { useJUnitPlatform() }
```

3) **`src/main/resources/application.yaml`**
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

4) **`RecipeFinderApplication.java`**
```java
@SpringBootApplication
public class RecipeFinderApplication {
  public static void main(String[] args) { SpringApplication.run(RecipeFinderApplication.class, args); }
}
```

5) **`Recipe.java`**
```java
public record Recipe(String name, String description, List<String> ingredients, List<String> instructions, String imageUrl) {}
```

6) **`RecipeFinderConfiguration.java`**
```java
@Configuration
class RecipeFinderConfiguration {
  @Value("classpath:/prompts/fix-json-response") Resource fixJsonResponsePromptResource;
  @Bean ChatClient chatClient(ChatClient.Builder builder) {
    return builder.defaultSystem(fixJsonResponsePromptResource).build();
  }
}
```

7) **Prompts**
- `prompts/fix-json-response`: instructions to always return valid JSON
- `prompts/recipe-for-ingredients`: template with `{ingredients}` placeholder

8) **`RecipeService.java`**
```java
@Service
class RecipeService {
  private final ChatClient chatClient;
  @Value("classpath:/prompts/recipe-for-ingredients") Resource recipePrompt;
  RecipeService(ChatClient chatClient) { this.chatClient = chatClient; }
  Recipe fetchRecipeFor(List<String> ingredients) {
    return chatClient.prompt()
      .user(us -> us.text(recipePrompt).param("ingredients", String.join(",", ingredients)))
      .call()
      .entity(Recipe.class);
  }
}
```

9) **`RecipeResource.java`**
```java
@RestController
@RequestMapping("/api/recipes")
class RecipeResource {
  private final RecipeService recipeService;
  RecipeResource(RecipeService recipeService) { this.recipeService = recipeService; }
  @GetMapping
  Recipe fetchRecipeFor(@RequestParam String ingredients) {
    var list = Arrays.asList(ingredients.split("\\s*,\\s*"));
    return recipeService.fetchRecipeFor(list);
  }
}
```

10) **Run & test**
```bash
./gradlew bootRun
curl "http://localhost:8080/api/recipes?ingredients=chicken,tomatoes,garlic"
```



