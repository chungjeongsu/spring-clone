# AGENT_TEMPLATE

아래 내용을 각 프로젝트의 `AGENTS.md`로 복사해 사용한다.

## Core Workflow

1. Analyze JOB.
2. Break JOB into TASKs.
3. Save PLAN markdown to `vibe/plan/`.
4. Reflect user Q&A and design feedback.
5. Save TASK DRAFT markdown to `vibe/draft/`.
6. Wait for user review.
7. Merge to codebase only after explicit merge request.
8. Execute tests before/after merge and report results.

## File Policy

- PLAN template: `vibe/plan/PLAN_TEMPLATE.md`
- DRAFT template: `vibe/draft/DRAFT_TEMPLATE.md`
- Prompt baseline: `vibe/VIBE_PROMPT.md`

## Guardrails

- No direct code merge without PLAN + DRAFT.
- No merge without user approval.
- No merge with failing tests.

## Learning Policy

- Every TASK should include at least one CS concept.
- Every DRAFT should include interview-ready learning points.
