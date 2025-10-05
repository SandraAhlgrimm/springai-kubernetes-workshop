# Workshop Code Structure

This workshop follows a **progressive learning approach** where each module builds upon the previous one. The Recipe Finder application evolves from a simple AI-powered API to a production-ready, scalable Kubernetes deployment.

## Learning Progression

```
Module 0: AI Fundamentals (Theory)
           ↓
Module 1: Basic Recipe Generation
  - Spring AI basics
  - ChatClient
  - Structured output
  - Prompt templates
           ↓
Module 2: Enterprise Features  
  - RAG (vector search)
  - Function calling
  - Web UI
  - Redis integration
           ↓
Module 3: Kubernetes Deployment
  - Containerization
  - K8s manifests
  - Scaling & monitoring
  - Production readiness
           ↓
Module 4: Model Optimization (Standalone)
           ↓
Module 5: AI Agents (Advanced)
```

## Module Structure

Each practical module (1-3) follows the same pattern:

```
module-X-name/
├── README.md          # Hands-on tasks with step-by-step instructions
└── solution/          # Complete working solution
    ├── README.md      # Solution documentation
    ├── src/           # Source code
    ├── build.gradle   # Dependencies
    └── deployment/    # K8s manifests (Module 3)
```

## The Recipe Finder Application

### What It Does
Generates recipes based on ingredients using AI, with optional:
- **RAG**: Search your own recipe PDFs
- **Function Calling**: Check what's in your fridge
- **Web UI**: User-friendly interface

### Technology Stack
- **Framework**: Spring Boot 3.5.6
- **AI**: Spring AI 1.0.2 with Ollama
- **LLM**: Llama 3.2 (self-hosted)
- **Vector DB**: Redis Stack
- **Orchestration**: Kubernetes
- **UI**: Thymeleaf

## Module 1: Getting Started with LLMs

**What You Build**: Basic recipe generation REST API

**Key Files**:
```
solution/src/main/java/com/example/
├── RecipeFinderApplication.java     # Spring Boot entry point
├── RecipeFinderConfiguration.java  # ChatClient configuration
└── recipe/
    ├── Recipe.java                  # Data model (record)
    ├── RecipeService.java           # AI logic
    └── RecipeResource.java          # REST controller
```

**Dependencies**: 3
- `spring-boot-starter-web`
- `spring-boot-starter-actuator`
- `spring-ai-starter-model-ollama`

**Key Learning**:
- How to configure Spring AI
- Using ChatClient fluent API
- Structured output with `.entity()`
- Prompt templates

**API**:
```bash
GET /api/recipes?ingredients=chicken,tomatoes
```

## Module 2: Building Enterprise Applications

**What You Add**: RAG + Function Calling + Web UI

**New Files**:
```
solution/src/main/java/com/example/recipe/
├── FetchRecipeData.java           # Form data binding
└── RecipeUiController.java        # Web controller

solution/src/main/resources/
├── templates/index.html           # Thymeleaf UI
├── static/custom.css              # Styling
└── prompts/
    ├── prefer-own-recipe          # RAG prompt
    └── recipe-for-available-*     # Function calling prompt
```

**New Dependencies**: +5
- `spring-boot-starter-thymeleaf`
- `spring-ai-advisors-vector-store`
- `spring-ai-starter-vector-store-redis`
- `spring-ai-pdf-document-reader`
- `commons-lang3`

**Key Learning**:
- ETL pipeline for documents
- QuestionAnswerAdvisor for RAG
- @Tool annotation for function calling
- Combining multiple AI techniques

**Services**:
- Redis: Vector storage
- Ollama: LLM inference

## Module 3: Running on Kubernetes

**What You Add**: Kubernetes deployment

**New Files**:
```
solution/deployment/kubernetes/
├── ollama.yaml           # LLM server
├── redis.yaml            # Vector database  
├── recipe-finder.yaml    # Application
└── hpa.yaml             # Autoscaling
```

