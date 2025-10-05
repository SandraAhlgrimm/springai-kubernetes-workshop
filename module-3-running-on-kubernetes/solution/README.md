# Module 3 Solution - Recipe Finder on Kubernetes

This solution includes the complete Recipe Finder application ready for deployment on Kubernetes.

## Project Structure

```
solution/
├── src/                           # Application source code (same as Module 2)
├── deployment/
│   └── kubernetes/
│       ├── ollama.yaml           # Deployment with Ollama
│       ├── openai.yaml           # Deployment with OpenAI
│       └── azure-openai.yaml     # Deployment with Azure OpenAI
├── build.gradle                   # Build configuration
└── german_recipes.pdf            # Sample PDF for RAG
```

## Building the Application

### Build with Gradle

```bash
cd solution
./gradlew clean bootJar
```

### Build Docker Image

```bash
docker build -t recipe-finder:latest .
```

Or using the gradle wrapper:

```bash
./gradlew bootBuildImage --imageName=recipe-finder:latest
```

## Deploying to Kubernetes

### Deploy with Ollama

```bash
kubectl apply -f deployment/kubernetes/ollama.yaml
```

This will create:
- Recipe Finder application deployment
- Ollama deployment with llama3.2 model
- Redis deployment for vector store
- Services for all components

### Access the Application

```bash
# Get the service URL
kubectl get svc recipe-finder-ollama

# Port forward if needed
kubectl port-forward svc/recipe-finder-ollama 8080:80
```

Visit: http://localhost:8080

## Configuration

The Kubernetes deployment includes environment variables:
- `SPRING_DOCKER_COMPOSE_ENABLED=false` - Disable Docker Compose
- `SPRING_DATA_REDIS_HOST=recipe-finder-redis` - Redis service name
- `SPRING_AI_OLLAMA_BASE_URL=http://recipe-finder-ollama-llama:11434` - Ollama service URL

## Monitoring

Check application health:
```bash
kubectl get pods
kubectl logs -f deployment/recipe-finder-ollama
```

## Scaling

Scale the application:
```bash
kubectl scale deployment recipe-finder-ollama --replicas=3
```
