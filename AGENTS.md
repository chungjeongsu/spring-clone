# AGENTS.md

이 파일은 이 저장소의 VIBE 코딩 규칙을 다른 환경에서도 동일하게 재사용하기 위한 기준 문서다.

## Source of Truth

- 실행 프롬프트: `vibe/VIBE_PROMPT.md`
- PLAN 템플릿: `vibe/plan/PLAN_TEMPLATE.md`
- DRAFT 템플릿: `vibe/draft/DRAFT_TEMPLATE.md`
- 에이전트 규칙: `AGENTS.md`

## Required Flow

1. JOB 분석 후 TASK로 분해한다.
2. PLAN 문서를 `vibe/plan/*.md`에 작성한다.
3. 사용자가 PLAN 기반 질문/설계 피드백을 주면 반영한다. (1차 학습)
4. TASK별 DRAFT 문서를 `vibe/draft/*.md`에 작성한다.
5. 사용자가 구현물/DRAFT를 리뷰한다. (2차 학습)
6. 사용자가 "병합"을 요청하면 실제 코드에 반영한다.
7. 병합은 테스트 기반으로 진행하고 결과를 보고한다.

## Portability Rule

- 다른 프로젝트로 이 규칙을 옮길 때도 동일한 디렉터리 구조를 유지한다.
- 최소 구조:
  - `AGENTS.md`
  - `vibe/VIBE_PROMPT.md`
  - `vibe/plan/PLAN_TEMPLATE.md`
  - `vibe/draft/DRAFT_TEMPLATE.md`
- 프로젝트 특화 정보(도메인, 아키텍처, 테스트 명령)만 교체하고 프로세스 골격은 유지한다.

## Merge Gate

- PLAN/DRAFT 없이 바로 코드 변경 금지.
- 사용자 승인 없는 병합 금지.
- 테스트 실패 상태 병합 금지.
- DRAFT의 `변경 파일 목록`에 있는 파일이 `변경 전 코드`/`코드 제안`에 모두 없으면 병합 금지.
- 신규/수정 파일이 1개라도 누락되면 DRAFT를 먼저 보완한 뒤 진행.

## Code Style

- 3항 연산자는 가독성이 명확히 좋아지는 경우가 아니면 지양한다.
- 조건 분기는 기본적으로 `if/else`를 우선 사용한다.

## Merge Incident Rule (추가)

- 병합 과정에서 새로운 문제(주입 경로 불일치, 컴파일/테스트 실패, 인코딩 오류, 설계 충돌 등)를 발견하면 즉시 병합 작업을 중지한다.
- 문제 내용, 원인, 수정 방향을 PLAN/DRAFT에 먼저 반영하고 사용자 확인을 받은 뒤에만 수정/재병합한다.
- 이미 병합한 뒤 문제가 발견되면, 해당 TASK에서 병합한 변경을 먼저 롤백한 다음 `PLAN/DRAFT 업데이트 -> 사용자 확인 -> 수정 -> 테스트 -> 재병합` 순서로 진행한다.
- 위 규칙은 예외 없이 적용한다.