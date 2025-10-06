# Building Private, Scalable AI Applications with Self-Hosted LLMs

[![Devoxx Belgium 2025](https://img.shields.io/badge/Devoxx-Belgium%202025-blue)](https://m.devoxx.com/events/dvbe25/talks/25270/building-private-scalable-ai-applications-with-selfhosted-llms)
[![Workshop](https://img.shields.io/badge/Type-Hands--on%20Lab-green)](https://m.devoxx.com/events/dvbe25/talks/25270)
[![Duration](https://img.shields.io/badge/Duration-3%20hours-orange)](https://m.devoxx.com/events/dvbe25/talks/25270)

## Workshop Overview

This hands-on workshop teaches you how to build and deploy secure, self-hosted AI applications using Java and Spring AI with local Large Language Models on Kubernetes. Learn how to create confidential, scalable AI solutions without third-party data exposure.

**Workshop Link:** [Devoxx Belgium 2025 - Building Private, Scalable AI Applications](https://m.devoxx.com/events/dvbe25/talks/25270/building-private-scalable-ai-applications-with-selfhosted-llms)

**When:** Monday, October 6, 2025, 09:30-12:30  
**Where:** Devoxx Belgium, Kinepolis - BOF 2  
**Level:** Beginner  
**Track:** Architecture

## Why This Workshop?

Tools like ChatGPT, Claude, and Copilot are powerful, but relying on them means giving up control of your data to third-party providers. For organizations bound by confidentiality or regulatory requirements, self-hosted models such as **Llama**, **Mistral**, **Phi**, and **Qwen** provide a secure alternative, running locally or on Kubernetes clusters.

## What You'll Learn

In this tutorial, you'll learn how to:
- ✅ Build a sample AI application using Java and Spring AI
- ✅ Integrate the application with a self-hosted LLM
- ✅ Deploy and scale both the application and the LLM on Kubernetes
- ✅ Implement an asynchronous architecture using message queues
- ✅ Ensure production readiness with resource requests, autoscaling, and real-time metrics
- ✅ Dive deeper into AI capabilities by creating an MCP Server and Client

> **Note:** While the example application uses Java, all concepts around LLM deployment and scaling are fully **language-agnostic**, making this workshop valuable for developers of all backgrounds.

## Slides

Slides are available [here.](https://docs.google.com/presentation/d/1w5qGxDqA7-vvHPQmMNt2KznCnPnuo9Wl-ChJLKN88zI/edit?usp=sharing)

## Workshop Modules

### [Module 0: (Generative) AI Fundamentals](./module-0-ai-fundamentals/README.md)
Understand the fundamentals of Generative AI, Large Language Models, and why self-hosted solutions matter for enterprise applications.

### [Module 1: Getting Started with Large Language Models](./module-1-getting-started-llms/README.md)
Get hands-on with self-hosted LLMs using Ollama, and build your first AI-powered Spring Boot application.

### [Module 2: Building Intelligent Enterprise Applications](./module-2-building-enterprise-apps/README.md)
Learn to build production-grade AI applications with RAG, vector databases, streaming responses, and advanced Spring AI features.

### [Module 3: Running AI Applications on Kubernetes](./module-3-running-on-kubernetes/README.md)
Deploy and scale AI applications on Kubernetes with asynchronous processing, autoscaling, and production monitoring.

### [Module 4: Adapting Large Language Models](./module-4-adapting-llms/README.md)
Master advanced techniques for customizing LLMs including prompt engineering, fine-tuning, quantization, and model optimization.

### [Module 5: AI Agents](./module-5-ai-agents/README.md)
Build autonomous AI agents that can plan, use tools, and accomplish complex multi-step tasks using the Model Context Protocol.

## Prerequisites

**Recommended Setup: GitHub Codespaces** (Everything pre-configured! ✨)

This workshop is designed to work seamlessly with GitHub Codespaces. Simply click the button below to get started with a fully configured development environment:

[![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/SandraAhlgrimm/springai-kubernetes-workshop)

The Codespace includes:
- ✅ Java 25 with Maven and Gradle 9
- ✅ Docker-in-Docker support
- ✅ Kubernetes (kubectl, Helm, Minikube)
- ✅ Ollama pre-installed for running LLMs
- ✅ All necessary development tools

### Alternative: Local Setup

If you prefer to run locally, ensure you have:

- **Java 17 or higher** - [Download](https://adoptium.net/)
- **Docker Desktop** - [Download](https://www.docker.com/products/docker-desktop)
- **kubectl** - [Install Guide](https://kubernetes.io/docs/tasks/tools/)
- **A Kubernetes cluster** (local Minikube/Kind or cloud-based)
- **Ollama** - [Install Guide](https://ollama.ai/)
- **Git** - [Download](https://git-scm.com/)
- **Your favorite IDE** (IntelliJ IDEA, VS Code, Eclipse, etc.)
- **Basic knowledge of Java and Spring Boot**

## Getting Started

### Option 1: GitHub Codespaces (Recommended)

1. **Launch Codespace:**
   
   [![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/SandraAhlgrimm/springai-kubernetes-workshop)

2. **Wait for setup to complete** (about 2-3 minutes)

3. **Verify your environment:**
   ```bash
   java -version
   docker --version
   kubectl version --client
   ollama --version
   ```

4. **Start learning:**
   Begin with [Module 0: (Generative) AI Fundamentals](./module-0-ai-fundamentals/README.md)

### Option 2: Local Setup

1. **Clone this repository:**
   ```bash
   git clone https://github.com/SandraAhlgrimm/springai-kubernetes-workshop.git
   cd springai-kubernetes-workshop
   ```

2. **Verify your setup:**
   Follow the instructions in [Module 0: (Generative) AI Fundamentals](./module-0-ai-fundamentals/README.md)

3. **Start learning:**
   Begin with Module 0 and progress through each module sequentially.

## Speakers

### Timo Salm
**Principal Solutions Engineer at Broadcom**

Timo Salm is a Principal Solutions Engineer at VMware Tanzu by Broadcom with over a decade of experience in customer-facing roles, modern applications, DevSecOps, and AI. He ensures that the most strategic customers in the EMEA region achieve their goals with VMware Tanzu's developer platform, data, AI, and commercial Spring products.

- 🐦 Twitter: [@salmto](https://x.com/salmto)
- 💼 LinkedIn: [timosalm](https://linkedin.com/in/timosalm)
- 🦋 Bluesky: [timosalm.bsky.social](https://bsky.app/profile/timosalm.bsky.social)

### Sandra Ahlgrimm
**Senior Cloud Advocate at Microsoft**

Sandra Ahlgrimm is a Senior Cloud Advocate at Microsoft, specializing in supporting Java Developers. With over a decade of experience as a Java developer, she brings a wealth of knowledge to her role. Sandra is passionate about containers and has recently learned to love AI. She actively contributes to the Berlin Java User Group (JUG) and the Berlin Docker MeetUp.

- 🐦 Twitter: [@skriemhild](https://x.com/skriemhild)
- 💼 LinkedIn: [sandraahlgrimm](https://linkedin.com/in/sandraahlgrimm)
- 🦋 Bluesky: [sandraahlgrimm](https://bsky.app/profile/sandraahlgrimm)

## Key Technologies

- **Spring Boot** - Application framework
- **Spring AI** - AI integration for Spring
- **Java 17+** - Programming language
- **Kubernetes** - Container orchestration
- **Docker** - Containerization
- **Self-hosted LLMs** - Llama, Mistral, Phi, Qwen
- **Message Queues** - RabbitMQ or Kafka
- **Prometheus & Grafana** - Monitoring

## Workshop Benefits

✨ **Data Privacy**: Keep your data within your infrastructure  
🔒 **Compliance**: Meet confidentiality and regulatory requirements  
💰 **Cost Control**: Avoid per-token pricing of cloud AI services  
🎯 **Customization**: Fine-tune models for your specific domain  
📈 **Scalability**: Learn to scale AI workloads on Kubernetes  
🏗️ **Architecture**: Understand Domain-Driven Design with small language models  

## Additional Resources

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Ollama - Run LLMs locally](https://ollama.ai/)
- [Model Context Protocol](https://spec.modelcontextprotocol.io/)

## License

This workshop material is licensed under the MIT License. See [LICENSE](LICENSE) file for details.

## Feedback and Contributions

We welcome feedback and contributions! Please open an issue or submit a pull request if you find any problems or have suggestions for improvements.

---

**Ready to build secure, scalable AI solutions that keep your data under control?** 🚀

Start with [Module 0: (Generative) AI Fundamentals](./module-0-ai-fundamentals/README.md) →
