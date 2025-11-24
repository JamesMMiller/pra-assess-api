# PRA Assessment API

A prototype API for automating Platform Readiness Assessments (PRA) using Gemini LLM with type-safe assessment templates.

## Overview

This API allows users to stream PRA assessments for a given GitHub repository. It uses Server-Sent Events (SSE) to deliver real-time feedback as the LLM assesses the codebase against a checklist.

## Features

- **Streaming API**: Real-time assessment results via SSE
- **Template System**: Type-safe assessment templates with context resources and customizable base prompts
- **LLM Integration**: Powered by Google's Gemini 2.0 Flash model
- **Dynamic Discovery**: Intelligently selects relevant files for each check (Token Efficient)
- **GitHub Integration**: Fetches file trees and content directly from GitHub
- **Context-Aware**: Templates include reference resources (e.g., MDTP Handbook) for LLM

## Token Efficiency Strategy

This API is designed to be highly token-efficient, avoiding the need for expensive context caching (which requires >32k tokens):

1.  **Dynamic Discovery**: Instead of sending the entire repository content, the API first asks the LLM to select only the files relevant to the specific check.
2.  **Shared Context**: A concise summary of the project structure is generated once and reused across all checks.
3.  **Base Prompts**: Assessor personas and instructions are defined in the system prompt, keeping user prompts focused.

## Prerequisites

- Java 21
- sbt
- A valid Gemini API Key

## Configuration

1. Create a `conf/local.conf` file (this file is gitignored):
   ```hocon
   pra.assessment.gemini.apiKey="YOUR_GEMINI_API_KEY"
   ```
2. Ensure `conf/application.conf` includes `local.conf`:
   ```hocon
   include "local.conf"
   ```

## Running the Application

To run the application locally:

```bash
sbt run
```

The API will be available at `http://localhost:9000`.

## Usage

### Basic Assessment

To assess a repository with the default template (MDTP PRA):

```bash
curl -N "http://localhost:9000/assess/stream?repoUrl=https://github.com/hmrc/pillar2-frontend"
```

### Template Selection

To use a specific assessment template:

```bash
curl -N "http://localhost:9000/assess/stream?repoUrl=https://github.com/hmrc/pillar2-frontend&templateId=mdtp-pra"
```

### Model Selection

To use a specific Gemini model (default is `gemini-2.0-flash`):

```bash
curl -N "http://localhost:9000/assess/stream?repoUrl=https://github.com/hmrc/pillar2-frontend&model=gemini-1.5-pro"
```

### Available Templates

- **`mdtp-pra`** (default): MDTP Platform Readiness Assessment
    - Comprehensive check (~25 items) covering:
        - Build & Resilience (Dependencies, HTTP Verbs, Timeouts)
        - Data Persistence (Mongo, TTL, Encryption)
        - Security (Auth usage)
        - Admin Services
        - Logging & Auditing
        - Testing & Accessibility
- **`test`**: Minimal template for testing purposes.ource
  - Assesses against HMRC architectural standards

### Response Format

The API streams JSON objects for each assessment item:

```json
{
  "checkId": "1.A",
  "checkDescription": "Does your service implement any non-standard patterns?",
  "status": "PASS",
  "confidence": 0.9,
  "requiresReview": false,
  "reason": "No non-standard patterns detected",
  "evidence": [
    {
      "githubUrl": "https://github.com/hmrc/pillar2-frontend/blob/main/app/controllers/HomeController.scala#L15-L20"
    }
  ]
}
```

**Status Values**: `PASS`, `FAIL`, `WARNING`, `N/A`

## Architecture

- **Framework**: Play Framework (Scala 3)
- **Concurrency**: Pekko Streams for reactive streaming
- **LLM Client**: Custom WSClient implementation for Gemini API (v1beta)
- **Templates**: Type-safe template registry with context resources

## Testing

Run all tests:

```bash
sbt test
```

Run specific test suites:

```bash
sbt "testOnly models.*"
sbt "testOnly templates.*"
```

**Test Coverage**: 40 tests including unit and integration tests

## Adding New Templates

To add a new assessment template, update `app/templates/TemplateRegistry.scala`:

```scala
val myTemplate = AssessmentTemplate(
  id = "my-template",
  name = "My Assessment Template",
  description = "Description of what this template assesses",
  basePrompt = "You are an expert assessor for...",
  contextResources = Seq(
    ContextResource(
      name = "Reference Guide",
      url = "https://example.com/guide",
      description = "Official standards and patterns"
    )
  ),
  checks = Seq(
    CheckItem("1.A", "First check description"),
    CheckItem("1.B", "Second check description")
  )
)
```
