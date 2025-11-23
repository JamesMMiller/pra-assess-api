feat: Add Model Selection to Assessment API

## Summary
Allows API consumers to specify which Gemini model to use for the assessment via a query parameter (e.g., `?model=gemini-1.5-pro`). This provides flexibility to use different models for different needs (speed vs. quality).

## Key Changes

### 1. API Update
- Updated `/assess/stream` endpoint to accept an optional `model` query parameter.
- Defaults to `gemini-2.0-flash` if not specified.

### 2. Service Layer
- Updated `AssessmentController`, `AssessmentOrchestrator`, and `GeminiService` to propagate the `model` parameter.
- Fixed Gemini API payload structure in `GeminiService` to correctly use the top-level `systemInstruction` field (required for some models/API versions).

### 3. Testing
- ✅ Added integration test case to verify `model` parameter acceptance.
- ✅ Verified all 41 tests passing.

## Documentation
- Updated README with usage examples for Model Selection.
