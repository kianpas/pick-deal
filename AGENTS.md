# AGENTS.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.


## 도메인 중심 패키지 구조

### 기본 원칙

1. **도메인 우선**: 비즈니스 도메인별로 최상위 패키지 분리
2. **역할별 컨트롤러**: 도메인 내에서 사용자/관리자 컨트롤러 분리
3. **서비스 통합**: 비즈니스 로직은 하나의 Service에서 관리
4. **DTO 세분화**: 요청/응답 DTO를 명확히 분리
5. SOLID - 객체 지향 설계 다섯 원칙 (SRP, OCP, LSP, ISP, DIP)
6. KISS -  Keep It Simple, Stupid – 단순하게 설계하자는 원칙
7. YAGNI - You Aren’t Gonna Need It – 필요하기 전엔 기능 추가하지 말라는 원칙

## SSR/CSR 데이터 접근 지침 (Spring Security)

- 기본 원칙: SSR/Route Handler에서는 serverFetch를 사용해 백엔드에 직접 접근하여 Spring Security 세션 쿠키와 CSRF 토큰을 안전하게 전달한다.
- Next 프록시 라우트는 브라우저에서 자주 호출되는 엔드포인트나 인증/보안 로직을 중앙에서 처리해야 할 때 활용하고, X-XSRF-TOKEN 주입, 허용 Origin 관리, 백엔드 호스트 보호를 한 곳에서 담당한다.
- 브라우저에서 백엔드를 직접 호출해야 한다면 Spring Security 설정(허용 Origin, CSRF 예외, 인증 정책)을 반드시 검토하고, 선택 근거를 문서로 남긴다.
