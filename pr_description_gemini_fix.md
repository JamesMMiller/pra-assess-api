fix: Enforce JSON schema and robust parsing for Gemini response

## Summary
Fixes a JSON decoding error where the Gemini API occasionally returns a `result` field instead of the expected `status` field, causing 500 errors.

## Key Changes

### 1. Schema Enforcement
- Updated `TemplateRegistry` base prompt to explicitly define the expected JSON structure, including the `status` field.

### 2. Robust Parsing
- Updated `GeminiService` to accept `result` as a fallback for `status` if the LLM still deviates from the schema.
- Improved error handling to provide more context on parsing failures.

### 3. Testing
- Added `GeminiServiceParsingSpec` to verify that the service correctly handles responses with the `result` field.

## Verification
- Verified with new unit test `GeminiServiceParsingSpec`.
