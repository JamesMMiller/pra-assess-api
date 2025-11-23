# ADR 001: PRA Assessment API Implementation Design

## Status
Accepted (Option 3 Selected)

## Decision
The team has selected **Option 3: Streaming API (Server-Sent Events)**.
This provides the best balance of user experience (immediate feedback) and technical feasibility with the Play Framework.

## Implementation Details
See [LLM Strategy Design](002-llm-strategy.md) for details on context management, prompting, and evidence handling.

## Context
We need to build a prototype service to automate the Platform Readiness Assessment (PRA) for HMRC digital services.
The service will:
1.  Accept a GitHub repository URL and a PRA template ID.
2.  Fetch the repository content.
3.  Use an LLM (Gemini via `openai-scala-client`) to assess the repository against the PRA checklist.
4.  Return a JSON result with Pass/Fail/Warning/NA status and evidence for each item.

The assessment process involves analyzing multiple files and checking against numerous criteria (approx 15-20 items in the example). LLM processing can be time-consuming (latency in seconds to minutes depending on code size and complexity).

We need to decide on the API interaction model to handle this latency and provide a good user experience.

## Options

### Option 1: Synchronous API

The client sends a request, and the connection remains open until the entire assessment is complete.

**Workflow:**
1.  `POST /assess` with `{ repoUrl, templateId }`
2.  Server fetches code, runs LLM assessment for all items.
3.  Server responds with full JSON report (200 OK).

**Pros:**
*   Simplest to implement (standard Request-Response).
*   Easy to consume for simple clients (scripts, curl).
*   No state management required on the server (stateless).

**Cons:**
*   **High Latency:** Request might time out (gateways/load balancers often have 30-60s timeouts).
*   **Poor UX:** User waits with no feedback until the very end.
*   **Fragile:** If one item fails or connection drops, the whole progress is lost.

### Option 2: Asynchronous API (Polling)

The client submits a job and polls for results.

**Workflow:**
1.  `POST /assess` -> Returns `202 Accepted` with `{ jobId, statusUrl }`.
2.  Server processes in background.
3.  Client polls `GET /assess/:jobId` periodically.
    *   Returns `200 OK` with `{ status: "IN_PROGRESS", completedItems: 5, totalItems: 20 }`.
    *   Returns `200 OK` with `{ status: "COMPLETED", report: { ... } }` when done.

**Pros:**
*   Resilient to timeouts.
*   Scalable (can use queues/workers).
*   Client can disconnect and reconnect.

**Cons:**
*   More complex to implement (needs state persistence/database/cache like Redis).
*   Client needs to implement polling logic.

### Option 3: Streaming API (Server-Sent Events)

The client connects, and the server pushes results as they become available.

**Workflow:**
1.  `GET /assess/stream?repoUrl=...` (or POST)
2.  Server keeps connection open (Content-Type: `text/event-stream`).
3.  Server pushes events:
    *   `event: progress`, `data: { item: "1.A", status: "PASS" }`
    *   `event: progress`, `data: { item: "1.B", status: "FAIL" }`
    *   `event: complete`, `data: { fullReport: ... }`

**Pros:**
*   **Real-time Feedback:** User sees progress immediately.
*   **Better UX:** Feels faster than waiting for everything.
*   Single request (no polling loop).

**Cons:**
*   Slightly more complex than Option 1.
*   Requires client to handle streams (easy in JS, slightly harder in some backend clients).
*   Connection stability (if drops, might need resume logic, though for a prototype restart is acceptable).

## Recommendation

For a **prototype**, we recommend **Option 3 (Streaming/SSE)** or **Option 1 (Synchronous)** depending on the expected processing time.

Given we are using Play Framework, which has excellent support for Akka Streams / Pekko Streams, **Option 3** is a strong candidate for a "wow" factor and good UX without the heavy state management overhead of Option 2 (no DB needed necessarily, just stream the processing flow).

However, if simplicity is paramount and we expect assessments to be quick (< 30s), **Option 1** is the fastest path to MVP.

## Decision
[To be decided by the team]
