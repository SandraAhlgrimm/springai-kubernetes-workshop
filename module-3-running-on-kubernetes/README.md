# Module 3: Running AI Applications on Kubernetes

## Overview
Deploy and scale your AI applications on Kubernetes. Learn how to containerize applications, deploy LLM servers, implement asynchronous processing with message queues, and ensure production-readiness with proper monitoring and autoscaling.

## Learning Objectives
- Containerize Spring Boot AI applications
- Deploy self-hosted LLMs on Kubernetes
- Implement asynchronous architecture with message queues
- Configure resource requests and limits
- Implement autoscaling (HPA, VPA, KEDA)
- Set up monitoring with Prometheus and Grafana
- Implement health checks and readiness probes
- Handle persistent storage for Redis vector stores

## Topics Covered
- Docker image creation with Spring Boot
- Kubernetes Deployments and Services
- ConfigMaps and Secrets
- Deploying Ollama on Kubernetes
- GPU support for LLM inference
- Message queues (RabbitMQ, Kafka) on Kubernetes
- Asynchronous AI request processing
- Resource management and limits
- Horizontal Pod Autoscaling (HPA)
- KEDA for event-driven autoscaling
- Prometheus and Grafana setup
- Persistent volumes for Redis vector stores
- Networking and service mesh considerations

## Architecture Overview

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Client    │─────▶│  Spring Boot │─────▶│   Message   │
│             │      │  Application │      │    Queue    │
└─────────────┘      └──────────────┘      └─────────────┘
                            │                     │
                            ▼                     ▼
                     ┌──────────────┐      ┌─────────────┐
                     │   Ollama     │◀─────│  AI Worker  │
                     │     LLM      │      │    Pods     │
                     └──────────────┘      └─────────────┘
                            │
                            ▼
                     ┌──────────────┐
                     │    Redis     │
                     │(Vector Store)│
                     └──────────────┘
```

## Why Async Architecture?

- **Better Resource Utilization**: Don't block threads waiting for AI responses
- **Scalability**: Process multiple requests concurrently
- **Resilience**: Handle failures gracefully with retries
- **User Experience**: Return immediately with request ID, deliver results later
- **Cost Efficiency**: Queue requests during low-demand periods

## Tasks

### 1. Create Dockerfile for Spring Boot Application

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and push:
```bash
docker build -t your-registry/ai-app:latest .
docker push your-registry/ai-app:latest
```

### 2. Deploy Ollama on Kubernetes

```yaml
# ollama-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ollama
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ollama
  template:
    metadata:
      labels:
        app: ollama
    spec:
      containers:
      - name: ollama
        image: ollama/ollama:latest
        ports:
        - containerPort: 11434
        resources:
          requests:
            memory: "8Gi"
            cpu: "2"
          limits:
            memory: "16Gi"
            cpu: "4"
        volumeMounts:
        - name: ollama-data
          mountPath: /root/.ollama
      volumes:
      - name: ollama-data
        persistentVolumeClaim:
          claimName: ollama-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: ollama
spec:
  selector:
    app: ollama
  ports:
  - port: 11434
    targetPort: 11434
```

### 3. Deploy Redis for Vector Storage

```yaml
# redis-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: redis
  name: redis
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - image: redis/redis-stack
        name: redis
        ports:
        - containerPort: 6379
          name: redis
---
apiVersion: v1
kind: Service
metadata:
  labels:
    app: redis
  name: redis
spec:
  ports:
  - name: redis
    port: 6379
    targetPort: redis
  selector:
    app: redis
```

### 4. Deploy Your Spring Boot Application

```yaml
# app-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ai-app
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ai-app
  template:
    metadata:
      labels:
        app: ai-app
    spec:
      containers:
      - name: ai-app
        image: your-registry/ai-app:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_AI_OLLAMA_BASE_URL
          value: "http://ollama:11434"
        - name: SPRING_RABBITMQ_HOST
          value: "rabbitmq"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: ai-app
spec:
  selector:
    app: ai-app
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

### 5. Implement Async Processing with Spring Cloud Stream

```java
// Message Producer
@Service
public class AiRequestProducer {
    
    private final StreamBridge streamBridge;
    
    public String submitRequest(String prompt) {
        String requestId = UUID.randomUUID().toString();
        AiRequest request = new AiRequest(requestId, prompt);
        streamBridge.send("ai-requests", request);
        return requestId;
    }
}

// Message Consumer (Worker)
@Component
public class AiRequestConsumer {
    
    private final ChatClient chatClient;
    private final StreamBridge streamBridge;
    
    @Bean
    public Consumer<AiRequest> processAiRequest() {
        return request -> {
            try {
                String response = chatClient.call(request.prompt());
                AiResponse aiResponse = new AiResponse(
                    request.id(), 
                    response, 
                    "SUCCESS"
                );
                streamBridge.send("ai-responses", aiResponse);
            } catch (Exception e) {
                // Handle error and send to DLQ
            }
        };
    }
}
```