**Key Learning**:
- Containerizing Spring Boot apps
- Kubernetes Deployments & Services
- Health probes (liveness/readiness)
- Resource requests & limits
- Horizontal Pod Autoscaling
- Service networking

**Architecture**:
```
LoadBalancer → Application Pods → Ollama Pod
                              ↘ Redis Pod
```

## How to Use This Workshop

### Option 1: Follow the Tasks (Recommended)
1. Start with Module 1 README
2. Complete each task step-by-step
3. Build the application incrementally
4. Check solution if stuck

### Option 2: Jump to Solutions
1. Go directly to `solution/` folder
2. Study the complete code
3. Run and experiment
4. Modify for your use case

### Option 3: Compare & Learn
1. Try building yourself first
2. Compare with solution
3. Understand differences
4. Learn best practices

## Running the Solutions

### Module 1 & 2: Local Development

```bash
cd module-X-name/solution
./gradlew bootRun
```

Access:
- Module 1: http://localhost:8080/api/recipes?ingredients=chicken
- Module 2: http://localhost:8080 (Web UI)

### Module 3: Kubernetes

```bash
cd module-3-running-on-kubernetes/solution

# Build image
./gradlew bootBuildImage --imageName=recipe-finder:latest

# Deploy all components
kubectl apply -f deployment/kubernetes/

# Access application
kubectl port-forward svc/recipe-finder-ollama 8080:80
```

## Key Differences Between Modules

| Feature | Module 1 | Module 2 | Module 3 |
|---------|----------|----------|----------|
| **AI Capability** | Basic generation | RAG + Tools | Same as M2 |
| **UI** | REST only | Web UI | Web UI |
| **Vector Store** | None | Redis (local) | Redis (K8s) |
| **Document Search** | ❌ | ✅ | ✅ |
| **Function Calling** | ❌ | ✅ | ✅ |
| **Deployment** | Local | Docker Compose | Kubernetes |
| **Scalability** | Single instance | Single instance | Multi-pod |
| **Health Checks** | Basic | Actuator | K8s probes |
| **Lines of Code** | ~150 | ~500 | Same + YAML |
| **Dependencies** | 3 | 8 | 8 |
| **Complexity** | Beginner | Intermediate | Advanced |

## Common Code Patterns

### 1. ChatClient Fluent API
```java
Recipe recipe = chatClient.prompt()
    .user(us -> us.text(prompt).param("key", value))
    .tools(this)              // Optional: function calling
    .advisors(ragAdvisor)     // Optional: RAG
    .call()
    .entity(Recipe.class);    // Structured output
```

### 2. RAG Configuration
```java
var ragAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
    .searchRequest(SearchRequest.builder()
        .topK(2)
        .similarityThreshold(0.7)
        .build())
    .promptTemplate(template)
    .build();
```

### 3. Function Calling
```java
@Tool(description = "Clear description for AI")
List<String> fetchIngredientsAvailableAtHome() {
    return availableIngredients;
}
```

### 4. Kubernetes Resource Definition
```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1"
```

## Troubleshooting

### "Connection refused to localhost:11434"
```bash
ollama serve  # Start Ollama server
```

### "Model not found"
```bash
ollama pull llama3.2
```

### "Redis connection failed"
```bash
docker-compose up redis -d
```

### "Pod CrashLoopBackOff"
```bash
kubectl logs <pod-name>
kubectl describe pod <pod-name>
```

## Next Steps

After completing these modules:
- **Module 4**: Learn prompt engineering and model optimization
- **Module 5**: Build autonomous AI agents
- **Production**: Add authentication, rate limiting, caching
- **Advanced**: Implement streaming responses, conversation memory

## Resources

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Ollama Documentation](https://ollama.ai/docs)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Workshop Repository](https://github.com/SandraAhlgrimm/springai-kubernetes-workshop)
- [Sample Code](https://github.com/SandraAhlgrimm/ai-recipe-finder)
- [Cookbook Reference](https://github.com/SandraAhlgrimm/cookbook)
