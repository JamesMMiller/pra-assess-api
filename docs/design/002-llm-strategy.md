# Design: LLM Assessment Strategy (Revised)

## Overview
This document outlines the strategy for using Gemini to assess repositories against the PRA checklist.
**Key Principles:**
1.  **Dynamic Discovery**: The LLM decides which files to read based on the file tree, rather than downloading the whole repo.
2.  **Shared Context**: An initialization step gathers high-level project info to inform all subsequent checks.
3.  **Human-in-the-Loop**: Low-confidence assessments are flagged for user review.
4.  **No Local Caching**: We rely on on-demand fetching (with potential upstream caching if needed, but no local persistence).

## 1. Workflow

### Phase 1: Initialization & Shared Context
Before running individual checks, we establish a "Shared Context" to ground the LLM.
1.  **Fetch File Tree**: API fetches the recursive file list (tree) from GitHub.
2.  **Structure Analysis**: LLM analyzes the file tree to determine:
    *   Project Type (Play, Microservice, Frontend, etc.)
    *   Key Directories (`app`, `conf`, `project`)
    *   Frameworks & Libraries used (high level)
3.  **Output**: A `SharedContext` summary string (e.g., "Scala 3 Play Service, using Mongo, standard directory structure").

### Phase 2: Assessment Loop (Per Checklist Item)
For each item in the PRA checklist:
1.  **File Selection**: Present the `SharedContext` + `FileTree` + `Checklist Item` to the LLM. Ask: *"Which files do you need to read to assess this?"*
2.  **Fetch Content**: API fetches the content of the selected files (e.g., `build.sbt`, `app/controllers/AuthController.scala`).
3.  **Assessment**: Present `SharedContext` + `File Content` + `Checklist Item` to the LLM.
4.  **Result**: LLM returns JSON verdict with confidence score.

## 2. Prompt Structure

### A. File Selection Prompt
```text
**Context**: {SharedContext}
**Task**: Identify files needed to check: "{Checklist Item}"
**Available Files**:
{FileTreeList}

**Output**: JSON array of file paths.
```

### B. Assessment Prompt
```text
**System**: You are an expert Platform Readiness Assessor.
**Context**: {SharedContext}
**Task**: Assess "{Checklist Item}"
**Files Provided**:
{FileContents}

**Instructions**:
1. Analyze the code.
2. Determine status: PASS, FAIL, WARNING, or N/A.
3. specificy confidence (0.0-1.0). If < 0.8, mark `requiresReview: true`.
4. Provide evidence (file path + line numbers).

**Output JSON**:
{
  "status": "PASS" | "FAIL" | "WARNING" | "N/A",
  "confidence": 0.9,
  "requiresReview": false,
  "reason": "...",
  "evidence": [{ "filePath": "...", "lines": "10-20" }]
}
```

## 3. Evidence & Linking
*   LLM provides `filePath` and line ranges.
*   API constructs GitHub URLs: `{repo_url}/blob/{commit}/{path}#L{start}-L{end}`.

## 4. Token & Cost Management
*   **Tree Pruning**: If the file tree is huge, we filter out obvious noise (`target/`, `.git/`, `node_modules/`) before sending to LLM.
*   **Gemini Context Caching**: We can cache the `FileTree` and `SharedContext` in Gemini's context cache (if supported/economical) to avoid re-sending it for every single check.

## 5. Implementation Plan
1.  **GitHub Client**:
    *   `getTree(repo)`: Returns list of paths.
    *   `getFile(repo, path)`: Returns content.
2.  **Orchestrator (Akka/Pekko Streams)**:
    *   Step 1: `InitFlow` -> Generates `SharedContext`.
    *   Step 2: `AssessmentFlow` (Parallelism = X):
        *   `CheckItem` -> `SelectFiles (LLM)` -> `FetchFiles` -> `Assess (LLM)` -> `Result`.
3.  **Prompt Manager**: Templates for the 3 distinct LLM calls (Init, Select, Assess).
