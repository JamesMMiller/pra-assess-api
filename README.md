# PRA Assessment API

A prototype API for automating Platform Readiness Assessments (PRA) using Gemini LLM.

## Overview

This API allows users to stream PRA assessments for a given GitHub repository. It uses Server-Sent Events (SSE) to deliver real-time feedback as the LLM assesses the codebase against a checklist.

## Features

- **Streaming API**: Real-time assessment results via SSE.
- **LLM Integration**: Powered by Google's Gemini 2.0 Flash model.
- **Dynamic Discovery**: Intelligently selects relevant files for each check.
- **GitHub Integration**: Fetches file trees and content directly from GitHub.

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

To assess a repository, use the `/assess/stream` endpoint:

```bash
curl -N "http://localhost:9000/assess/stream?repoUrl=https://github.com/hmrc/pillar2-frontend"
```

### Response Format

The API streams JSON objects for each assessment item:

```json
{
  "item": "Check for sensitive data",
  "status": "PASS",
  "confidence": 0.9,
  "requiresReview": false,
  "reason": "No secrets found in code.",
  "evidence": []
}
```

## Architecture

- **Framework**: Play Framework (Scala)
- **Concurrency**: Pekko Streams for reactive streaming.
- **LLM Client**: Custom WSClient implementation for Gemini API (v1beta).

## Testing

Run the integration tests:

```bash
sbt test
```
