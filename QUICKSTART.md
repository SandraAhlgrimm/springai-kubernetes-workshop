# Quick Start Guide

Get started with the Spring AI + Kubernetes workshop in minutes!

## 🚀 Fastest Way: GitHub Codespaces

1. Click the button in the main README or visit:
   ```
   https://codespaces.new/SandraAhlgrimm/springai-kubernetes-workshop
   ```

2. Wait for the container to build (2-3 minutes)

3. All tools are pre-installed:
   - ✅ Java 21
   - ✅ Docker
   - ✅ kubectl
   - ✅ Ollama
   - ✅ Git

4. Start with Module 1:
   ```bash
   cd module-1-getting-started-llms
   cat README.md  # Read the tasks
   ```

## 🏃 Quick Start: Run the Solutions

### Module 1: Basic Recipe API

```bash
cd module-1-getting-started-llms/solution

# Pull the LLM model
ollama pull llama3.2

# Run the application
./gradlew bootRun

# Test it (in another terminal)
curl "http://localhost:8080/api/recipes?ingredients=chicken,tomatoes"
```

Expected: JSON response with a complete recipe!

### Module 2: Full Recipe Finder with UI

```bash
cd module-2-building-enterprise-apps/solution

# Ensure Ollama model is available
ollama pull llama3.2

# Run the application (starts Ollama + Redis via Docker Compose)
./gradlew bootRun

# Open your browser
open http://localhost:8080
```

Try these features:
- ☐ Enter ingredients: `pasta, tomatoes, garlic`
- ☐ Check "Use available ingredients" (adds bacon/onions from config)
- ☐ Check "Prefer own recipes" (searches german_recipes.pdf)

### Module 3: Kubernetes Deployment

```bash
cd module-3-running-on-kubernetes/solution

# Build container image
./gradlew bootBuildImage --imageName=recipe-finder:latest

# Deploy to Kubernetes
kubectl apply -f deployment/kubernetes/ollama.yaml
kubectl apply -f deployment/kubernetes/redis.yaml
kubectl apply -f deployment/kubernetes/recipe-finder.yaml

# Wait for pods to be ready
kubectl get pods -w

# Access the application
kubectl port-forward svc/recipe-finder-ollama 8080:80

# Open browser
open http://localhost:8080
```

## 📚 Learning Path

### If You're New to AI Development
Start here:
1. Read `module-0-ai-fundamentals/README.md`
2. Complete `module-1-getting-started-llms/README.md` tasks
3. Study the solution code
4. Move to Module 2

### If You Know Spring AI Basics
Skip to:
1. `module-2-building-enterprise-apps/README.md`
2. Focus on RAG and function calling
3. Build the web UI

### If You Want Kubernetes Experience
Jump to:
1. `module-3-running-on-kubernetes/README.md`
2. Learn deployment patterns
3. Experiment with scaling

### If You Want Complete Code
Go directly to:
- `module-1-getting-started-llms/solution/`
- `module-2-building-enterprise-apps/solution/`
- `module-3-running-on-kubernetes/solution/`

## 🎯 What You'll Build

A **Recipe Finder** application that:
- Generates recipes from ingredients using AI
- Searches your own recipe PDFs (RAG)
- Checks what's in your fridge (function calling)
- Has a beautiful web interface
- Runs on Kubernetes at scale

**Module 1**: Basic REST API
```
User → REST API → ChatClient → Ollama → Recipe JSON
```

**Module 2**: Enterprise Features
```
User → Web UI → RecipeService → ChatClient + RAG + Tools → Recipe
                                      ↓           ↓
                                   Vector DB   Functions
```

**Module 3**: Kubernetes
```
User → LoadBalancer → Pod(s) → Ollama Pod
                              → Redis Pod
         [Auto-scaling based on load]
```

## 🛠️ Prerequisites

### For Local Development (without Codespaces)
- Java 21 or later
- Docker Desktop
- Ollama installed
- kubectl (for Module 3)
- Git

### For Codespaces
- Just a GitHub account! Everything else is pre-configured.

## 🔧 Common Commands

### Check if Ollama is running
```bash
ollama list
ollama ps
```

### Pull a model
```bash
ollama pull llama3.2
```

### Start Ollama server
```bash
ollama serve
```

### Check Kubernetes pods
```bash
kubectl get pods
kubectl logs -f <pod-name>
```

### Build the application
```bash
./gradlew clean build
```

### Run tests
```bash
./gradlew test
```

## 📖 Documentation

- **Main README**: Workshop overview and setup
- **WORKSHOP_STRUCTURE.md**: Detailed architecture and patterns
- **INTEGRATION_SUMMARY.md**: What's included and how to use it
- **Each module's README**: Hands-on tasks
- **Each solution's README**: Complete documentation

## 🐛 Troubleshooting

### Port already in use
```bash
# Kill process on port 8080
lsof -ti:8080 | xargs kill -9
```

### Ollama connection refused
```bash
ollama serve
```

### Model not found
```bash
ollama pull llama3.2
```

### Kubernetes pods not starting
```bash
kubectl describe pod <pod-name>
kubectl logs <pod-name>
```

### Slow responses
- First request loads model (normal)
- Check system resources
- Try smaller model: `ollama pull phi3`

## 💡 Tips

1. **Start Simple**: Complete Module 1 before moving to Module 2
2. **Read the Code**: Solution code is well-commented
3. **Experiment**: Change prompts, try different models, adjust parameters
4. **Use the Solution**: Don't spend hours stuck - check the solution
5. **Ask Questions**: Use the workshop Discord/Slack for help

## 🎓 Learning Resources

- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/)
- [Ollama Documentation](https://ollama.ai/docs)
- [Kubernetes Tutorials](https://kubernetes.io/docs/tutorials/)
- [Spring Boot Guides](https://spring.io/guides)

## 🚦 Next Steps

1. **Choose your path** (beginner → Module 1, experienced → Module 2/3)
2. **Open the module README**
3. **Complete the tasks** or **run the solution**
4. **Experiment and modify**
5. **Move to the next module**

## 🎉 Success Criteria

You'll know you're done when:
- ✅ Module 1: REST API returns recipe JSON
- ✅ Module 2: Web UI shows recipes with RAG and function calling
- ✅ Module 3: Application runs in Kubernetes and scales

Happy coding! 🚀
