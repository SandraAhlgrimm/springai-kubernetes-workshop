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
Press `Ctrl + Shift + P` to open command palette.  
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












