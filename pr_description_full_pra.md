feat: Implement Full PRA Template

## Summary
Implements a comprehensive "MDTP Platform Readiness Assessment" template based on the standard PRA checklist. This template includes ~25 checks across 13 categories that are suitable for static analysis by an LLM.

## Key Changes

### 1. Template Registry
- Updated `mdtpPra` template with checks for:
    - Build & Resilience (Dependencies, HTTP Verbs, Timeouts)
    - Data Persistence (Mongo, TTL, Encryption)
    - Security (Auth usage)
    - Admin Services
    - Logging & Auditing
    - Testing & Accessibility
- Added a lightweight `test` template to facilitate integration testing without hitting API rate limits.

### 2. Testing
- Updated `TemplateRegistrySpec` to verify the full template structure.
- Updated `AssessmentIntegrationSpec` to use the `test` template to avoid 429 errors during CI/testing.

### 3. Documentation
- Updated README to list the capabilities of the full `mdtp-pra` template.

## Note on Rate Limits
The full `mdtp-pra` template triggers ~25 parallel API calls. This can easily hit the Gemini API rate limit (RPM/TPM). For integration testing, use the `test` template (`?templateId=test`).