### 6. Configure Horizontal Pod Autoscaling

```yaml
# hpa.yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: ai-app-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ai-app
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 7. Deploy Prometheus and Grafana

```bash
# Using Helm
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack

# Access Grafana
kubectl port-forward svc/prometheus-grafana 3000:80
```

### 8. Expose Spring Boot Metrics

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

### 9. Deploy KEDA for Event-Driven Autoscaling

```yaml
# keda-scaledobject.yaml
apiVersion: keda.sh/v1alpha1
kind: ScaledObject
metadata:
  name: ai-worker-scaler
spec:
  scaleTargetRef:
    name: ai-worker
  minReplicaCount: 1
  maxReplicaCount: 20
  triggers:
  - type: rabbitmq
    metadata:
      queueName: ai-requests
      queueLength: "10"
      protocol: amqp
      host: amqp://guest:guest@rabbitmq:5672
```

## GPU Support for LLMs

For production workloads, consider GPU acceleration:

```yaml
resources:
  limits:
    nvidia.com/gpu: 1
```

Requires:
- GPU-enabled Kubernetes nodes
- NVIDIA GPU Operator installed
- GPU-compatible Ollama image

## Best Practices

1. **Resource Limits**: Always set requests and limits
2. **Health Checks**: Implement proper liveness and readiness probes
3. **Graceful Shutdown**: Handle SIGTERM properly
4. **Persistent Storage**: Use PVCs for model storage and Redis data
5. **Secrets Management**: Never hardcode credentials
6. **Network Policies**: Restrict pod-to-pod communication
7. **Monitoring**: Track latency, throughput, and errors
8. **Backup Strategy**: Regular backups of vector database

## Hands-On Tasks

In this module, you'll deploy the Recipe Finder application from Module 2 to Kubernetes, including Ollama and Redis Vector Store.

### Prerequisites
- Completed Module 2 or use the Module 2 solution
- Access to a Kubernetes cluster (Minikube, Kind, or cloud)
- Docker installed (for building images)
- kubectl configured

### Task 1: Build a Docker Image

**Goal**: Container the Recipe Finder application.

1. Using Spring Boot's built-in Buildpacks (easiest!):
```bash
cd solution
./gradlew bootBuildImage --imageName=recipe-finder:latest
```

This creates an optimized OCI image without writing a Dockerfile!

2. **Optional**: Traditional Dockerfile approach:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
./gradlew clean bootJar
docker build -t recipe-finder:latest .
```

3. Verify the image:
```bash
docker images | grep recipe-finder
docker run -p 8080:8080 recipe-finder:latest
```

### Task 2: Deploy Redis to Kubernetes

**Goal**: Set up Redis for vector storage and RAG.

Create `deployment/kubernetes/redis.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  labels:
    app: redis
  name: redis
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
      - image: redis/redis-stack
        name: redis
        ports:
        - containerPort: 6379
---
apiVersion: v1
kind: Service
metadata:
  name: recipe-finder-redis
spec:
  type: ClusterIP
  selector:
    app: redis
  ports:
    - port: 6379
      targetPort: 6379
```

Deploy:
```bash
kubectl apply -f deployment/kubernetes/redis.yaml
kubectl get pods -l app=redis
```

### Task 3: Deploy Ollama to Kubernetes

**Goal**: Run the LLM server in Kubernetes.

Create `deployment/kubernetes/ollama.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: recipe-finder-ollama-llama
spec:
  selector:
    matchLabels:
      app: recipe-finder-ollama-llama
  template:
    metadata:
      labels:
        app: recipe-finder-ollama-llama
    spec:
      containers:
        - name: ollama
          image: ollama/ollama
          ports:
          - containerPort: 11434
          resources:
            requests:
              memory: "4Gi"
              cpu: "2"
            limits:
              memory: "8Gi"
              cpu: "4"
        - name: ollama-init
          image: ollama/ollama
          command: ["/bin/bash"]
          args:
            - -c
            - |
              set -ex
              sleep 10
              ollama run llama3.2
              sleep infinity
---
apiVersion: v1
kind: Service
metadata:
  name: recipe-finder-ollama-llama
spec:
  type: ClusterIP
  ports:
    - port: 11434
      targetPort: 11434
  selector:
    app: recipe-finder-ollama-llama
```

