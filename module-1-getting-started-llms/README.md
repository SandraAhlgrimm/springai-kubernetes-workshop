# Module 1: Getting Started with Large Language Models

## Overview
In this module, you’ll get hands-on experience with Large Language Models by setting up and running them locally. You’ll also learn how to interact with and experiment with self-hosted LLMs.

## Learning Objectives
- Install and run self-hosted LLMs locally using Ollama
- Understand different model sizes and their trade-offs
- Learn to interact with LLMs via CLI and API
- Configure model parameters (temperature, context window, etc.)
- Compare performance of different models

## Getting Started with Ollama

### What is Ollama?
Ollama is a tool that makes it easy to run large language models locally. It handles model downloads, provides a simple API, and manages model lifecycle.

### Installing Ollama (Optional)
**Ollama is already installed in the configured GitHub Codespace for this repository!**

For local installation, use the following commands.

**macOS:**
```bash
brew install ollama
```

**Linux:**
```bash
curl -fsSL https://ollama.com/install.sh | sh
```

**Windows:**
Download from [ollama.com](https://ollama.com/download/windows)

### Running Your First Model

Let's now download one of the models available in the Ollama reigistry.
```bash
ollama pull qwen3:1.7b
```

To start an interaction with the downloaded model, you can use the Ollama CLI.
```bash
ollama run qwen3:1.7b "Provide a recipe that includes the ingredient waffles" --hidethinking
```
The `--hidethinking` parameter of ollama run hides the model’s intermediate reasoning or “thinking” steps, showing only the final response in the output.

If you want to run multiple prompts against the model, simply omit the prompt parameter and type your prompts line by line in interactive mode.
```bash
ollama run qwen3:1.7b --hidethinking
```

## Understanding Model Parameters

Models expose parameters that shape how they respond. You can tune these to trade off precision, creativity, determinism, and length. The examples below highlight some of the most commonly used options.

*Temperature* adjusts the probability distribution of tokens, with higher values increasing creativity and less predictable outputs
*Top-k* restricts selection to the k most probable tokens, ensuring coherence 
*Top-p* (nucleus sampling) sets a cumulative probability threshold to filter out less likely tokens, creating a dynamic vocabulary for the model to choose from
*Max tokens* caps the length of the output
*Seed* is a starting number that, when fixed, makes the entire sampling process reproducible, yielding the same output for identical inputs and parameters

You can view the default model parameter values in Ollama using the `ollama show` command.
```bash
ollama show qwen3:1.7b
```
Example output:
```
Parameters
    top_p             0.95              
    repeat_penalty    1                 
    stop              "<|im_start|>"    
    stop              "<|im_end|>"      
    temperature       0.6               
    top_k             20      
```

For example, a temperature value of 0.8 produces more creative and varied responses, while lower values such as 0.1–0.3 make outputs more focused and deterministic.
```bash
ollama run qwen3:1.7b "Provide a recipe that includes the ingredient waffles" --hidethinking --temperature 0.1
```

## Balancing Speed, Quality, and Resources

You can also inspect model characteristics that affect performance using the same `ollama show` command.
```bash
ollama show qwen3:1.7b
```
Example output:
```
Model              qwen3
Parameters         2.0B
Context length     40960
Embedding length   2048
Quantization       Q4_K_M  
```

This information helps you understand how large the model is, how much text it can process at once, and how it’s optimized.

**Key Performance Factors**
*Model Size and Parameters*: Larger models (e.g., 70B) are more capable but slower and memory-intensive. Smaller ones (e.g., 2B–7B) respond faster. Qwen3 was chosen in our example because of its small size (2B version), but it’s also available in larger variants, such as the 235B model.

*Quantization*: Reduces precision (e.g., 4-bit or 5-bit) to lower memory use and improve speed, usually with minimal quality loss.

*Context Length*: Defines how many tokens the model can handle per request. Longer contexts enable richer conversations but slow inference.

*Embedding Length*: Determines the dimensionality of vector representations; longer embeddings can improve accuracy but require more compute.

**Optimization Techniques for Better Performance**
- Choose the right model size and quantization level for your use-case
- Use GPU acceleration where available
- Keep prompts concise to reduce context size
- Tune threading and resource allocation for optimal throughput
- Keep models preloaded to minimize startup delay
- Stream token outputs for faster perceived response times
- Track latency and errors, and scale out (add instances) if needed

## Interacting with the Ollama REST API
After exploring the CLI, you can also interact with Ollama programmatically through its built-in REST API. Using the REST API allows you to integrate LLM capabilities directly into your applications.

The Ollama process that listens for API requests by default on port 11434 can be started by running `ollama serve`.
```bash
ollama serve
```

The primary endpoint for text generation is http://localhost:11434/api/generate. It expects a JSON payload containing at least a model name and a prompt​.
```bash
curl http://localhost:11434/api/generate \
  -d '{
    "model": "qwen3:1.7b",
    "prompt": "Provide a recipe that includes the ingredient waffles",
    "stream": false,
    "think": false
  }'
```

Ollama also provides an experimental **OpenAI-compatible API** at http://localhost:11434/v1/chat/completions and related paths. This compatibility is valuable because the OpenAI API is increasingly becoming the de facto standard for interacting with LLMs. It allows existing tools, SDKs, and client libraries to work seamlessly with Ollama, making it easy to integrate local or self-hosted models without changing your application code.
```bash
curl http://localhost:11434/v1/chat/completions \
  -d '{
    "model": "qwen3:1.7b",
    "messages": [{"role":"user","content":"Provide a recipe that includes the ingredient waffles"}],
    "stream": false
  }'
```

## What's Next?
Continue to [Module 2: Building Intelligent Enterprise Applications](../module-2-building-enterprise-apps/README.md) to build an enterprise application that integrates the GenAI capabilities provided by Ollama.

## Resources
- [Ollama Models](https://ollama.com/search)
- [Ollama Documentation](https://docs.ollama.com)

