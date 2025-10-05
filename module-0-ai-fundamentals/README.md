# Module 0: (Generative) AI Fundamentals

## Overview
This module introduces the fundamental concepts of Generative AI and Large Language Models. You'll learn about AI terminology, how LLMs work, and understand the landscape of AI technologies relevant to enterprise applications.

## Learning Objectives
- Understand what Generative AI is and how it differs from traditional AI
- Learn about Large Language Models (LLMs) and their capabilities
- Explore the AI landscape: cloud-based vs. self-hosted models
- Understand data privacy and compliance considerations
- Get familiar with key AI concepts: tokens, context windows, embeddings, and prompting

## Topics Covered
- What is Generative AI?
- Understanding Large Language Models (LLMs)
- Cloud-based vs. Self-hosted AI models
- Popular LLM models: GPT, Claude, Llama, Mistral, Phi, Qwen
- Key AI concepts: tokens, context windows, temperature, top-p
- Embeddings and vector databases
- Prompt engineering basics
- Data privacy and compliance in AI
- Why choose self-hosted LLMs?

## Key Concepts

### Generative AI
Generative AI creates new content (text, images, code) based on patterns learned from training data. Unlike traditional AI that classifies or predicts, generative AI produces original outputs.

### Large Language Models (LLMs)
LLMs are neural networks trained on vast amounts of text data. They understand and generate human-like text, making them ideal for:
- Natural language understanding
- Code generation
- Translation
- Summarization
- Question answering

### Cloud vs. Self-Hosted Models

**Cloud-based (ChatGPT, Claude, Gemini):**
- ✅ Easy to use, no infrastructure needed
- ✅ State-of-the-art performance
- ❌ Data leaves your control
- ❌ Ongoing costs per token
- ❌ Potential compliance issues

**Self-hosted (Llama, Mistral, Phi, Qwen):**
- ✅ Full data control and privacy
- ✅ Compliance with regulations
- ✅ Predictable costs
- ✅ Customizable and fine-tunable
- ❌ Requires infrastructure
- ❌ Need to manage scaling

### Why Self-Hosted LLMs?
- **Data Privacy**: Keep sensitive data within your infrastructure
- **Regulatory Compliance**: Meet GDPR, HIPAA, and other requirements
- **Cost Control**: No per-token charges, predictable infrastructure costs
- **Customization**: Fine-tune models for your specific domain
- **Offline Operation**: Work without internet connectivity

## Tasks

### 1. Explore AI Model Capabilities
Try interacting with different AI models to understand their capabilities:
- ChatGPT (OpenAI)
- Claude (Anthropic)
- Public demos of open-source models

### 2. Understand Token Economics
Learn how tokens work and why they matter for cost and context windows.

### 3. Research Self-Hosted Options
Explore available self-hosted models:
- Llama 2 & 3 (Meta)
- Mistral (Mistral AI)
- Phi-3 (Microsoft)
- Qwen (Alibaba)

### 4. Setup Your Environment

**Using GitHub Codespaces (Recommended):**

If you're using GitHub Codespaces, everything is already set up! Verify your environment:

```bash
# Check Java version (25)
java -version

# Check Docker
docker --version

# Check kubectl
kubectl version --client

# Check Ollama
ollama --version

# Check Git
git --version
```

**Using Local Setup:**

If you're running locally, verify you have the required tools:

```bash
# Check Java version (17+)
java -version

# Check Docker
docker --version
docker ps

# Check kubectl
kubectl version --client
kubectl cluster-info

# Check Ollama
ollama --version

# Check Git
git --version
```

## Prerequisites for the Workshop

### GitHub Codespaces (Recommended - Everything Pre-configured!)

Simply open this repository in GitHub Codespaces:

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/SandraAhlgrimm/springai-kubernetes-workshop)

The Codespace includes:
- Java 25 with Maven and Gradle
- Docker-in-Docker support
- Kubernetes tools (kubectl, Helm, Minikube)
- Ollama pre-installed
- All development tools configured

### Alternative: Local Setup

- Java 17 or higher - [Download](https://adoptium.net/)
- Docker Desktop - [Download](https://www.docker.com/products/docker-desktop)
- kubectl - [Install Guide](https://kubernetes.io/docs/tasks/tools/)
- A Kubernetes cluster (local Minikube/Kind or cloud-based)
- Ollama - [Install](https://ollama.ai/)
- Git
- Your favorite IDE (IntelliJ IDEA, VS Code, Eclipse, etc.)
- Basic knowledge of Java and Spring Boot

## Next Steps
Once you understand AI fundamentals, proceed to [Module 1: Getting Started with Large Language Models](../module-1-getting-started-llms/README.md).

## Resources
- [Understanding Large Language Models](https://www.anthropic.com/index/core-views-on-ai-safety)
- [Hugging Face Model Hub](https://huggingface.co/models)
- [Ollama - Run LLMs locally](https://ollama.ai/)
- [Meta Llama](https://ai.meta.com/llama/)
- [Mistral AI](https://mistral.ai/)
- [What are tokens?](https://platform.openai.com/tokenizer)
- [Prompt Engineering Guide](https://www.promptingguide.ai/)