**Understanding this manifest**:
- **Two containers**: One runs Ollama, one pulls the model
- **Init pattern**: `ollama-init` downloads llama3.2 on first run
- **Resource limits**: LLMs need RAM! Adjust based on your cluster
- **ClusterIP**: Internal service (not exposed outside)

Deploy:
```bash
kubectl apply -f deployment/kubernetes/ollama.yaml

# Watch the model download (takes a few minutes)
kubectl logs -f deployment/recipe-finder-ollama-llama -c ollama-init
```

### Task 4: Deploy the Recipe Finder Application

**Goal**: Run your Spring Boot application in Kubernetes.

Create `deployment/kubernetes/recipe-finder.yaml`:
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: recipe-finder-ollama
spec:
  selector:
    matchLabels:
      app: recipe-finder-ollama
  template:
    metadata:
      labels:
        app: recipe-finder-ollama
    spec:
      containers:
        - name: workload
          image: recipe-finder:latest
          imagePullPolicy: Never  # For local images
          env:
            - name: SPRING_DOCKER_COMPOSE_ENABLED
              value: "false"
            - name: SPRING_DATA_REDIS_HOST
              value: recipe-finder-redis
            - name: SPRING_AI_OLLAMA_BASE_URL
              value: http://recipe-finder-ollama-llama:11434
            - name: MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE
              value: "*"
            - name: MANAGEMENT_ENDPOINT_ENV_SHOW_VALUES
              value: ALWAYS
          ports:
            - containerPort: 8080
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "1Gi"
              cpu: "1"
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: recipe-finder-ollama
spec:
  type: LoadBalancer
  selector:
    app: recipe-finder-ollama
  ports:
    - port: 80
      targetPort: 8080
