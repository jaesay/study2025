# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot application demonstrating various OpenAI/Spring AI integration patterns. The project showcases text generation, embeddings, RAG (Retrieval Augmented Generation), image processing, speech-to-text/text-to-speech, tool calling, and content moderation capabilities.

## Development Commands

### Build and Run
```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test

# Clean build artifacts
./gradlew clean

# Generate Javadoc
./gradlew javadoc
```

### Application Access
- Local development server: http://localhost:8080
- Main landing page provides links to all demo features

## Architecture Overview

### Package Structure
- `com.jaesay.openaidemo` - Root package
  - `services/` - Core OpenAI service layer (currently stubbed)
  - `text/` - Text generation controllers and prompt templates
  - `embeddings/` - Vector embeddings and similarity search
  - `rag/` - Retrieval Augmented Generation implementations
  - `imageprocessing/` - Image generation and analysis features
  - `speech/` - Speech-to-text and text-to-speech controllers
  - `tools/` - Function calling and tool integration
  - `moderations/` - Content moderation functionality

### Key Components

**Controllers**: Follow Spring MVC pattern with Thymeleaf templates
- All controllers inject `OpenAiService` (currently empty stub)
- Standard pattern: `@GetMapping` for page display, `@PostMapping` for processing
- Return template names that correspond to `src/main/resources/templates/*.html`

**Templates**: Located in `src/main/resources/templates/`
- `index.html` - Main navigation page organizing features by category
- Individual feature templates for each demo capability

**Service Layer**: 
- `OpenAiService` - Central service for OpenAI integrations (currently empty)
- All AI functionality should be implemented here

### Spring AI Integration
- Uses Spring AI Starter with OpenAI: `spring-ai-starter-model-openai`
- Configuration requires `OPENAI_API_KEY` environment variable
- Spring AI version: 1.0.1

## Configuration

### Required Environment Variables
```bash
OPENAI_API_KEY=your_openai_api_key_here
```

### Application Properties
- Located in `src/main/resources/application.properties`
- Application name: `openai-demo`
- OpenAI API key configured via environment variable

## Development Patterns

### Adding New Features
1. Create controller in appropriate package (text/, embeddings/, etc.)
2. Inject `OpenAiService` using `@Autowired`
3. Follow GET/POST pattern for display/processing
4. Create corresponding Thymeleaf template
5. Add navigation link to `index.html` in appropriate category

### Controller Pattern
```java
@Controller
public class ExampleController {
    @Autowired
    private OpenAiService service;
    
    @GetMapping("/showExample")
    public String showExample() {
        return "exampleTemplate";
    }
    
    @PostMapping("/example")
    public String processExample(@RequestParam String input, Model model) {
        // Process with OpenAiService
        return "exampleTemplate";
    }
}
```

## Feature Categories

**Text Generation**: Basic chat completion and Q&A
**Prompt Templates**: Structured prompts for specific use cases  
**Embeddings**: Vector similarity and semantic search
**RAG**: Document retrieval and question answering
**Image Processing**: Generation and analysis of images
**Speech**: Audio to text and text to audio conversion
**Tool Calling**: Function calling and external integrations
**Moderations**: Content safety and filtering

## Technology Stack
- **Framework**: Spring Boot 3.5.5
- **Java**: Version 21 (toolchain configured)
- **AI Integration**: Spring AI 1.0.1 with OpenAI
- **Web**: Spring MVC with Thymeleaf templates
- **Build**: Gradle 8.14+ with wrapper
- **Testing**: JUnit 5 platform