feat: Batch Assessment Endpoint

## Summary
Adds a new `/assess/batch` endpoint that groups assessment checks by category to significantly reduce the number of Gemini API calls. This addresses rate limit issues encountered with the streaming endpoint.

## Key Changes

### 1. New Endpoint: `/assess/batch`
- Synchronous endpoint that returns a single JSON array of all results.
- Groups checks by category (e.g., "1.", "2.") and processes each group in a single LLM call.
- Reduces API calls from ~25 (one per check) to ~13 (one per category).

### 2. Service Updates
- **GeminiService**: Added `assessBatch` and `selectFilesForCategory` to handle grouped assessments.
- **AssessmentOrchestrator**: Added `runBatchAssessment` to manage the grouping and batch execution.

### 3. Documentation
- Updated README to recommend `/assess/batch` for CI/CD and rate-limited environments.

## Verification
- **Unit Tests**: Added `GeminiServiceBatchSpec`.
- **Integration Tests**: Added `BatchAssessmentIntegrationSpec`.