```

**Key concepts**:
- **Environment variables**: Point to Kubernetes services
- **Health probes**: Kubernetes knows when app is ready
- **Resource requests/limits**: Prevent resource starvation
- **LoadBalancer**: Exposes app externally

Deploy:
```bash
kubectl apply -f deployment/kubernetes/recipe-finder.yaml
kubectl get pods -l app=recipe-finder-ollama
```

### Task 5: Access Your Application

**Goal**: Test the deployed Recipe Finder.

1. Get the service URL:
```bash
kubectl get svc recipe-finder-ollama
```

2. **For Minikube**:
```bash
minikube service recipe-finder-ollama --url
```

3. **For cloud providers**, use the EXTERNAL-IP:
```bash
export APP_URL=$(kubectl get svc recipe-finder-ollama -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
curl "http://$APP_URL/api/recipes?ingredients=chicken,tomatoes"
```

4. **For local clusters without LoadBalancer**, use port-forward:
```bash
kubectl port-forward svc/recipe-finder-ollama 8080:80
# Then access: http://localhost:8080
```

### Task 6: Monitor Your Application

**Goal**: Understand what's happening in your cluster.

1. Check all pods:
```bash
kubectl get pods
```

Expected:
```
NAME                                      READY   STATUS    RESTARTS
recipe-finder-ollama-xxx                  1/1     Running   0
recipe-finder-ollama-llama-xxx            2/2     Running   0
recipe-finder-redis-xxx                   1/1     Running   0
```

2. View logs:
```bash
# Application logs
kubectl logs -f deployment/recipe-finder-ollama

# Ollama logs
kubectl logs deployment/recipe-finder-ollama-llama -c ollama

# Redis logs
kubectl logs deployment/recipe-finder-redis
```

3. Check resource usage:
```bash
kubectl top pods
```

4. Describe a pod to see events:
```bash
kubectl describe pod recipe-finder-ollama-xxx
```

### Task 7: Implement Horizontal Pod Autoscaling (HPA)

**Goal**: Automatically scale based on CPU usage.

1. Create `deployment/kubernetes/hpa.yaml`:
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: recipe-finder-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: recipe-finder-ollama
  minReplicas: 1
  maxReplicas: 5
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

Deploy:
```bash
kubectl apply -f deployment/kubernetes/hpa.yaml
kubectl get hpa
```

2. Generate load to test:
```bash
# Install hey load generator
go install github.com/rakyll/hey@latest

# Generate load
hey -z 60s -c 10 "http://<your-service>/api/recipes?ingredients=pasta"

# Watch scaling
watch kubectl get hpa
watch kubectl get pods
```

### Task 8: Configure Resource Limits

**Goal**: Prevent pods from consuming too many resources.

Understanding resource configuration:
```yaml
resources:
  requests:      # Minimum guaranteed
    memory: "512Mi"
    cpu: "500m"   # 0.5 CPU cores
  limits:        # Maximum allowed
    memory: "1Gi"
    cpu: "1"
```

**Best practices**:
- Set requests = typical usage
- Set limits = burst capacity
- For LLMs: High memory, moderate CPU
- For app: Moderate memory, variable CPU

### Task 9: Implement Health Checks

**Goal**: Let Kubernetes restart unhealthy pods automatically.

Your application already has these from `spring-boot-starter-actuator`!

Test them:
```bash
APP_POD=$(kubectl get pod -l app=recipe-finder-ollama -o jsonpath='{.items[0].metadata.name}')

# Liveness: Is the app running?
kubectl exec $APP_POD -- curl -s localhost:8080/actuator/health/liveness

# Readiness: Can the app serve traffic?
kubectl exec $APP_POD -- curl -s localhost:8080/actuator/health/readiness
```

**What happens if probes fail?**
- **Liveness fails**: Kubernetes restarts the pod
- **Readiness fails**: Kubernetes stops sending traffic

### Task 10: Clean Up Resources

**Goal**: Remove all deployed resources.

```bash
kubectl delete -f deployment/kubernetes/recipe-finder.yaml
kubectl delete -f deployment/kubernetes/ollama.yaml
kubectl delete -f deployment/kubernetes/redis.yaml
kubectl delete hpa recipe-finder-hpa
```

Or delete by label:
```bash
kubectl delete all -l app=recipe-finder-ollama
```

## 🎯 Success Criteria

You've completed this module when:
- ✅ Recipe Finder runs in Kubernetes
- ✅ Ollama serves LLM requests
- ✅ Redis Vector Store is properly configured
- ✅ Health probes work correctly
- ✅ You can access the UI from your browser
- ✅ HPA scales pods based on load

## 💡 Solution

Complete Kubernetes manifests are in [`solution/deployment/kubernetes/`](./solution/deployment/kubernetes/).

## Architecture in Kubernetes

```
┌─────────────────┐
│   Ingress/LB    │ ← External traffic
└────────┬────────┘
         │
         v
┌─────────────────────────┐
│ recipe-finder-ollama    │ ← Spring Boot App
│  Service (LoadBalancer) │
└────────┬────────────────┘
         │
         v
┌─────────────────────────┐
│  recipe-finder-ollama   │ ← Pod(s)
│    Deployment           │
│  - Liveness probe       │
│  - Readiness probe      │
│  - Resource limits      │
└──┬──────────────────┬───┘
   │                  │
   v                  v
┌──────────────┐  ┌────────────────────┐
│ Redis        │  │ Ollama LLM         │
│ Vector Store │  │  ClusterIP         │
│  ClusterIP   │  │                    │
└──────────────┘  └────────────────────┘
```

## Key Kubernetes Concepts

### 1. Deployments
Manage pod replicas and rolling updates:
```yaml
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
```

### 2. Services
Provide stable networking:
- **ClusterIP**: Internal only
- **LoadBalancer**: External access
- **NodePort**: Access via node IP

### 3. ConfigMaps & Secrets
Externalize configuration:
```bash
kubectl create configmap app-config --from-literal=SPRING_PROFILES_ACTIVE=prod
kubectl create secret generic app-secrets --from-literal=API_KEY=xxx
```

### 4. Health Probes
- **Liveness**: Restart if unhealthy
- **Readiness**: Remove from service if not ready
- **Startup**: Give app time to start

## What's Next?

🎯 **In Module 4**, you'll learn to:
- Optimize models with quantization
- Create custom Ollama Modelfiles
- Implement prompt engineering patterns
- Fine-tune models for your domain
- Evaluate model performance

## Code Examples
Complete deployment manifests in [`solution/deployment/kubernetes/`](./solution/deployment/kubernetes/).

## Next Steps
Continue to [Module 4: Adapting Large Language Models](../module-4-adapting-llms/README.md) to optimize and customize your models!

## Resources
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
- [KEDA Documentation](https://keda.sh/)
- [Spring Cloud Stream](https://spring.io/projects/spring-cloud-stream)
- [Ollama Kubernetes Deployment](https://github.com/ollama/ollama/blob/main/docs/kubernetes.md)
- [Prometheus Operator](https://prometheus-operator.dev/)
